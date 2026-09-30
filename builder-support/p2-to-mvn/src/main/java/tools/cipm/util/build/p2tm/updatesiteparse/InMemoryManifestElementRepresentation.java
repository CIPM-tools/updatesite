package tools.cipm.util.build.p2tm.updatesiteparse;

import java.util.Map;

/**
 * An abstract class for classes that represent {@link ManifestElement}
 * instances.
 */
public abstract class InMemoryManifestElementRepresentation extends InMemoryManifestRepresentation {

	/**
	 * All attribute name/value pairs declared on this package entry (e.g.
	 * {@code version}, {@code bundle-symbolic-name}, {@code mandatory}).
	 * Unmodifiable.
	 */
	public final Map<String, String> attributes;

	/**
	 * All directive name/value pairs declared on this package entry (e.g.
	 * {@code resolution}, {@code uses}, {@code x-internal}). Unmodifiable.
	 */
	public final Map<String, String> directives;

	/**
	 * Creates an instance with the given attributes and directives.
	 *
	 * @param attributes all attribute name/value pairs (may be {@code null})
	 * @param directives all directive name/value pairs (may be {@code null})
	 */
	public InMemoryManifestElementRepresentation(Map<String, String> attributes, Map<String, String> directives) {
		this.attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
		this.directives = Map.copyOf(directives == null ? Map.of() : directives);
	}

	/**
	 * Creates an instance with no attributes nor directives (their Map values will
	 * still be non-null).
	 */
	public InMemoryManifestElementRepresentation() {
		this(null, null);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof InMemoryManifestElementRepresentation other))
			return false;
		return attributes.equals(other.attributes) && directives.equals(other.directives);
	}

	/**
	 * @return String version of all attributes stored in this instance
	 */
	protected String getAttributesString() {
		return "attributes=" + attributes;
	}

	/**
	 * @return String version of all directives stored in this instance
	 */
	protected String getDirectivesString() {
		return "directives=" + directives;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{" + getAttributesString() + ", " + getDirectivesString() + '}';
	}
}
