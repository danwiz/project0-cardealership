# PostgreSQL credential-rotation audit trigger — CI security proof

**Draft PR #44 / 2026-10-28, stacked on #43.** A disposable PostgreSQL 16 fixture demonstrates a transaction-coupled audit trail for password-hash changes, without altering the shipped migration version 4 or production grants.

## Why this control matters

A dedicated trusted credential service alone cannot stop someone who steals its database-writer login from executing direct SQL against arbitrary account hashes. It can, however, make each committed hash change produce a database audit record that the compromised restricted writer cannot modify.

The CI fixture establishes:

1. A separately privileged migration owner creates the `credential_rotation_audit` table (audit ID, account username, originating SQL session login, timestamp), a qualified `AFTER UPDATE OF encoded_credential` trigger, and a `SECURITY DEFINER` function with catalog-only `search_path`.
2. The normal application SQL principal may SELECT account records but not change hashes.
3. The isolated credential writer may UPDATE only the `encoded_credential` column and can neither read nor modify the audit table or disable its trigger.
4. A separate auditor SQL login can SELECT the audit records but cannot change them.
5. Trigger execution is part of the same transaction as the credential UPDATE, so rolled-back password changes do not leave committed audit rows.
6. Direct SQL credential updates through the writer identity **still succeed** and are recorded. The audit is detection/accountability, **not** prevention of SQL credential compromise.

No plaintext passwords or full encoded password hashes are written into the audit records. The test uses fixture-only SQL role secrets, randomly named schemas/roles, and an exact local CI URL check.

## Limits of the guarantee

The audit is **tamper-resistant only relative to the restricted application/writer/auditor roles**. Database owners and superusers (and anyone with equivalent DDL/privileged access) can alter audit structures or remove evidence. Host compromise, backup deletion, database rollback, direct disk modification, retention policies and replication lag are not covered. It is not an external append-only/WORM log or a legally certified audit system.

A real deployment requires a reviewed schema migration, independently controlled privileged DDL, immutable external event forwarding, retention/alert policy, monitoring of missing/failed audit events, provenance/change-ticket correlation, timestamp integrity, transaction isolation, backup and incident recovery. It must also enforce proper client authentication, throttling and the secure custody of the credential-writer SQL secret outside the normal JVM.

**Release gate:** Keep PR #44 draft, issue #29 open, no production grants/deployments, no paid private-ERM GitHub Code Security entitlement.

References: [PostgreSQL CREATE TRIGGER](https://www.postgresql.org/docs/16/sql-createtrigger.html), [PL/pgSQL triggers](https://www.postgresql.org/docs/16/plpgsql-trigger.html), [CREATE FUNCTION security definer](https://www.postgresql.org/docs/16/sql-createfunction.html), [Function security](https://www.postgresql.org/docs/16/perm-functions.html).
