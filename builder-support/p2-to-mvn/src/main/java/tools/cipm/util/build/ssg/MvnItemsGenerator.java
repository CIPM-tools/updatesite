package tools.cipm.util.build.ssg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NodeList;

import com.google.gson.Gson;

public class MvnItemsGenerator {
    private static final String POM_FILE_EXTENSION = ".pom";
    private static final String ITEM_GROUP_ID = "groupId";
    private static final String ITEM_ARTIFACT_ID = "artifactId";
    private static final String ITEM_VERSION = "version";
    private static final String ITEM_DEPENDENCIES = "dependencies";
    private static final String ITEM_DEPENDENCY = "dependency";

    public static void main(String[] args) {
        List<Pom> poms = new ArrayList<>();

        try {
            Files
                .walk(Paths.get("..", "mvn"))
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().endsWith(POM_FILE_EXTENSION))
                .forEach(file -> {
                    try {
                        var xmlPom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
                        var projectChilds = xmlPom.getFirstChild().getChildNodes();

                        Pom pom = extractPom(projectChilds);
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

    private static List<Pom> extractDependencies(NodeList nodes) {
        List<Pom> deps = new ArrayList<>();
        for (var idx = 0; idx < nodes.getLength(); idx++) {
            var childNode = nodes.item(idx);
            if (childNode.getNodeName().equals(MvnItemsGenerator.ITEM_DEPENDENCY)) {
                deps.add(extractPom(nodes.item(idx).getChildNodes()));
            }
        }
        return deps;
    }

    private static Pom extractPom(NodeList nodes) {
        var pom = new Pom();

        for (int index = 0; index < nodes.getLength(); index++) {
            var childNode = nodes.item(index);
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
                case MvnItemsGenerator.ITEM_DEPENDENCIES:
                    pom.dependencies = extractDependencies(childNode.getChildNodes());
                    break;
                default:
                    break;
            }
        }

        return pom;
    }

    private static class Pom {
        private String groupId;
        private String artifactId;
        private String version;
        private List<Pom> dependencies;
    }
}