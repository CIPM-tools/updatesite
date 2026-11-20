/**
 * Contains the means to parse entire Eclipse update sites, in the form of
 * {@link UpdateSiteBuilder}. {@link UpdateSite} instances represent entire
 * Eclipse update sites. {@link UpdateSiteBundle} instances represent individual
 * OSGI bundles. {@link BundleDependency} instances represent dependencies
 * between OSGI bundles (Require-Bundle). {@link UpdateSitePackage} instances
 * represent the package imports / exports (Import-Package / Export-Package).
 * <p>
 * For completeness, the contents of the manifest files of the bundles as well
 * as most attributes and directives are parsed and stored.
 */
package tools.cipm.util.build.p2tm.updatesiteparse;