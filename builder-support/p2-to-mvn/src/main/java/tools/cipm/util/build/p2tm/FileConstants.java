package tools.cipm.util.build.p2tm;

/**
 * Contains constants about file names, extensions, as well as URI prefixes
 */
public final class FileConstants {
	private FileConstants() {
	}

	public static final String TARGET_DIR_NAME = "target";
	public static final String TARGET_JARS_DIR_NAME = "jars";

	public static final String JAR_FILE_EXTENSION = ".jar";
	public static final String JAR_MANIFEST_ENTRY_NAME = "META-INF/MANIFEST.MF";

	public static final String URI_FILE_PREFIX = "file://";
	public static final String URI_HTTP_SCHEME = "http";
	public static final String URI_HTTPS_SCHEME = "https";
}
