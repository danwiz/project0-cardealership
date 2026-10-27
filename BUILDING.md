# Building and Testing the Legacy Baseline

This branch establishes a reproducible build and characterization-test safety net without changing the legacy application behavior.

## Prerequisites

- Java Development Kit 11 or newer
- Apache Maven 3.9 or newer

The compiler release is currently fixed at Java 11 to minimize modernization risk during baseline characterization.

## Commands

Compile production sources:

```bash
mvn clean compile
```

Run all tests:

```bash
mvn clean test
```

Run only tests tagged as legacy behavior:

```bash
mvn test -Dgroups=legacy-behavior
```

Create the package after tests pass:

```bash
mvn clean verify
```

## Legacy Layout Note

The repository uses an Eclipse-era source layout:

- Production code: `src/com/...`
- Characterization tests: `src/test/java/...`

The Maven configuration deliberately preserves the production layout and excludes `src/test/**` from the main compiler. Moving production sources into the conventional `src/main/java` tree is deferred until baseline behavior is sufficiently protected.

## Test Classification

- `legacy-behavior`: records observable existing behavior that should remain stable during structural refactoring.
- `known-defect`: safely reproduces a confirmed defect and documents the desired correction separately.
- `target-behavior`: specifies approved behavior that replaces a known defect.
- `security`: specifies security controls and prohibited behavior.
- `migration`: verifies compatibility during package, persistence, or data migration.

## Change-Control Rule

Do not combine broad behavioral corrections with build-system or source-layout migration. Each commit must remain independently reviewable and reversible.
