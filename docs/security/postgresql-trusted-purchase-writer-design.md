# Trusted PostgreSQL purchase-writer boundary — evaluation

**Status:** Disposable CI architecture proof, **not production implementation, migration or approved release**. Follow issues #89 and #85.

## Threat and acceptance goal

In the previous [restricted-purchase principal proof](postgresql-purchase-authorization-boundary.md), the JDBC purchase role was restricted by table-level and column-level grants but still retained raw SQL access to inventory and request updates and ownership/payment inserts. Compromise of those SQL credentials could bypass the Java command's authentication and state-machine checks.

The proposed stronger control splits credentials and responsibilities:

| Principal | SQL permissions | Trust assumptions |
|---|---|---|
| Trusted schema owner/migrator | Owns tables and one narrowly scoped function; installs/revokes grants and migrations | Isolated operator identity; never exposed to general application runtime |
| Ordinary application **reader** | SELECT on the permitted business tables; no INSERT/UPDATE/DELETE/CREATE and **no EXECUTE** on purchase-finalization function | Credential compromise alone cannot directly finalize a purchase or alter rows |
| Dedicated purchase **writer** | Schema USAGE, schema_history SELECT, and **EXECUTE on exactly one owner-controlled function**; no business-table SELECT/INSERT/UPDATE/DELETE/CREATE | Credential must be isolated from ordinary runtime; compromise *still permits calling the function* with allowed parameters |

The PostgreSQL 16 disposable CI test creates one randomized schema and random role names and proves denial of raw SQL and accidental PUBLIC function execution. The writer calls a schema-qualified `SECURITY DEFINER` PL/pgSQL function with `SET search_path = pg_catalog, pg_temp`; the function uses **fully qualified table names** and checks `session_user` so an owner-defined function does not mistake its own `current_user` for the caller. It checks a known CI reviewer role, buyer eligibility, request status and available inventory, then changes request, stock, ownership and payment plan inside the **single SQL statement's transaction**, with contract-linked replay only when ownership/plan exists.

### Proof-specific restrictions and non-transferable assumptions

- **The proof's reviewer name is deliberately a static CI fixture, not real identity binding.** Do **not** promote the sample SQL function to production. A compromised dedicated writer credential can still approve purchases despite Java authentication. The production design needs an authenticated, authorization-checked approval message passed from an isolated service with a verifiable binding between reviewer, request, immutable terms and environment, e.g. a short-lived one-time authorization envelope validated independently of caller-controlled usernames. Key management, replay prevention, identity revocation and audit logging require separate design/review.
- The SQL template contains placeholders filled only with internally generated UUID-based identifiers in CI. It is **not** an operator-run production migration script, and no production schema/role/credential is modified.
- Function creation, `REVOKE EXECUTE FROM PUBLIC` and selective `GRANT EXECUTE TO writer` occur in **one owner transaction**, preventing a temporary public-execution window. The dedicated writer does not own the function or business tables.
- The `search_path` excludes untrusted app schemas and places `pg_temp` last; references to business tables are schema-qualified. Security review must also evaluate search-path-dependent types, operators, SQL function code, default public grants and possible temporary-object masking.
- PostgreSQL automatically performs each function call atomically inside the containing transaction. **Do not** presume this SQL fixture implements all semantics of the Java `JdbcPurchaseApprovalService`: `rejectOtherPending`, real reviewer identity/session, execution tracing, deadlock retries, and payment-ledger semantics remain unimplemented.
- A dedicated writer SQL credential is a capability. It must not live in the same process or secret store as the general runtime DB credentials without an explicit threat model. Use independent credential source, network boundary, least-privileged service identity and audit controls.
- The test's owner can update/revoke the reviewer and EXECUTE authorization; it proves revocation is checked at the function boundary, not an atomic session-kill guarantee for already-running commits.

## Mandatory production design questions

1. Who is permitted to issue an approval message, and how does the writer validate the exact authenticated reviewer, buyer, request ID, terms, current role and request version **without trusting a caller-controlled User or database credential alone**?
2. Which trusted process/role holds the writer connection? What prevents a compromised ordinary runtime, including its host credentials, from accessing that credential or writer endpoint?
3. How are messages authenticated, authorized, audited, time-bound, signed or otherwise non-forgeable, and replay-rejected? Which versioned idempotency keys survive crashes/retries?
4. Can the schema owner/operator review and sign the function definition, grants, revocations, search-path settings and migrations, detecting drift?
5. How will the application implement `rejectOtherPending`, payment-plan/ledger integrity and real multi-connection `40001/40P01` conflict handling?
6. What evidence confirms recovery after **actual** COMMIT acknowledgment loss, role revocation races and process failover?
7. Can the team prove all privileged writes go through the trusted writer, with DB-denied direct DML for general app users, including future code and operator tools?

## CI acceptance

- Run `PostgresqlTrustedPurchaseWriterBoundaryTest` against the exact disposable PostgreSQL 16 CI fixture, never another database.
- Confirm general reader cannot call function, INSERT/UPDATE/DELETE or DDL; writer cannot select business tables or issue direct DML/DDL.
- Confirm writer-only function succeeds on a first purchase, is idempotent on exact replay, refuses sold-out second request and refuses a revoked owner-provisioned reviewer.
- Confirm revoking writer `EXECUTE` takes effect independently of its own SQL credentials.
- Verify both Java 11 and 17 Maven CI; keep this as a **draft-only reference proof** until code and security reviewers accept a real production design.

## Authoritative references

- PostgreSQL 16: [CREATE FUNCTION — Writing SECURITY DEFINER Functions Safely](https://www.postgresql.org/docs/16/sql-createfunction.html)
- PostgreSQL 16: [Privileges](https://www.postgresql.org/docs/16/ddl-priv.html)
- PostgreSQL 16: [GRANT](https://www.postgresql.org/docs/16/sql-grant.html)
