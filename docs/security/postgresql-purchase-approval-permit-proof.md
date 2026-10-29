# PostgreSQL one-use purchase approval permit — disposable CI proof

Status: **DRAFT / NON-PRODUCTION.** Trace: #89 atomic purchase and #85 branch reconciliation, after #95's SQL reader/writer isolation proof.

## Concrete risk

PR #95 demonstrated a general read-only SQL principal cannot write purchases, while a dedicated writer can EXECUTE one owner-controlled `SECURITY DEFINER` procedure. Its CI function nevertheless trusted a static reviewer; a stolen writer SQL credential could authorize purchases without an independent authorization decision.

This successor proof makes the SQL writer's password **insufficient by itself**: the function requires an **unexpired, unrevoked, owner-issued opaque UUID permit**, atomically bound to a persisted reviewer, buyer, exact request and installment terms. The SQL writer cannot create, read, modify or delete permits, or write business tables directly.

### Data and authority contract

- **Trusted issuer (CI schema owner only):** creates a `purchase_approval_permits` row with random UUID, reviewer identity, buyer identity, request ID, payment term, UTC-aware `expires_at`, `revoked`, optional consumed contract/price and `consumed_at`. The owner can revoke eligibility by changing roles or setting the permit revoked flag. The owner credential **never** appears in the writer connection.
- **Dedicated writer (EXECUTE-only):** calls `approve_purchase_with_permit(permit UUID, request ID, payment months)`. It cannot SELECT/INSERT/UPDATE/DELETE the permit table or purchase entities. The function uses `SECURITY DEFINER`, a pinned `search_path = pg_catalog, pg_temp`, owner-qualified relations, and `session_user` to restrict caller identity. CREATE/REVOKE PUBLIC/GRANT happen in one owner transaction.
- **Ordinary reader:** lacks EXECUTE and direct purchase DML, even with a permit.
- **Function:** locks the permit row, checks non-consumption, revocation, expiration with `clock_timestamp()`, and exact request/term binding. It locks and checks the persisted reviewer and buyer roles, pending request owner and stock. In one transaction it decrements inventory, accepts request, inserts ownership/payment plan and records consumption. A statement/transaction failure rolls back **both** permit consumption and business writes.
- **Replay:** the same permit is rejected after its first successful use; a second permit targeting an already accepted request is also rejected. The owner can reconcile a consumed permit using the stored contract ID and purchase price. This proof differs intentionally from the earlier Java command's idempotent replay; production must define an explicit **read-only reconciliation/receipt endpoint**, not replay a write authorization.
- **Expiry:** uses PostgreSQL `clock_timestamp()` (the actual time at evaluation), not `CURRENT_TIMESTAMP` (the start time of a possibly old transaction). See [PostgreSQL 16 datetime functions](https://www.postgresql.org/docs/16/functions-datetime.html).

## CI-only test coverage

`PostgresqlPurchasePermitBoundaryTest` runs only when the exact disposable PostgreSQL 16 CI environment variables match. It creates random schema and distinct SQL logins and never touches production:
- A real approved permit succeeds; wrong request, payment terms, unknown token, and attempts to mint permits from the writer are denied.
- The general reader lacks EXECUTE; writer lacks direct business table modification.
- First success persists inventory/request/owned vehicle/plan and consumes the permit. Reuse or a second valid permit for the accepted request fails without duplicate stock decrement.
- Expired, revoked, wrong buyer, and now-demoted reviewer permits fail before changing stock.
- An owner-injected `BEFORE INSERT` ownership failure returns an error, leaves request PENDING and stock untouched, and **does not consume** the permit. Once the fault is removed, the original permit is still valid and succeeds.
- Full Maven Java 11/17 PostgreSQL CI must prove tests actually ran rather than being skipped.

## Critical remaining security/design gaps

1. **Issuer authentication is NOT implemented.** The fixture uses the PostgreSQL schema owner directly to mint a row; this must never be a production application identity or a general API. Production needs a separately authenticated, authorized issuer that verifies the reviewer’s live identity, permissions, buyer consent, exact request version and terms before minting a permit. SQL row binding alone does not prove the issuer performed those checks.
2. The permit UUID is a **bearer capability** in this test. A compromised writer that steals a valid unspent token may spend it within its scoped terms. A production service needs independent caller/service attestation, credential isolation, token confidentiality, bounded TTL, replay audit and incident revocation.
3. **The production Java executor is not wired to this function.** It still has direct table DML from the #94 design. Do not claim this proof establishes least-privilege production operation.
4. The function does not implement `rejectOtherPending`, signed buyer/reviewer receipts, durable security-event publishing or payment ledger updates. Define behavior and transaction boundaries before integration.
5. PostgreSQL's SERIALIZABLE contention retry and unknown-COMMIT acknowledgement need an explicit reconciliation contract; the permit's `consumed_at`/contract can help, but an authoritative read path must be separately authorized, audited and resistant to data leakage.
6. Review privilege default changes, schema owner and function integrity, audit/monitoring, drift detection, backups, migration/rollback, SQL injection and temporary-schema masking before deployment.
7. Keep original legacy code and competing development stacks preserved; this fixture is evidence toward a design review, **not** a release or merge acceptance.

Authoritative docs: [PostgreSQL 16 CREATE FUNCTION](https://www.postgresql.org/docs/16/sql-createfunction.html), [GRANT](https://www.postgresql.org/docs/16/sql-grant.html), [Date/time functions](https://www.postgresql.org/docs/16/functions-datetime.html).
