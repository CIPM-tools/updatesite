package tools.cipm.util.build.p2tm;

import java.io.IOException;
import java.nio.file.Paths;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws IOException, InterruptedException {
        var converter = new P2ToMvnConverter();
		converter.convertLocalJarDirectory(Paths.get("..", "mvn").toAbsolutePath().normalize());
	}
}
