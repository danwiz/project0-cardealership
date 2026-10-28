# PostgreSQL credential ownership feasibility — per-customer SQL principal

**Draft research and CI proof (2026-10-28), stacked PR #42 on #41.** No live SQL role provisioning or runtime authentication architecture has changed.

## Threat and necessary trust binding

PR #41 proves an ordinary **shared** JDBC login can be granted INSERT of CUSTOMER accounts and credential-only UPDATE. It cannot protect one user's credential from another caller who obtains that same database login. A Java method's current-password check does not apply to direct SQL.

For a testable PostgreSQL-enforced ownership model, PR #42 uses **two distinct real SQL authentication identities**, each with its own secret, and two corresponding application customer records. Only the privileged fixture owner creates accounts, SQL logins and policies. The test uses the authenticated `session_user`, not a caller-supplied `SET app.username`, request parameter, or `current_setting()` value.

## Disposable CI policy shape

Only in a generated temporary schema, with test customer usernames intentionally identical to their PostgreSQL login role names:

```sql
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
CREATE POLICY customer_own_select ON accounts FOR SELECT TO customer_a, customer_b
  USING (username = session_user);
CREATE POLICY customer_own_credential_update ON accounts FOR UPDATE TO customer_a, customer_b
  USING (username = session_user) WITH CHECK (username = session_user);
GRANT SELECT ON accounts, schema_history TO customer_a, customer_b;
GRANT UPDATE (encoded_credential) ON accounts TO customer_a, customer_b;
```

No customer INSERT, DELETE, role-column UPDATE, schema DDL or account-administration privileges are granted. Role membership is deliberately absent. The exact test code uses randomized role/schema names and requires the CI-only local database URL and fixture credentials.

The verification checks:

- Each SQL login sees only its own account and encoded hash via `JdbcUserAccountRepository`.
- Authenticated credential rotation via `UserLoginService.changePassword` works for the session's actual account.
- Both a forged Java credential-update request and raw SQL UPDATE for the other customer's row fail to alter it; row filtering can return **zero affected rows** rather than `42501`.
- SQL `SET ROLE` into the other customer, direct UPDATE of `role`, and account DELETE are denied with `42501`.
- The other customer's hash and authentication remain unchanged.

## Deployment decision is explicitly **open**

This demonstrates that separate authenticated PostgreSQL principals plus RLS can bind each credential operation to a database-verified identity. **The current dealership application does not do this.** It currently uses a single runtime JDBC identity in `DealershipCompositionRoot.createJdbc`; we have not changed connection lifecycle, registration, deployment or user provisioning.

Per-customer SQL roles impose account/role lifecycle coupling, extra SQL logins and database secrets, connection pooling complexity, role cleanup and operational overhead. A production alternative is a separately deployed, trusted credential service that owns narrowly scoped SQL credentials and accepts authenticated operations through an independently validated identity channel. Neither option has been selected or certified.

Before production implementation, decide the identity architecture, threat model, account-to-SQL principal mapping, role membership inheritance, secure session pooling, secret storage, password reset/recovery, audit, TLS, incident revocation and privacy of account hashes. Privileged operators remain a separate control plane. PostgreSQL owners, superusers and BYPASSRLS roles can bypass policies, so the runtime must not use those roles.

References: [PostgreSQL row security](https://www.postgresql.org/docs/16/ddl-rowsecurity.html), [CREATE POLICY](https://www.postgresql.org/docs/16/sql-createpolicy.html), and [GRANT](https://www.postgresql.org/docs/16/sql-grant.html).

Keep PR #42 draft, issue #29 open, all production privileges unchanged, and private ERM paid Code Security pending.
