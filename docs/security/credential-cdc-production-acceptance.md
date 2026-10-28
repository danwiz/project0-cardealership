# Production CDC source acceptance gates — provider-neutral, non-deployed

**Status:** Requirements and CI fixtures only. No production PostgreSQL adapter,
replication slot, external audit archive, checkpoint service, secret, alert transport,
cloud entitlement, database migration or deployment has been activated.

The `AuditSource.readAfter(cursor, limit)` adapter is trusted to establish **complete
commit-order replay** before it returns events or an empty batch. The forwarder can
validate ordering and stop on explicit failure, but it cannot independently detect
an omitted committed event. A passing `test_decoding` integration test does not
constitute production source coverage evidence.

## Required source guarantees

1. **Source-generation identity:** Bind checkpoints, replication state, and archive
   namespace to an independently verified source generation. A database restore,
   slot recreation, promotion or timeline change must not silently reuse the same
   stream identity without a reviewed continuity proof. Unknown continuity fails
   closed; do not jump to the current head of WAL.
2. **Complete transactions in commit order:** Preserve every committed audit row
   exactly or at least once in transaction commit order. The cursor is a stable
   `(unsigned commit position, per-transaction audit-event ordinal)`, including
   transactions with several audit rows and audit IDs allocated before commit.
   Never derive ordering from `audit_id` or assume numeric LSN gaps are data loss.
3. **Bounded pages without transaction loss:** `readAfter(C, N)` returns at most
   `N` events, each strictly after C. Repeated calls after the last event in the
   page must continue within the same commit as necessary. Page boundaries,
   process restart, retry, WAL retransmission and parser buffering must not
   renumber the ordinal or lose the remainder of the transaction.
4. **Retention/coverage preflight:** Verify that the requested checkpoint is
   still fully replayable, including the relevant replication slot, its identity,
   confirmed position and required retained WAL. Distinguish **verified idle**
   from unavailable source, slot loss, pruned history, invalidated stream,
   unexpectedly advanced slot acknowledgement, or unprovable generation.
   On the latter cases throw `AuditHistoryUnavailableException` or a transient
   source exception as appropriate; neither may look like an empty success.
5. **Metadata fidelity and authorization:** Decode real audit ID, subject,
   database actor, event timestamp and trusted source identity, with correct
   encoding and bounds. No credential plaintext or encoded credential may enter
   audit payloads or alerts. Verify least-privilege replication and independently
   controlled credentials in the eventual target environment.
6. **Acknowledgement boundary:** The source may be acknowledged/advanced only
   as far as records already durably appended and durably checkpointed. A
   checkpoint failure after append must replay the same immutable event; the
   archive deduplicates identical keys and rejects conflicting content. Check
   cross-worker CAS, fencing, crash timing and archive outage recovery.

For PostgreSQL 16, inspect `pg_replication_slots` metadata such as
`restart_lsn`, `confirmed_flush_lsn`, `wal_status` and `safe_wal_size` as part
of the adapter's retained-history proof. A healthy-looking existing slot is
not sufficient by itself: a slot may have advanced past an unacknowledged
external checkpoint. PostgreSQL documents that logical slot positions can
move backwards after crash and retransmit changes, and that logical streaming
may begin at the later of the requested LSN or `confirmed_flush_lsn`. The
adapter must prove resumption semantics, not infer them from a successful SQL
query.

- [PostgreSQL 16: logical decoding concepts](https://www.postgresql.org/docs/16/logicaldecoding-explanation.html)
- [PostgreSQL 16: replication slot state](https://www.postgresql.org/docs/16/view-pg-replication-slots.html)
- [PostgreSQL 16: streaming replication protocol](https://www.postgresql.org/docs/16/protocol-replication.html)

## Acceptance matrix before any production integration

| Scenario | Current automated evidence | Production release gate |
| --- | --- | --- |
| Out-of-order audit IDs due to late commit | Disposable PostgreSQL 16 `test_decoding` fixture | Verify on real source adapter with overlapping transactions and rollback |
| Multiple rows in one commit; limit=1 pages | CI fixture checkpoints between two same-commit ordinals | Prove stable ordinals across network frame and restart boundaries |
| Checkpoint fails after archive append | CI fixture retries after simulated restart; immutable fake sink deduplicates | Verify crash/power-loss timing with real durable sink and checkpoint |
| Duplicate key with conflicting payload | Unit tests reject in-page and cross-page collision | Real archive rejects mismatch without replacing stored record |
| Slot removed after checkpoint | CI fixture halts and raises history-unavailable alert | Distinguish source recreation, generation mismatch and stream replacement |
| Slot exists but WAL is pruned or was over-acknowledged | **Not tested** | Force retention exhaustion and over-acknowledgement; prove fail-closed behavior |
| PostgreSQL crash and retransmission | **Not tested** | Restart database/consumer independently, verify no omission or data conflict |
| Replica promotion, timeline/source restore | **Not tested** | Prove continuity or reject generation and require reviewed rebootstrap |
| Large transaction / bounded buffering | **Not tested** | Verify finite memory and backpressure with batches splitting transactions |
| Actual metadata and tamper resistance | Test decoder uses synthetic timestamps | Validate real timestamp/actor/subject and independent owner-threat assessment |
| External WORM destination and audited alerts | In-memory test fakes only | Independent retention, write-once, access, dedup, alert delivery and review |

**Stop condition:** Any unknown history/identity, missing committed row,
unstable replay cursor, conflicting duplicate key, or unverifiable archive
acknowledgement blocks release. Keep all work draft/unmerged until independent
security review and production integration approvals.
