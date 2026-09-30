package tools.cipm.util.build.p2tm.updatesiteparse;

import java.util.Map;

/**
 * An abstract class for classes that represent manifest files. Encapsulates the
 * entire manifest string (copies it, so that the original manifest file
 * contents are not modified).
 */
public abstract class InMemoryManifestRepresentation {
	/**
	 * The entire manifest file contents. Stored here, since it is already parsed in
	 * the process.
	 */
	private Map<String, String> entireManifest;

	/**
	 * Sets the entire manifest file content
	 * 
	 * @param entireManifest The entire manifest file content in map form (header,
	 *                       value)
	 */
	public void setEntireManifest(Map<String, String> entireManifest) {
		this.entireManifest = Map.copyOf(entireManifest == null ? Map.of() : entireManifest);
	}

	/**
	 * @return A map (header, value) that represents the entire manifest file's
	 *         contents.
	 */
	public Map<String, String> getEntireManifest() {
		return Map.copyOf(entireManifest);
	}
}
