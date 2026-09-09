from pathlib import Path

root = Path('modern')

pom = root / 'pom.xml'
pom_text = pom.read_text().replace('<version>1.0.21</version>', '<version>1.0.22</version>', 1)
if '<version>1.0.22</version>' not in pom_text:
    raise SystemExit('expected v1.0.21 pom baseline not found')
pom.write_text(pom_text)

metrics = root / 'src/main/java/com/revature/project0/reliability/ReliabilityMetrics.java'
text = metrics.read_text()
old = '''    public ReliabilityMetrics(MeterRegistry registry, OutboxRepository outbox) {
        this.outboxPublished = Counter.builder("project0.outbox.published").register(registry);
        this.outboxFailures = Counter.builder("project0.outbox.publish.failures").register(registry);
        this.outboxDeadLetters = Counter.builder("project0.outbox.dead.letters").register(registry);
        registry.gauge("project0.outbox.pending", pendingOutbox);
        refreshPending(outbox);
    }
'''
new = '''    public ReliabilityMetrics(MeterRegistry registry) {
        this.outboxPublished = Counter.builder("project0.outbox.published").register(registry);
        this.outboxFailures = Counter.builder("project0.outbox.publish.failures").register(registry);
        this.outboxDeadLetters = Counter.builder("project0.outbox.dead.letters").register(registry);
        registry.gauge("project0.outbox.pending", pendingOutbox);
    }
'''
if old not in text:
    raise SystemExit('expected eager ReliabilityMetrics constructor not found')
metrics.write_text(text.replace(old, new, 1))

print('V1_0_22_RELIABILITY_STARTUP_ORDER_PATCH_APPLIED')
