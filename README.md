# Zea TCP for Java 8

Zea TCP for Java 8 is a Gradle port of Zea TCP Transport. The fork version is
**0.8.5**, with the synchronized Java 21 upstream **0.8.5** as its behavior baseline.
This version contains only upstream synchronization and equivalent compatibility
rewrites; it introduces no independent framework functionality.

The production packages and public event/sending APIs remain `com.akakata.*`.
TCP, HTTP, WebSocket, ordering, admission, backpressure, authentication, monitoring
and resource ownership follow the synchronized upstream contracts.

Source repository: [Zea-tcp-java8](https://github.com/irwinmc/Zea-tcp-java8).
The Java 21 upstream remains [Zea-tcp](https://github.com/irwinmc/Zea-tcp).

## Build

Install a JDK 8 and select it through `JAVA_HOME` or the IDE's Java toolchain
settings. The checked-in wrapper pins Gradle 8.14.5 and verifies its distribution
checksum; a system Gradle installation is unnecessary.

```bash
./gradlew --version
./gradlew --no-daemon --console=plain clean build testJavadoc
```

Compilation, test JVMs and JavaDoc use the Java 8 toolchain. Tests include the 320
inherited regression cases plus 25 Java 8 equivalence cases. JavaDoc includes
private members and test sources; warnings and errors fail the build.

The library JAR is `build/libs/zea-tcp-java8-0.8.5.jar`; source and JavaDoc archives
are produced alongside it. It contains no executable application entry point and
does not bundle its runtime dependencies. The intended coordinates are
`com.akakata:zea-tcp-java8:0.8.5`; publication is pending verification and Central acceptance.

Test reports are under `build/reports/tests/test/` and `build/test-results/test/`.
Use `scripts/test_report.py` to retain an aggregate result and individual case reports:

```bash
python3 scripts/test_report.py \
  --report-dir .internal-docs/migration-reports/local \
  --name verify -- ./gradlew --no-daemon --console=plain clean build testJavadoc
```

## Run an Example

```bash
./gradlew --no-daemon --console=plain runExample
./gradlew --no-daemon --console=plain runExample -PexampleMain=com.akakata.HttpOnlyServer
```

Examples use the test classpath and Java 8 runtime. Stop them with Ctrl+C. The
repository TestServer explicitly enables demonstration credentials; normal
application contexts retain deny-by-default login. Review listener addresses and
ports before launching an example.

Applications still provide their own `CommandLine` entry point, Game and
`ServerComponents`, and load configuration from classpath resources. Public method
names such as `Session.onEvent()`, `Game.send()` and `MessageSender.sendMessage()`
retain their inherited contracts.

## Compatibility Dependencies

| Dependency | Pinned version |
| --- | --- |
| Netty | 4.2.19.Final |
| Jackson core/databind/YAML and Java 8/time modules | 2.22.3 |
| Dagger runtime and annotation processor | 2.51.1 |
| javax.inject | 1 |
| Caffeine | 2.9.3 |
| SLF4J | 2.0.20 |
| Logback | 1.3.16 |
| Logstash Logback Encoder | 7.4 |
| JUnit Jupiter / Platform | 5.12.2 / 1.12.2 |

`build.gradle` is authoritative. JSON adapters retain the synchronized defaults,
including property naming/order, trailing-token checks, null primitive rejection,
UTF-8 surrogate handling, legacy date strings and Java time/optional support.
Former records retain constructors, accessors, validation and value methods.
Virtual-thread operations use independent daemon platform threads on Java 8;
timing and resource costs require separate workload measurements.

## Documentation

| Guide | Contents |
| --- | --- |
| [Migration status](MIGRATION.md) | Synchronization provenance, completed adaptation and verification boundaries |
| [Changelog](CHANGELOG.md) | Porting-only changes and inherited upstream history |
| [Configuration and protocols](docs/CONFIGURATION.md) | Configuration keys, wire formats and limits |
| [Core components and security](docs/CORE_COMPONENTS.md) | Application injection, authentication and token keys |
| [Event loops and lifecycle](docs/EVENT_LOOP.md) | Ordering, backpressure and payload ownership |
| [Shutdown and lifecycle](docs/LIFECYCLE.md) | Ownership, bounded shutdown and completion observation |
| [Slow business operations](docs/SLOW_BUSINESS.md) | Bounded offloading and ordered result application |
| [Monitoring](docs/MONITORING.md) | Health checks, metrics and application counters |
| [Banner customization](docs/BANNER_CUSTOMIZATION.md) | Application startup banners |

## License

The upstream project declares the Apache License 2.0; see the
[license text](https://www.apache.org/licenses/LICENSE-2.0).
