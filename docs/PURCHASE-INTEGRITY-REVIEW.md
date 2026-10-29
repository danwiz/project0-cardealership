# Purchase integrity and transaction boundary review

Status: draft PR #82; not merged into master.

## Verified behavior

- A reviewer must authenticate and hold purchase-review permission.
- A pending buyer must exist as a registered customer.
- Full listing price, per-listing inventory and a validated payment plan are checked before approval.
- Ownership is keyed by customer username; the reviewer does not become the owner.
- Offer mutations and the purchase operation synchronize on the shared Offer instance. Customer ownership writes and snapshot reads are synchronized on the CustomerLoginService instance.
- The purchase service stages ownership before calling Offer.setAccept. If Offer.setAccept throws, it attempts to remove only that staged ownership record, preserving any older records.
- JUnit tests cover simultaneous approvals of a one-unit listing, ownership-write failure before inventory mutation, inventory-commit exception before mutation, sold-out/repeated approval and customer isolation.

## Explicit limitations / remaining blockers

1. **Not an ACID transaction.** All state is process-local and is lost on restart. No durable purchase, inventory, or payment ledger is implemented.
2. **Partial mutation on a thrown exception.** If a future Offer.setAccept implementation mutates stock or pending requests *before* throwing, compensation removes the staged ownership but cannot restore Offer state. Current tests inject an exception before mutation only.
3. **Independent legacy mutation paths.** Direct callers can use Offer.setAccept without creating ownership, bypassing the coordinated service. Synchronization prevents overlapping mutations but not business-logic bypass.
4. **Lock scope.** Separate Offer or CustomerLoginService instances do not coordinate. The shared service objects must be used throughout the running application.
5. **No durable idempotency.** There is no persisted transaction identifier or unique approval constraint to prevent duplicate processing across restarts.
6. **No durable audit trail.** Purchase authorization and financial events are not committed to an append-only store.
7. **Compensation can fail.** If removal of a staged record itself fails, the original exception retains the rollback failure as a suppressed exception; manual reconciliation may be necessary.

## Merge gate

- Confirm Java 11 and Java 17 CI on the final head.
- Review direct Offer approval call sites and decide whether to restrict them or route them through the authorized purchase service.
- Define an explicit transaction/idempotency model before introducing persistence or payments.
- Preserve historical source and provenance; no direct modifications to master from this PR.

## Direct-call audit (integration branch)

- `CarDealership.reviewPurchaseRequests` calls `AuthorizedPurchaseReviewService.approveForCustomer`; no direct `setAccept` call was found in this console entry point.
- `AuthorizedPurchaseReviewService` is the coordinated purchase call site for `Offer.setAccept(int,int,boolean)` in the inspected production files.
- `Offer` still exposes public `setAccept()` and `setAccept(int,int,boolean)` methods. Their visibility allows external code to bypass the coordinated ownership ledger; a full-repository search and API migration are required before claiming enforcement across all consumers.
- This review is scoped to the three inspected production files; it does not certify every repository path or external caller.

## Legacy API migration decision

The existing `OfferCharacterizationTest` directly exercises `Offer.setAccept(int,int,boolean)` for baseline acceptance, per-listing stock, sold-out behavior and rejected requests. Removing or making this method private inside the integration PR would break those characterization contracts and complicate the legacy preservation review.

**Decision for PR #82:** retain the public legacy method for compatibility, but treat it as a documented bypass rather than an authorized purchase endpoint. Do not claim full authorization enforcement while it remains public. A separate modernization change should introduce a transaction-oriented inventory interface, migrate production consumers, and then deprecate/restrict the old method with an explicit compatibility decision. The no-argument `setAccept()` is also a legacy state setter, not a coordinated purchase operation.

**Additional test needed before a production guarantee:** fault injection that throws *after* an Offer mutation, to verify either an Offer rollback mechanism or an atomic inventory commit implementation. The current compensation protects staged ownership when inventory approval throws before mutation, not when it throws after changing stock or the pending queue.
