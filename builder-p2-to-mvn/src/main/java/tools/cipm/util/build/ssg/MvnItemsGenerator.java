package tools.cipm.util.build.ssg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import com.google.gson.Gson;

public class MvnItemsGenerator {
    private static final String POM_FILE_EXTENSION = ".pom";
    private static final String ITEM_GROUP_ID = "groupId";
    private static final String ITEM_ARTIFACT_ID = "artifactId";
    private static final String ITEM_VERSION = "version";

    public static void main(String[] args) {
        List<Pom> poms = new ArrayList<>();

        try {
            Files
                .walk(Paths.get("..", "mvn"))
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().endsWith(POM_FILE_EXTENSION))
                .forEach(file -> {
                    try {
                        var pom = new Pom();
                        var xmlPom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
                        var projectChilds = xmlPom.getFirstChild().getChildNodes();

                        for (int index = 0; index < projectChilds.getLength(); index++) {
                            var childNode = projectChilds.item(index);
                            var nodeName = childNode.getNodeName();

                            switch (nodeName) {
                                case MvnItemsGenerator.ITEM_GROUP_ID:
                                    pom.groupId = childNode.getTextContent();
                                    break;
                                case MvnItemsGenerator.ITEM_ARTIFACT_ID:
                                    pom.artifactId = childNode.getTextContent();
                                    break;
                                case MvnItemsGenerator.ITEM_VERSION:
                                    pom.version = childNode.getTextContent();
                                    break;
                                default:
                                    break;
                            }
                        }

                        poms.add(pom);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } 
                });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (var writer = Files.newBufferedWriter(Paths.get("..", "mvn", "items.json"))) {
            var gson = new Gson();
            writer.append(gson.toJson(poms));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Pom {
        private String groupId;
        private String artifactId;
        private String version;
    }
}