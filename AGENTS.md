# Project Instructions

These instructions apply to this project and its subdirectories. Preserve
unrelated user work and follow the user's explicitly requested migration stage.

## Porting and Publication Only

- This project is a Java 8 port of Zea TCP Transport. The behavior baseline is
  upstream version 0.8.6, commit `64b474694bd98bcad3403ac27e00a0794f4b84a7`.
  The fork version is 0.8.6. User-requested upstream synchronization imports
  that baseline; independent functionality changes remain prohibited.
- This fork permits only behavior-preserving rewrites and migration work:
  Java 8 syntax, equivalent JDK/library API adaptations, Gradle build/publication metadata,
  dependency compatibility, build/test paths and accurate migration documentation.
- Do not add or remove features, change observable behavior, fix unrelated bugs,
  optimize algorithms, redesign modules or perform cleanup unrelated to compatibility.
  This prohibition applies to every production path, dependency replacement, test,
  example and configuration file, including changes described as improvements.
- Preserve public packages, method names and signatures, message bytes and limits,
  configuration keys and defaults, authentication decisions, metrics meanings,
  error outcomes, ordering, backpressure, admission, lifecycle and resource ownership.
- Adapt language-generated behavior faithfully. Replacing a record requires
  equivalent construction, accessors, equality, hashing and string representation.
  Collection replacements preserve immutability, null handling, duplicate handling
  and iteration behavior relevant to the existing contract.
- If an incompatible API has no equivalent replacement, record the blocker and
  leave the behavior intact. Do not hide a gap by disabling a feature or relaxing
  tests, limits, security or cleanup.

## Current Stage: Behavior-Preserving Java 8 Adaptation

- The user has authorized synchronizing all upstream changes and beginning source
  and dependency adaptation. The bootstrap-only source freeze is superseded.
- Preserve the synchronized upstream behavior while adapting Java syntax, JDK APIs,
  dependency versions, test tooling and Gradle infrastructure to Java 8.
- Keep an upstream snapshot and migration reports for every synchronization and
  verification run. Preserve local port-specific build and policy changes.
- Treat configuration success separately from compilation and runtime equivalence.
  Use a real Java 8 compiler/test runtime and keep the full inherited suite active.
- Read [migration status](MIGRATION.md) before resuming source or dependency work.

## Project and Build

- Use the Gradle wrapper. Read `build.gradle`, `gradle.properties` and
  `settings.gradle` for build settings and dependency versions.
- Use a Java 8 compiler and test runtime for compatibility acceptance. A newer
  JDK may run Gradle; it must not substitute for Java 8 compatibility verification.
- Keep the existing Dagger assembly and classpath properties loader. Application
  implementations are selected through `ServerComponents`; the normal application
  owns one Game. Do not add room management or another assembly mechanism.
- Preserve `Session.onEvent()`, `Game.send()` and `MessageSender.sendMessage()`.
  Keep Java packages compatible while the separate artifact is `zea-tcp-java8`.
- Keep dependencies pinned. Replace them only when required for Java 8 and verify
  equivalent behavior. Do not reintroduce Agrona or JVM module-access flags.

## Java Style

- Use four spaces. Every conditional and loop body uses braces and multiline layout,
  including single statements and each `else if` branch.
- Keep methods, `try`/`catch`, synchronized blocks and statement lambdas multiline.
  Separate members and logical steps with blank lines.
- Use `//` for ordinary comments and standard multiline JavaDoc for documentation.
  Attach JavaDoc to its declaration; document parameters, results and exceptions.
- Write new comments, JavaDoc and documentation in English. Apply the same style
  to production code, tests and executable examples.

## Configuration and Security Contracts

- Preserve exact configuration keys. All six listener settings remain required,
  including disabled listeners: `tcp.port`, `tcp.bind.host`, `http.port`,
  `http.bind.host`, `web.socket.port` and `web.socket.bind.host`.
- Preserve rejection of blank, malformed or out-of-range settings and unresolvable
  addresses. Port zero retains ephemeral-port behavior. Optional defaults apply
  only to absent settings; configuration errors identify the offending key.
- Keep authentication closed by default and anonymous mode explicitly opt-in.
  Preserve externally supplied token keys and expiry; never add fixed secrets,
  expose credentials or copy local SSH configuration into this project.
- Preserve the bundled HTTP loopback address and existing listener/TLS behavior.

## Concurrency and Ownership Contracts

- Preserve Session ordering, the serial Game queue and session-instance handler
  identity. Keep broadcasts explicitly addressed and retain fan-out encoding reuse.
- Session closure releases its own work and registrations without closing shared
  executors or removing a replacement session for the same credential.
- Preserve context ownership, partial-startup cleanup, idempotent closure, bounded
  shutdown observation and actual completion accounting after an observation timeout.
- Keep blocking work off I/O, Session and Game executors. Retain bounded offloading,
  detached inputs and ordered result application with session/sequence validation.
- Preserve native reference counting and cleanup on completion, rejection,
  cancellation and closure. Counts and bytes include executing work until it exits;
  rejected work never runs inline on the submitting thread.
- Consult [event contracts](docs/EVENT_LOOP.md) and [lifecycle](docs/LIFECYCLE.md)
  when adapting executors, buffers or closure. Consult [configuration](docs/CONFIGURATION.md),
  [components/security](docs/CORE_COMPONENTS.md) and [blocking work](docs/SLOW_BUSINESS.md)
  when adapting those contracts. Copied guides describe baseline behavior until
  their build/runtime instructions are explicitly ported.

## Verification and Reports

- Bootstrap acceptance consists of copy hashes, unchanged source/resources,
  wrapper checksum verification and Gradle configuration/task-graph checks.
  Save a report for every verification run, including failures and known omissions,
  under Git-ignored `.internal-docs/migration-reports/`.
- Once source migration is explicitly requested, preserve the existing regression
  suite and add only meaningful equivalence checks for compatibility rewrites.
  Do not delete assertions, skip failing cases or reset terminal global state.
- Lifecycle tests use isolated JVMs. Compile before generating strict main/private
  and test JavaDoc so Dagger-generated types are available. After source migration:

```bash
./gradlew --no-daemon --console=plain clean build testJavadoc
git diff --check
```

- JavaDoc warnings and errors fail validation. Do not edit generated Dagger sources
  or build outputs. For IDE races, use a temporary snapshot excluding `.git`,
  `.gradle`, `build`, `.idea`, `.internal-docs` and local secret-bearing files.
- Verify production/test dependency bytecode and the complete runtime classpath
  against Java 8 before claiming compatibility. Do not publish the bootstrap.

## Documentation and Git

- Keep the project README and `MIGRATION.md` honest about completed and remaining
  work. Record only porting and publication changes in this fork's changelog; upstream release
  history describes inherited baseline behavior, not new features added here.
- Keep public guides focused on usage and supported contracts. Keep maintainer
  archives, deployment budgets, internal benchmarks and credentials Git-ignored.
  Public guides must not link to local private notes.
- Inspect the working tree before edits. Do not initialize a remote, commit, push
  or publish without authorization for that action. Never reuse the upstream
  Maven publication workflow for this Gradle fork or overwrite its artifacts or release tags.
- Central publication must pass Java 8 tests, strict main/test JavaDoc, signed bundle
  validation and local publisher regressions. Use `publishCentral` and preserve its
  deployment journal; success requires `PUBLISHED` and matching downloaded artifacts.
  Signing keys and Portal tokens come from the environment or Git-ignored local
  configuration. Maintainers may consult `.internal-docs/PUBLISHING.md` when present.
