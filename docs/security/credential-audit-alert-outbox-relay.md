# Provider-neutral audit alert outbox relay — test-only acceptance contract

**Status:** Non-deployed seam and in-memory contract tests. No actual email,
pager, chat notification, cloud queue, database job, durable external service,
network destination, credential or paid entitlement is configured.

PR #54 established an independently persisted metadata-only alert **outbox**
and exact-key/content receipts. Outbox persistence is not the same as an
operator receiving a critical security alert. This increment adds
`AuditAlertOutboxRelay` to define the missing delivery and acknowledgement
order while remaining neutral to alert routing, queuing and storage providers.

## Delivery and failure semantics

1. `VerifiedAuditAlertSink` creates each immutable `AuditFailureNotice` with a
   new unique ID. The external outbox backend persists it and returns a durable
   receipt before the originating failure is considered recorded. Original audit forwarding
   failures remain unmasked even when alert persistence fails; a separate
   monitor must detect outbox storage outages.
2. A relay worker obtains a **bounded, atomically leased** batch via
   `PendingNoticeStore.claimPending(limit, ttl)`. Every claim includes the
   metadata-only notice and a new opaque UUID **fencing token**. The backend
   must make expired undelivered notices eligible again and prevent concurrent
   claims with the same current token. A claim is not an acknowledgement.
3. `DeliveryTransport.deliver(notice)` sends only permitted metadata and
   returns a receipt with the **same notice key and canonical content SHA-256**
   *after durable delivery acceptance*. Missing or mismatched receipts halt
   the batch. An accepted send may be retransmitted if acknowledgement fails.
4. Only after matching delivery acknowledgement may the worker call
   `markDelivered(claim)`. The backend must durably and atomically match the
   **current claim token and unchanged notice digest**. A stale token or
   changed notice returns false, halting this worker. Retain immutable outbox
   evidence according to the selected retention policy; do not delete it merely
   because a send was attempted.
5. After a network error, worker crash, lease expiry or an ambiguous downstream
   acknowledgement, **retry** the same notice key/content. The destination
   should deduplicate if it supports it, but duplicate notifications may still
   occur. This is **at-least-once**, not exactly-once, delivery. Failed retries
   need backoff, attempt limits, poison-notice quarantine and dead-letter
   treatment without silently dropping evidence.

## CI-only acceptance cases

| Failure injection | Required observation |
| --- | --- |
| Two pending notices, batch limit one | Two deliveries, then verified empty queue |
| Transport unavailable before acceptance | No outbox acknowledgement; retry after lease expiry |
| Transport accepted, connection lost before reply | Exact notice replay, one immutable accepted record, then acknowledgement |
| Wrong delivery receipt key | Fail closed; pending notice survives to retry |
| Lease expires and second worker reclaims | Old worker cannot acknowledge second worker's notice |
| Outbox acknowledgement persistence fails after successful delivery | Retry duplicates delivery attempt but not immutable destination key |
| Duplicate notice claim in one batch | Reject before outbound message |
| Invalid batch or TTL configuration | Reject before operation |

## Production integration gates (not satisfied)

- Actual outbox and transport must be independently administered, encrypted,
  authenticated, retention-controlled and failure-monitored. A return value
  from a fixture does **not** establish delivery durability or receipt honesty.
- Review whether the destination offers idempotent acceptance by notice ID.
  Consumer retries can generate duplicates even within visibility timeouts;
  choose routing, deduplication and escalation policies with operators.
- Implement scheduled draining, capacity/age alarms, lag SLO, backpressure,
  bounded retry with jitter, dead-letter review and out-of-band alerting for
  failing outbox/relay/destination. Do not recursively depend on the same
  failed alert path to report its own outage.
- Test real concurrency, clock skew, token uniqueness, failover, outage
  recovery, poison records, latency, log redaction and permission separation.
  A JVM's `synchronized` is not distributed fencing.
- Subject, actor, password, hash, request headers and raw exception messages
  must **never** be added to the relay notice or delivered through this path.

The [AWS SQS visibility-timeout documentation](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html)
illustrates why visibility/lease expiry and at-least-once delivery require
idempotent consumers. It is a reference example, **not** a provider selection.

Keep the PR draft/unmerged and issue #29 open. This seam is not a substitute
for independent security review or live integration acceptance testing.

The [production integration plan](credential-security-production-integration-plan.md)
and [blank evidence register](credential-security-release-evidence-register.md)
list the real external alert and independent monitoring tests that must pass
before any deployment approval.
