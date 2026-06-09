package org.example;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DocsPlansTest {
    private static final Path REPO_ROOT = Paths.get("").toAbsolutePath();
    private static final Path DOCS_PLANS = REPO_ROOT.resolve("docs").resolve("plans");
    private static final Path CANONICAL_PLAN = DOCS_PLANS.resolve("2026-06-08-twilio-java-rc-testing-baseline.md");
    private static final Path POST_DIAL_PLAN = DOCS_PLANS.resolve("2026-06-09-post-dial-route.md");
    private static final Path POST_INVALID_DIAL_PLAN = DOCS_PLANS.resolve("2026-06-09-post-invalid-dial-target.md");
    private static final Path IDE_METADATA_PLAN = DOCS_PLANS.resolve("2026-06-09-ide-metadata-ignore.md");

    @Test
    public void canonicalPlanIsCompletedAndVerified() throws IOException {
        assertTrue("docs/plans must exist", Files.isDirectory(DOCS_PLANS));

        List<Path> plans = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DOCS_PLANS, "*.md")) {
            for (Path plan : stream) {
                plans.add(plan);
            }
        }

        assertFalse("docs/plans must contain markdown plans", plans.isEmpty());
        assertTrue("canonical baseline plan must exist", plans.contains(CANONICAL_PLAN));
        assertTrue("POST dial route plan must exist", plans.contains(POST_DIAL_PLAN));
        assertTrue("POST invalid dial target plan must exist", plans.contains(POST_INVALID_DIAL_PLAN));
        assertTrue("IDE metadata ignore plan must exist", plans.contains(IDE_METADATA_PLAN));

        for (Path plan : plans) {
            String text = new String(Files.readAllBytes(plan), StandardCharsets.UTF_8);
            assertTrue(plan + " must record completed status", text.contains("Status: Completed"));
            assertTrue(plan + " must document make check verification", text.contains("make check"));
        }
    }

    @Test
    public void ignoresLocalIdeMetadata() throws IOException {
        String gitignore = new String(Files.readAllBytes(REPO_ROOT.resolve(".gitignore")), StandardCharsets.UTF_8);

        assertTrue("IntelliJ project directories must be ignored", gitignore.contains(".idea/"));
        assertFalse("ignore rules should not only cover one local IntelliJ file", gitignore.contains(".idea/workspace.xml"));
    }
}
