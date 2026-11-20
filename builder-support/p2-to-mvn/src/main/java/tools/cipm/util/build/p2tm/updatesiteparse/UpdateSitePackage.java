package tools.cipm.util.build.p2tm.updatesiteparse;

import java.util.Map;
import java.util.Objects;

import tools.cipm.util.build.p2tm.OsgiHeaders;

/**
 * A single package export or import entry, parsed from the Export-Package or
 * Import-Package manifest header.
 *
 * <p>
 * Both headers share the same structure: a package name plus optional
 * attributes (such as {@code version="..."}) and directives (such as
 * {@code resolution:=optional}), so a single type models both directions.
 * </p>
 *
 * <p>
 * For completeness, all attributes and directives declared on the package entry
 * are preserved in their respective maps, so no information is lost even for
 * qualifiers this class does not interpret directly (e.g. {@code uses:=},
 * {@code x-internal:=}, {@code mandatory}, etc.).
 * </p>
 */
public class UpdateSitePackage extends InMemoryManifestElementRepresentation {

	/** The package name. */
	public final String packageName;

	/**
	 * Creates a package requirement with the given attributes and directives.
	 *
	 * @param packageName the package name
	 * @param attributes  all attribute name/value pairs (may be {@code null})
	 * @param directives  all directive name/value pairs (may be {@code null})
	 */
	public UpdateSitePackage(String packageName, Map<String, String> attributes, Map<String, String> directives) {
		super(attributes, directives);
		this.packageName = Objects.requireNonNull(packageName, "packageName");
	}

	/**
	 * The parsed {@code version} attribute, or {@link VersionRange#any()} if
	 * absent.
	 */
	public VersionRange getVersionRange() {
		return VersionRange.parse(attributes.get(OsgiHeaders.VERSION));
	}

	/**
	 * @return Whether this representation declares the {@code optional} directive.
	 */
	public boolean isOptional() {
		return OsgiHeaders.OPTIONAL.equals(directives.get(OsgiHeaders.RESOLUTION));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof UpdateSitePackage other))
			return false;
		return packageName.equals(other.packageName) && super.equals(other);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{" + "packageName='" + packageName + '\'' + ", "
				+ getAttributesString() + ", " + getDirectivesString() + '}';
	}
}