package com.rescatevial.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the committer date for the version currently deployed by the app. */
@RestController
public class DeploymentInfoController {
    private static final Path DEPLOYED_COMMIT_FILE = Path.of("/var/lib/rescatevial/deployed-commit");

    @GetMapping("/api/deployment-info")
    public ResponseEntity<Map<String, String>> deploymentInfo() {
        try {
            Path repository = findRepository();
            String commit = readDeployedCommit();
            String updatedAt = readCommitDate(repository, commit);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("updatedAt", updatedAt));
        } catch (Exception exception) {
            return ResponseEntity.status(503)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("status", "unavailable"));
        }
    }

    private Path findRepository() throws IOException {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path deployedRepository = workingDirectory.resolve("repo");
        if (Files.exists(deployedRepository.resolve(".git"))) {
            return deployedRepository;
        }
        if (Files.exists(workingDirectory.resolve(".git"))) {
            return workingDirectory;
        }
        throw new IOException("Git repository not found");
    }

    private String readDeployedCommit() throws IOException {
        if (Files.isReadable(DEPLOYED_COMMIT_FILE)) {
            String commit = Files.readString(DEPLOYED_COMMIT_FILE, StandardCharsets.UTF_8).trim();
            if (commit.matches("[0-9a-fA-F]{40,64}")) {
                return commit;
            }
        }
        return "HEAD";
    }

    private String readCommitDate(Path repository, String commit) throws Exception {
        Process process = new ProcessBuilder("git", "-C", repository.toString(),
                "show", "-s", "--format=%cI", commit)
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(2, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Git date lookup timed out");
        }
        String updatedAt;
        try (var output = process.getInputStream()) {
            updatedAt = new String(output.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        if (process.exitValue() != 0) {
            throw new IOException("Git date lookup failed");
        }
        Instant.parse(updatedAt);
        return updatedAt;
    }
}
