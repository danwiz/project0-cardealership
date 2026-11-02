# v2.1 — Car Dealership integration adjudication (review only)
Date: 2026-11-02
Status: PROPOSED. No merge, source resolution, or application acceptance performed.

## Pinned inputs
- Default master: 90741d3abe51d9cc002aaeb1dacae27b933fba10
- Short PR #84: 086fbf91294f74fe4bd967256498ef94e92c501a
- Long PR #104: 45eccdc73ce36d4298326498a3603e2363251fd6
- Common ancestor: ad8d03574c7f048b236c9d24f0c03ac54dffcbd7
- Git evidence: https://github.com/danwiz/project0-cardealership/actions/runs/41493351165
- Tracking issue: https://github.com/danwiz/project0-cardealership/issues/85

Git confirmed 15 conflicts (13 content, 2 add/add). DO NOT merge sequentially or select one side wholesale.

## Proposed architecture baseline
Prefer the long typed-domain/JDBC architecture as an integration candidate because it includes verified sessions and durable transaction work. This is NOT an acceptance of its implementation. Explicitly port short-branch behavioral assertions (FIFO/stock preservation, concurrent account creation, role revalidation). Reject any resolution that weakens CI, hashing strength, or privileged-registration checks.

## Conflict-by-conflict proposed dispositions
| Path | Proposed disposition | Required test |
| --- | --- | --- |
| .github/workflows/maven-ci.yml | Combine all required checks, audit rights | All required jobs run on the integrated SHA |
| src/com/revature/cardealer/CarDealership.java | Keep configured composition, adapt safe short UI | No public privileged registration; revalidate roles |
| src/com/revature/cardealer/Offer.java | Keep typed request/listing model, port short FIFO behavior | Single-winner, bounds, no stock duplication |
| src/com/revature/cardealer/OwnedVehicle.java | Add/add: choose canonical typed ownership contract | Stable identity, authorization and references |
| src/com/revature/cardealer/PaymentPlan.java | Add/add: choose canonical payment model | Monetary precision, no double-charge |
| src/com/revature/service/AdminLoginService.java | Verified privileged provisioning only | Unauthorized provisioning denied |
| src/com/revature/service/CustomerLoginService.java | Preserve verified sessions and customer identity checks | Revocation and role forgery denied |
| src/com/revature/service/EmployeeLoginService.java | Fail-closed staff authentication | No customer-to-employee elevation |
| src/com/revature/service/InMemoryUserAccountRepository.java | Keep revision-aware changes, add duplicate-account tests | Atomic duplicate rejection |
| src/com/revature/service/UserLoginService.java | Preserve hashed credentials and session revision | Strong hashing and stale-session rejection |
| src/test/java/com/revature/cardealer/CarDealershipCharacterizationTest.java | Port nonredundant legacy behavior | Characterization + configured-mode flows |
| src/test/java/com/revature/cardealer/OfferCharacterizationTest.java | Port short FIFO/stock assertions to long domain | Concurrent single-winner, capacity, bounds |
| src/test/java/com/revature/service/RoleAuthorizationServiceTest.java | Combine role policy and persisted verified-session tests | Role/hash restoration must not revive session |
| src/test/java/com/revature/service/RoleLoginServiceCharacterizationTest.java | Keep negative privileged-provisioning cases | Unauthenticated operator denied |
| src/test/java/com/revature/service/UserLoginServiceCharacterizationTest.java | Combine credential and concurrency negative tests | Invalid credentials and duplicate mutations denied |

## Required order
1. Preserve immutable source SHAs and archive original PR patches.
2. Create a disposable separate integration branch from an explicitly reviewed long-head baseline; do not touch master.
3. Resolve CI and authorization gates; no reduced coverage.
4. Resolve ownership, payment, inventory and request models and then console/adapters.
5. Execute actual integrated build, database transaction/failure-injection tests and 12 acceptance scenarios.
6. Require independent security review and explicit owner signoff before any merge.

Release decision: BLOCKED. This document is an adjudication proposal, not resolved code or a release certificate.
