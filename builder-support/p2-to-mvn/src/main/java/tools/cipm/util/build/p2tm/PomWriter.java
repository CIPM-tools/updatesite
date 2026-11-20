package tools.cipm.util.build.p2tm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import tools.cipm.util.build.p2tm.mvnosgimap.Coordinate;

/**
 * Renders and writes a Maven POM that declares given dependencies.
 */
public final class PomWriter {

	private PomWriter() {
	}

	/**
	 * Writes a POM file that declares all given dependencies. Creates the POM file
	 * and all necessary directories, if not already present.
	 * 
	 * <p>
	 * FIXME Not tested
	 * 
	 * @param targetDir    The directory, under which the POM file will be generated
	 * @param pomFileName  The name of the generated POM file
	 * @param groupId      The group ID of the generated POM file
	 * @param artifactId   The artifact ID of the generated POM file
	 * @param version      The version of the generated POM file
	 * @param dependencies All dependencies as Maven coordinates
	 *                     ({@link Coordinate})
	 * @return The path to the generated POM file
	 * @throws IOException If creating the POM file fails
	 */
	public static Path write(Path targetDir, String pomFileName, String groupId, String artifactId, String version,
			Collection<Coordinate> dependencies) throws IOException {
		String xml = render(groupId, artifactId, version, dependencies);
		if (!Files.exists(targetDir)) {
			Files.createDirectories(targetDir);
		}
		Path pomFile = targetDir.resolve(pomFileName);
		Files.writeString(pomFile, xml);
		return pomFile;
	}

	/**
	 * Generates the String content of the POM file, which declares all given
	 * dependencies.
	 * 
	 * @param groupId      The group ID of the generated POM file
	 * @param artifactId   The artifact ID of the generated POM file
	 * @param version      The version of the generated POM file
	 * @param dependencies All dependencies as Maven coordinates
	 *                     ({@link Coordinate})
	 * @return The content of the POM for the given dependencies.
	 */
	public static String render(String groupId, String artifactId, String version,
			Collection<Coordinate> dependencies) {
		StringBuilder deps = new StringBuilder();
		if (dependencies != null) {
			for (Coordinate c : dependencies) {
				// Skip self-dependency.
				if (c.artifactId.equals(artifactId) && c.groupId.equals(groupId)) {
					continue;
				}
				deps.append("    <dependency>\n").append("      <groupId>").append(xmlEscape(c.groupId))
						.append("</groupId>\n").append("      <artifactId>").append(xmlEscape(c.artifactId))
						.append("</artifactId>\n").append("      <version>").append(xmlEscape(c.version))
						.append("</version>\n").append("    </dependency>\n");
			}
		}
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
				+ "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
				+ "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
				+ "https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" + "  <modelVersion>4.0.0</modelVersion>\n"
				+ "  <groupId>" + xmlEscape(groupId) + "</groupId>\n" + "  <artifactId>" + xmlEscape(artifactId)
				+ "</artifactId>\n" + "  <version>" + xmlEscape(version) + "</version>\n"
				+ "  <packaging>jar</packaging>\n" + "  <dependencies>\n" + deps + "  </dependencies>\n"
				+ "</project>\n";
	}

	private static String xmlEscape(String s) {
		if (s == null)
			return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&apos;");
	}
}