package tools.cipm.util.build.p2tm;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.cipm.util.build.p2tm.mvnosgimap.Coordinate;
import tools.cipm.util.build.p2tm.mvnosgimap.MvnRepositoryIndex;
import tools.cipm.util.build.p2tm.updatesiteparse.BundleDependency;
import tools.cipm.util.build.p2tm.updatesiteparse.UpdateSiteBundle;

/**
 * Derives the Maven dependencies of an {@link UpdateSiteBundle} from its
 * {@code Require-Bundle} requirements, resolving each required bundle to a
 * concrete {@link Coordinate} in the {@link MvnRepositoryIndex}.
 */
public final class TransitiveDependencyResolver {
	private static final Logger logger = LoggerFactory.getLogger(TransitiveDependencyResolver.class);

	private MvnRepositoryIndex mvnIndex;

	public TransitiveDependencyResolver(MvnRepositoryIndex mvnIndex) {
		this.mvnIndex = mvnIndex;
	}

	/**
	 * Resolves and returns the de-duplicated transitive Maven dependencies of the
	 * given bundle.
	 * 
	 * <p>
	 * Note: Currently de-duplicates the required bundles by name. FIXME May
	 * potentially require fixes, since version conflicts are ignored for now
	 *
	 * @param bundle the bundle whose requirements to resolve
	 * @return a de-duplicated list of coordinates (never null)
	 */
	public List<Coordinate> resolve(UpdateSiteBundle bundle) {
		Set<String> seen = new LinkedHashSet<>(); // dedup by bundle name
		List<Coordinate> result = new ArrayList<>();

		for (BundleDependency required : bundle.requiredBundles) {
			String bundleName = required.bundleName;

			// Skip duplicate required bundles (e.g. org.eclipse.ocl.ecore
			// appearing twice in a manifest).
			if (!seen.add(bundleName)) {
				continue;
			}

			// Prefer a concrete coordinate from the Maven repository.
			var coords = mvnIndex.findByBundleName(bundleName);
			Coordinate coord = null;
			if (!coords.isEmpty()) {
				// Ignore the dependency, if the Maven repository does not have a JAR file for
				// it. Under the current settings, all necessary JAR files should be present in
				// the local Maven repository. Therefore, if a specific JAR file is not present,
				// assume that its dependency should not be considered (e.g. the dependency is
				// to a native Java library)
				//
				// Attempt to find the coordinate with the exact version first. If there is no
				// such coordinate, just return the first coordinate found.
				//
				// FIXME May require adjustments as multiple versions for the required
				// dependencies are introduced and conflicts between the individual versions
				// arise.
				//
				coord = coords.stream().filter(c -> required.getVersionRange().inRange(c.version)).findFirst()
						.orElse(coords.stream().findFirst().get());
				result.add(coord);
			} else {
				logger.warn("There is no JAR file under the local Maven repository for: " + bundleName);
			}
		}
		return result;
	}
}