package tools.cipm.util.build.p2tm.p2.data;

import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Constants;

/**
 * A bundle-level dependency, from a Require-Bundle header.
 *
 * <p>
 * For completeness, all attributes and directives declared on the
 * {@code Require-Bundle} entry are preserved in their respective maps, so no
 * information is lost even for qualifiers this class does not interpret
 * directly (e.g. {@code visibility:=reexport}, {@code resolution:=optional}).
 * </p>
 */
public class BundleDependency extends InMemoryManifestElementRepresentation {
	/** The name of the required bundle. */
	private final String bundleName;

	/**
	 * Creates a dependency with the given attributes and directives.
	 *
	 * @param bundleName the required bundle name
	 * @param version    the {@code bundle-version} attribute string, may be
	 *                   {@code null}
	 * @param attributes all attribute name/value pairs (may be {@code null})
	 * @param directives all directive name/value pairs (may be {@code null})
	 */
	public BundleDependency(String bundleName, Map<String, String> attributes, Map<String, String> directives) {
		super(attributes, directives);
		this.bundleName = Objects.requireNonNull(bundleName, "bundleName is missing");
	}

	/**
	 * The parsed {@code version} attribute, or {@link VersionRange#any()} if
	 * absent.
	 */
	public VersionRange getVersionRange() {
		return VersionRange.parse(attributes.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
	}

	/**
	 * @return Whether this representation declares the {@code optional} directive.
	 */
	public boolean isOptional() {
		return Constants.RESOLUTION_OPTIONAL.equals(directives.get(Constants.RESOLUTION_DIRECTIVE));
	}

	public String getBundleName() {
		return bundleName;
	}
}
