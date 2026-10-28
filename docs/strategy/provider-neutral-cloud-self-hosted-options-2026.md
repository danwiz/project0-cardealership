# Provider-neutral cloud and self-hosted decision reference

**Research snapshot: 2026-10-28. Status: decision support only; no account,
resource, credit, payment, retention lock, credential or deployment activated.**

This document maps the current dealership security contracts (issue
[#29](https://github.com/danwiz/project0-cardealership/issues/29), draft PRs
#44 and #60–#75) to AWS, Microsoft Azure, Google Cloud and self-hosted
alternatives. **A service listing is not evidence that an adapter meets the
security acceptance contract**. The initial implementation stays provider-
neutral and disposable CI-only.

## 1. Free and free-tier entry points (new-account eligibility applies)

| Option | Official free/trial entry point | Useful capacity / limitations | Best initial use |
| --- | --- | --- | --- |
| **AWS** | For eligible new accounts: $100 signup credit and up to $100 additional activity credits; Free plan up to 6 months or credit exhaustion, plus 30+ always-free offerings with limits | Free plan restricts some services; it is **not** an indefinite production entitlement. On expiry AWS suspends Free-plan projects; failure to upgrade inside its recovery period can lead to permanent account/data erasure. Legacy accounts may have different entitlements | Temporary feature evaluation and architecture rehearsals, not the trust anchor for production audit records |
| **Azure** | Eligible new accounts: $200 credit for 30 days, specified free monthly quantities for 12 months, plus always-free quantities for selected services | Must upgrade to pay-as-you-go within 30 days to keep the 12-month free benefits; eligibility, region, billing meters, storage and outbound traffic vary | Microsoft-first client ecosystems and identity/integration rehearsals |
| **Google Cloud** | Eligible new users: $300 credit for 90 days plus 20+ products with always-free monthly quotas | Free e2-micro equivalent in selected US regions, up to 30 GB standard persistent disk and limited egress; request-based Cloud Run free tier includes 2 million requests/month, 360,000 GB-seconds and 180,000 vCPU-seconds. More products and networking may incur charges; expired unconverted trials stop resources | Stateless developer APIs and event-driven pilots with explicit spend quotas |
| **Self-hosted on existing hardware** | Open-source software may have $0 license charge; no cloud promotional clock | Capacity limited by actual CPU/RAM/disks/power/network; electricity, equipment depreciation, backup, patching and labor are real costs | Local development, CI runners, private demonstrations and learning |
| **Self-managed VPS / dedicated server** | Usually pay-as-you-go or monthly hosting; do not assume an ongoing free entitlement | VM resources contractually sized; operator owns guest patching, backups, hardening and monitoring | Small predictable services once a paid budget and off-site recovery exist |

Sources: [AWS Free Tier FAQ](https://aws.amazon.com/free/free-tier-faqs/),
[Azure free services](https://azure.microsoft.com/en-us/pricing/free-services/),
[Azure free account](https://azure.microsoft.com/en-us/pricing/purchase-options/azure-account),
[Google Cloud Free Program](https://docs.cloud.google.com/free/docs/free-cloud-features),
[Google Cloud Compute Engine](https://cloud.google.com/products/compute),
[Cloud Run pricing](https://cloud.google.com/run/pricing).

**Operational rule:** free credits are temporary training/proof-of-concept
financing, not a security, retention, business continuity or capacity SLA.
Do not put immutable evidence, client data or a privileged writer secret in a
temporary trial account. A provider's free tier can require a billing account,
and zero service charges do not imply zero transfer, logging, DNS, key, storage,
backup, notification or labor costs. Never use a free-tier account as the sole
copy of business or security records.

## 2. Broad platform capability families

These representative products illustrate **breadth and deployment options**;
they do not imply equivalent pricing, free-tier coverage or capacity limits.

| Capability | AWS | Azure | Google Cloud | Self-hosted equivalents |
| --- | --- | --- | --- | --- |
| VMs / general compute | EC2, Lightsail | Azure Virtual Machines | Compute Engine | KVM/Proxmox, Linux VPS, physical servers |
| Containers, orchestration and functions | ECS/EKS, Lambda | Container Apps/AKS, Functions | Cloud Run/GKE, Cloud Run functions | Docker/Podman, k3s/Kubernetes, local job runners |
| Object, block and shared files | S3, EBS, EFS | Blob Storage, Managed Disks, Azure Files | Cloud Storage, Persistent Disk, Filestore | Ceph RGW/RBD, NFS, NAS, local encrypted volumes |
| SQL and NoSQL data | RDS/Aurora, DynamoDB | Azure Database for PostgreSQL, Azure SQL, Cosmos DB | Cloud SQL, Spanner, Firestore | PostgreSQL, MariaDB, Redis, managed on rented VPS |
| Networks, DNS and delivery | VPC, Route 53, CloudFront | Virtual Network, Azure DNS, Front Door | VPC, Cloud DNS, Cloud CDN | WireGuard, reverse proxy, authoritative DNS and CDN vendors |
| Observability and operations | CloudWatch, CloudTrail, Config | Azure Monitor, Activity Logs, Policy | Cloud Logging, Cloud Monitoring, Audit Logs | OpenTelemetry, Prometheus, Loki, auditd, alert relay |
| Data, analytics, AI extensions | Redshift, Glue, Bedrock | Fabric/analytics services, Azure AI offerings | BigQuery, Vertex AI | PostgreSQL/ClickHouse, open-source ETL and independently hosted models |

Provider catalogs: [AWS services](https://docs.aws.amazon.com/whitepapers/latest/aws-overview/),
[Azure products](https://azure.microsoft.com/en-us/products),
[Google Cloud products](https://cloud.google.com/products).

**Capacity and option tradeoff:** virtual machines give control and consume
compute hours even when idle; serverless products price or limit invocation,
memory and CPU consumption, often scaling toward zero; managed databases
reduce patching effort but have minimum compute, storage and I/O charges;
self-hosted capacity depends on reserved cores, RAM, SSD endurance, network
uplink and recovery hardware. Workload measurement—not catalog size—is the
basis of selection.

## 3. Candidate capability crosswalk for security adapters

| Required seam | AWS candidates | Azure candidates | Google Cloud candidates | Self-hosted candidates |
| --- | --- | --- | --- | --- |
| Workload identity and request trust | IAM roles / OIDC federation, API Gateway or equivalent ingress | Entra workload identity / managed identities, API Management or equivalent ingress | IAM / Workload Identity Federation, API Gateway or equivalent ingress | External OIDC/Keycloak, reverse proxy, mTLS; independently operated identity service |
| Credential-writer secret / rotation | IAM temporary credentials, Secrets Manager, KMS | Managed identities, Key Vault, RBAC | Workload identity, Secret Manager, Cloud KMS | OpenBao, external KMS/HSM, short-lived mTLS and restricted PostgreSQL roles |
| Privileged database path | PostgreSQL/RDS or independently hosted PostgreSQL, scoped SQL roles | Azure Database for PostgreSQL or independent PostgreSQL | Cloud SQL for PostgreSQL or independent PostgreSQL | PostgreSQL with separately restricted writer role and owner-held triggers |
| Immutable evidence + exact pin document | S3 conditional create and Object Lock with version identity | Blob conditional create and version/container immutability | Cloud Storage generation preconditions and Bucket/Object Retention Lock | Ceph RGW S3-compatible retention/holds, independently reviewed append-only storage |
| External monotonic witness / fenced checkpoint | Independently administered durable compare-and-swap authority **to be selected and verified** | Same | Same | Separate admin/operator domain and rollback-resistant attestation, not merely another DB table |
| CDC transport, outbox, notifications | DMS/CDC architecture, SQS/EventBridge, CloudWatch/SNS | Change-feed/CDC architecture, Event Hubs/Service Bus, Azure Monitor | CDC architecture, Pub/Sub, Cloud Logging/Monitoring | PostgreSQL logical decoding, durable queue, OpenTelemetry and Alertmanager, off-site immutable alert receipts |
| Infrastructure and CI | EC2, containers/serverless, IAM-controlled pipelines | VMs, Container Apps/Functions, DevOps/GitHub | Compute Engine, Cloud Run, GKE, GitHub | Docker/Podman Compose, k3s, Proxmox, Linux runners |

Candidate services are **design mappings**, not claim of compatibility, free
entitlement, or audited fail-closed behavior. Validate each control's
regional availability, latency, consistency, encryption, quotas, IAM,
version-selection API, resource isolation and cost in a separately approved
staging acceptance run.

## 4. Critical immutable publication semantics

The `ImmutableDocumentStore` contract requires atomic **create-if-absent**
for the exact `(canonicalKey, revision)`, independently authenticated
read-back of the same signed bytes, and no later replacement. This is a
compound property, not a product checkbox.

| Candidate | Supported primitive | **Must resolve before approval** |
| --- | --- | --- |
| **AWS S3** | `If-None-Match: *` on conditional PUT; strong read-after-write consistency; Object Lock retention/legal hold on object versions | Object Lock **does not stop** creation of another version under an existing key or delete markers hiding a current version. Enforce conditional writes, prohibited deletes, immutable retention, exact version-ID reads and independent IAM; test collision races and hidden-version recovery. |
| **Azure Blob** | Conditional write (`If-None-Match`) plus container or version-level immutable storage, legal holds and time-based retention | Versioning/immutability compatibility depends on account features; test conditional create, policy scope, exact version reads, cross-region behavior and administrator privilege boundaries. |
| **Google Cloud Storage** | `ifGenerationMatch=0`, exact object generation reads, bucket retention locks / object retention | Generation-match zero succeeds if there is **no live object**, even when only noncurrent versions remain. Retention, deletion prevention, exact generation pinning and independent history must close that loophole. |
| **Ceph RGW** | S3-compatible object retention/legal-hold API and bucket policies | **Not** assumed equivalent to AWS Object Lock. Verify release-specific semantics, concurrent conditional PUT, multisite consistency, admin/superuser threat and operational burden. |

Primary docs: [S3 conditional PUT](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html),
[S3 Object Lock](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html),
[Azure immutability](https://learn.microsoft.com/en-us/azure/storage/blobs/immutable-storage-overview),
[Azure Blob conditional headers](https://learn.microsoft.com/en-us/rest/api/storageservices/specifying-conditional-headers-for-blob-service-operations),
[GCS generation preconditions](https://docs.cloud.google.com/storage/docs/request-preconditions),
[GCS retention controls](https://docs.cloud.google.com/storage/docs/control-data-lifecycles),
[Ceph RGW object operations](https://docs.ceph.com/en/latest/radosgw/s3/objectops/).

**Fail closed:** a valid issuer signature confirms current contents, *not*
that historical replacement never occurred. Real append-only custody,
anti-delete retention, external audit and witness ownership remain G07/G08
production NO-GO until independently tested.

## 5. Capacity planning — size by workload, not promotional credits

| Tier | Illustrative technical envelope (not a promised quota) | Preferred deployment pattern | Acceptance trigger |
| --- | --- | --- | --- |
| Local CI / lab | One developer host, one disposable database, small signed fixtures; no uptime promise | Compose/Podman and CI containers | Code signatures, isolated roles and fault-injection tests green |
| Solo pilot | One small stateless service, low request rate, tens of GB non-sensitive content | Serverless / small VPS / single managed service, with independent backup | Budget alerts, identity, restoration test, quota limits, no client secrets in test systems |
| Boutique and microbusiness | Multiple apps, team access, hundreds of GB to low TB assets; customer-dependent volumes | Managed database/object store and least-privileged service identities; optional private CI | Explicit RPO/RTO, client segregation, monthly reconciliation, payer controls |
| MSME | Multiple tenants/sites, sustained traffic, TB-scale archives or event history | Replicated stateless services, managed HA database, independent archive/witness and alerting | Load/soak tests, replica/zone failures, queue backlog budget and on-call coverage |
| Enterprise | Regional compliance, large history, multi-zone or multi-region recovery; no fixed universal size | Multi-account/subscription/project isolation; independently reviewed network, identity, retention and DR | Contractual SLO, external pen test, compliance/legal review, witnessed failure exercises |

Only the business owner can set actual targets for sustained RPS, peak RPS,
event bytes per rotation, retention duration, storage growth, egress, read
ratio, downtime tolerance and geography. The following quantities should
be captured **before** producing a provider price estimate:

- Requests: average/peak RPS, concurrency, p95/p99 response latency, payload
  size, authentication and rate-limit overhead.
- Storage: objects/day, bytes/object, minimum retained versions, retention
  years, replication, restore traffic and annual growth.
- Database: transactions/sec, WAL MB/sec, CDC delay/backlog, read/write
  mix, standby/restore characteristics.
- Secrets and monitoring: rotations/month, secret reads, key operations,
  events/alerts per month, retention of audit logs.
- People and service: regions, access roles, number of clients, security
  incident SLO, RPO/RTO and quarterly disaster-recovery exercise hours.

Free-tier CPU-hours or request quotas **cannot** be extrapolated into an
enterprise availability or compliance guarantee. As one self-hosted sizing
reference, k3s currently lists **2 cores / 2 GB RAM** as its *server-node
minimum before application workloads*; a real business stack needs more
headroom. See [k3s requirements](https://docs.k3s.io/installation/requirements).

## 6. Self-hosted environment menu

| Option | License/capacity profile | Suitable now | Security / operations caution |
| --- | --- | --- | --- |
| Linux host + Docker Compose | Low-entry, single host; Compose covers development, CI and simple production patterns | Local prototypes and low-risk internal tools | Host failure, guest patching, secrets and backup remain your responsibility |
| Rootless Podman + Compose provider | Linux non-root containers with separate compose provider | Security-focused dev and a small VPS | Validate compose provider, networking, storage/UID mapping |
| Proxmox VE + Linux VMs | Hardware virtualization and snapshots, multiple guests | Home office lab, isolated client demo environments | Free/no-subscription repository is not the same as subscribed production support |
| k3s | Lightweight Kubernetes; can scale from one node to clusters | Repeatable edge and boutique deployment lab | Multi-node HA, storage, routing, certificates and etcd/backups add operator time |
| Ceph RGW + PostgreSQL + OpenBao | Open-source storage, database and secret management | Advanced lab; candidate for dedicated independent operator domains | Multi-node costs, hardening, storage governance and recovery **must** be tested |
| OpenStack / Swift | Large private-cloud control plane / object storage | Later-stage MSP/enterprise or specialized compliance | High staffing, capital, lifecycle and inter-operator coordination cost |
| **Archived MinIO Community repo** | Historical AGPLv3 source; original GitHub repo was archived April 25, 2026 | Existing-system evaluation only | **Not a default new production dependency**; archived/unmaintained source, legal obligations and support path must be separately reviewed |

Sources: [Docker Compose production](https://docs.docker.com/compose/how-tos/production/),
[Podman rootless](https://docs.podman.io/en/latest/markdown/podman.1.html),
[Proxmox VE administration](https://pve.proxmox.com/pve-docs/pve-admin-guide.pdf),
[k3s](https://docs.k3s.io/),
[Ceph Object Gateway](https://docs.ceph.com/en/latest/radosgw/s3/objectops/),
[OpenBao](https://openbao.org/docs/what-is-openbao/),
[OpenStack Swift](https://docs.openstack.org/swift/latest/admin/objectstorage-intro.html),
[MinIO archive](https://github.com/minio/minio).

Do not equate **self-hosted** with **independently administered**. If the
same sole proprietor controls application, database, object store, secret
store, retention settings and audit witness, the organizational separation
required by G07/G08 is not achieved, no matter how many containers run.

## 7. Selection gates and a reversible operating sequence

1. **No-charge discovery (now):** stay on GitHub Java 11/17 CI and local
   disposable PostgreSQL. Keep adapter source code cloud-independent and
   exercise PIN-ADP-01..06; no cloud sign-up necessary.
2. **Portfolio pilot (approval required):** choose one eligible free/trial
   program only for synthetic demo data, with budget/expiry reminder, export
   runbook, and no credentials/immutable source-of-truth.
3. **Paid staging design (separate approval):** compare at least two provider
   estimates at the same workload, region, retention and availability level;
   separately cost self-hosted hardware, power, remote backup and labor.
4. **Real control acceptance (NOT RUN):** enforce atomic create, signed exact
   readback, version-pinned retention, externally administered witness,
   identity separation, secret rotation, alerting and recovery. Preserve
   independent evidence for ARCH-01/02, CKPT-01 and SECRET-01.
5. **Production decision (NO-GO):** only after security review, RPO/RTO,
   legal/privacy and operations approval. No free-tier marketing claim
   substitutes for required technical or administrative evidence.

Provider billing calculators, for actual region-specific estimates:
[AWS](https://aws.amazon.com/aws-cost-management/aws-pricing-calculator/),
[Azure](https://azure.microsoft.com/en-us/pricing/calculator/),
[Google Cloud](https://cloud.google.com/products/calculator).

See [business cases, SWOT and cost-benefit model](cloud-business-cases-swot-cba-2026.md),
the [four provider-specific immutable publication blueprints](immutable-pin-provider-blueprints-2026.md),
the [equal-workload cost and growth threshold model](normalized-cloud-workload-cost-thresholds-2026.md),
the [adapter acceptance matrix](../security/immutable-pin-adapter-acceptance-matrix.md)
and [security integration plan](../security/credential-security-production-integration-plan.md).
