# JDBC runtime trust boundary and credential policy

**Status:** Draft security review (2026-10-28). No real database privileges, deployments, credentials, or administrator accounts have been changed. This document supports issue #29 and stacked PRs #31–#33.

## Confirmed repository findings

1. `InfrastructureConfiguration.fromSystemProperties()` historically substituted `sa` and an empty password whenever JDBC mode was selected. This is appropriate only for intentionally ephemeral H2 tests, **not** a persistent database. PR #33 rejects implicit or empty credentials for non-`jdbc:h2:mem:` URLs; existing in-memory test behavior is retained.
2. `DealershipCompositionRoot.createJdbc()` calls `JdbcDatabase.migrate()` every time the runtime is composed. `JdbcDatabase.migrate()` executes schema-changing DDL and updates migration history. Consequently, current startup **does not** support a truly DDL-free runtime account without refactoring. Do **not** claim least-privilege database deployment has been implemented.
3. `JdbcUserAccountRepository.save(User)` uses an upsert that includes the account's **role and encoded credential**. `ConfiguredDealershipApplication.getAccounts()` exposes that repository to trusted in-process code. A caller already able to execute arbitrary code with this infrastructure object/database credential can bypass application-level permission checks. This is a privileged infrastructure capability, **not** a supported untrusted API.
4. The `AdministratorBootstrapService` directly inserts the first admin and a single-use marker within one JDBC transaction, but an overprivileged database operator can still modify the database. Database grants, host/OS access and backup governance are part of the trust boundary.
5. SQL syntax such as H2 `MERGE ... KEY(username)` and current migration DDL is **H2-oriented**. Testing with H2's `MODE=PostgreSQL` is *not* proof of compatibility or privilege behavior against an actual PostgreSQL server.

## New configuration guard

| Connection configuration | Policy |
|---|---|
| `jdbc:h2:mem:...` test database | Existing `sa` / empty-password convenience may be used within a transient test process |
| `jdbc:h2:file:...` persistent database | Explicit nonblank username and nonempty password required |
| `jdbc:postgresql://...` or other remote/persistent JDBC URL | Explicit nonblank username and nonempty password required; driver/dialect support requires separate testing |
| JDBC mode via system properties | Reject missing persistent credentials before connecting or migrating; do not log credential values |

This is a **configuration safeguard**, not a claim that an operator selected a strong password or configured TLS, network policy, and secret rotation correctly. Prefer a managed secret-delivery mechanism over exposing credentials on a command line; current configuration still relies on Java system properties, which requires deployment review.

## Target privilege and capability separation (not yet implemented)

| Principal / component | Expected authority | Not allowed |
|---|---|---|
| Migration operator | Apply reviewed DDL/schema history on a staged, backed-up database | Serve arbitrary user requests |
| First-admin bootstrap operator | Run dedicated offline CLI; insert a first admin and marker once under change control | Repeat initialization or expose network registration |
| Ordinary application runtime | Read/write only permitted domain records after authenticated use cases | CREATE/ALTER/DROP schema, modify bootstrap marker, arbitrarily elevate account roles |
| Privileged account-provisioning service | Create staff/admin accounts only after verified administrative approval | Client-supplied role assignment without authenticated authorization |
| Auditor / monitoring | Read approved evidence and redacted events | Mutate account records or recover secrets |
| In-memory CI fixture | Create deterministic privileged users for isolated tests | Supply production database credentials |

**Blocking redesign:** separate privileged account operations from raw repository `save`/upsert, remove arbitrary role writes from ordinary runtime capability, and split the migration/DDL account from the runtime account. That requires an explicit design review, application refactoring and integration tests; GitHub CI's current H2 test databases cannot prove actual production grants.

## Authorization and session boundaries

- PR #32 binds protected command/query checks to verified in-process login objects and persisted account state. This prevents simple caller-constructed role forgery at those application adapters.
- It is **not** an unforgeable remote session system: the public Java in-process context and repositories remain trusted infrastructure. Do not expose them through a REST API, browser bridge, plugin sandbox or remote RPC without an immutable principal/opaque session design.
- Domain-level transaction consistency and authorization must be enforced before (and where necessary within) database writes; merely hiding a console menu is insufficient.
- Track logout, password rotation, account deletion, multi-session revocation, process restart, audit events and role changes as separate test cases.

## Release acceptance tests (pending)

1. Start against a **staged persistent database** using a truly restricted runtime principal and prove DDL fails, approved DML works, and arbitrary account-role updates are denied.
2. Execute migration with a separate operator principal; verify schema version 4, single-use marker and repeat-attempt denial; restore a backup in a separate test environment.
3. Verify the real target database dialect, uniqueness/concurrency, transaction rollback and failure during bootstrap; H2 compatibility mode is insufficient proof.
4. Assert that missing/blank persistent JDBC credentials fail **before any connection**; verify test-only H2 memory mode remains available on Java 11 and 17.
5. Exercise customer/staff/admin permissions and direct repository-access negative tests from the actual deployed entrypoint, not only mocked ports.
6. Ensure secrets are redacted from CI/job logs, command lines, exception reports and documentation; store only approved evidence metadata.
7. Record infrastructure owner, risk approval, recovery procedure and gate evidence before merging or deploying.

## Related sources and artifacts

- `docs/security/administrator-bootstrap.md` — operator-only bootstrap, current limitations.
- `docs/security/verified-principal-boundary.md` — application-layer verified session gate.
- `src/com/revature/application/InfrastructureConfiguration.java`
- `src/com/revature/application/DealershipCompositionRoot.java`
- `src/com/revature/repository/jdbc/JdbcDatabase.java`
- `src/com/revature/repository/jdbc/JdbcUserAccountRepository.java`
- `src/com/revature/repository/jdbc/AdministratorBootstrapService.java`
- Oracle JDBC [prepared statements](https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html), [transactions](https://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html).

Keep the security issue open until these controls have been **implemented and verified**, not merely documented.
