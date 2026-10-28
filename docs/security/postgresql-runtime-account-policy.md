# PostgreSQL ordinary account runtime privilege policy — CI-only

**Proposed draft PR #41 (2026-10-28), stacked on draft PR #40.** This is an intentionally narrow policy test, not a production migration or a completed full-runtime least-privilege model.

## Security invariant exercised

The dealership's public Java registration flow creates only `CUSTOMER` accounts, but a shared JDBC login would otherwise permit callers with unrestricted accounts INSERT/UPDATE to forge `ADMINISTRATOR` accounts directly. The disposable PostgreSQL CI schema therefore tests **two independent database controls**:

1. Column-level SQL privileges: account SELECT, INSERT of `username`, `encoded_credential`, `role`, and UPDATE of `encoded_credential` only. The runtime role has no DELETE, no UPDATE of `role`, no DDL and no bootstrap-marker privileges.
2. PostgreSQL row-level security: a permissive account SELECT policy, an INSERT `WITH CHECK (role = 'CUSTOMER')` policy, and an UPDATE policy. The UPDATE policy does **not** enforce ownership; column grants only restrict which fields may be modified.

**Fixture DDL, never an automatic production grant:**

```sql
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
CREATE POLICY runtime_account_read ON accounts FOR SELECT TO runtime_login USING (true);
CREATE POLICY runtime_customer_insert ON accounts FOR INSERT TO runtime_login
  WITH CHECK (role = 'CUSTOMER');
CREATE POLICY runtime_credential_change ON accounts FOR UPDATE TO runtime_login
  USING (true) WITH CHECK (true);
GRANT SELECT ON accounts, schema_history TO runtime_login;
GRANT INSERT (username, encoded_credential, role) ON accounts TO runtime_login;
GRANT UPDATE (encoded_credential) ON accounts TO runtime_login;
```

The test uses unique per-run schema and role names (instead of the illustrative `runtime_login`), validates that migrated schema version 4 works, then proves:

- `UserLoginService.registerUser` creates a customer and the stored password can be changed through authenticated compare-and-swap.
- Direct JDBC `save` with `ADMINISTRATOR` role is rejected with SQLSTATE `42501` by the row-security policy.
- Direct SQL role promotion, account deletion, schema creation, and bootstrap-state modifications are denied with SQLSTATE `42501`.
- SQL login is separately scoped to the exact local disposable `dealership_ci` PostgreSQL fixture. CI must report a non-skipped integration test.

## What this does **not** yet protect

- **Credential ownership:** The ordinary runtime role has SELECT of password hashes and UPDATE permission on `encoded_credential` for *every account*. An attacker with that shared database login can rotate another user's hash directly. Compare-and-swap in Java is not a database-enforced ownership rule.
- **Broader runtime:** The fixture deliberately has no inventory, purchase, ownership, or payment table privileges. Applying it as the single live application connection would break those flows. The application must split capabilities into separately authorized database identities/routines or introduce end-user-scoped, database-verified caller context before declaring full runtime least privilege.
- **Privileged operators:** PR #40's writer is separate but still has broad accounts DML in its own fixture. Direct SQL bypass, immutable audit, change-ticket approvals, role inheritance, TLS, backup/restore and recovery remain outstanding.
- **Existing databases:** This is **not** an approved migration or revocation procedure. Review existing grants, account roles, other policies, extensions, owner bypass and default privileges before adapting it.

Reference: [PostgreSQL CREATE POLICY](https://www.postgresql.org/docs/16/sql-createpolicy.html), [PostgreSQL row security](https://www.postgresql.org/docs/16/ddl-rowsecurity.html), and [GRANT](https://www.postgresql.org/docs/16/sql-grant.html).

Keep draft PR #41 unmerged and issue #29 open. No production data, grants, credentials, deployment or paid GitHub entitlement are changed.

## Follow-up PR #42: customer-specific database sessions

Draft PR #42 introduces a separate PostgreSQL CI fixture with two authenticated customer SQL roles and own-row policies bound to `session_user`, exercising isolation of hashes and credential UPDATEs. Unlike the shared runtime policy above, this **does** test database-enforced per-customer ownership. It is not wired to the existing application's shared JDBC identity; see `docs/security/postgresql-credential-ownership-options.md` for production tradeoffs.
