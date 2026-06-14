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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocsPlansTest {
    private static final Path REPO_ROOT = Paths.get("").toAbsolutePath();
    private static final Path DOCS_PLANS = REPO_ROOT.resolve("docs").resolve("plans");
    private static final Path CANONICAL_PLAN = DOCS_PLANS.resolve("2026-06-08-twilio-java-rc-testing-baseline.md");
    private static final Path POST_DIAL_PLAN = DOCS_PLANS.resolve("2026-06-09-post-dial-route.md");
    private static final Path POST_INVALID_DIAL_PLAN = DOCS_PLANS.resolve("2026-06-09-post-invalid-dial-target.md");
    private static final Path IDE_METADATA_PLAN = DOCS_PLANS.resolve("2026-06-09-ide-metadata-ignore.md");
    private static final Path SCRIPTED_BASELINE_PLAN = DOCS_PLANS.resolve("2026-06-09-scripted-baseline-check.md");
    private static final Path UNUSED_DEPENDENCIES_PLAN = DOCS_PLANS.resolve("2026-06-09-unused-legacy-dependencies.md");
    private static final Path DEPENDENCIES_AND_CI_PLAN =
            DOCS_PLANS.resolve("2026-06-10-dependencies-and-ci.md");
    private static final Path HTTP_RESPONSE_HEADERS_PLAN =
            DOCS_PLANS.resolve("2026-06-10-http-response-headers.md");
    private static final Path LIVE_DIAL_RATE_LIMIT_PLAN =
            DOCS_PLANS.resolve("2026-06-13-live-dial-rate-limit.md");
    private static final Path STRICT_DIAL_FORM_PLAN =
            DOCS_PLANS.resolve("2026-06-13-strict-dial-form-parsing.md");
    private static final Path SUPPORTED_TOOLCHAIN_VERSIONS_PLAN =
            DOCS_PLANS.resolve("2026-06-14-supported-toolchain-versions.md");

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
        assertTrue("scripted baseline plan must exist", plans.contains(SCRIPTED_BASELINE_PLAN));
        assertTrue("unused dependency cleanup plan must exist", plans.contains(UNUSED_DEPENDENCIES_PLAN));
        assertTrue(
                "dependencies and CI plan must exist",
                plans.contains(DEPENDENCIES_AND_CI_PLAN)
        );
        assertTrue("HTTP response headers plan must exist", plans.contains(HTTP_RESPONSE_HEADERS_PLAN));
        assertTrue("live dial rate-limit plan must exist", plans.contains(LIVE_DIAL_RATE_LIMIT_PLAN));
        assertTrue("strict dial form plan must exist", plans.contains(STRICT_DIAL_FORM_PLAN));
        assertTrue(
                "supported toolchain versions plan must exist",
                plans.contains(SUPPORTED_TOOLCHAIN_VERSIONS_PLAN)
        );

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

    @Test
    public void checkGateRunsScriptedBaseline() throws IOException {
        String makefile = new String(Files.readAllBytes(REPO_ROOT.resolve("Makefile")), StandardCharsets.UTF_8);

        assertTrue(
                "make check must run the scripted baseline guard from the repository root",
                makefile.contains("\"$(ROOT)/scripts/check-baseline.sh\"")
        );
        assertTrue(
                "ROOT must resist command-line reassignment",
                makefile.contains("override ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))")
        );
        assertFalse("ROOT must not depend on the caller's directory", makefile.contains("ROOT := $(CURDIR)"));
        assertTrue("the Maven executable must remain configurable", makefile.contains("MVN ?= mvn"));
        assertTrue(makefile.contains("cd \"$(ROOT)\" && $(MVN)"));
    }

    @Test
    public void dependenciesUseVerifiedStableVersions() throws IOException {
        String pom = read("pom.xml");

        assertTrue(
                "Twilio must use the current stable SDK",
                pom.contains("<version>12.1.1</version>")
        );
        assertFalse("Spark must not reintroduce vulnerable Jetty", pom.contains("spark-core"));
        assertFalse("Jetty must not be a runtime dependency", pom.contains("jetty-"));
        assertTrue("Jackson must use the fixed BOM", pom.contains("<version>2.18.8</version>"));
        assertTrue("HttpCore must use the fixed release", pom.contains("<version>5.3.6</version>"));
        assertFalse("Twilio release candidates must not return", pom.contains("9.0.0-rc.1"));
    }

    @Test
    public void hostedVerificationIsPinnedAndLeastPrivilege() throws IOException {
        String workflow = read(".github/workflows/check.yml");

        List<Path> workflows = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                REPO_ROOT.resolve(".github/workflows")
        )) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    workflows.add(path.getFileName());
                }
            }
        }

        assertEquals("only the canonical hosted workflow may exist", 1, workflows.size());
        assertTrue(workflows.contains(Paths.get("check.yml")));
        assertTrue(workflow.contains("permissions:\n  contents: read"));
        assertTrue(workflow.contains("persist-credentials: false"));
        assertTrue(workflow.contains("group: check-${{ github.workflow }}-${{ github.ref }}"));
        assertTrue(workflow.contains("cancel-in-progress: true"));
        assertTrue(workflow.contains("runs-on: ubuntu-24.04"));
        assertTrue(workflow.contains("timeout-minutes: 10"));
        assertTrue(workflow.contains("java-version: [\"8\", \"11\", \"17\", \"21\"]"));
        assertTrue(workflow.contains("workflow_dispatch:"));
        assertTrue(workflow.contains("actions/checkout@df4cb1c069e1874edd31b4311f1884172cec0e10 # v6.0.3"));
        assertTrue(workflow.contains("actions/setup-java@be666c2fcd27ec809703dec50e508c2fdc7f6654 # v5.1.0"));
        assertTrue(workflow.contains("run: make check"));
        assertFalse("workflow must not use floating runners", workflow.contains("ubuntu-latest"));
        assertFalse("actions must use immutable commits", workflow.contains("@v"));
        assertFalse("workflow must not grant write permissions", workflow.matches(
                "(?s).*\\n\\s*[A-Za-z0-9_-]+:\\s*write(?:\\s|$).*"
        ));
        assertFalse("workflow must not use pull_request_target", workflow.contains("pull_request_target:"));
    }

    @Test
    public void supportedVersionsMatchExecutableContracts() throws IOException {
        String readme = read("README.md");
        String pom = read("pom.xml");
        String workflow = read(".github/workflows/check.yml");

        assertTrue(readme.contains("Java source and target: 8"));
        assertTrue(readme.contains("Verified Java runtimes: 8, 11, 17, and 21"));
        assertTrue(readme.contains("Reproduced local Maven baseline: 3.6.3"));
        assertTrue(readme.contains("Twilio Java SDK: exactly 12.1.1"));
        assertTrue(readme.contains("minimum supported Maven release"));
        assertTrue(pom.contains("<java.version>1.8</java.version>"));
        assertTrue(pom.contains("<artifactId>twilio</artifactId>"));
        assertTrue(pom.contains("<version>12.1.1</version>"));
        assertTrue(workflow.contains("java-version: [\"8\", \"11\", \"17\", \"21\"]"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(REPO_ROOT.resolve(relativePath)),
                StandardCharsets.UTF_8
        );
    }
}
