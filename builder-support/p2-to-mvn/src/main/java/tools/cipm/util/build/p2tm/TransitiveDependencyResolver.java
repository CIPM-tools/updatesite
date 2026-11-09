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

			if (entry != null && required.getVersionRange().inRange(entry.getVersion())) {
				resolved.add(entry);
			} else if (entry != null) {
				missing.add(entry);
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

		return new ResolutionResult(resolved, missing);
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
}
