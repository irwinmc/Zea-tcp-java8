# Java 8 Migration Status

## Baseline and Scope

The fork release version is **0.8.6**. Its synchronized behavior baseline is
Zea TCP Transport **0.8.6**, commit
`64b474694bd98bcad3403ac27e00a0794f4b84a7`.

All upstream source, configuration, regression and guide changes since the initial
copy were inspected and synchronized before compatibility rewrites. Gradle build
settings and the porting-only policy were preserved. The new upstream CI workflow
was adapted to Java 8 and Gradle. Gradle publication tooling is separate from
framework behavior and preserves the existing compatibility scope.

This version permits synchronization and equivalent rewrites only. Framework
APIs, protocols, configuration, authentication, ordering, admission, backpressure,
shutdown and resource ownership follow the synchronized upstream contracts.

### Upstream Fixes Synchronized for 0.8.6

On 2026-11-13 the user explicitly authorized fixing three upstream issues and
synchronizing their Java 8 equivalents: empty-string frame decoding, predictable
random-provider failure fallback, and filesystem work on HTTP I/O loops. These
changes are included in the synchronized 0.8.6 baseline and introduce no independent
fork features. The previously published 0.8.5 artifacts are unchanged.

Filesystem workers and cleanup ownership use the same Java 8-compatible implementation
as upstream. Existing path/blank-string adaptations retain their equivalent behavior.
The complete inherited and port-compatibility suites remain active. Final acceptance
passes 359 cases on a real Java 8 runtime, including 14 synchronized upstream
regressions, plus strict main/private and test JavaDoc. All 252 framework classes
and 136 test classes use Java 8 bytecode; dependencies remain unchanged.

## Completed Adaptation

- Preserve the copied 0.8.5 lifecycle, listener readiness, blocking-task termination,
  HTTP file-transfer admission/deadlines and transport metrics behavior.
- Rewrite 13 record declarations as immutable final classes with matching accessors,
  constructor validation, equality, hashing, string forms and JSON properties.
- Replace local-variable inference, pattern matching, switch arrows and post-Java-8
  collection, stream, future, string, thread and filesystem APIs with equivalent
  contracts. Preserve single evaluation of pattern operands and completion observers
  that cannot cancel actual owned work.
- Keep immutable factory null/duplicate rejection separate from stream-to-list's
  null-permitting behavior. Preserve Unicode whitespace checks and file text errors.
- Replace virtual-thread operations with independent named daemon platform threads;
  preserve asynchronous cleanup, callback context and bounded completion observation.
- Select Java 8-compatible Jackson, Caffeine, Dagger processor and logging generations.
  Keep Java time and Optional handling, bean naming/order, trailing-token checks,
  null primitive rejection, UTF-8 surrogate behavior, legacy Gregorian date strings
  and Duration output aligned with the upstream defaults.
- Adapt report scripts, helper JAR paths and child-JVM classpaths to Gradle output.
  Keep the full inherited regression suite and isolated test JVMs.
- Configure strict main/private and test JavaDoc. Since JDK 8 lacks `-Werror`, the
  build captures JavaDoc warnings and fails the task when any warning appears.
- Add 21 byte expectations captured from the verified Java 21 upstream, including
  Unicode, bean naming, record value methods, dates before the Gregorian cutover,
  BCE/extended years, Java time, optionals and numbers. Add four adapter regressions.

`build.gradle` is the dependency source of truth; `AGENTS.md` defines the porting
constraints. The inherited history in `CHANGELOG.md` retains upstream release
provenance rather than presenting imported changes as new features in this fork.

## Verification

A real Azul Zulu JDK **1.8.0_504**, verified against its vendor SHA-256, was used for
compilation and test execution on macOS ARM64. The Gradle wrapper is pinned to
8.14.5 and its official wrapper/distribution checksums are verified.

The original isolated Java 21 0.8.5 baseline passed all **320** inherited cases and strict main/test
JavaDoc. The Java 8 port preserves those cases and adds **25** compatibility cases.
Every verification run, failure and individual JUnit outcome is retained locally;
configuration-only checks are never presented as compatibility acceptance.

Release 0.8.5 also passes all **345** cases and strict main/test JavaDoc on
Temurin **8.0.512+9**, Linux x86-64. All **29** publishing regressions pass.
Maven Central confirms `PUBLISHED`; the five published artifacts and their
signatures match the verified release bytes. Independent downloads confirm all
247 library classes use Java 8 bytecode and all five signatures are valid.

Run the complete acceptance command with a Java 8 toolchain:

```bash
./gradlew --no-daemon --console=plain clean build testJavadoc
```

The build creates the library, source and JavaDoc archives. Regression coverage
includes real loopback TCP/WebSocket traffic, resource ownership, rejection,
backpressure, HTTP file handles, health and shutdown races. This verifies the
framework's existing NIO transport paths on the tested JDK/platform.

Netty 4.2 retains optional newer-JDK integrations in its aggregate dependencies,
including io_uring and internal VarHandle queues. The framework uses its existing
NIO transport and Netty's Java 8-compatible queue paths. Optional newer-JDK
transports are not part of this port's runtime compatibility claim.

## Work Outside This Migration

- GitHub CI is configured for Java 8 branch pushes and pull requests. Remote
  results are available in the repository's Actions history.
- Java 8 performance/capacity, other operating systems and additional JDK vendors
  require their own measurements; the Java 21 benchmark results do not establish
  Java 8 capacity or allocation costs.
- No remote test machine or capacity test was used for this adaptation.
- Gradle publication supplies signed library, sources, JavaDoc, POM and module
  metadata with MD5, SHA-1, SHA-256 and SHA-512 checksums. It uses the Central
  Publisher API with a resumable deployment journal and downloaded-byte checks.
  Local publisher regressions use only loopback and a temporary signing identity.
- The Java 8 port has its own [source repository](https://github.com/irwinmc/Zea-tcp-java8).
  The POM identifies this repository and retains the synchronized upstream URL,
  version and commit as provenance properties. Full port sources ship in the sources JAR.

No independent framework functionality or optimization is included.
