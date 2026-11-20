package tools.cipm.util.build.p2tm.updatesiteparse;

import java.util.Objects;

/**
 * An immutable, parsed representation of a version range.
 *
 * <p>
 * Supports both OSGi and Maven range syntax, e.g.
 * </p>
 * <ul>
 * <li>{@code 1.2.3} — a single (exact) version, no bounds. Exact versions are
 * internally stored as the lower version and the upper version is null in that
 * case</li>
 * <li>{@code [1.0,2.0)} — inclusive lower, exclusive upper</li>
 * <li>{@code [1.0,2.0]} — inclusive both</li>
 * <li>{@code (1.0,2.0)} — exclusive both</li>
 * <li>{@code [1.0,)} / {@code [1.0,∞)} — inclusive lower, unbounded upper</li>
 * <li>{@code (,2.0]} — unbounded lower, inclusive upper</li>
 * </ul>
 */
public final class VersionRange {
	/**
	 * A constant instance that represents a version range that matches all
	 * versions.
	 */
	public static final VersionRange ANY = new VersionRange("0", true, null, false, true);

	private final String lowerVersion; // may be null when unbounded, exact versions are stored here
	private final boolean includeLower; // Whether lowerVersion is inclusive
	private final String upperVersion; // may be null when unbounded
	private final boolean includeUpper; // Whether upperVersion is inclusive
	private final boolean bounded; // false when it's a single exact version

	private VersionRange(String lowerVersion, boolean includeLower, String upperVersion, boolean includeUpper,
			boolean bounded) {
		this.lowerVersion = lowerVersion;
		this.includeLower = includeLower;
		this.upperVersion = upperVersion;
		this.includeUpper = includeUpper;
		this.bounded = bounded;
	}

	/**
	 * The unconstrained "any version" range: {@code [0,∞)}.
	 *
	 * @return a range matching any version
	 */
	public static VersionRange any() {
		return ANY;
	}

	/**
	 * A single exact version (no bounds), e.g. {@code "1.2.3"}.
	 *
	 * @param version the exact version string
	 * @return a non-null unbounded single-version range
	 */
	public static VersionRange exact(String version) {
		String v = version == null ? "" : version.trim();
		return new VersionRange(v, true, null, false, false);
	}

	/**
	 * Parses a version-range string into a {@link VersionRange}.
	 *
	 * @param value the raw header value, may be null if no version range or exact
	 *              version is specified
	 * @return the parsed range, always non-null, defaults to {@link #any()} if the
	 *         given value is empty or blank or null
	 * @throws IllegalArgumentException if {@code value} is non-blank but malformed
	 *                                  (e.g. an unterminated or comma-less
	 *                                  bracketed range)
	 */
	public static VersionRange parse(String value) {
		// Null / empty / blank -> "any" version.
		if (value == null || value.isBlank()) {
			return any();
		}

		String s = value.trim();

		// A single version without brackets — an exact version.
		if (s.charAt(0) != '[' && s.charAt(0) != '(') {
			return exact(s);
		}

		// Bracketed range, e.g. "[1.0,2.0)" / "[1.0,2.0]" / "[1.0,)".
		int comma = s.indexOf(',');
		if (comma == -1) {
			throw new IllegalArgumentException("Malformed version range (missing ','): " + value);
		}
		if (!s.endsWith("]") && !s.endsWith(")")) {
			throw new IllegalArgumentException("Malformed version range (missing closing bracket): " + value);
		}

		char open = s.charAt(0);
		char close = s.charAt(s.length() - 1);
		boolean includeLower = (open == '[');
		boolean includeUpper = (close == ']');

		String lower = normalizeBound(s.substring(1, comma).trim());
		String upper = normalizeBound(s.substring(comma + 1, s.length() - 1).trim());

		return new VersionRange(lower, includeLower, upper, includeUpper, true);
	}

	/**
	 * Handles cases, where no bounds are specified in version ranges.
	 * 
	 * @param raw The version bound as string or empty if no bound is specified
	 * @return Returns null for empty/∞ bounds (unbounded).
	 */
	private static String normalizeBound(String raw) {
		if (raw == null)
			return null;
		String t = raw.trim();
		if (t.isEmpty() || t.equals("\u221E") || t.equals("INF")) {
			return null;
		}
		return t;
	}

	/**
	 * @return the lower bound version, or {@code null} if unbounded
	 */
	public String getLowerVersion() {
		return lowerVersion;
	}

	/**
	 * @return {@code true} if the lower bound is inclusive ({@code [})
	 */
	public boolean isIncludeLower() {
		return includeLower;
	}

	/**
	 * @return the upper bound version, or {@code null} if unbounded
	 */
	public String getUpperVersion() {
		return upperVersion;
	}

	/**
	 * @return {@code true} if the upper bound is inclusive ({@code ]})
	 */
	public boolean isIncludeUpper() {
		return includeUpper;
	}

	/**
	 * @return {@code true} if this is a bounded range (i.e. not a single exact
	 *         version)
	 */
	public boolean isBounded() {
		return bounded;
	}

	/**
	 * Checks whether the given version string lies within this range.
	 *
	 * <p>
	 * The comparison is performed as a string-based lexical comparison against this
	 * range's bounds:
	 * </p>
	 * <ul>
	 * <li>If this range is {@link #any()}, the version is considered in range (any
	 * version matches).</li>
	 * <li>If the given version is equal to an inclusive bound, it is in range.</li>
	 * <li>Otherwise, the version must fall strictly between a lower and an upper
	 * bound (inclusive or exclusive per the bound markers).</li>
	 * </ul>
	 *
	 * <p>
	 * FIXME Check whether version compatibility can actually be computed like this.
	 * Since different versions follow different conventions, a lexical comparison
	 * may not always suffice. This method was provided as a best-effort version
	 * compatibility check.
	 *
	 * @param versionString the version to check; may be {@code null}
	 * @return {@code true} if the version is within this range, {@code false}
	 *         otherwise
	 */
	public boolean inRange(String versionString) {
		// The "any" range matches every version.
		if (isAny(this)) {
			return true;
		}

		if (versionString == null) {
			return false;
		}

		// Bounded ranges compare against both bounds.
		if (bounded) {
			if (lowerVersion != null) {
				int lowerCmp = versionString.compareTo(lowerVersion);
				if (lowerCmp < 0 || (lowerCmp == 0 && !includeLower)) {
					return false;
				}
			}
			if (upperVersion != null) {
				int upperCmp = versionString.compareTo(upperVersion);
				if (upperCmp > 0 || (upperCmp == 0 && !includeUpper)) {
					return false;
				}
			}
			return true;
		}

		// An unbounded single-version range: exact equality only.
		return versionString.equals(lowerVersion);
	}

	/**
	 * @return Whether the given VersionRange is {@link #ANY}
	 */
	public static boolean isAny(VersionRange range) {
		return range == ANY;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof VersionRange other))
			return false;
		return includeLower == other.includeLower && includeUpper == other.includeUpper && bounded == other.bounded
				&& Objects.equals(lowerVersion, other.lowerVersion) && Objects.equals(upperVersion, other.upperVersion);
	}

	@Override
	public String toString() {
		if (!bounded) {
			return lowerVersion;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(includeLower ? '[' : '(');
		sb.append(lowerVersion == null ? "" : lowerVersion);
		sb.append(',');
		sb.append(upperVersion == null ? "" : upperVersion);
		sb.append(includeUpper ? ']' : ')');
		return sb.toString();
	}
}