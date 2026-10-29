# Read-only reconciliation after uncertain PostgreSQL purchase completion

Status: **CI-ONLY ARCHITECTURE PROOF; NOT A DEPLOYMENT MIGRATION OR PRODUCTION API.**
References: issue #89, integration issue #85, prior draft #96.

## Problem and safety principle

A network break during `COMMIT` may leave the caller unable to tell whether PostgreSQL committed an otherwise valid purchase. Repeating `approve_purchase_with_permit` is **not** a safe recovery mechanism: after a successful commit its one-use permit is already consumed, and a second approval token must not sell the same inventory twice. Retrying a write is different from *reconciling* the original durable request.

A read-only, independently permissioned **receipt/reconciliation** operation checks the original permit, request and matching durable acquisition contract and payment plan. It never creates new approval rights and never changes inventory or ledger state.

## Three explicitly distinct observations

| Receipt outcome | Evidence | Required caller behavior |
|---|---|---|
| `COMMITTED` | Owner-issued permit was consumed and request ACCEPTED with matching contract, buyer, payment months, owned vehicle and payment-plan price | Return a verified receipt of the original purchase; do **not** submit another write |
| `NOT_CONFIRMED` | Permit exists and is unconsumed, while its purchase request remains PENDING in the observer's snapshot | **Not** permission to retry. Another transaction could start after this observation; obtain a fresh authorization decision under the operator's concurrency and idempotency policies |
| `INCONSISTENT` | Permit consumption and request/ownership/payment records are contradictory, incomplete, missing or tampered | Stop; initiate restricted investigation and corrective reconciliation, with no automatic purchase write |

Unknown UUID, wrong request ID or wrong installment terms are denied (SQLSTATE 42501). Do not reveal whether a token exists to unauthorized callers.

The reader uses a separate **reconciler** database identity, granted `EXECUTE` on one owner-controlled `SECURITY DEFINER` receipt function and **no direct SELECT/INSERT/UPDATE/DELETE** on permit or business tables. Ordinary reader and dedicated purchase writer credentials cannot call the receipt function. Fixed trusted `search_path = pg_catalog, pg_temp`, fully qualified relations and verified `session_user` are required. The function first waits on a `FOR SHARE` permit row lock to avoid reading a partially committed consumption, then cross-checks request, ownership and payment-plan records. The function itself performs no DML or DDL.

## Disposable PostgreSQL CI regression

`PostgresqlPurchasePermitBoundaryTest` adds three tests:
- A newly issued, unconsumed permit reports `NOT_CONFIRMED`; after authorized completion, `COMMITTED` returns matching acquisition ID and price. Wrong token/scope, reader/writer invocations and reconciler direct SQL access are denied. Deliberate owner-side corruption produces `INCONSISTENT`, not a positive receipt.
- Simulated **application-level loss of the response AFTER successful COMMIT** (SQLException SQLSTATE 08006 generated in the test client) is resolved from a new reconciler connection without running purchase again. **This is not an actual dropped network packet or PostgreSQL failover test.**
- An owner-injected ownership-insert trigger failure rolls back the transaction, retains an unused permit, returns `NOT_CONFIRMED`, and never produces an invented committed receipt. A separate, explicitly authorized call can complete later, but the receipt function itself cannot.

All fixtures require the exact CI-only PostgreSQL 16 database URL, CI flag and known disposable credentials. Randomized schema and roles are created by the CI owner, not by an application account. Creating functions, revoking default PUBLIC EXECUTE and granting exactly one reconciler role happen in a **single trusted owner transaction**.

## Remaining release blockers

1. **Neither the issuer nor the reconciler is integrated with production**. The real configured Java JDBC command still uses the table-writing executor. Separate approval/receipt service processes, secure transport, narrow roles and authentic caller/session identity must be designed, provisioned and reviewed before real deployment.
2. **Permit UUID is a bearer secret**, and the receipt function trusts the reconciler SQL login. Production needs independently authenticated authorization for each receipt, scoped access, secrecy, rate limiting, audit and retention rules to avoid leaking purchase information.
3. The receipt function is a snapshot of available evidence, **not proof of global linearizability or the absence of a future write**. `NOT_CONFIRMED` must never be interpreted as permission to retry automatically. Consider transaction ID, status versioning, durable outbox and signed receipts under the eventual production design.
4. The current CI loss simulation throws a client exception **after a known successful database commit**. Actual loss of COMMIT acknowledgment, process crash, restart, failover, standby replication lag and cross-region recovery are **not verified**.
5. Function definition, schema owner, role grants and revocation, search-path integrity, operator migrations and read-only guarantee require independent database and security review. Avoid untrusted function/extension names and blanket EXECUTE grants.
6. Purchase state/payment-plan consistency is not a complete payment ledger audit; `rejectOtherPending`, buyer consent, account revocation races and real PostgreSQL serialization/deadlock conditions still require integration tests.
7. The short and long development PR stacks remain separate. **Do not merge or publish a production release** based on this draft.

References: [PostgreSQL 16 function security](https://www.postgresql.org/docs/16/sql-createfunction.html), [transaction isolation](https://www.postgresql.org/docs/16/transaction-iso.html), and [explicit locking](https://www.postgresql.org/docs/16/explicit-locking.html).
