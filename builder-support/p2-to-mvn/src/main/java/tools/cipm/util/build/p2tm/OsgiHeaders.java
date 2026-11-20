package tools.cipm.util.build.p2tm;

/**
 * Central constants for OSGi manifest header names and their attribute /
 * directive names, so parsing code references these by name rather than
 * repeating string literals.
 */
public final class OsgiHeaders {
	private OsgiHeaders() {
	}

	// Manifest header names
	public static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
	public static final String BUNDLE_LOCALIZATION = "Bundle-Localization";
	public static final String REQUIRE_BUNDLE = "Require-Bundle";
	public static final String IMPORT_PACKAGE = "Import-Package";
	public static final String EXPORT_PACKAGE = "Export-Package";
	public static final String PROVIDE_CAPABILITY = "Provide-Capability";
	public static final String REQUIRE_CAPABILITY = "Require-Capability";
	public static final String DYNAMIC_IMPORT_PACKAGE = "DynamicImport-Package";
	public static final String FRAGMENT_HOST = "Fragment-Host";

	// Attributes
	public static final String VERSION = "version";
	public static final String BUNDLE_VERSION = "bundle-version";
	public static final String BUNDLE_SYMBOLIC_NAME_ATTR = "bundle-symbolic-name";
	public static final String SPECIFICATION_VERSION = "specification-version";
	public static final String INCLUDE = "include";
	public static final String EXCLUDE = "exclude";
	public static final String MANDATORY = "mandatory";

	// Directives
	public static final String SINGLETON = "singleton";
	public static final String RESOLUTION = "resolution";
	public static final String USES = "uses";
	public static final String OPTIONAL = "optional";
	public static final String X_INTERNAL = "x-internal";
	public static final String X_FRIENDS = "x-friends";
}