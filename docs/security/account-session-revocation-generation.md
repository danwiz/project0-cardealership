# Account session revocation generations — draft migration and security boundary

Status: **CI-only P0 candidate, not approved for production or for merging to `master`**. Related [#98](https://github.com/danwiz/project0-cardealership/issues/98), [draft #101](https://github.com/danwiz/project0-cardealership/pull/101), and [integration #85](https://github.com/danwiz/project0-cardealership/issues/85). Preserve original legacy source and database evidence.

## Why username, role and password hash are insufficient

Previously `RoleAuthorizationService.VerifiedSession` retained only username/role/encoded password. An administrator could demote and subsequently restore an employee without changing the password, making an old verified Java object match persisted data again. Deleting/recreating the same username with the same encoded hash also recreated the old tuple. These are authorization revocation failures even if the login verification was initially correct.

A *server-issued opaque UUID session revision* is now persisted on every account and bound to a verified session. The revision is not a bearer credential: passing a UUID in a new User object cannot create a verified session, because the verifier still requires a successful credential-based login and binds the specific Java object. No public account projection exposes stored credential hashes or the revision.

## Data model and generation boundary

| Storage | Creation | Mutation | Restore |
| --- | --- | --- | --- |
| In-memory repository | Each `save` creates a new UUID; caller-supplied values discarded | `updateCredentialIfCurrent` and `updateRoleIfCurrent` regenerate the UUID in the same synchronized operation | Rehydrated snapshots use `save`, minting new revisions even when a historical serialized User has one |
| H2 local/JUnit | Schema v5 adds UUID NOT NULL DEFAULT `RANDOM_UUID()` | Supported JDBC repository updates set a new UUID explicitly; no claim is made about arbitrary external H2 SQL writes | File-based H2 retains revisions; snapshot import creates fresh in-memory identities |
| PostgreSQL 16 | Schema v5 adds UUID NOT NULL DEFAULT `gen_random_uuid()` to migrate old rows and to mint IDs on SQL insert | Owner-provisioned BEFORE UPDATE trigger rotates the revision when role/credential actually changes, including direct SQL writers with only UPDATE(encoded_credential) privilege | New connections read committed revisions; owner migrations governed independently |

The SQL revision trigger is a transaction participant, so rollback preserves the old revision. It does **not** grant untrusted writers permission to UPDATE the `session_revision` column. PostgreSQL table owners and superusers remain fully trusted; an actual deployment requires independent DBA/schema-owner review and grant inspection.

## Version 4 -> 5 upgrade procedure

1. Obtain operator authorization and a recovery-tested backup/snapshot. Preserve `schema_history` and all original artifact checksums.
2. Stop/cordon application writers and record the exact release SHA, existing DB version, role grants and current application identities.
3. As a **separately authorized schema owner**, invoke `JdbcDatabase.migrate()` with the database in a maintenance window. Ordinary runtime initialization *only* invokes `requireCurrentSchema()` for persistent stores, **never DDL**. The PostgreSQL migration creates the new UUID column/default and owner function/trigger in the same transaction as `schema_history` version 5.
4. Check each existing account has a nonnull revision, distinct revisions where expected, and that SQL-only credential changes rotate it while restricted reader/writer grants remain enforced. Validate no table owner DDL permission leaked.
5. Restart/re-authenticate. All pre-upgrade sessions must be treated as invalid; prior revision-less sessions fail closed. Verify account deletion/recreation, demotion-restoration, password rotation and restart scenarios with a test account.
6. If the upgrade fails, roll back the entire transaction or restore the approved backup and roll back application code **together**. Do not revert `schema_history` alone or silently run a v5 app on a v4 database. Data restore must be evaluated for account-incarnation reuse across processes.

## Regression and negative tests

- Four `SessionRevocationEpochRegressionTest` cases cover role demote/restore and delete/recreate of exactly the same username/role/encoded hash on both in-memory and JDBC/H2.
- `PostgresqlSessionRevisionMigrationTest` (only disposable exact PG16 CI fixture) simulates populated schema-v4 shape then upgrades to v5, checks distinct revisions, default on direct SQL insert, JDBC mutation, direct SQL credential update, and rolled-back mutation retaining the old generation.
- Existing account authorization, bootstrap, SQL row-level ownership, credential-only writer, audit trigger, purchase retry/concurrency and migration checks must continue passing with Java 11/17.
- A forged User object with copied UUID must never be treated as an authenticated session.

## Explicitly outside scope

- A real distributed session broker, signed/OIDC identity proof, JWT revocation lists, TLS, managed secrets and cross-process logout notification.
- Atomic authorization inside business transactions: a role may change **after** a Java policy check and **before** an independent SQL purchase writes. Address with transaction-bound identity/permission checks and locking in [#89](https://github.com/danwiz/project0-cardealership/issues/89).
- Post-restore identifier rollback across an externally restored PostgreSQL snapshot is a trust-boundary issue requiring a persistent authoritative identity-epoch service or a procedure that forces all session renewal on restoration. Do not claim a UUID in a reverted database snapshot alone prevents resurrecting sessions in other still-running processes.
- Arbitrary SQL account modifications on H2 and actions by compromised PostgreSQL owner/superuser.

**No schema migration, release, deployment, credential provisioning or default-branch merge was conducted by creating this document.**
