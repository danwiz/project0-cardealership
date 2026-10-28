# Disposable PostgreSQL signed-pin publication and privilege rehearsal

**Scope:** CI PostgreSQL 16 container only. The test refuses to run unless
`CI=true` and the JDBC URL, owner username, and fixture password exactly match
the dedicated GitHub Actions environment. No managed cloud account, external
database, secret manager or paid service is involved.

`PostgresqlSignedPinPublicationIntegrationTest` creates a random schema and
two temporary login roles **inside the disposable CI container**. It seeds
one test revision-floor row and a database-owner-controlled trigger that
rejects key changes and non-increasing revisions.

| CI principal | Allowed | Explicitly rejected |
| --- | --- | --- |
| Reader | `USAGE` on isolated schema; `SELECT` on pin floor | Revision `UPDATE` |
| Publisher | `USAGE`, `SELECT` and column-scoped `UPDATE(minimum_revision)` | Change key, `DELETE`, trigger disable/DDL, direct non-increasing revision |
| Fixture owner | Creates schema, table, roles, trigger and initial floor **only within CI** | Never injected into runtime reader or publisher adapter |

The provider-neutral JDBC adapter is composed with the actual PostgreSQL
reader and publisher roles, independent synthetic pin-issuer and publisher
RSA public-key trust anchors, a simulated in-memory monotonic witness and a
separate durable-document read-back seam.

1. A valid signed publisher command is **denied** when the signed pin is not
   yet visible to the independent read-back adapter; neither witness nor SQL
   advances.
2. After the same exact signed document is made readable, the command claims
   revision 5 from an existing revision-4 floor; the test reloads floor 5
   through the reader identity.
3. Replay of the identical signed command is denied without a second advance.

The revision trigger is owned by the CI database owner, not the publisher.
Actual production design must independently validate trigger ownership,
column grants, row/tenant scoping, role revocation, schema search-path safety,
function replacement/DDL bypass, WAL/replication, and tamper-evident audit.
The fixture's in-memory witness and signed-document map do **not** prove
external durability, independent custody, immutable retention or true
cross-provider read-after-write consistency.

Production issue #29 stays open, and G03–G09, ARCH-01/02, CKPT-01 and
SECRET-01 remain **NOT RUN / NO-GO**. No CI fixture SQL is a production
migration or authorization to deploy.
