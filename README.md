# Project0 Car Dealership — legacy Java application

An educational Java console car-dealership project, preserved as a legacy baseline while its domain, persistence, authentication, and operational controls undergo incremental modernization.

> **Status: under active remediation; not production-ready.** The default `master` branch is an older baseline, while substantial modernization and security work remains in unmerged pull requests. Do not treat a passing build or a green draft PR as a security release approval. See [security blocker #29](https://github.com/danwiz/project0-cardealership/issues/29).

## What the project demonstrates

- Console-driven customer and employee workflows, vehicle inventory and offers, and payment-related domain models.
- Java object-oriented modeling and a legacy file-serialization data access adapter.
- Service-layer credential storage using salted PBKDF2 hashes in the current baseline.
- JUnit characterization tests and a Maven build checked against Java 11 and Java 17 by GitHub Actions.
- A sequence of isolated, reviewable modernization and security changes in draft pull requests.

The historical console implementation contains known functional and authorization defects. Its menu options, role boundaries, persistence behavior, and error handling **must not be relied on for real users or transactions**. This repository is for study, local experimentation with fictitious data, and controlled refactoring.

## Repository map

| Path | Purpose |
| --- | --- |
| `src/com/revature/cardealer/` | Legacy console entry point and dealership domain objects |
| `src/com/revature/service/` | User-account repository abstraction, login services, and password hashing |
| `src/com/revature/DAOService/` | Historical Java serialization adapter |
| `src/test/java/` | JUnit characterization and regression tests |
| `pom.xml` | Java 11-compatible Maven build |
| `BUILDING.md` | Build, test categories, and change-control guidance |
| `.github/workflows/` | Continuous integration definitions |

The main class is `com.revature.cardealer.CarDealership`. The source tree uses an Eclipse-era `src/com/...` layout intentionally retained for compatibility; it has not yet been moved to the conventional Maven `src/main/java` layout.

## Build and verification

Requirements:

- JDK 11 or 17
- Apache Maven 3.9+

From the repository root:

```bash
mvn clean verify
```

To launch the **legacy, non-production console** after compilation:

```bash
java -cp target/classes com.revature.cardealer.CarDealership
```

Use only invented accounts and data. The interactive console is preserved for characterization and may fail on some menu paths; successful compilation is **not** evidence of working end-to-end application flows. See [BUILDING.md](BUILDING.md) for focused test commands and source-layout details.

## Data and configuration safety

- No production database or cloud service is required to build the default branch.
- The legacy serialization adapter may write local `*.dat` files. These are ignored by Git; never commit real personal, account, or payment information.
- Never add passwords, API tokens, JDBC URLs containing credentials, database backups, or generated binaries to commits.
- The staged JDBC/PostgreSQL security experiments are **not** a deployed configuration; review each draft's documented prerequisites before attempting any local integration tests.

## Current review gates

1. Resolve the open [role escalation and privileged account security blocker](https://github.com/danwiz/project0-cardealership/issues/29), including a production-worthy trusted identity and credential boundary.
2. Review and integrate the stacked pull requests in dependency order, with independent security review and post-merge CI.
3. Validate the real database grants, operator bootstrap/recovery procedures, audit forwarding, rate limits, and secret management before any production use.
4. Complete [portfolio modernization issue #25](https://github.com/danwiz/project0-cardealership/issues/25): verify which similarly named repository is authoritative, capture a sanitized demonstration, complete ownership/license decisions, and only then consider profile pinning.

**License:** No repository license has been selected yet. Until the owner chooses one, do not assume this public repository grants reuse permissions beyond applicable law.

Security concerns should be reported privately to the repository owner rather than demonstrated against any live systems or with real credentials.
