package tools.cipm.util.build.p2tm;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
		// initMvnRepo();
		// installJarsFromLocalDirectory();
		// installJarsFromRemoteRepository();
		// installJarsFromRepository("", FileConstants.URI_FILE_PREFIX + "", true);
        P2ToMvnConverter.resolveTransitiveDependencies();
	}
}
