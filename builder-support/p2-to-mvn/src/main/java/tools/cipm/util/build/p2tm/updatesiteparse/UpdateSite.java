package tools.cipm.util.build.p2tm.updatesiteparse;

import java.util.List;
import java.util.Map;

/**
 * Complete in-memory representation of an Eclipse p2 update site, built by
 * combining:
 * <ul>
 * <li>{@code org.openntf.maven:p2-layout-resolver} — to enumerate bundles and
 * their download locations, and</li>
 * <li>{@code org.eclipse.osgi.util.ManifestElement} — to accurately parse each
 * UpdateSiteBundle's OSGi manifest (Require-UpdateSiteBundle / Import-Package /
 * Export-Package), and optionally {@code org.eclipse.equinox.p2} metadata for
 * the resolved graph.</li>
 * </ul>
 */
public final class UpdateSite {

	/** The repository root (e.g. the p2 update-site URL). */
	public final String repositoryUri;

	/**
	 * Every OSGi UpdateSiteBundle in the update site, keyed by its symbolic name.
	 */
	public final Map<String, UpdateSiteBundle> bundlesByName;

	/** Every exported package -> the UpdateSiteBundle(s) that export it. */
	public final Map<UpdateSitePackage, List<UpdateSiteBundle>> packagesToBundles;

	public UpdateSite(String repositoryUri, Map<String, UpdateSiteBundle> bundlesByName,
			Map<UpdateSitePackage, List<UpdateSiteBundle>> packagesToBundles) {
		this.repositoryUri = repositoryUri;
		this.bundlesByName = Map.copyOf(bundlesByName);
		this.packagesToBundles = packagesToBundles;
	}
}