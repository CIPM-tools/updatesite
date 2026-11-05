package tools.cipm.util.build.p2tm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import tools.cipm.util.build.p2tm.mvn.MvnEntry;

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
	 * @param targetDir    The directory, under which the POM file will be generated
	 * @param entry        The entry for which the pom.xml is generated.
	 * @param dependencies All dependencies as Maven coordinates
	 *                     ({@link MvnEntry})
	 * @return The path to the generated POM file
	 * @throws IOException If creating the POM file fails
	 */
	public static Path write(Path targetDir, MvnEntry entry, Collection<MvnEntry> dependencies) throws IOException {
		String xml = render(entry, dependencies);

		if (!Files.exists(targetDir)) {
			Files.createDirectories(targetDir);
		}
		Path pomFile = targetDir.resolve(FileConstants.POM_FILE_NAME);
		Files.writeString(pomFile, xml);

		return pomFile;
	}

	/**
	 * Generates the String content of the POM file, which declares all given
	 * dependencies.
	 * 
	 * @param entry        The Maven entry.
	 * @param dependencies All dependencies as Maven coordinates
	 *                     ({@link MvnEntry})
	 * @return The content of the POM for the given dependencies and Maven entry.
	 */
	private static String render(MvnEntry entry, Collection<MvnEntry> dependencies) {
		StringBuilder builder = new StringBuilder();
		builder
			.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n")
			.append("        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
			.append("        xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n")
			.append("   <modelVersion>4.0.0</modelVersion>\n")
			.append("    <groupId>")
			.append(xmlEscape(entry.getGroupId()))
			.append("</groupId>\n")
			.append("    <artifactId>")
			.append(xmlEscape(entry.getArtifactId()))
			.append("</artifactId>\n")
			.append("    <version>")
			.append(xmlEscape(entry.getVersion()))
			.append("</version>\n")
			.append("    <packaging>jar</packaging>\n")
			.append("    <dependencies>\n");

		if (dependencies != null) {
			for (MvnEntry depEntry : dependencies) {
				// Skip self-dependency.
				if (depEntry.getArtifactId().equals(entry.getArtifactId()) && depEntry.getGroupId().equals(entry.getGroupId())) {
					continue;
				}

				builder
					.append("        <dependency>\n")
					.append("            <groupId>")
					.append(xmlEscape(depEntry.getGroupId()))
					.append("</groupId>\n")
					.append("            <artifactId>")
					.append(xmlEscape(depEntry.getArtifactId()))
					.append("</artifactId>\n")
					.append("            <version>")
					.append(xmlEscape(depEntry.getVersion()))
					.append("</version>\n")
					.append("        </dependency>\n");
			}
		}

		builder.append("    </dependencies>\n")
			.append("</project>\n\n");
		return builder.toString();
	}

	private static String xmlEscape(String s) {
		if (s == null) {
			return "";
		}
		return s
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&apos;");
	}
}