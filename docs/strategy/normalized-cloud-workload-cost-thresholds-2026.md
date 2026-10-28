# Equal-workload cloud and self-hosted cost / growth model

**Model snapshot: 2026-10-28. Status: planning calculator on paper only.**
No cloud prices, billing account, credit, promotional eligibility, resource,
deployment or spend approval has been claimed. The numeric unit rates below
are **invented for sensitivity analysis, not AWS/Azure/GCP/Ceph quotes**.

This complements the [cloud/SWOT/CBA business cases](cloud-business-cases-swot-cba-2026.md)
and the [provider-neutral service catalog](provider-neutral-cloud-self-hosted-options-2026.md).
Each prospective provider receives identical workload, geographic placement,
retention, redundancy, trust boundary and recovery obligations.

## 1. Reference workload: immutable signed-document receipts

Baseline assumptions:

- Each signed event is **4 KiB** of object payload including its signature;
  metadata, index, audit records, transport frames and keys **excluded**.
- Exactly **one new object and one independent exact-object read** per event,
  with a 30-day billable month and continuous daily activity.
- Every event is kept **365 days**; retained capacity is the **steady-state**
  rolling 365-day amount, not first-month occupied capacity. No compression,
  storage class transitions, replicas, indexes, multipart uploads or retries.
- Decimal event counts; binary storage conversion **GiB = bytes / 2^30**.
  One event/day averaged over a day is 1/86,400 requests/sec; the user must
  supply peaks and concurrency separately. Even a small daily mean can have
  a large peak.

| Profile | Events per day | New events per 30 days | New GiB / 30 days | 365-day retained GiB | Average write RPS |
| --- | ---: | ---: | ---: | ---: | ---: |
| **Learning / CI lab** | 100 | 3,000 | 0.0114 | 0.1392 | 0.00116 |
| **Solo / boutique service** | 10,000 | 300,000 | 1.1444 | 13.9236 | 0.11574 |
| **MSME multi-tenant** | 100,000 | 3,000,000 | 11.4441 | 139.2365 | 1.15741 |
| **Enterprise illustrative** | 1,000,000 | 30,000,000 | 114.4409 | 1,392.3645 | 11.57407 |

These tiers are **hypothetical workload samples**, not a claim that freelance
or MSME customers actually generate this number of security events. A
personal-brand static site could generate almost none. Use actual tenant
instrumentation and privacy-reviewed event estimates before sizing.

### Repeatable calculation

```text
monthly_writes = events_per_day * 30
monthly_exact_reads = monthly_writes * reads_per_write  # baseline 1
monthly_new_gib = monthly_writes * payload_bytes / 2^30
steady_retained_gib = events_per_day * retention_days * payload_bytes / 2^30

monthly_storage = steady_retained_gib * storage_usd_per_gib_month
monthly_puts = monthly_writes / 1000 * put_usd_per_1000
monthly_gets = monthly_exact_reads / 1000 * get_usd_per_1000
monthly_raw_object_cost = monthly_storage + monthly_puts + monthly_gets
```

For a **purely illustrative** low-volume object-meter sensitivity, take
`$0.025/GiB-month` storage, `$0.006/1,000` write operations and
`$0.0005/1,000` exact reads:

| Profile | Illustrative storage/month | Writes/month | Reads/month | **Raw object subtotal/month** |
| --- | ---: | ---: | ---: | ---: |
| Learning / CI lab | $0.0035 | $0.018 | $0.0015 | **$0.0230** |
| Solo / boutique | $0.3481 | $1.80 | $0.15 | **$2.2981** |
| MSME | $3.4809 | $18.00 | $1.50 | **$22.9809** |
| Enterprise | $34.8091 | $180.00 | $15.00 | **$229.8091** |

**Do not use these subtotals as a cloud spending forecast**. A request can
also incur encryption-key operations, audit logging/retention, versioning,
replication, lifecycle transitions, monitoring, NAT/egress, backup, data
scanning, access governance, network transfer, witness/CAS, queue, database,
support and people costs. Immutable legal holds, storage minimums and
replication can multiply occupied bytes. At these small object sizes,
per-request and security-control charges can exceed raw byte storage.
Self-hosting also incurs disks, power, physical controls, reserve capacity,
offsite media, patching, monitoring and on-call.

### Sensitivities to rerun before business case approval

| Input change | Arithmetic effect | Security / operations implication |
| --- | --- | --- |
| 4 KiB becomes 16 KiB/event | Storage ×4; request count unchanged | Enforce payload bound and measure metadata, versions and indexes |
| Two required exact reads per write | Read operations ×2 | Stronger validation can cost money; never skip needed read-back to cut costs |
| Retain 3 years instead of 365 days | Steady retained bytes about ×3 | Retention/legal requirements and irreversible settings need owner approval |
| Two independent replicas | Storage approximately ×2 **before** replication traffic/ops | Replica is not independent audit witness by itself |
| Event volume ×10 | Stored bytes and baseline request counts ×10 | Re-run concurrency, quota, incident, WAL/CDC backlog and cost tests |
| 5% additional write attempts | Request count may rise up to 1.05× depending on retry policy | Ambiguous commit resolution must use exact read, not duplicate PUT |
| External key per-object operations | Separate, provider-dependent line item | KMS key continuity, revoke tests, cost and trust separation are required |

## 2. Equal-scope quotes: do not mix free-tier limits with an enterprise SLA

Fill every cell from **official calculators / actual contract terms**, attach
snapshot date and chosen region, and record missing items explicitly. All
current numeric quote entries are intentionally **TO QUOTE**.

| Control / cost input | AWS S3 | Azure Blob | Google Cloud Storage | Ceph RGW self-host |
| --- | --- | --- | --- | --- |
| Target geography / legal residency | TO CHOOSE | TO CHOOSE | TO CHOOSE | Site(s) TO CHOOSE |
| Storage tier and billable bytes | TO QUOTE | TO QUOTE | TO QUOTE | Disk capacity, replication and reserve |
| Conditional create, 30-day write count | TO QUOTE | TO QUOTE | TO QUOTE | Verified feature or **BLOCKED** |
| Exact generation/version reads | TO QUOTE | TO QUOTE | TO QUOTE | Release/cluster compatibility TO PROVE |
| 365-day retention/lock/versions | TO QUOTE | TO QUOTE | TO QUOTE | Lock/admin isolation TO PROVE |
| Requests, egress, cross-zone replication | TO QUOTE | TO QUOTE | TO QUOTE | Network, site sync and ISP costs |
| IAM/workload identity, KMS and rotation | TO QUOTE | TO QUOTE | TO QUOTE | Key custody, HSM/open-source operation |
| Independent audit, witness, alert, checkpoint | TO DESIGN/QUOTE | TO DESIGN/QUOTE | TO DESIGN/QUOTE | Separate administration/site required |
| DR RPO/RTO, restore rehearsal, coverage | TO PRICE/TEST | TO PRICE/TEST | TO PRICE/TEST | Hardware replacement, backup and labor |
| Monthly operations labor; 3-year exit cost | TO ESTIMATE | TO ESTIMATE | TO ESTIMATE | TO ESTIMATE |
| Free-tier/credit applicability | ELIGIBILITY TO VERIFY | ELIGIBILITY TO VERIFY | ELIGIBILITY TO VERIFY | No promotional credit; license ≠ zero TCO |
| Production security verdict | **NO-GO** | **NO-GO** | **NO-GO** | **NO-GO / atomic PUT BLOCKED** |

Cost evidence should include the calculator inputs, currency, tax treatment,
regions, currency exchange assumption, estimated operations/egress, price
date, promotional expiry/eligibility, support tier and sensitivity to 10×
traffic. Do not substitute a cloud pricing-page headline for a durable-archive
architecture quote.

Official quote tools and pricing dimensions:
[AWS calculator](https://aws.amazon.com/aws-cost-management/aws-pricing-calculator/),
[S3 pricing](https://aws.amazon.com/s3/pricing/),
[Azure calculator](https://azure.microsoft.com/en-us/pricing/calculator/),
[Blob Storage pricing](https://azure.microsoft.com/en-us/pricing/details/storage/blobs/),
[Google Cloud calculator](https://cloud.google.com/products/calculator),
[Cloud Storage pricing](https://cloud.google.com/storage/pricing/).

## 3. Business case and cost-benefit checkpoints

For each option, compare **three-year total economic cost**, not only the
cloud invoice:

```text
TCO_3y =
  36 * (compute + DB + storage + writes + reads + encryption + networking
        + audit/witness + backup + support + power/colocation
        + operations_hours * fully_loaded_hourly_rate)
  + migration + acquisition + incidents_reserve + exit_and_restore_tests

monthly_contribution_margin =
  recurring_service_revenue - variable_delivery_cost - allocated_support

payback_months =
  one_time_project_cost / monthly_net_incremental_benefit
  # Only if the denominator is positive and evidence-backed
```

Use the [worked $75/hour operator-time example](cloud-business-cases-swot-cba-2026.md)
to illustrate how self-hosting can appear cash-cheaper but consume far more
valuable founder time. Model at least $0/$50/$75/$125 an hour **as scenarios,
not a claim about the owner's fees**; add opportunity cost and downtime.

| Operating stage | Revenue / nonfinancial benefit | Recommended starting scope | Gate to justify more spend |
| --- | --- | --- | --- |
| **Freelance operations** | Quicker offers, scheduling, billing, client follow-up | Free local templates/CI, non-sensitive SaaS free tiers if eligible | Logged time saved, signed work, collection rate |
| **Personal brand operations** | Discovery, trust, inquiries | Static site, source-controlled content, low-traffic hosting | Lead quality, uptime and measurable conversion |
| **Founder operations** | Pipeline, pricing, cash runway and delivery standards | Portable CRM/reporting and separate finances; no production security credentials in trial accounts | Repeatable profitable offers and monthly cost ceiling |
| **Solo / sole proprietor / boutique** | Productized support and retainers | Small stateless services, per-client isolation, independent backups | Paid MRR, contribution margin, restores and on-call budget |
| **MSME** | Multi-user/site automation, SLAs and security assurance | Redundant services with staged customer tenancy and signed audit evidence | Tested peak concurrency, recovery and audit obligations |
| **Enterprise** | Regulated integration, contracted assurance | Independently owned archive/witness, HA/DR and formal procurement | Security gates G03–G14, independent review and contracted SLO |

**Free-tier decision:** continuing locally on GitHub CI and disposable
database fixtures does not create an incremental cloud-resource bill;
GitHub limits and existing labor still matter. Trial credits are
time-limited and subject to eligibility, service exclusions and shutdown
rules. Free storage is not sufficient proof of audit immutability or an
enterprise business case. Do not provision even a free account under this
current project authorization.

## 4. Proposed growth thresholds and stop conditions

These are **planning triggers, not approved spend caps or production SLOs**.
A service owner must choose actual currency budgets, quotas, user impact
criteria, page routes and regulatory retention *before* activation.

| Signal | Illustrative trigger | Owner response |
| --- | --- | --- |
| Free-tier quota headroom | Forecast use reaches **50%** of an eligible per-period allowance | Reconcile eligibility/expiry and compute paid overage; do not assume unused credit indefinitely |
| Monthly budget forecast | Forecast exceeds **80%** of an owner-approved monthly ceiling | Alert, inspect cost drivers, authorize or throttle noncritical work |
| Unapproved spend | Would exceed **100%** of approved cap | Stop new *noncritical* jobs/provisioning; preserve security evidence and mandated retention |
| Provider service quota | Sustained utilization above **75%** of published quota across seven days | Review peak load, request-quota procedure and bounded failure behavior; no automatic paid upgrade |
| Throughput change | Measured 10× sustained events or material 99th-percentile delay | Repeat complete concurrency/soak, recoverability, security and CBA exercises |
| Billing anomaly | Unexpected paid meter or sharp unexplained cost trend | Notify founder/owner and review charges; do not delete retained evidence to save money |
| Security receipt gap | Any mismatched digest, absent immutable version, compromised permission, failed witness, undeliverable critical alert | **Immediate fail-closed security NO-GO** and independent incident response, regardless of budget |
| Uncertain archive commit | Read-back cannot establish exact signed revision before bounded deadline | Deny publication; preserve state; no witness or SQL-floor advance |

Never silently stop legally mandated audit forwarding or delete locked
records to satisfy a cost threshold. Financial circuit breakers must be
designed around the security fail-closed policy. No automatic cloud upgrades,
retention policy locks or spend changes are authorized by this table.

### Evidence backlog for the next provider-neutral CI increment

1. Add deterministic HTTP/SDK-boundary request serialization tests that
   reject missing conditional-write headers or generation preconditions,
   latest-version reads and provider error misclassification.
2. Add fake responses for current/delete-marker/noncurrent-version states,
   unknown commit, auth revoke and timeout. Confirm exact-read reconciliation
   and the distinction between content authenticity and historical custody.
3. Attach an evidence schema with `provider`, `version/generation`,
   `logical_revision`, `content_digest`, `external_witness_receipt`,
   `retention_status`, `error_class` and `reviewer` fields; keep credentials
   and personal data out.
4. Re-evaluate providers **only** when a named customer/owner supplies real
   volumes, retention, tenancy, geography, RPO/RTO, hours and a budget.

The detailed [provider blueprints](immutable-pin-provider-blueprints-2026.md)
retain **NOT RUN** status; issue
[#29](https://github.com/danwiz/project0-cardealership/issues/29)
remains open and security PRs remain draft/unmerged.
