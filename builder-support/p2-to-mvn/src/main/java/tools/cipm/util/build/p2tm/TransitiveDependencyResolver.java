package tools.cipm.util.build.p2tm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.cipm.util.build.p2tm.mvn.MvnEntry;
import tools.cipm.util.build.p2tm.mvn.MvnRepositoryIndex;
import tools.cipm.util.build.p2tm.p2.data.BundleDependency;
import tools.cipm.util.build.p2tm.p2.data.UpdateSite;
import tools.cipm.util.build.p2tm.p2.data.UpdateSiteBundle;

/**
 * Derives the Maven dependencies of an {@link UpdateSiteBundle}, resolving each required bundle to a
 * concrete {@link MvnEntry} in the {@link MvnRepositoryIndex}.
 */
public final class TransitiveDependencyResolver {
	public static record ResolutionResult(List<MvnEntry> resolved, List<MvnEntry> missing) {}

	private MvnRepositoryIndex mvnIndex;
	private UpdateSite updateSite;

	public TransitiveDependencyResolver(MvnRepositoryIndex mvnIndex, UpdateSite updateSite) {
		this.mvnIndex = mvnIndex;
		this.updateSite = updateSite;
	}

	/**
	 * Resolves and returns the de-duplicated transitive Maven dependencies for all bundles.
	 * 
	 * <p>
	 * Note: Currently de-duplicates the required bundles by name. FIXME May
	 * potentially require fixes, since version conflicts are ignored for now
	 *
	 * @param bundle the bundle whose requirements to resolve
	 * @return a de-duplicated list of entries (never null)
	 */
	public Map<UpdateSiteBundle, ResolutionResult> resolve() {
		Map<UpdateSiteBundle, ResolutionResult> result = new HashMap<>();

		updateSite
			.getBundlesByName()
			.values()
			.stream()
			.forEach(bundle -> result.put(bundle, resolveBundle(bundle)));

		return result;
	}

	private ResolutionResult resolveBundle(UpdateSiteBundle bundle) {
		Set<String> seen = new LinkedHashSet<>(); // dedup by bundle name
		List<MvnEntry> resolved = new ArrayList<>();
		List<MvnEntry> missing = new ArrayList<>();

		for (BundleDependency required : bundle.getRequiredBundles()) {
			String bundleName = required.getBundleName();

			// Skip duplicate required bundles (e.g. org.eclipse.ocl.ecore
			// appearing twice in a manifest).
			if (!seen.add(bundleName) || required.isOptional()) {
				continue;
			}

			// Prefer a concrete entry from the Maven repository.
			var entry = mvnIndex.findByBundleName(bundleName);

			if (entry == null) {
				// Check if the required bundle is included in the update site.
				var requiredBundle = updateSite.getBundlesByName().get(bundleName);
				if (requiredBundle != null) {
					entry = convertToMvnEntry(requiredBundle);
				}
			}

			if (entry != null) {
				resolved.add(entry);
			} else {
				missing.add(new MvnEntry("", required.getBundleName(), required.getVersionRange().toString(), required.getBundleName(), new HashSet<>()));
			}
		}

		for (var pkg : bundle.getImportedPackages()) {
			var exportingBundle = updateSite
				.getPackagesToBundles()
				.entrySet()
				.stream()
				.filter(entry -> entry.getKey().getPackageName().equals(pkg.getPackageName()))
				.flatMap(entry -> entry.getValue().stream())
				.findAny();

			if (exportingBundle.isPresent()) {
				resolved.add(convertToMvnEntry(exportingBundle.get()));
			}
			else {
				var mvnEntry = mvnIndex
					.getMvnEntries()
					.stream()
					.filter(entry -> entry.hasPackage(pkg.getPackageName()))
					.findAny();
				if (mvnEntry.isPresent()) {
					resolved.add(mvnEntry.get());
				} else {
					missing.add(new MvnEntry("", pkg.getPackageName(), "", "unknown", List.of(pkg.getPackageName())));
				}
			}
		}

		missing = filterOutIgnoredDependencies(missing);
		var mappingResult = mapMissingDependencies(missing);
		resolved.addAll(mappingResult.resolved());

		mapGroupIds(
			resolved
				.stream()
				.filter(entry -> entry.getGroupId().isBlank())
				.toList()
			);

		return new ResolutionResult(resolved, mappingResult.missing());
	}

	private MvnEntry convertToMvnEntry(UpdateSiteBundle bundle) {
		return new MvnEntry(
			"",
			bundle.getSymbolicName(),
			bundle.getVersion(),
			bundle.getSymbolicName(),
			bundle.getExportedPackages().stream().map(pkg -> pkg.getPackageName()).toList()
		);
	}

	private List<MvnEntry> filterOutIgnoredDependencies(List<MvnEntry> missing) {
		Set<String> ignoredBundleNames = Set.of(
			"org.eclipse.ui.console",
			"org.eclipse.ui",
			"org.eclipse.emf.edit.ui",
			"org.eclipse.core.runtime",
			"org.eclipse.ui.ide",
			"org.eclipse.debug.ui",
			"de.uka.ipd.sdq.workflow.launchconfig",
			"org.eclipse.xtext.ui",
			"org.osgi.service.log",
			"org.eclipse.uml2.common.edit",
			"org.eclipse.core.resources",
			"org.eclipse.ui.editors",
			"org.eclipse.xtext.ui.shared",
			"org.eclipse.xtext.xbase.ide",
			"java.lang.invoke",
			"org.eclipse.xtext.ui.codetemplates.ui",
			"org.eclipse.gmf.runtime.diagram.ui",
			"org.eclipse.emf.ecore.edit",
			"org.eclipse.core.expressions",
			"org.eclipse.jdt.debug.ui",
			"java.util.function",
			"java.lang.annotation",
			"org.eclipse.xtext.ide",
			"org.eclipse.emf.compare.edit",
			"org.eclipse.help.webapp",
			"org.osgi.service.component.annotations",
			"edu.kit.ipd.sdq.commons.util.eclipse",
			"org.eclipse.jface.text",
			"org.eclipse.emf.edit",
			"edu.kit.ipd.sdq.metamodels.insurance",
			"org.eclipse.xtext.xbase.ui",
			"org.eclipse.xtext.xbase.testing",
			"java.lang",
			"java.util",
			"java.nio.file",
			"java.util.stream",
			"org.palladiosimulator.pcm.edit",
			"java.io",
			"org.eclipse.gmf.runtime.diagram.ui.resources.editor",
			"org.eclipse.ui.intro",
			"org.eclipse.ui.workbench",
			"edu.kit.ipd.sdq.metamodels.families",
			"org.eclipse.osgi.service.debug",
			"org.eclipse.emf.mwe.core",
			"org.osgi.util.tracker",
			"org.eclipse.jface",
			"org.eclipse.emf",
			"org.eclipse.ui.intro.universal",
			"org.eclipse.osgi",
			"org.eclipse.xtext.common.types.ui",
			"org.osgi.framework",
			"org.eclipse.sirius.ui",
			"org.eclipse.swt",
			"org.eclipse.equinox.common",
			"org.eclipse.core.commands",
			"org.eclipse.compare",
			"org.eclipse.emf.common.ui",
			"org.eclipse.ui.forms",
			"org.eclipse.help.ui",
			"org.eclipse.pde.ui",
			"edu.kit.ipd.sdq.metamodels.persons",
			"org.eclipse.gmf.runtime.emf.core",
			"org.eclipse.help",
			"org.eclipse.emf.transaction",
			"org.eclipse.equinox.registry",
			"ch.qos.logback.classic",
			"ch.qos.logback.core",
			"org.eclipse.xtext.builder",
			"org.eclipse.uml2",
			"org.eclipse.xtend.standalone",
			"org.palladiosimulator.edp2",
			"org.palladiosimulator.edp2.util",
			"org.palladiosimulator.metricspec",
			"de.uka.ipd.sdq.workflow",
			"de.uka.ipd.sdq.workflow.mdsd"
		);

		return missing
			.stream()
			.filter(entry -> !ignoredBundleNames.contains(entry.getBundleName()))
			.filter(entry -> !ignoredBundleNames.contains(entry.getArtifactId()))
			.toList();
	}

	private ResolutionResult mapMissingDependencies(List<MvnEntry> missing) {
		Map<String, MvnEntry> mappings = new HashMap<>();
		mappings.put("org.apache.log4j", new MvnEntry("org.apache.logging.log4j", "log4j-1.2-api", "2.20.0", "org.apache.log4j", List.of()));
		mappings.put("org.eclipse.jdt.core", new MvnEntry("org.eclipse.jdt", "org.eclipse.jdt.core", "3.32.0", "org.eclipse.jdt.core", List.of()));
		mappings.put("org.eclipse.emf.ecore", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.ecore", "2.26.0", "org.eclipse.emf.ecore", List.of()));
		mappings.put("org.eclipse.emf.ecore.util", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.ecore", "2.26.0", "org.eclipse.emf.ecore", List.of()));
		mappings.put("org.eclipse.emf.ecore.plugin", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.ecore", "2.26.0", "org.eclipse.emf.ecore", List.of()));
		mappings.put("org.eclipse.emf.ecore.resource", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.ecore", "2.26.0", "org.eclipse.emf.ecore", List.of()));
		mappings.put("org.eclipse.emf.ecore.impl", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.ecore", "2.26.0", "org.eclipse.emf.ecore", List.of()));
		mappings.put("org.eclipse.emf.ecore.change", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.ecore.change", "2.14.0", "org.eclipse.emf.ecore.change", List.of()));
		mappings.put("org.eclipse.emf.ecore.xmi", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.ecore.xmi", "2.17.0", "org.eclipse.emf.ecore.xmi", List.of()));
		mappings.put("org.eclipse.emf.common", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.common", "2.26.0", "org.eclipse.emf.common", List.of()));
		mappings.put("org.eclipse.emf.common.util", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.common", "2.26.0", "org.eclipse.emf.common", List.of()));
		mappings.put("org.eclipse.emf.common.notify", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.common", "2.26.0", "org.eclipse.emf.common", List.of()));
		mappings.put("org.eclipse.emf.common.notify.impl", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.common", "2.26.0", "org.eclipse.emf.common", List.of()));
		mappings.put("org.junit.jupiter.api", new MvnEntry("org.junit.jupiter", "junit-jupiter-api", "5.9.2", "org.junit.jupiter.api", List.of()));
		mappings.put("org.junit.jupiter.params", new MvnEntry("org.junit.jupiter", "junit-jupiter-params", "5.9.2", "org.junit.jupiter.api", List.of()));
		mappings.put("org.hamcrest.core", new MvnEntry("org.hamcrest", "hamcrest-core", "2.2", "org.hamcrest.core", List.of()));
		mappings.put("com.google.common.collect", new MvnEntry("com.google.guava", "guava", "33.8.0-jre", "com.google.guava", List.of()));
		mappings.put("com.google.common.cache", new MvnEntry("com.google.guava", "guava", "33.8.0-jre", "com.google.guava", List.of()));
		mappings.put("com.google.common.eventbus", new MvnEntry("com.google.guava", "guava", "33.8.0-jre", "com.google.guava", List.of()));
		mappings.put("com.google.common.base", new MvnEntry("com.google.guava", "guava", "33.8.0-jre", "com.google.guava", List.of()));
		mappings.put("com.google.guava", new MvnEntry("com.google.guava", "guava", "33.8.0-jre", "com.google.guava", List.of()));
		mappings.put("com.google.inject", new MvnEntry("com.google.inject", "guice", "7.0.0", "com.google.guice", List.of()));
		mappings.put("com.google.inject.binder", new MvnEntry("com.google.inject", "guice", "7.0.0", "com.google.guice", List.of()));
		mappings.put("com.google.inject.name", new MvnEntry("com.google.inject", "guice", "7.0.0", "com.google.guice", List.of()));
		mappings.put("org.emftext.language.java.resource", new MvnEntry("tools.mdsd", "jamopp.resource", "5.1.0-cipm", "jamopp.resource", List.of()));
		mappings.put("org.antlr.runtime", new MvnEntry("org.antlr", "antlr-runtime", "3.2", "org.antlr.runtime", List.of()));
		mappings.put("org.eclipse.xtend.lib", new MvnEntry("org.eclipse.xtend", "org.eclipse.xtend.lib", "2.28.0", "org.eclipse.xtend.lib", List.of()));
		mappings.put("org.eclipse.xtext.xbase.lib", new MvnEntry("org.eclipse.xtext", "org.eclipse.xtext.xbase.lib", "2.28.0", "org.eclipse.xtext.xbase.lib", List.of()));
		mappings.put("org.eclipse.xtext.xbase", new MvnEntry("org.eclipse.xtext", "org.eclipse.xtext.xbase", "2.28.0", "org.eclipse.xtext.xbase", List.of()));
		mappings.put("org.eclipse.xtext.util", new MvnEntry("org.eclipse.xtext", "org.eclipse.xtext.util", "2.28.0", "org.eclipse.xtext.util", List.of()));
		mappings.put("org.eclipse.xtext.common.types", new MvnEntry("org.eclipse.xtext", "org.eclipse.xtext.common.types", "2.28.0", "org.eclipse.xtext.common.types", List.of()));
		mappings.put("org.eclipse.xtext.xtext.generator", new MvnEntry("org.eclipse.xtext", "org.eclipse.xtext.xtext.generator", "2.28.0", "org.eclipse.xtext.xtext.generator", List.of()));
		mappings.put("org.apache.commons.math", new MvnEntry("org.apache.commons", "commons-math", "2.1", "org.apache.commons.math", List.of()));
		mappings.put("org.eclipse.emf.ecore.xmi.impl", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.ecore.xmi", "2.17.0", "org.eclipse.emf.ecore.xmi", List.of()));
		mappings.put("org.eclipse.emf.codegen.ecore", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.codegen.ecore", "2.29.0", "org.eclipse.emf.codegen.ecore", List.of()));
		mappings.put("org.eclipse.emf.mapping.ecore2xml", new MvnEntry("org.eclipse.emf", "org.eclipse.emf.mapping.ecore2xml", "2.12.0", "org.eclipse.emf.mapping.ecore2xml", List.of()));
		mappings.put("org.apache.commons.io", new MvnEntry("commons-io", "commons-io", "2.11.0", "org.apache.commons.io", List.of()));
		mappings.put("org.eclipse.xtend.lib.macro", new MvnEntry("org.eclipse.xtend", "org.eclipse.xtend.lib.macro", "2.28.0", "org.eclipse.xtend.lib.macro", List.of()));
		mappings.put("org.eclipse.xtext.ecore", new MvnEntry("org.eclipse.xtext", "org.eclipse.xtext.ecore", "2.28.0", "org.eclipse.xtext.ecore", List.of()));
		mappings.put("org.junit", new MvnEntry("junit", "junit", "4.13.2", "org.junit", List.of()));
		mappings.put("org.junit.platform.commons", new MvnEntry("org.junit.platform", "junit-platform-commons", "1.9.2", "org.junit.platform.commons", List.of()));
		mappings.put("org.slf4j.api", new MvnEntry("org.slf4j", "slf4j-api", "2.0.7", "org.slf4j.api", List.of()));
		mappings.put("lpg.runtime.java", new MvnEntry("lpg.runtime", "lpg.runtime.java", "2.0.17.v201004271640", "lpg.runtime.java", List.of()));

		MvnEntry xtextRuntime = new MvnEntry("org.eclipse.xtext", "org.eclipse.xtext", "2.28.0", "org.eclipse.xtext", List.of());
		List.of(
			"org.eclipse.xtext",
			"org.eclipse.xtext.conversion",
			"org.eclipse.xtext.conversion.impl",
			"org.eclipse.xtext.generator",
			"org.eclipse.xtext.linking",
			"org.eclipse.xtext.linking.impl",
			"org.eclipse.xtext.naming",
			"org.eclipse.xtext.nodemodel",
			"org.eclipse.xtext.nodemodel.util",
			"org.eclipse.xtext.parser",
			"org.eclipse.xtext.parser.antlr",
			"org.eclipse.xtext.preferences",
			"org.eclipse.xtext.resource",
			"org.eclipse.xtext.resource.containers",
			"org.eclipse.xtext.resource.impl",
			"org.eclipse.xtext.scoping",
			"org.eclipse.xtext.scoping.impl",
			"org.eclipse.xtext.serializer",
			"org.eclipse.xtext.serializer.acceptor",
			"org.eclipse.xtext.serializer.analysis",
			"org.eclipse.xtext.serializer.diagnostic",
			"org.eclipse.xtext.serializer.impl",
			"org.eclipse.xtext.serializer.sequencer",
			"org.eclipse.xtext.service",
			"org.eclipse.xtext.validation"
		).forEach(pkg -> mappings.put(pkg, xtextRuntime));

		List<MvnEntry> resolved = new ArrayList<>();
		List<MvnEntry> stillMissing = new ArrayList<>();
		for (MvnEntry entry : missing) {
			// Required bundles are looked up by bundle name, imported packages
			// (bundle name "unknown") by their package name stored as artifact id.
			MvnEntry mapped = mappings.getOrDefault(entry.getBundleName(), mappings.get(entry.getArtifactId()));
			if (mapped == null) {
				stillMissing.add(entry);
			} else if (!resolved.contains(mapped)) {
				resolved.add(mapped);
			}
		}
		return new ResolutionResult(resolved, stillMissing);
	}

	private void mapGroupIds(List<MvnEntry> withoutGroupIds) {
		withoutGroupIds.forEach(entry -> {
			if (GroupIdMappings.ARTIFACT_ID_TO_GROUP_ID.containsKey(entry.getArtifactId())) {
				entry.setGroupId(GroupIdMappings.ARTIFACT_ID_TO_GROUP_ID.get(entry.getArtifactId()));
			}
		});
	}
}
