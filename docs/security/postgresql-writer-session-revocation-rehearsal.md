# PostgreSQL writer-session revocation rehearsal — disposable CI only

**Status: TEST IMPLEMENTATION / NO PRODUCTION ACCEPTANCE.** This is an
executable design rehearsal of the [writer connection gate](fenced-writer-connection-drain.md)
against **PostgreSQL 16** in the existing Java 11/17 GitHub Actions service
container. It does **not** approve G03/G04, prove SECRET-01, configure a real
secret manager, or revoke any production role.

## Isolation precondition

`PostgresqlWriterSessionDrainIntegrationTest` aborts unless all four values
match the disposable GitHub Actions fixture exactly:

- `CI=true`
- `DEALERSHIP_PG_TEST_URL=jdbc:postgresql://localhost:5432/dealership_ci`
- `DEALERSHIP_PG_TEST_USER=dealership_ci_owner`
- `DEALERSHIP_PG_TEST_PASSWORD=ci-only-postgres-fixture`

The test creates random schema and writer-role names, uses **synthetic**
credential values, and only calls `pg_terminate_backend` with a backend PID
obtained from its own writer connection and independently verified against
`pg_stat_activity.usename`. This is destructive fault injection on a
**throwaway test server**, never on a shared or operator-provided database.
The fixture instance, role, schema and transaction history are discarded
with the CI service container after the job.

## Assertions and boundaries

| Rehearsal | PostgreSQL evidence | Remaining production gap |
| --- | --- | --- |
| Local drain | Tracked writer transaction holds an uncommitted credential update; the gate pauses and closes it, the callback cannot report success, and the owner sees neither the mutation nor its trigger audit row | No multi-replica or pool inventory, independent operations witness or server-controlled quiescence |
| New generation | Old password cannot establish a new login (`28P01`); while the fixture credential source is unavailable the gate remains closed; fresh login and a strictly newer test-attested generation can reopen; a committed update adds one audit row | Test attestor is a local fixture, not workload identity, a secure lease provider or a durable generation store |
| Explicit termination | Changing a PostgreSQL role password does **not** itself end an established session; its uncommitted transaction remains active until an operator ends the confirmed test PID with `pg_terminate_backend(pid, 5000)`; commit then fails and the trigger audit row is absent | Production authorization, target-PID inventory, timeout escalation, active commit races and independent committed-event reconciliation |

PostgreSQL documents that a **nonzero timeout** on `pg_terminate_backend`
waits for process termination; a zero timeout merely confirms a signal was
sent. The test uses 5000 ms and asserts success. See the official
[server administration functions](https://www.postgresql.org/docs/16/functions-admin.html)
and [role alterations](https://www.postgresql.org/docs/16/sql-alterrole.html).

The negative audit assertions concern **uncommitted** updates only.
If a live transaction commits before a drain or termination fence, it may
already have taken effect; suppressing the callback result does **not** undo
that commit. A real security design must reconcile all committed trigger
rows with the independent append-only archive and prove no event was lost.

## Verification and release criteria

The ordinary `mvn test` and `mvn verify` jobs run this class automatically
on Java 11 and 17 when the exact disposable CI fixture is present. The CI
Maven output and Surefire XML are diagnostic evidence of a **CI fixture**, not
signed independent staging acceptance. Before marking production integration
gates passed, an independently authorized reviewer must capture:

1. An approved isolated staging database, credential-writer workload, real
   secret provider and two service replicas, with IDs and configuration digests.
2. Current and prior generation inventory, actual pool and backend PIDs,
   old/new-login denial and explicit termination/rollback confirmation.
3. A witnessed race with a transaction that commits immediately before the
   fence, including immutable audit event/archive reconciliation.
4. Outage, delayed connection open, stale replica, TLS/network partition,
   forced kill and restart cases with redacted, immutable evidence.
5. Reviewers, timestamps, artifact digests and explicit decisions in the
   [evidence register](credential-security-release-evidence-register.md).

Until then, keep security issue #29 **open**, this and upstream PRs
**draft/unmerged**, and production integration **NO-GO**. No deployment,
credential rotation, customer-data access or cloud subscription is authorized
by this CI test.
