package tools.cipm.util.build.p2tm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import org.openntf.maven.p2.model.P2Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2ToMvnConverter {
    private static final Logger logger = LoggerFactory.getLogger(P2ToMvnConverter.class);
    private static final String JAR_FILE_EXTENSION = ".jar";

    public static void main(String[] args) {
        // installJarsFromLocalDirectory();
        // installJarsFromRemoteRepository();
    }

    private static void installJarsFromLocalDirectory() {
        var consideredPath = Paths.get("target", "jars");
        if (Files.notExists(consideredPath)) {
            System.out.println("Cannot consider the directory. It does not exist.");
            return;
        }

        try(var scanner = new Scanner(System.in)) {
            var lastGroupIdContainer = new StringBuilder();
            Files
                .walk(consideredPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    var fileName = path.getFileName().toString();
                    if (!fileName.endsWith(JAR_FILE_EXTENSION)) {
                        return;
                    }

                    var fileNameParts = fileName.split("_");

                    System.out.println("You need to specify a group ID for the artifact "
                        + fileName + ". Please enter it. Leave the ID empty if the last group ID "
                        + lastGroupIdContainer.toString() + " should be reused.");
                    var potentialGroupId = scanner.next();
                    if (!potentialGroupId.isBlank()) {
                        lastGroupIdContainer.delete(0, lastGroupIdContainer.length());
                        lastGroupIdContainer.append(potentialGroupId);
                    }

                    try {
                        installJarLocally(
                            path.toString(),
                            lastGroupIdContainer.toString(),
                            fileNameParts[0],
                            fileNameParts[1].substring(0, fileNameParts[1].length() - JAR_FILE_EXTENSION.length())
                        );
                    } catch(IOException | InterruptedException e) {
                        System.out.println("Could not process " + fileName);
                    }
                });
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static void installJarsFromRemoteRepository() {
        String id = System.getProperty("p2tm.id");
        String repoUri = System.getProperty("p2tm.uri");
        if (id == null || repoUri == null) {
            System.out.println("No Id or URI given. Stopping.");
            return;
        }

        P2Repository p2Repo = P2Repository.getInstance(URI.create(repoUri), logger);
        var allBundles = p2Repo.getBundles();

        HttpClient client = HttpClient.newHttpClient();
        for (var bundle : allBundles) {
            System.out.println("Downloading " + bundle.getId());
            
            try {
                var tempStorageFile = Paths.get("target", bundle.getId() + "_" + bundle.getVersion() + ".jar");

                if (Files.notExists(tempStorageFile)) {
                    var bodyHandler = BodyHandlers.ofFile(tempStorageFile);
                    client.send(
                        HttpRequest.newBuilder(bundle.getUri("")).GET().build(),
                        bodyHandler
                    );
                }

                installJarLocally(tempStorageFile.toString(), id, bundle.getId(), bundle.getVersion());

                Files.delete(tempStorageFile);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void installJarLocally(String filePath, String groupId, String artifactId, String version) throws IOException, InterruptedException {
        // Only supports Linux for now.
        var subProcess = new ProcessBuilder(
            "./mvnw", "install:install-file", "-DlocalRepositoryPath=../mvn",
            "-Dfile=" + filePath, "-DgroupId=" + groupId,
            "-DartifactId=" + artifactId, "-Dversion=" + version,
            "-Dpackaging=jar", "-DcreateChecksum=true")
            .inheritIO()
            .start();
        var subProcessResult = subProcess.waitFor();
        System.out.println(subProcessResult);
    }
}