package tools.cipm.util.build.p2tm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.openntf.maven.p2.model.P2Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2ToMvnConverter {
    private static final Logger logger = LoggerFactory.getLogger(P2ToMvnConverter.class);

    public static void main(String[] args) {
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

                // Only support Linux.
                var subProcess = new ProcessBuilder(
                    "./mvnw", "install:install-file", "-DlocalRepositoryPath=../mvn",
                    "-Dfile=" + tempStorageFile.toString(), "-DgroupId=" + id,
                    "-DartifactId=" + bundle.getId(), "-Dversion=" + bundle.getVersion(),
                    "-Dpackaging=jar", "-DcreateChecksum=true")
                    .inheritIO()
                    .start();
                var subProcessResult = subProcess.waitFor();
                System.out.println(subProcessResult);

                Files.delete(tempStorageFile);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}