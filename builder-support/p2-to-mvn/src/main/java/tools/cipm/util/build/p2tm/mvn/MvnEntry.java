package tools.cipm.util.build.p2tm.mvn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Maven identity with its contained packages.
 */
public final class MvnEntry {
	private final String groupId;
	private final String artifactId;
	private final String version;
	private final List<String> packageNames = new ArrayList<>();
	/**
	 * The OSGi symbolic name of this potential bundle. Never {@code null}.
	 */
	private final String bundleName;

	/**
	 * Creates a new {@code Coordinate}.
	 *
	 * @param groupId     the Maven group ID
	 * @param artifactId  the Maven artifact ID
	 * @param version     the Maven version
	 * @param bundleName  the OSGi symbolic name of this Maven element.
	 */
	public MvnEntry(String groupId, String artifactId, String version, String bundleName, Collection<String> packageNames) {
		this.groupId = Objects.requireNonNull(groupId, "groupId missing");
		this.artifactId = Objects.requireNonNull(artifactId, "artifactId missing");
		this.version = Objects.requireNonNull(version, "version missing");
		this.bundleName = Objects.requireNonNull(bundleName, "bundleName missing");
		this.packageNames.addAll(packageNames);
	}

	/**
	 * @param packageName name of the package to check.
	 * @return Whether this instance contains the package.
	 */
	public boolean hasPackage(String packageName) {
		return this.packageNames.contains(packageName);
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public List<String> getPackageNames() {
		return packageNames;
	}

	public String getBundleName() {
		return bundleName;
	}
}
