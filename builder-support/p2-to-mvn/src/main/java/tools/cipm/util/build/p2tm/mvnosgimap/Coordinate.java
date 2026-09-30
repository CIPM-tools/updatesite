package tools.cipm.util.build.p2tm.mvnosgimap;

import java.util.Objects;

/**
 * The Maven identity of a bundle or an exported package thereof. Coordinates
 * for entire bundles use the {@link #DEFAULT_EXPORTED_PACKAGE_NAME} (
 * {@value #DEFAULT_EXPORTED_PACKAGE_NAME} ), while Coordinates for exported
 * packages will contain the name of the exported package.
 *
 * <p>
 * This class can be used both to look up the bundle providing an
 * {@code Import-Package} dependency (via {@link #packageName}) and to resolve
 * whole-bundle dependencies (via {@link #bundleName}).
 * </p>
 */
public final class Coordinate {
	public static final String DEFAULT_EXPORTED_PACKAGE_NAME = "";

	/** The Maven group ID of the bundle that exports {@link #packageName}. */
	public final String groupId;

	/** The Maven artifact ID of the bundle that exports {@link #packageName}. */
	public final String artifactId;

	/** The Maven version of the bundle that exports {@link #packageName}. */
	public final String version;

	/**
	 * The single exported package this coordinate represents. Never {@code null}.
	 */
	public final String packageName;

	/**
	 * The OSGi symbolic name of the bundle that exports {@link #packageName}. Never
	 * {@code null}.
	 */
	public final String bundleName;

	public Coordinate(String groupId, String artifactId, String version, String bundleName) {
		this(groupId, artifactId, version, DEFAULT_EXPORTED_PACKAGE_NAME, bundleName);
	}

	/**
	 * Creates a new {@code Coordinate}.
	 *
	 * @param groupId     the Maven group ID of the exporting bundle
	 * @param artifactId  the Maven artifact ID of the exporting bundle
	 * @param version     the Maven version of the exporting bundle
	 * @param packageName the name of the exported package; may be null when the
	 *                    bundle exports no packages
	 * @param bundleName  the OSGi symbolic name of the exporting bundle
	 */
	public Coordinate(String groupId, String artifactId, String version, String packageName, String bundleName) {
		this.groupId = Objects.requireNonNull(groupId, "groupId missing");
		this.artifactId = Objects.requireNonNull(artifactId, "artifactId missing");
		this.version = Objects.requireNonNull(version, "version missing");

		// Null-safe: fall back to DEFAULT_EXPORTED_PACKAGE_NAME when no package is
		// exported.
		this.packageName = (packageName == null || packageName.isBlank()) ? DEFAULT_EXPORTED_PACKAGE_NAME : packageName;

		this.bundleName = Objects.requireNonNull(bundleName, "bundleName missing");
	}

	/**
	 * @return Whether this instance is for an exported package from a bundle.
	 */
	public boolean hasPackage() {
		return !this.packageName.isBlank() && !this.packageName.equals(DEFAULT_EXPORTED_PACKAGE_NAME);
	}

	/**
	 * Returns {@code true} if the given object is another {@code Coordinate} that
	 * identifies the exact same bundle or exported package
	 *
	 * @param o the object to compare with
	 * @return {@code true} if equal, {@code false} otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Coordinate other))
			return false;
		return groupId.equals(other.groupId) && artifactId.equals(other.artifactId) && version.equals(other.version)
				&& packageName.equals(other.packageName) && bundleName.equals(other.bundleName);
	}

	/**
	 * Returns a human-readable representation of this coordinate.
	 *
	 * @return a string of the form
	 *         {@code "package  <-  groupId:artifactId:version (bundle ...)"}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (hasPackage()) {
			sb.append(packageName + "  <-  ");
		}
		sb.append(groupId + ":" + artifactId + ":" + version);
		if (!bundleName.equals(artifactId)) {
			sb.append("  (bundle " + bundleName + ")");
		}
		return sb.toString();
	}
}