# PostgreSQL purchase approval: restricted runtime privilege proof

Status: **DRAFT, not a production privilege policy or release approval.**
Trace: [P0 #89](https://github.com/danwiz/project0-cardealership/issues/89), [integration #85](https://github.com/danwiz/project0-cardealership/issues/85), stacked long-branch PR #93 and successor.

## Trust boundaries

The purchase executor is application-authenticated and must recheck the actor's current verified session. It also reads persisted reviewer and customer roles within the same SERIALIZABLE transaction that accepts the request and inserts ownership and the payment plan. Those controls do **not** replace SQL privilege separation or operator-owned migrations.

For PostgreSQL 16, `SELECT ... FOR UPDATE` on `accounts` requires `UPDATE` permission on at least one column, in addition to `SELECT`. **Do not grant blanket account UPDATE permissions.** The disposable CI proof demonstrates one deliberately limited design: grant an otherwise unnecessary column-level `UPDATE(encoded_credential)` privilege solely to satisfy PostgreSQL's row-lock permission check, while installing an owner-controlled, fail-closed `BEFORE UPDATE` trigger that unconditionally rejects every account mutation by the purchase role. The trigger is created under a separate trusted schema owner, not by the runtime process. Any real deployment requires independent review and maintenance of that trigger and its ownership, invocation security, migration ordering, grants, backup/restore, and defense against tampering.

Primary PostgreSQL references:
- https://www.postgresql.org/docs/16/ddl-priv.html
- https://www.postgresql.org/docs/16/explicit-locking.html
- https://www.postgresql.org/docs/16/mvcc-serialization-failure-handling.html

## Disposable CI test and grant matrix

`PostgresqlPurchaseLeastPrivilegeIntegrationTest` runs **only** against the exact known disposable PostgreSQL 16 CI instance. It generates a unique schema and runtime login, executes schema migration and account provisioning as a separate owner, then uses the restricted SQL principal for the real purchase executor. It never applies grants, triggers or migrations to external databases.

| Table | CI purchase role allowance | Important denial |
|---|---|---|
| `schema_history` | SELECT to verify migrated version | No DDL or mutation |
| `accounts` | SELECT and `UPDATE(encoded_credential)` needed for row locks, with owner-installed **deny-all UPDATE trigger** | No actual password/role updates, inserts or deletes |
| `inventory_listings` | SELECT; UPDATE `stock_quantity, active` | No arbitrary price or vehicle edits, inserts or deletes |
| `purchase_requests` | SELECT; UPDATE `status, payment_months, monthly_payment, contract_id` | No new unreviewed requests/deletes by purchase role |
| `owned_vehicles` | SELECT; INSERT only purchase contract, owner and vehicle fields | No deletion, arbitrary record replacement or DDL |
| `payment_plans` | SELECT; INSERT purchase plan fields | No arbitrary post-sale balance UPDATE or delete |

CI assertions cover the restricted transaction's **successful** approval and replay and denied SQL attempts to forge credentials, elevate account roles, insert/delete accounts, delete ownership and create tables.

## Open security and operational risks

1. A compromised purchase SQL principal still has the granted inventory/request UPDATE and ownership/plan INSERT powers. This fixture **does not** prove that arbitrary direct SQL cannot forge a purchase. A truly restrictive production architecture may require a narrow owner-controlled stored procedure, separate trusted writer service, RLS/constraint enforcement or stronger SQL authorization. Do not reuse the sample credential or role grants in production without that design review.
2. The deny-all trigger's code, ownership, execution privileges, immutability and schema migration/restore lifecycle are **critical**. Failing to install or verify it reopens a credential mutation path. Provision independently with least-privilege operator approval and monitor for drift.
3. A transaction's verified-session recheck does not guarantee an operator's account revocation becomes effective before a concurrently completing transaction; define explicit lock-order/revocation semantics and test them independently.
4. Retry logic classifies `40001/40P01` as bounded whole-transaction retries and connection/unknown completion as reconciliation-required; it does not by itself prove recovery after a lost actual COMMIT response.
5. Preserve original legacy source and all unreconciled drafts. Independent PostgreSQL, security and migration review must pass before any production release.

## Acceptance gates

- [ ] Disposable PostgreSQL 16 CI runs this restricted-principal test on both Java 11 and Java 17, with real connection, owner trigger, SQL denial assertions and atomic purchase/replay.
- [ ] Review alternative **stored-procedure/trusted-service** architecture and document why direct table grants are acceptable or not.
- [ ] Operator-signed privilege/trigger manifest, drift audit and migration/rollback plan for any actual deployment.
- [ ] Real serialization and deadlock events, lost COMMIT acknowledgment, restart, and concurrent revocation tests.
- [ ] Integrate competing branch stacks per #85 and complete an independent security/code review; do not merge or release prematurely.
