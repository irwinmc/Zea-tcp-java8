#!/usr/bin/env python3
"""Verify and publish signed Gradle bundles through the Central Publisher API."""

import argparse
import base64
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import struct
import subprocess
import tempfile
import time
from urllib.parse import urlencode, urlsplit
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[1]
ENDPOINT = "https://central.sonatype.com/api/v1/publisher"
CENTRAL = "https://repo.maven.apache.org/maven2"
ALGORITHMS = ("md5", "sha1", "sha256", "sha512")
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def save_report(directory, name, record):
    """Persist public evidence without credentials or raw authenticated responses."""
    directory.mkdir(parents=True, exist_ok=True)
    for extension, content in [("json", json.dumps(record, indent=2) + "\n"),
                               ("txt", "\n".join(f"{k}: {v}" for k, v in record.items()) + "\n")]:
        target = directory / f"{name}.{extension}"
        temporary = target.with_suffix(target.suffix + ".tmp")
        temporary.write_text(content, encoding="utf-8")
        temporary.replace(target)


def load_credentials(path):
    """Read Portal tokens from the environment or an ignored local JSON file."""
    local = json.loads(path.read_text(encoding="utf-8")) if path.is_file() else {}
    values = {name: os.environ.get(name) or local.get(name, "") for name in
              ("MAVEN_CENTRAL_USERNAME", "MAVEN_CENTRAL_PASSWORD", "GPG_KEY_ID")}
    for name in ("MAVEN_CENTRAL_USERNAME", "MAVEN_CENTRAL_PASSWORD"):
        value = values[name]
        if not isinstance(value, str) or not value or any(ord(c) < 32 for c in value):
            raise ValueError(f"Missing or malformed {name}; configure a Central Portal user token")
    return values


def release_prefix(group, artifact, version):
    """Validate coordinates before using them in paths, XML or HTTP requests."""
    if group != "com.akakata" or artifact != "zea-tcp-java8":
        raise ValueError("Publication is restricted to com.akakata:zea-tcp-java8")
    if not re.fullmatch(r"[0-9][0-9A-Za-z._-]*", version) or version.upper().endswith("-SNAPSHOT"):
        raise ValueError("A literal release version is required")
    return f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}"


def validate_bundle(content, group, artifact, version):
    """Reject wrong coordinates, incomplete signatures, metadata and invalid archives."""
    prefix = release_prefix(group, artifact, version)
    with zipfile.ZipFile(io.BytesIO(content)) as archive:
        entries = [entry for entry in archive.infolist() if not entry.is_dir()]
        names = [entry.filename for entry in entries]
        if len(set(names)) != len(names):
            raise ValueError("Duplicate bundle entries")
        if sum(entry.file_size for entry in entries) > 100 * 1024 * 1024:
            raise ValueError("Unexpectedly large publication bundle")
        for name in names:
            path = PurePosixPath(name)
            if path.is_absolute() or ".." in path.parts or "\\" in name:
                raise ValueError("Unsafe bundle path")
        files = {name: archive.read(name) for name in names}
    artifacts = [prefix + suffix for suffix in (".jar", ".pom", "-sources.jar", "-javadoc.jar")]
    if prefix + ".module" in files:
        artifacts.append(prefix + ".module")
    expected = {name + suffix for artifact_name in artifacts
                for name in (artifact_name, artifact_name + ".asc")
                for suffix in ("", *("." + algorithm for algorithm in ALGORITHMS))}
    if set(files) != expected:
        raise ValueError("Unexpected or missing bundle files: " + ", ".join(sorted(set(files) ^ expected)))
    for name in artifacts:
        if not files[name + ".asc"].startswith(b"-----BEGIN PGP SIGNATURE-----"):
            raise ValueError("Invalid detached signature: " + name)
        for signed in (name, name + ".asc"):
            for algorithm in ALGORITHMS:
                digest = hashlib.new(algorithm, files[signed]).hexdigest().encode("ascii")
                if files[signed + "." + algorithm].strip() != digest:
                    raise ValueError("Invalid checksum: " + signed + "." + algorithm)
    pom = ET.fromstring(files[prefix + ".pom"])
    for key, value in [("groupId", group), ("artifactId", artifact), ("version", version)]:
        if pom.findtext("m:" + key, namespaces=NS) != value:
            raise ValueError("POM coordinate mismatch: " + key)
    for path in ("name", "description", "url", "licenses/license/name", "licenses/license/url",
                 "developers/developer/name", "developers/developer/email", "scm/connection",
                 "scm/developerConnection", "scm/url"):
        if not (pom.findtext("m:" + path.replace("/", "/m:"), namespaces=NS) or "").strip():
            raise ValueError("Missing POM metadata: " + path)
    for dependency in pom.findall("m:dependencies/m:dependency", NS):
        name = dependency.findtext("m:artifactId", namespaces=NS) or ""
        if "junit" in name or name == "dagger-compiler":
            raise ValueError("Test/compiler dependency leaked into the public POM")
    with zipfile.ZipFile(io.BytesIO(files[prefix + ".jar"])) as jar:
        classes = [name for name in jar.namelist() if name.endswith(".class")]
        if not classes:
            raise ValueError("Empty library JAR")
        for name in classes:
            bytecode = jar.read(name)
            if len(bytecode) < 8 or bytecode[:4] != b"\xca\xfe\xba\xbe" or struct.unpack(">H", bytecode[6:8])[0] != 52:
                raise ValueError("Non-Java-8 library bytecode: " + name)
    for suffix, predicate in [("-sources.jar", lambda name: name.endswith(".java")),
                              ("-javadoc.jar", lambda name: name == "index.html")]:
        with zipfile.ZipFile(io.BytesIO(files[prefix + suffix])) as jar:
            if not any(predicate(name) for name in jar.namelist()):
                raise ValueError("Empty source or JavaDoc archive")
    if prefix + ".module" in files:
        module = json.loads(files[prefix + ".module"])
        component = module.get("component", {})
        if any(component.get(k) != v for k, v in [("group", group), ("module", artifact), ("version", version)]):
            raise ValueError("Gradle metadata coordinate mismatch")
        runtime = [variant for variant in module.get("variants", [])
                   if variant.get("attributes", {}).get("org.gradle.category") == "library"
                   and variant.get("attributes", {}).get("org.gradle.usage") in ("java-api", "java-runtime")]
        if not runtime or any(variant.get("attributes", {}).get("org.gradle.jvm.version") != 8 for variant in runtime):
            raise ValueError("Gradle metadata does not declare Java 8 compatibility")
    return files, artifacts, len(classes)


def verify_signatures(files, artifacts, key_id=None):
    """Cryptographically verify each artifact using the existing public keyring."""
    fingerprints = set()
    with tempfile.TemporaryDirectory(prefix="zea-central-signatures-") as temporary:
        directory = Path(temporary)
        for index, name in enumerate(artifacts):
            artifact = directory / f"artifact-{index}"
            signature = directory / f"artifact-{index}.asc"
            artifact.write_bytes(files[name])
            signature.write_bytes(files[name + ".asc"])
            result = subprocess.run(["gpg", "--batch", "--status-fd", "1", "--verify", str(signature), str(artifact)],
                                    capture_output=True, text=True, timeout=30)
            lines = [line.split() for line in result.stdout.splitlines() if line.startswith("[GNUPG:] VALIDSIG ")]
            if result.returncode or len(lines) != 1:
                raise ValueError("Signature verification failed: " + name)
            primary = lines[0][-1].upper()
            if key_id and not primary.endswith(key_id.upper()) and not lines[0][2].upper().endswith(key_id.upper()):
                raise ValueError("Unexpected signing identity: " + name)
            fingerprints.add(primary)
    if len(fingerprints) != 1:
        raise ValueError("Artifacts use different signing identities")
    return sorted(fingerprints)


def curl_request(url, method="GET", token=None, bundle=None):
    """Use system TLS trust; keep credentials out of process arguments and logs."""
    def quote(value):
        if "\n" in value or "\r" in value:
            raise ValueError("Invalid curl configuration value")
        return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'

    with tempfile.TemporaryDirectory(prefix="zea-central-http-") as temporary:
        response = Path(temporary) / "response"
        config = ["silent", "show-error", "max-time = 90", "connect-timeout = 15",
                  "url = " + quote(url), "request = " + quote(method),
                  "output = " + quote(str(response)), 'write-out = "%{http_code}"']
        if method == "HEAD":
            config.append("head")
        if token:
            config.append("header = " + quote("Authorization: Bearer " + token))
        if bundle:
            config.append("form = " + quote("bundle=@" + str(bundle)))
        result = subprocess.run(["curl", "--config", "-"], input="\n".join(config) + "\n",
                                capture_output=True, text=True, timeout=100)
        if result.returncode:
            raise RuntimeError("HTTP transport failed; curl exit " + str(result.returncode))
        return int(result.stdout), response.read_bytes() if response.exists() else b""


class Publisher:
    """Upload once, resume by deployment ID, and require confirmed publication."""

    def __init__(self, bundle, group, artifact, version, reports, endpoint=ENDPOINT,
                 credentials_file=None, timeout=1800, poll_interval=5):
        self.bundle = bundle.resolve()
        self.group, self.artifact, self.version = group, artifact, version
        self.prefix = release_prefix(group, artifact, version)
        self.reports = reports
        self.endpoint = endpoint.rstrip("/")
        parsed = urlsplit(self.endpoint)
        if self.endpoint != ENDPOINT and not (parsed.scheme == "http" and parsed.hostname in ("127.0.0.1", "localhost", "::1")):
            raise ValueError("Only the official Central API or a loopback test publisher is permitted")
        self.credentials_file = credentials_file or ROOT / ".internal-docs/central-publishing.local.json"
        self.timeout, self.poll_interval = timeout, poll_interval
        self.journal_path = reports / f"deployment-{artifact}-{version}.json"
        self.journal = None

    def save(self):
        """Atomically persist non-secret upload identity and observed state."""
        self.reports.mkdir(parents=True, exist_ok=True)
        temporary = self.journal_path.with_suffix(".json.tmp")
        temporary.write_text(json.dumps(self.journal, indent=2) + "\n")
        temporary.replace(self.journal_path)

    def verify(self):
        """Validate the exact signed bytes before any authenticated upload."""
        started = time.monotonic()
        content = self.bundle.read_bytes()
        files, artifacts, classes = validate_bundle(content, self.group, self.artifact, self.version)
        local = json.loads(self.credentials_file.read_text()) if self.credentials_file.is_file() else {}
        key_id = os.environ.get("GPG_KEY_ID") or local.get("GPG_KEY_ID")
        fingerprints = verify_signatures(files, artifacts, key_id)
        record = {"status": "PASS", "coordinates": f"{self.group}:{self.artifact}:{self.version}",
                  "bundle_sha256": hashlib.sha256(content).hexdigest(), "bundle_entries": len(files),
                  "signed_artifacts": len(artifacts), "java8_classes": classes,
                  "signing_fingerprints": fingerprints, "seconds": round(time.monotonic() - started, 3)}
        save_report(self.reports, "bundle-verification", record)
        print("Verified signed Java 8 bundle: " + record["coordinates"], flush=True)
        return record, files, artifacts

    def publish(self):
        """Never repeat an upload whose outcome is unknown or already journalled."""
        record, files, artifacts = self.verify()
        credentials = load_credentials(self.credentials_file)
        token = base64.b64encode((credentials["MAVEN_CENTRAL_USERNAME"] + ":" +
                                 credentials["MAVEN_CENTRAL_PASSWORD"]).encode()).decode()
        if self.journal_path.is_file():
            self.journal = json.loads(self.journal_path.read_text())
            if self.journal.get("bundle_sha256") != record["bundle_sha256"] or self.journal.get("endpoint") != self.endpoint:
                raise ValueError("Existing deployment journal describes a different bundle or endpoint")
            if not self.journal.get("deployment_id"):
                raise ValueError("Previous upload outcome is unknown; inspect Central before any new upload")
        else:
            if self.endpoint == ENDPOINT:
                status, _ = curl_request(CENTRAL + "/" + self.prefix + ".pom", "HEAD")
                save_report(self.reports, "central-immutability", {"status": "PASS" if status == 404 else "FAIL", "http_status": status})
                if status != 404:
                    raise ValueError("Version already exists or its absence could not be confirmed; publication refused")
            self.journal = {"bundle_sha256": record["bundle_sha256"], "coordinates": record["coordinates"],
                            "endpoint": self.endpoint, "state": "UPLOAD_STARTED", "deployment_id": None}
            self.save()
            status, body = curl_request(self.endpoint + "/upload?" + urlencode({"name": record["coordinates"],
                                        "publishingType": "AUTOMATIC"}), "POST", token, self.bundle)
            if status not in (200, 201):
                self.journal["state"] = "UPLOAD_REJECTED" if status in (400, 401, 403) else "UPLOAD_OUTCOME_UNKNOWN"
                self.journal["http_status"] = status
                self.save()
                raise RuntimeError("Central upload failed with HTTP " + str(status))
            deployment = body.decode().strip()
            if not re.fullmatch(r"[0-9a-fA-F-]{36}", deployment):
                raise ValueError("Upload response did not contain a valid deployment ID; inspect Central")
            self.journal.update({"deployment_id": deployment, "state": "PENDING"})
            self.save()
            save_report(self.reports, "central-upload", {"status": "PASS", "deployment_id": deployment,
                        "publishing_type": "AUTOMATIC", "bundle_sha256": record["bundle_sha256"]})
        deadline = time.monotonic() + self.timeout
        previous = None
        while time.monotonic() < deadline:
            status, body = curl_request(self.endpoint + "/status?" + urlencode({"id": self.journal["deployment_id"]}), "POST", token)
            if status != 200:
                raise RuntimeError("Central status request failed with HTTP " + str(status) + "; deployment journal retained")
            state = json.loads(body).get("deploymentState")
            if state not in ("PENDING", "VALIDATING", "VALIDATED", "PUBLISHING", "PUBLISHED", "FAILED"):
                raise ValueError("Unrecognized Central deployment state")
            self.journal["state"] = state
            self.save()
            if state != previous:
                print("Central deployment " + self.journal["deployment_id"] + ": " + state, flush=True)
                previous = state
            if state == "FAILED":
                save_report(self.reports, "central-status", {"status": "FAIL", "deployment_id": self.journal["deployment_id"], "state": state})
                raise RuntimeError("Central validation failed; inspect the deployment in the Portal")
            if state == "PUBLISHED":
                save_report(self.reports, "central-status", {"status": "PASS", "deployment_id": self.journal["deployment_id"], "state": state})
                if self.endpoint == ENDPOINT:
                    self.verify_downloads(files, artifacts)
                return self.journal
            time.sleep(self.poll_interval)
        save_report(self.reports, "central-status", {"status": "TIMEOUT", "deployment_id": self.journal["deployment_id"], "state": self.journal["state"]})
        raise TimeoutError("Publication observation timed out; resume with the same bundle and journal")

    def verify_downloads(self, files, artifacts):
        """Confirm public repository bytes match the locally verified signed artifacts."""
        deadline = time.monotonic() + min(self.timeout, 300)
        pending = [name for artifact in artifacts for name in (artifact, artifact + ".asc")]
        verified = []
        while pending and time.monotonic() < deadline:
            for name in list(pending):
                status, content = curl_request(CENTRAL + "/" + name)
                if status == 404:
                    continue
                if status != 200 or content != files[name]:
                    raise ValueError("Published artifact unavailable or differs from the verified bundle: " + name)
                verified.append({"path": name, "sha256": hashlib.sha256(content).hexdigest()})
                pending.remove(name)
            if pending:
                time.sleep(self.poll_interval)
        save_report(self.reports, "central-downloads", {"status": "PASS" if not pending else "TIMEOUT", "verified": verified, "pending": pending})
        if pending:
            raise TimeoutError("Central confirms PUBLISHED but repository propagation is incomplete; journal retained")


def main():
    """Keep verification separate from explicitly requested external publication."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=("verify", "publish"))
    parser.add_argument("--bundle", type=Path, required=True)
    parser.add_argument("--group", default="com.akakata")
    parser.add_argument("--artifact", default="zea-tcp-java8")
    parser.add_argument("--version", required=True)
    parser.add_argument("--report-dir", type=Path, required=True)
    parser.add_argument("--credentials-file", type=Path)
    parser.add_argument("--endpoint", default=ENDPOINT)
    parser.add_argument("--timeout", type=int, default=1800)
    parser.add_argument("--poll-interval", type=float, default=5)
    args = parser.parse_args()
    try:
        publisher = Publisher(args.bundle, args.group, args.artifact, args.version, args.report_dir,
                              args.endpoint, args.credentials_file, args.timeout, args.poll_interval)
        publisher.verify() if args.operation == "verify" else publisher.publish()
    except (OSError, ValueError, RuntimeError, TimeoutError, zipfile.BadZipFile, ET.ParseError) as error:
        # Error messages contain only controlled metadata, never credentials or HTTP bodies.
        save_report(args.report_dir, "publication-error", {"status": "FAIL", "type": type(error).__name__, "message": str(error)})
        parser.exit(1, str(error) + "\n")


if __name__ == "__main__":
    main()
