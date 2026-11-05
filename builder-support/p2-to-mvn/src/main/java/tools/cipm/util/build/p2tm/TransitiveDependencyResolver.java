package tools.cipm.util.build.p2tm;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.cipm.util.build.p2tm.mvn.MvnEntry;
import tools.cipm.util.build.p2tm.mvn.MvnRepositoryIndex;
import tools.cipm.util.build.p2tm.p2.data.BundleDependency;
import tools.cipm.util.build.p2tm.p2.data.UpdateSiteBundle;

/**
 * Derives the Maven dependencies of an {@link UpdateSiteBundle}, resolving each required bundle to a
 * concrete {@link MvnEntry} in the {@link MvnRepositoryIndex}.
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
	 * @return a de-duplicated list of entries (never null)
	 */
	public List<MvnEntry> resolve(UpdateSiteBundle bundle) {
		Set<String> seen = new LinkedHashSet<>(); // dedup by bundle name
		List<MvnEntry> result = new ArrayList<>();

		for (BundleDependency required : bundle.getRequiredBundles()) {
			String bundleName = required.getBundleName();

			// Skip duplicate required bundles (e.g. org.eclipse.ocl.ecore
			// appearing twice in a manifest).
			if (!seen.add(bundleName)) {
				continue;
			}

			// Prefer a concrete entry from the Maven repository.
			var entry = mvnIndex.findByBundleName(bundleName);

			if (entry != null && required.getVersionRange().inRange(entry.getVersion())) {
				result.add(entry);
			} else {
				logger.warn("There is no suitable JAR file under the local Maven repository for: " + bundleName);
			}
		}
		return result;
	}
}
