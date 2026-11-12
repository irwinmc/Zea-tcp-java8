#!/usr/bin/env python3
"""Test the real Gradle publication configuration against an isolated local Portal."""

import argparse
import base64
import hashlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import threading
import time
import unittest
from urllib.parse import parse_qs, urlsplit
import zipfile


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("central_publishing", ROOT / "scripts/central_publishing.py")
CENTRAL = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CENTRAL)
VERSION = "0.8.5"
PREFIX = CENTRAL.release_prefix("com.akakata", "zea-tcp-java8", VERSION)
DEPLOYMENT = "00000000-0000-4000-8000-000000000001"
USERNAME = "synthetic-portal-user"
PASSWORD = "synthetic-portal-password"
PASSPHRASE = "synthetic-protected-key-passphrase"


def archive_files(files):
    """Build a ZIP for negative validation fixtures."""
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w") as archive:
        for name, content in files.items():
            archive.writestr(name, content)
    return output.getvalue()


def replace_artifact(files, name, content):
    """Update digests when exercising a deeper structural validation branch."""
    files[name] = content
    for algorithm in CENTRAL.ALGORITHMS:
        files[name + "." + algorithm] = hashlib.new(algorithm, content).hexdigest().encode()


class LocalPortal(BaseHTTPRequestHandler):
    """Validate real signed bundles while keeping all traffic on loopback."""

    def log_message(self, *args):
        """Keep synthetic authentication headers out of request logs."""

    def do_POST(self):
        """Emulate upload, validation, publication and authentication rejection."""
        # Drain multipart data before rejecting authentication to avoid a TCP reset.
        body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
        expected = "Bearer " + base64.b64encode((USERNAME + ":" + PASSWORD).encode()).decode()
        if self.headers.get("Authorization") != expected or self.server.mode == "unauthorized":
            self.send_response(401)
            self.end_headers()
            return
        parsed = urlsplit(self.path)
        if parsed.path.endswith("/upload"):
            self.server.uploads += 1
            try:
                if parse_qs(parsed.query).get("publishingType") != ["AUTOMATIC"]:
                    raise ValueError("Automatic publishing was not requested")
                from email.parser import BytesParser
                from email.policy import default
                message = BytesParser(policy=default).parsebytes(
                    ("Content-Type: " + self.headers["Content-Type"] + "\r\n\r\n").encode() + body)
                parts = [part for part in message.iter_parts() if part.get_param("name", header="content-disposition") == "bundle"]
                if len(parts) != 1:
                    raise ValueError("Missing multipart bundle")
                files, artifacts, _ = CENTRAL.validate_bundle(parts[0].get_payload(decode=True), "com.akakata", "zea-tcp-java8", VERSION)
                CENTRAL.verify_signatures(files, artifacts, self.server.key_id)
            except Exception as error:
                self.server.error = type(error).__name__ + ": " + str(error)
                self.send_response(400)
                self.end_headers()
                return
            self.send_response(201)
            self.end_headers()
            self.wfile.write(DEPLOYMENT.encode())
            return
        if parsed.path.endswith("/status"):
            self.server.polls += 1
            if parse_qs(parsed.query).get("id") != [DEPLOYMENT]:
                self.send_response(404)
                self.end_headers()
                return
            if self.server.mode == "failed":
                state = "FAILED"
            elif self.server.mode == "pending":
                state = "PENDING"
            else:
                state = ["VALIDATING", "PUBLISHING", "PUBLISHED"][min(self.server.polls - 1, 2)]
            self.send_response(200)
            self.end_headers()
            self.wfile.write(json.dumps({"deploymentId": DEPLOYMENT, "deploymentState": state}).encode())
            return
        self.send_response(404)
        self.end_headers()


class PublicationTest(unittest.TestCase):
    """Exercise real signing, staging and upload plus negative release cases."""

    @classmethod
    def setUpClass(cls):
        """Build a minimal Java 8 fixture using the repository's exact publishing script."""
        # GnuPG's agent uses Unix sockets with short path limits on macOS.
        cls.temporary = tempfile.TemporaryDirectory(prefix="zea-publish-", dir="/tmp")
        cls.addClassCleanup(cls.cleanup_fixture)
        cls.workspace = Path(cls.temporary.name)
        cls.key_home = cls.workspace / "keyring"
        cls.key_home.mkdir(mode=0o700)
        cls.previous = {name: os.environ.get(name) for name in
                        ("GNUPGHOME", "GPG_KEY_ID", "MAVEN_CENTRAL_USERNAME", "MAVEN_CENTRAL_PASSWORD")}
        cls.env = dict(os.environ)
        cls.env["GNUPGHOME"] = str(cls.key_home)
        generated = subprocess.run(["gpg", "--batch", "--pinentry-mode", "loopback", "--passphrase-fd", "0",
                                    "--quick-generate-key", "Publication Fixture <fixture@example.invalid>",
                                    "rsa2048", "sign", "0"], input=PASSPHRASE, env=cls.env,
                                   capture_output=True, text=True, timeout=60)
        if generated.returncode:
            raise RuntimeError("Could not create the temporary signing identity: " + generated.stderr)
        listed = subprocess.run(["gpg", "--with-colons", "--list-secret-keys"], env=cls.env,
                                capture_output=True, text=True, check=True)
        cls.key_id = next(line.split(":")[9] for line in listed.stdout.splitlines() if line.startswith("fpr:"))
        exported = subprocess.run(["gpg", "--batch", "--pinentry-mode", "loopback", "--passphrase-fd", "0",
                                   "--armor", "--export-secret-keys", cls.key_id], input=PASSPHRASE,
                                  env=cls.env, capture_output=True, text=True, check=True)
        cls.env.update({"GPG_PRIVATE_KEY": exported.stdout, "GPG_PASSPHRASE": PASSPHRASE,
                        "GPG_KEY_ID": cls.key_id, "MAVEN_CENTRAL_USERNAME": USERNAME,
                        "MAVEN_CENTRAL_PASSWORD": PASSWORD})
        for name in cls.previous:
            os.environ[name] = cls.env[name]
        shutil.copy2(ROOT / "gradlew", cls.workspace / "gradlew")
        shutil.copytree(ROOT / "gradle", cls.workspace / "gradle")
        (cls.workspace / "scripts").mkdir()
        shutil.copy2(ROOT / "scripts/central_publishing.py", cls.workspace / "scripts/central_publishing.py")
        (cls.workspace / "gradle.properties").write_text(
            "group=com.akakata\nversion=0.8.5\norg.gradle.jvmargs=-Xmx192m\norg.gradle.workers.max=1\n")
        (cls.workspace / "settings.gradle").write_text("rootProject.name = 'zea-tcp-java8'\n")
        (cls.workspace / "build.gradle").write_text(
            "plugins { id 'java-library'; id 'maven-publish'; id 'signing' }\n"
            "java { toolchain { languageVersion = JavaLanguageVersion.of(8) }; withSourcesJar(); withJavadocJar() }\n"
            "tasks.register('testJavadoc', Javadoc) { source = sourceSets.test.allJava }\n"
            "apply from: 'gradle/central-publishing.gradle'\n")
        source = cls.workspace / "src/main/java/fixture"
        source.mkdir(parents=True)
        (source / "PublicationProbe.java").write_text(
            "package fixture;\n\n/**\n * Provides a minimal publication fixture.\n */\n"
            "public final class PublicationProbe {\n\n"
            "    /**\n     * Creates the publication fixture.\n     */\n"
            "    public PublicationProbe() {\n    }\n}\n")
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), LocalPortal)
        cls.server.key_id, cls.server.error = cls.key_id, None
        cls.server.uploads, cls.server.polls, cls.server.mode = 0, 0, "success"
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.endpoint = "http://127.0.0.1:" + str(cls.server.server_port) + "/api/v1/publisher"
        result = subprocess.run(["./gradlew", "--no-daemon", "--offline", "--console=plain", "publishCentral",
                                 "-PcentralEndpoint=" + cls.endpoint, "-PcentralPollInterval=0.02"],
                                cwd=cls.workspace, env=cls.env, capture_output=True, text=True, timeout=180)
        cls.gradle_exit, cls.gradle_output = result.returncode, result.stdout + result.stderr
        if result.returncode:
            raise RuntimeError("Real Gradle signed publication failed:\n" + cls.gradle_output[-6000:])
        cls.bundle = cls.workspace / "build/central/central-bundle.zip"
        cls.files, cls.artifacts, _ = CENTRAL.validate_bundle(cls.bundle.read_bytes(), "com.akakata", "zea-tcp-java8", VERSION)

    @classmethod
    def cleanup_fixture(cls):
        """Release only this fixture's server, signing agent and temporary files."""
        if hasattr(cls, 'server'):
            cls.server.shutdown()
            cls.server.server_close()
            cls.thread.join(timeout=5)
        if hasattr(cls, 'key_home'):
            subprocess.run(["gpgconf", "--homedir", str(cls.key_home), "--kill", "gpg-agent"], capture_output=True)
        for name, value in getattr(cls, 'previous', {}).items():
            if value is None:
                os.environ.pop(name, None)
            else:
                os.environ[name] = value
        cls.temporary.cleanup()

    def setUp(self):
        """Keep each validation mutation independent."""
        self.files = dict(self.files)
        self.server.mode = "success"

    def validate(self):
        """Exercise the production bundle validator with the current fixture."""
        return CENTRAL.validate_bundle(archive_files(self.files), "com.akakata", "zea-tcp-java8", VERSION)

    def publisher(self, name, **options):
        """Create an isolated deployment journal for a local publisher probe."""
        return CENTRAL.Publisher(self.bundle, "com.akakata", "zea-tcp-java8", VERSION,
                                 self.workspace / "reports" / name, endpoint=self.endpoint,
                                 credentials_file=self.workspace / "missing.json", timeout=5, poll_interval=0.01, **options)

    def test_real_gradle_signed_publication(self):
        self.assertEqual(self.gradle_exit, 0)
        self.assertIsNone(self.server.error)
        self.assertGreaterEqual(self.server.uploads, 1)
        self.assertGreaterEqual(self.server.polls, 3)
        self.assertIn("PUBLISHED", self.gradle_output)

    def test_complete_bundle_and_signatures(self):
        files, artifacts, classes = self.validate()
        self.assertEqual(classes, 1)
        self.assertEqual(len(artifacts), 5)
        self.assertEqual(CENTRAL.verify_signatures(files, artifacts, self.key_id), [self.key_id])

    def test_no_repository_metadata(self):
        self.files["com/akakata/zea-tcp-java8/maven-metadata.xml"] = b"metadata"
        with self.assertRaisesRegex(ValueError, "Unexpected or missing"):
            self.validate()

    def test_no_tracking_files(self):
        self.files[str(Path(PREFIX).parent / "_remote.repositories")] = b"tracking"
        with self.assertRaisesRegex(ValueError, "Unexpected or missing"):
            self.validate()

    def test_missing_pom(self):
        del self.files[PREFIX + ".pom"]
        with self.assertRaises(ValueError):
            self.validate()

    def test_missing_sources(self):
        del self.files[PREFIX + "-sources.jar"]
        with self.assertRaises(ValueError):
            self.validate()

    def test_missing_javadoc(self):
        del self.files[PREFIX + "-javadoc.jar"]
        with self.assertRaises(ValueError):
            self.validate()

    def test_missing_signature(self):
        del self.files[PREFIX + ".jar.asc"]
        with self.assertRaises(ValueError):
            self.validate()

    def test_invalid_artifact_checksum(self):
        self.files[PREFIX + ".jar.sha256"] = b"0" * 64
        with self.assertRaisesRegex(ValueError, "Invalid checksum"):
            self.validate()

    def test_invalid_signature_checksum(self):
        self.files[PREFIX + ".jar.asc.sha512"] = b"0" * 128
        with self.assertRaisesRegex(ValueError, "Invalid checksum"):
            self.validate()

    def test_forged_signature(self):
        replace_artifact(self.files, PREFIX + ".jar.asc", b"-----BEGIN PGP SIGNATURE-----\nforged\n")
        files, artifacts, _ = self.validate()
        with self.assertRaisesRegex(ValueError, "Signature verification failed"):
            CENTRAL.verify_signatures(files, artifacts, self.key_id)

    def test_mismatched_signing_identity(self):
        with self.assertRaisesRegex(ValueError, "Unexpected signing identity"):
            CENTRAL.verify_signatures(self.files, self.artifacts, "0" * 40)

    def test_unsafe_zip_path(self):
        self.files["../secret"] = b"fixture"
        with self.assertRaisesRegex(ValueError, "Unsafe bundle path"):
            self.validate()

    def test_duplicate_bundle_entries(self):
        output = io.BytesIO()
        with zipfile.ZipFile(output, "w") as archive:
            archive.writestr(PREFIX + ".pom", b"one")
            archive.writestr(PREFIX + ".pom", b"two")
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            CENTRAL.validate_bundle(output.getvalue(), "com.akakata", "zea-tcp-java8", VERSION)

    def test_wrong_pom_version(self):
        replace_artifact(self.files, PREFIX + ".pom", self.files[PREFIX + ".pom"].replace(b"<version>0.8.5</version>", b"<version>0.8.6</version>"))
        with self.assertRaisesRegex(ValueError, "POM coordinate mismatch"):
            self.validate()

    def test_missing_license(self):
        import re
        pom = re.sub(rb"<licenses>.*?</licenses>", b"", self.files[PREFIX + ".pom"], flags=re.S)
        replace_artifact(self.files, PREFIX + ".pom", pom)
        with self.assertRaisesRegex(ValueError, "Missing POM metadata"):
            self.validate()

    def test_test_dependency_not_published(self):
        pom = self.files[PREFIX + ".pom"].replace(b"</project>", b"<dependencies><dependency><artifactId>junit-jupiter</artifactId></dependency></dependencies></project>")
        replace_artifact(self.files, PREFIX + ".pom", pom)
        with self.assertRaisesRegex(ValueError, "Test/compiler dependency"):
            self.validate()

    def test_java21_bytecode_rejected(self):
        with zipfile.ZipFile(io.BytesIO(self.files[PREFIX + ".jar"])) as archive:
            files = {name: archive.read(name) for name in archive.namelist()}
        name = next(name for name in files if name.endswith(".class"))
        files[name] = files[name][:6] + b"\x00\x41" + files[name][8:]
        replace_artifact(self.files, PREFIX + ".jar", archive_files(files))
        with self.assertRaisesRegex(ValueError, "Non-Java-8"):
            self.validate()

    def test_wrong_gradle_jvm_requirement(self):
        module = self.files[PREFIX + ".module"].replace(b'"org.gradle.jvm.version": 8', b'"org.gradle.jvm.version": 21')
        replace_artifact(self.files, PREFIX + ".module", module)
        with self.assertRaisesRegex(ValueError, "Java 8 compatibility"):
            self.validate()

    def test_snapshot_rejected(self):
        with self.assertRaises(ValueError):
            CENTRAL.release_prefix("com.akakata", "zea-tcp-java8", "0.8.5-SNAPSHOT")

    def test_upstream_artifact_rejected(self):
        with self.assertRaises(ValueError):
            CENTRAL.release_prefix("com.akakata", "zea-tcp", VERSION)

    def test_release_tag_mismatch(self):
        result = subprocess.run(["./gradlew", "--no-daemon", "--offline", "--console=plain", "validateCentralVersion"],
                                cwd=self.workspace, env={**self.env, "RELEASE_TAG": "v0.8.4"},
                                capture_output=True, text=True, timeout=60)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Release tag does not match", result.stdout + result.stderr)

    def test_remote_test_endpoint_rejected(self):
        with self.assertRaisesRegex(ValueError, "official Central API"):
            CENTRAL.Publisher(self.bundle, "com.akakata", "zea-tcp-java8", VERSION,
                              self.workspace / "bad-endpoint", endpoint="https://example.invalid/api")

    def test_resume_does_not_upload_twice(self):
        publisher = self.publisher("resume")
        publisher.publish()
        uploads = self.server.uploads
        publisher.publish()
        self.assertEqual(self.server.uploads, uploads)

    def test_different_bundle_journal_rejected(self):
        publisher = self.publisher("mismatch")
        publisher.publish()
        journal = json.loads(publisher.journal_path.read_text())
        journal["bundle_sha256"] = "0" * 64
        publisher.journal_path.write_text(json.dumps(journal))
        uploads = self.server.uploads
        with self.assertRaisesRegex(ValueError, "different bundle"):
            publisher.publish()
        self.assertEqual(self.server.uploads, uploads)

    def test_unknown_upload_outcome_not_retried(self):
        publisher = self.publisher("unknown")
        publisher.reports.mkdir(parents=True)
        publisher.journal_path.write_text(json.dumps({"bundle_sha256": hashlib.sha256(self.bundle.read_bytes()).hexdigest(),
                                                       "endpoint": self.endpoint, "deployment_id": None}))
        uploads = self.server.uploads
        with self.assertRaisesRegex(ValueError, "outcome is unknown"):
            publisher.publish()
        self.assertEqual(self.server.uploads, uploads)

    def test_failed_deployment_is_not_success(self):
        self.server.mode = "failed"
        publisher = self.publisher("failed")
        with self.assertRaisesRegex(RuntimeError, "validation failed"):
            publisher.publish()
        self.assertEqual(json.loads(publisher.journal_path.read_text())["state"], "FAILED")

    def test_timeout_retains_deployment_and_resumes(self):
        self.server.mode = "pending"
        publisher = self.publisher("timeout")
        publisher.timeout = 0.08
        with self.assertRaises(TimeoutError):
            publisher.publish()
        self.assertEqual(json.loads(publisher.journal_path.read_text())["deployment_id"], DEPLOYMENT)
        uploads = self.server.uploads
        self.server.mode = "success"
        publisher.timeout = 5
        publisher.publish()
        self.assertEqual(self.server.uploads, uploads)

    def test_authentication_failure_has_no_secrets_in_reports(self):
        self.server.mode = "unauthorized"
        publisher = self.publisher("unauthorized")
        with self.assertRaisesRegex(RuntimeError, "HTTP 401"):
            publisher.publish()
        for file in publisher.reports.glob("*"):
            text = file.read_text()
            self.assertNotIn(USERNAME, text)
            self.assertNotIn(PASSWORD, text)
            self.assertNotIn("PRIVATE KEY", text)


class ReportingResult(unittest.TextTestResult):
    """Write an independent report for every publication regression."""

    def startTest(self, test):
        self.started = time.monotonic()
        super().startTest(test)

    def record(self, test, status):
        CENTRAL.save_report(REPORTS / "cases", test.id(), {"test": test.id(), "status": status,
                            "seconds": round(time.monotonic() - getattr(self, 'started', time.monotonic()), 3)})

    def addSuccess(self, test):
        self.record(test, "PASS")
        super().addSuccess(test)

    def addFailure(self, test, error):
        self.record(test, "FAIL")
        super().addFailure(test, error)

    def addError(self, test, error):
        self.record(test, "ERROR")
        super().addError(test, error)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report-dir", type=Path, default=ROOT / ".internal-docs/publication-regression-reports")
    args = parser.parse_args()
    REPORTS = args.report_dir
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(PublicationTest)
    result = unittest.TextTestRunner(verbosity=2, resultclass=ReportingResult).run(suite)
    CENTRAL.save_report(REPORTS, "summary", {"status": "PASS" if result.wasSuccessful() else "FAIL",
                         "tests": result.testsRun, "failures": len(result.failures), "errors": len(result.errors),
                         "network_scope": "Loopback only; protected temporary signing key; no Central deployment"})
    raise SystemExit(0 if result.wasSuccessful() else 1)
