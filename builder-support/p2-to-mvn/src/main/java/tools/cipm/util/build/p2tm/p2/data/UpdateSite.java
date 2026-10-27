package tools.cipm.util.build.p2tm.p2.data;

import java.util.List;
import java.util.Map;

/**
 * Complete in-memory representation of an Eclipse p2 update site, built by
 * combining:
 * <ul>
 * <li>{@code org.openntf.maven:p2-layout-resolver} — to enumerate bundles and
 * their download locations, and</li>
 * <li>{@code org.eclipse.osgi.util.ManifestElement} — to accurately parse each
 * UpdateSiteBundle's OSGi manifest (Require-Bundle / Import-Package /
 * Export-Package), and optionally {@code org.eclipse.equinox.p2} metadata for
 * the resolved graph.</li>
 * </ul>
 */
public final class UpdateSite {
	/** The repository root (e.g. the p2 update-site URL). */
	private final String repositoryUri;
	/**
	 * Every OSGi UpdateSiteBundle in the update site, keyed by its symbolic name.
	 */
	private final Map<String, UpdateSiteBundle> bundlesByName;
	/**
	 * Every exported package -> the UpdateSiteBundle(s) that export it.
	 */
	private final Map<BundlePackage, List<UpdateSiteBundle>> packagesToBundles;

	public UpdateSite(String repositoryUri, Map<String, UpdateSiteBundle> bundlesByName,
			Map<BundlePackage, List<UpdateSiteBundle>> packagesToBundles) {
		this.repositoryUri = repositoryUri;
		this.bundlesByName = Map.copyOf(bundlesByName);
		this.packagesToBundles = packagesToBundles;
	}

	public String getRepositoryUri() {
		return repositoryUri;
	}

	public Map<String, UpdateSiteBundle> getBundlesByName() {
		return bundlesByName;
	}

	public Map<BundlePackage, List<UpdateSiteBundle>> getPackagesToBundles() {
		return packagesToBundles;
	}
}