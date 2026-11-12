# Changelog

This fork records upstream synchronization and behavior-preserving Java 8 migration.
Inherited release history below describes the Java 21 baseline, not independent
features introduced by the port.

## 0.8.5 (Java 8 Port, Publication Pending)

- Prepare `com.akakata:zea-tcp-java8:0.8.5` without changing the synchronized
  framework sources, configuration, dependencies or behavior.
- Add Gradle Maven publication and signing, source/JavaDoc archives, Java 8 module
  metadata and four checksum algorithms. Preserve upstream license, developer
  information and the exact synchronized SCM commit.
- Validate signed bundles and exercise publication against an isolated local
  publisher. Persist deployment IDs for safe resumption and require `PUBLISHED`
  plus matching public repository artifacts before reporting release completion.
- Preserve individual reports for the inherited and compatibility tests and the
  publication regressions. Keep credentials and maintainer runbooks Git-ignored.

## 0.8.4 (Unreleased Java 8 Port)

- Synchronize upstream 0.8.5, including listener readiness, static-file admission
  and deadlines, and actual blocking-worker termination tracking.
- Replace active Maven build/CI paths with a checksum-pinned Gradle wrapper,
  Java 8 toolchains, Dagger processing and isolated JUnit execution.
- Rewrite records, local inference, pattern matching and newer JDK APIs with
  equivalent construction, value, cancellation, collection and ownership contracts.
- Adapt dependency generations required for Java 8. Retain inherited JSON defaults
  and validate application payload bytes against captured upstream outputs.
- Add 25 equivalence regression cases while retaining all 320 inherited cases.
- Keep publication inactive; this version adds no independent framework features.

## Inherited Upstream History

These notes retain the upstream Java 21 release descriptions, Maven coordinates
and validation history. See [migration status](MIGRATION.md) for this fork.

### Upstream 0.8.5

Version 0.8.5 improves listener readiness, blocking-worker shutdown and HTTP file
transfers while preserving ordered Session and Game delivery.

#### Changes

- Require all selected listener channels to be active before publishing readiness.
  Sample listener availability without acquiring the blocking startup monitor or
  coupling HTTP health presentation to the server manager.
- Expose actual blocking-executor termination through `closeAsync()`. Context shutdown
  remains `STOPPING` until those workers exit, even after an observation timeout.
  Running operations retain their task and byte reservations until they return.
- Reserve static-file transfers before opening a file. Bound both per-connection and
  context-wide reservations, and impose an absolute transfer deadline that subsequent
  requests cannot renew. Close handles on completion, rejection, cancellation and
  connection closure; return admission only after the handle closes.
- Expose pending-file, rejection and timeout metrics and worker closed/terminated gauges.
- Run Java 21 regression tests, strict main/test JavaDoc, signed-publication fixtures
  and whitespace checks on branch pushes and pull requests with read-only CI permissions.
- Add lifecycle, file-transfer, admission and configuration regressions and update
  English API and application documentation.

#### Migration from 0.8.4

Update the dependency to `com.akakata:zea-tcp:0.8.5`. Java 21, dependency versions,
event API names and network frame formats remain unchanged.

Custom `Server` implementations must override the nonblocking `isListening()`
contract to report an active listener; its default is unavailable. Disabled listeners
are excluded from readiness requirements.

`BlockingTaskExecutor.close()` now throws `ShutdownTimeoutException` when a positive
wait expires. A zero `business.blocking.shutdown.millis` requests shutdown without
waiting. Use `closeAsync()` to observe actual exit in either case; cancelling its
returned observation does not cancel cleanup.

Static-file defaults are two pending transfers per connection, 128 across the
context and a 30-second absolute transfer deadline. Configure the three
`http.file.*` settings in [configuration](docs/CONFIGURATION.md). Admission failure
closes the requesting connection without inserting a response into a pending
pipelined body. No new dependencies or JVM module-access flags are required.

#### Validation

The release checks include 320 regression cases, with 19 new cases for listener
availability, actual worker exit, HTTP file-transfer admission and deadlines, and
configuration validation. Strict production and test JavaDoc and seven local
signed-publication fixtures also pass. Branch pushes and pull requests run the
same validation in CI; release publication runs through the separate Central workflow.

Maven coordinates: `com.akakata:zea-tcp:0.8.5`.

### Upstream 0.8.4

Version 0.8.4 hardens shutdown, HTTP resource ownership, operational monitoring
and static-file boundaries while preserving ordered Session and Game events.

#### Changes

- Split context, network and dispatcher shutdown requests from completion observation.
  Avoid monitor-held waits, worker self-waits and reentrant cleanup cycles. Bound
  external waits and continue initiating cleanup after stalled or failing stages.
  Track partial assembly independently and close resources returned after shutdown.
- Preserve active callback payloads and resource credits until actual completion,
  including after a shutdown deadline expires.
- Release HTTP response source buffers on success, write rejection and copy failure.
- Record real socket lifetimes, encoded bytes, decoded application messages,
  successful outbound messages and failure callbacks in the default pipelines.
- Replace uptime and cumulative-error readiness heuristics with lifecycle and named
  resource checks. Return JSON with HTTP 503 when readiness is unavailable; detect
  closed resources and outstanding events older than the configured health deadline.
- Reject static traversal, hidden paths and symbolic links. Open files without
  following the final link, use directory handles where supported, and close acquired
  file handles on failed transfers and conditional responses.
- Add 51 regression cases and English lifecycle, monitoring, configuration and
  JavaDoc contracts. Separate resource ownership, shutdown coordination, health
  policy and static-resource resolution from their HTTP and application presenters.

#### Migration from 0.8.3

1. Update the dependency to `com.akakata:zea-tcp:0.8.4`. Existing event and sending
   method names, ordered delivery and network frame formats remain compatible.
   Java 21 remains required and dependency versions are unchanged.
2. External context `close()` waits up to `server.shutdown.timeout.millis`, default
   5000, and throws `ShutdownTimeoutException` if cleanup is unfinished. Observe
   `closeAsync()` for actual completion. Managed workers and lifecycle callbacks
   request closure without waiting; an expired wait never releases active resources.
3. Configure probes for HTTP 503 and JSON `DOWN` responses during unavailable
   readiness. Readiness now requires successful listener startup and named resource
   checks; `/health/live` remains live during draining. Review
   `health.maximum.event.delay.millis`, default 5000, for application event deadlines.
   Custom dispatchers implement `isHealthy(long)` and custom Games report shutdown.
4. Default pipelines now record transport and decoded business traffic. Remove
   duplicate application instrumentation from the shared `ServerMetrics` collector,
   or use an independent collector. Errors count failure callbacks, not unique
   failed messages.
5. Static-file roots and their directory structure must remain application-controlled.
   Symlinks, hidden paths and traversal are rejected, including links pointing inside
   the root. Deploy actual files beneath the configured static directory.

See [lifecycle](docs/LIFECYCLE.md), [monitoring](docs/MONITORING.md),
[configuration](docs/CONFIGURATION.md) and [event ownership](docs/EVENT_LOOP.md)
for the complete contracts and filesystem-specific boundary guarantees.

#### Validation

The release checks include 301 regression cases and strict production and test
JavaDoc. New coverage includes shutdown races and reentry, late factory ownership,
observation deadlines, HTTP buffer and file-handle cleanup, health policy, static
path boundaries and real TCP/WebSocket traffic counters. GitHub Actions validates
and signs release artifacts before Maven Central publication.

Maven coordinates: `com.akakata:zea-tcp:0.8.4`.

### Upstream 0.8.3

Version 0.8.3 improves bounded socket backpressure, reduces unicast and JSON
encoding allocations, and preserves ordered Session and Game delivery.

#### Changes

- Pause bounded socket drains when a channel becomes unwritable, flush admitted
  batches early, and resume from channel writability events. Bursts and slow
  readers retain FIFO ordering without treating a soft write watermark as a hard
  resource-budget failure.
- Keep one write deadline from admission through completion. Release pending
  payloads and reservations on completion, rejection, cancellation and closure,
  including stopped or rejected event-loop cleanup. Expose write-timeout counts
  through JSON and Prometheus flow-control metrics.
- Optimize explicit single-recipient sends by skipping identity deduplication and
  shared fan-out encoding leases. Preserve Game queue ordering, Session admission,
  overload handling and native payload ownership. Multiple-recipient sends retain
  canonical encoding reuse.
- Stream JSON directly into a bounded native application frame, avoiding a
  whole-payload heap array. Preserve the wire format, UTF-8 bytes, null controls
  and 65535-byte application-frame boundary. Release partial output on serializer
  failure or overflow, retaining the frame-limit exception.
- Add regressions for unicast ownership, mixed send ordering, burst and slow-reader
  backpressure, exact JSON bytes, protocol boundaries and failed serialization.

#### Migration from 0.8.2

1. Update the dependency to `com.akakata:zea-tcp:0.8.3`. Public event and sending
   APIs and network frame formats remain compatible. Java 21 remains required.
2. Retain the existing task, payload and encoded-byte limits. Soft channel
   unwritability now pauses admitted writes; hard budget rejection and the
   admitted-write deadline still protect resources. Review these budgets against
   representative traffic rather than increasing them to hide overload.
3. Continue treating published event data as immutable and following the
   documented native-buffer ownership contract. Unicast follows the normal
   Game-to-Session-to-socket retention path; fan-out keeps its shared encoding lease.
4. Keep authentication and required listener endpoint configuration from 0.8.2.
   Validate resource budgets with the application's actual handlers and traffic.

See [configuration](docs/CONFIGURATION.md), [event ownership](docs/EVENT_LOOP.md),
[monitoring](docs/MONITORING.md) and [slow business operations](docs/SLOW_BUSINESS.md)
for the complete application contracts.

#### Validation

The release checks include 250 regression cases and strict production and test
JavaDoc. Coverage includes ordering, backpressure, native-buffer ownership,
protocol boundaries and real TCP/WebSocket behavior. GitHub Actions validates
the release artifacts before publication.

Maven coordinates: `com.akakata:zea-tcp:0.8.3`.

### Upstream 0.8.2

Version 0.8.2 adds application component injection, secure authentication defaults
and bounded resource management while preserving ordered Session and Game events.

#### Changes

- Configure application verifiers, token services, Game implementations and service
  factories through `ServerComponents`, using the existing Dagger assembly and
  classpath properties loader.
- Deny login by default. Support explicit anonymous mode for development and
  authenticated AES-GCM tokens with expiry, random secrets and optional persistent
  keys supplied through environment variables.
- Bound outbound source payloads and encoded writes with per-channel limits and a
  shared context budget. Include HTTP in total connection admission and add HTTP
  connection, body, request deadline and idle limits.
- Add `BlockingTaskExecutor` for bounded slow operations. Queued cancellation
  releases admission credits; running work retains its credits until it exits.
- Clean up partial initialization and startup failures, and expose resource usage
  through HTTP and Prometheus metrics.
- Validate required listener endpoints and reject malformed numeric or boolean
  configuration. Keep HTTP bound to loopback in the bundled configuration.
- Expand English documentation and JavaDoc, standardize multiline control flow and
  add repository instructions in `AGENTS.md`.

#### Migration from 0.8.1

1. Supply an application `CredentialsVerifier`. Local demos may explicitly enable
   `security.allow.anonymous=true`; this mode does not authenticate an account.
2. Provide all six listener settings, even for disabled listeners: `tcp.port`,
   `tcp.bind.host`, `http.port`, `http.bind.host`, `web.socket.port` and
   `web.socket.bind.host`. Missing or malformed values now fail initialization.
3. Reauthenticate clients using tokens issued by the old fixed-key implementation.
   For tokens that must survive restarts, set `security.token.key.environment` to
   the name of an environment variable containing a Base64 AES key, or inject an
   application-owned `SecurityService`.
4. Review resource limits for your workload. Keep slow operations off Netty I/O,
   Session and Game executors. Submit detached inputs to the bounded worker pool,
   then post results through the original Session or Game event API and validate
   session identity and operation sequence before applying them.

See [configuration](docs/CONFIGURATION.md), [core components and security](docs/CORE_COMPONENTS.md)
and [slow business operations](docs/SLOW_BUSINESS.md) for the complete contracts.

#### Validation

The release checks include the regression suite, strict production and test
JavaDoc, and a signed Maven publication fixture using a local publisher server.
GitHub Actions repeats these checks before signing and publishing to Maven Central.

Maven coordinates: `com.akakata:zea-tcp:0.8.2`.
