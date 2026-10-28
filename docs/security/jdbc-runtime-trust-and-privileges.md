# JDBC runtime trust boundary and credential policy

**Status:** Draft security review (2026-10-28). No real database privileges, deployments, credentials, or administrator accounts have been changed. This document supports issue #29 and stacked PRs #31–#37.

## Confirmed repository findings

1. `InfrastructureConfiguration.fromSystemProperties()` historically substituted `sa` and an empty password whenever JDBC mode was selected. This is appropriate only for intentionally ephemeral H2 tests, **not** a persistent database. PR #33 rejects implicit or empty credentials for non-`jdbc:h2:mem:` URLs; existing in-memory test behavior is retained.
2. PR #34 separates automatic transient H2 migration from the persistent JDBC runtime: `ensureSchemaReady()` checks an existing schema using read-only `schema_history` queries for file-backed/remote URLs. The composition root and five JDBC adapter constructors use this check instead of migrating. `JdbcDatabase.migrate()` remains an **explicit operator action**, and the dedicated bootstrap service still invokes it under its privileged operator boundary. No production grants have been configured; **do not** claim least-privilege deployment certification.
3. PR #35 replaces mutable repositories returned by `ConfiguredDealershipApplication.getAccounts()` and `DealershipApplicationContext.getAccountRepository()` with credential-redacted, read-only facades that refuse `save`/`removeByUsername`. Internal authentication and authorized provisioning services retain their writer references. **PR #36:** raw `save(User)` is now insert-only (no role/hash upsert), with explicit compare-and-update credential/role primitives and authenticated service methods. **PR #37:** normal JDBC composition now supplies no administrative repository to any login service; these services refuse provisioning, role updates and deletion. An independent operator service may be explicitly constructed with an administrative repository, and JDBC context snapshot replacement is denied. **Remaining:** trusted infrastructure holding raw JDBC credentials can still call repository primitives directly; actual database grants and service/process separation still need deployment verification. This is a privileged infrastructure capability, **not** a supported untrusted API.
4. The `AdministratorBootstrapService` directly inserts the first admin and a single-use marker within one JDBC transaction, but an overprivileged database operator can still modify the database. Database grants, host/OS access and backup governance are part of the trust boundary.
5. Historical H2 `MERGE ... KEY(username)` was removed by PR #36, but migration DDL and other SQL remain **H2-oriented**. Testing with H2's `MODE=PostgreSQL` is *not* proof of compatibility or privilege behavior against an actual PostgreSQL server.

## New configuration guard

| Connection configuration | Policy |
|---|---|
| `jdbc:h2:mem:...` test database | Existing `sa` / empty-password convenience may be used within a transient test process |
| `jdbc:h2:file:...` persistent database | Explicit nonblank username and nonempty password required |
| `jdbc:postgresql://...` or other remote/persistent JDBC URL | Explicit nonblank username and nonempty password required; driver/dialect support requires separate testing |
| JDBC mode via system properties | Reject missing persistent credentials before connecting or migrating; do not log credential values |

This is a **configuration safeguard**, not a claim that an operator selected a strong password or configured TLS, network policy, and secret rotation correctly. Prefer a managed secret-delivery mechanism over exposing credentials on a command line; current configuration still relies on Java system properties, which requires deployment review.

## Target privilege and capability separation (partly implemented; operator grants still pending)

| Principal / component | Expected authority | Not allowed |
|---|---|---|
| Migration operator | Apply reviewed DDL/schema history on a staged, backed-up database | Serve arbitrary user requests |
| First-admin bootstrap operator | Run dedicated offline CLI; insert a first admin and marker once under change control | Repeat initialization or expose network registration |
| Ordinary application runtime | Read/write only permitted domain records after authenticated use cases | CREATE/ALTER/DROP schema, modify bootstrap marker, arbitrarily elevate account roles |
| Privileged account-provisioning service | Create staff/admin accounts only after verified administrative approval | Client-supplied role assignment without authenticated authorization |
| Auditor / monitoring | Read approved evidence and redacted events | Mutate account records or recover secrets |
| In-memory CI fixture | Create deterministic privileged users for isolated tests | Supply production database credentials |

**Remaining blocking redesign:** PRs #36–#37 remove silent role/credential upserts, add authenticated change methods and disable privileged account changes in normal JDBC runtime composition. However, direct infrastructure callers with database credentials may still use the explicit update primitives or deletion method. Grant narrowly scoped privileges to ordinary runtime and isolate administrative account modifications; public getters are read-only, but these Java checks are not a security boundary against arbitrary trusted code holding database credentials. PR #34 adds a DDL-free runtime startup *path*, but creating distinct real migration/runtime credentials, granting the minimum database privileges, an independent migration runner, and testing the production target DB remain operator/deployment gates. GitHub CI's H2-only proof cannot certify actual production grants.

## Authorization and session boundaries

- PR #32 binds protected command/query checks to verified in-process login objects and persisted account state. This prevents simple caller-constructed role forgery at those application adapters.
- It is **not** an unforgeable remote session system: the public Java in-process context and repositories remain trusted infrastructure. Do not expose them through a REST API, browser bridge, plugin sandbox or remote RPC without an immutable principal/opaque session design.
- Domain-level transaction consistency and authorization must be enforced before (and where necessary within) database writes; merely hiding a console menu is insufficient.
- Track logout, password rotation, account deletion, multi-session revocation, process restart, audit events and role changes as separate test cases.

## Release acceptance tests (partially covered in H2; production gates pending)

1. PR #34 exercises a restricted H2 user for DDL-free schema validation; additionally start against the **staged target database** using a truly restricted runtime principal and prove DDL fails, approved DML works, and arbitrary account-role updates are denied.
2. Execute migration with a separate operator principal; verify schema version 4, single-use marker and repeat-attempt denial; restore a backup in a separate test environment.
3. Verify the real target database dialect, uniqueness/concurrency, transaction rollback and failure during bootstrap; H2 compatibility mode is insufficient proof.
4. Assert that missing/blank persistent JDBC credentials fail **before any connection**; verify test-only H2 memory mode remains available on Java 11 and 17.
5. PRs #35–#38 test read-only views, insert-only creation, authenticated updates, no administrative mutation channel in ordinary JDBC composition, and a transactional last-admin safeguard in the JDBC repository. Additionally exercise direct infrastructure capabilities from the deployed entrypoint, serializable transaction conflicts on the actual PostgreSQL target, and real database grants.
6. Ensure secrets are redacted from CI/job logs, command lines, exception reports and documentation; store only approved evidence metadata.
7. Record infrastructure owner, risk approval, recovery procedure and gate evidence before merging or deploying.

## Related sources and artifacts

- `docs/security/account-administration-channels.md` — PR #37 runtime/operator separation and first-/last-admin safeguards.

- `docs/security/administrator-bootstrap.md` — operator-only bootstrap, current limitations.
- `docs/security/verified-principal-boundary.md` — application-layer verified session gate.
- `src/com/revature/application/InfrastructureConfiguration.java`
- `src/com/revature/application/DealershipCompositionRoot.java`
- `src/com/revature/repository/jdbc/JdbcDatabase.java`
- `src/com/revature/repository/jdbc/JdbcUserAccountRepository.java`
- `src/com/revature/repository/jdbc/AdministratorBootstrapService.java`
- Oracle JDBC [prepared statements](https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html), [transactions](https://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html).

Keep the security issue open until these controls have been **implemented and verified**, not merely documented.
