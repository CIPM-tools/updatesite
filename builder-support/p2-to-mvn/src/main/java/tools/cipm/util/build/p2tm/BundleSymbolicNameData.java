package tools.cipm.util.build.p2tm;

import java.util.Map;

import tools.cipm.util.build.p2tm.updatesiteparse.InMemoryManifestElementRepresentation;

/**
 * The parsed Bundle-SymbolicName: its value plus all attributes/directives.
 */
public class BundleSymbolicNameData extends InMemoryManifestElementRepresentation {
	public final String symbolicName;

	public BundleSymbolicNameData(String symbolicName, Map<String, String> attributes, Map<String, String> directives) {
		super(attributes, directives);
		this.symbolicName = symbolicName;
	}

	public BundleSymbolicNameData(String symbolicName) {
		this(symbolicName, null, null);
	}

	public boolean isSingleton() {
		return Boolean.parseBoolean(directives.get(OsgiHeaders.SINGLETON));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof BundleSymbolicNameData other))
			return false;
		return symbolicName.equals(other.symbolicName) && super.equals(other);
	}

	@Override
	public String toString() {
		return "{symbolicName=" + symbolicName + ", " + getAttributesString() + ", " + getDirectivesString() + "}";
	}
}
