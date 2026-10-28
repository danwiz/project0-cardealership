# PostgreSQL operator principal and grant validation — CI-only

**Draft checkpoint (2026-10-28):** Stacked PR #40 adds an isolated PostgreSQL 16 service to the Java 11/17 Actions test jobs. It tests repository migration SQL and a separately authorized operator's database principals. This is **not** a production grant rollout or a runtime least-privilege certificate.

## Isolated test architecture

Each invocation of `PostgresqlOperatorPrivilegeIntegrationTest` generates fresh, unguessable-as-input test-only names (UUID suffixes) for:

1. A distinct schema (`currentSchema` in the PostgreSQL JDBC URL).
2. An identity-reader SQL login.
3. An account-operator SQL login.
4. Seeded administrator/employee records and target accounts.

The GitHub Actions PostgreSQL container is ephemeral and initialized with **CI-only fixture credentials**. Its administrator creates the schema and roles, runs application schema migrations and seeds accounts. The test grants both SQL roles schema `USAGE` and account/schema-history `SELECT`; only the dedicated operator receives `INSERT`, `UPDATE` and `DELETE` privileges on that schema's accounts table.

The reader must be denied:

- Direct INSERT into accounts, even if it supplies an administrator role.
- Direct UPDATE of an employee to administrator.
- Direct DELETE of an employee.
- Schema DDL and modifications to the administrator bootstrap marker.

Expected PostgreSQL permission SQLSTATE for the direct negative cases: `42501`. The separate writer must successfully invoke the service's authenticated staff provisioning, role reassignment and deletion, and be prevented by the repository guard from deleting the final administrator.

The test also verifies `JdbcDatabase.migrate()` against actual PostgreSQL syntax, independent of H2's compatibility mode.

## Exact proof boundaries

This validation checks **only the separate offline account operator's identity-reader and writer grants**:

- It does not establish a least-privilege role for the full dealership runtime (registration, payments, inventory, purchases, customer password rotation).
- The operator has broad accounts-table DML for the fixture. Direct SQL with that privileged credential can bypass the Java last-admin guard, authorization, audit and change-ticket checks. Production operator rights must therefore be further restricted and coupled to an audited control plane.
- No real secrets, external PostgreSQL server, production database or live GitHub billing entitlement are accessed.
- Connection TLS, role inheritance/default privileges, backups, restore concurrency, immutable audit receipts and emergency access have not been certified.
- The test creates fixture schemas and users inside a runner-owned, disposable database. It does not grant or revoke access on the user's infrastructure.

The production model should separate migration owner, restricted runtime, independent identity reader and account operator; require change management and a durable tamper-resistant audit trail; and test role-level, column-level, and approved-routine privileges against the selected production database before rollout.

Official references:

- PostgreSQL [GRANT privileges](https://www.postgresql.org/docs/current/sql-grant.html).
- pgJDBC [connection parameters, including currentSchema](https://jdbc.postgresql.org/documentation/use/).
- GitHub Actions [PostgreSQL service containers](https://docs.github.com/en/actions/tutorials/use-containerized-services/create-postgresql-service-containers).

**Release status:** Keep PR #40 draft and security issue #29 open until PostgreSQL tests pass, independent security review completes and the broader runtime/operator authorization gaps are resolved.
