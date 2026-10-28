# Separate account-administration operator CLI — draft

**Status (2026-10-28):** Test-only implementation, stacked draft PR #39, tracking issue #29. The CLI has **not** been exercised against PostgreSQL with real operator grants. Do not deploy or use it with production accounts until its role assignments, audit, TLS and recovery plan are approved.

## Intent and trust boundary

The regular composed JDBC runtime intentionally does **not** inject an account-administration writer. A separate, local, interactive CLI is provided at `com.revature.application.AccountAdministrationCli` and accepts exactly one action flag:

- `--provision-staff` — create EMPLOYEE or ADMINISTRATOR accounts. CUSTOMER signup remains in the ordinary registration use case.
- `--change-role` — compare and update an existing role.
- `--delete-account` — delete an account after exact confirmation.

All target usernames, role choices, passwords and the external approval/change-ticket reference are entered interactively; no command-line secret or target argument is accepted. The program requires `System.console()`, refuses ephemeral H2 memory URLs, refuses credentials embedded in JDBC URLs, and rejects identical identity and operator principal names. It clears mutable password buffers before exit but Java's JDBC API/hasher still use immutable `String` objects; process/terminal controls remain necessary.

The CLI constructs **two separate JDBC connections** to the same persistent database:

| Connection | Purpose | Recommended privileges (not applied by this PR) |
|---|---|---|
| Identity reader | Read the current persisted account, PBKDF2 hash and role for administrator authentication | SELECT on `schema_history` and `accounts`; no account DML or DDL |
| Account operator | Perform approved account inserts, compare-and-update role changes, and deletions | SELECT on `schema_history` and `accounts`; limited account DML; no DDL or bootstrap-marker writes |
| Schema migrator / bootstrap owner (separate offline process) | Apply approved schema migrations and initial bootstrap | DDL, version-history and initial-marker writes controlled under change management |

Merely using distinct usernames is **not proof of separation**. Database ownership, role memberships, inherited grants, default privileges and use of an unrestricted database owner must be inspected. In PostgreSQL, grants may be scoped by table and column; the runtime application's account-registration and credential-update design still needs database-enforced restrictions before runtime roles can be considered least privilege. See [PostgreSQL GRANT](https://www.postgresql.org/docs/current/sql-grant.html).

## Safe operational sequence (not a deployment instruction)

1. Obtain an approved change ticket, reviewed authorization and a recent tested backup.
2. Verify separate database login identities, explicit grants and a secure local operator host. Disallow shell history, screen recording and unauthorized terminal multiplexing.
3. Build a reviewed version with Java 11+ and the dependencies in `pom.xml`; ensure the PostgreSQL JDBC driver is available when targeting PostgreSQL. Run the dedicated Java main class with one action flag and no additional parameters.
4. Enter a persistent JDBC URL, read-only identity principal and password, distinct account-operator principal and password, authenticated administrator username/password, approved ticket, target username and action-specific fields.
5. Review the requested role change or deletion before confirming. The service reauthenticates the persisted administrator and authorizes `MANAGE_USERS`; the JDBC repository's last-admin guard rejects removal/demotion of the final administrator.
6. Record outcome in an **external, immutable audit system** and reconcile approved ticket, actor, operation, target, timestamps and DB transaction evidence.

The CLI prints a **minimal receipt only after successful mutation**. That console line is **not** a durable or tamper-resistant audit log. The implementation does not create an independent audit ledger, establish dual approval or rotate credentials. Running the command is a privileged change requiring separate operator authorization; these instructions are solely a review/checklist until validation is complete.

## Evidence, negative tests and unresolved gates

Automated H2 **file-backed** tests create different local principals and explicit SELECT-only versus DML-capable grants. They prove that a reader cannot directly insert or modify accounts, while an authenticated administrator can request allowed writes through a separate operator repository. This is a test fixture, **not** evidence of real PostgreSQL permissions or runtime-path safety.

Before any real deployment:

- Confirm production-target PostgreSQL migration syntax, JDBC SSL/TLS requirements, database roles and exact effective grants without relying on an owner or superuser runtime.
- Verify separate runtime, identity-reader, account-operator, and migration principals with direct positive/negative SQL tests; ensure public registration cannot submit STAFF/ADMINISTRATOR roles and runtime credentials cannot alter roles or the bootstrap marker.
- Test concurrency, transaction rollback and last-admin recovery under PostgreSQL, including direct SQL and stale role/session states.
- Provide centralized immutable audit events and change-ticket correlation; use an approved secret-delivery mechanism (never a URL, Java `-D` secret, commit or CI log).
- Rehearse backup/restore and emergency access with recorded owner/reviewer approval. Keep issue #29 open and PR #39 draft.

**No production database grants, accounts, schema, credentials, deployment or paid GitHub entitlement are modified by this patch.**
