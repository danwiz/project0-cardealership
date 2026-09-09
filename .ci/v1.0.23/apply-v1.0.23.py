from pathlib import Path

root = Path('modern')

pom = root / 'pom.xml'
s = pom.read_text().replace('<version>1.0.22</version>', '<version>1.0.23</version>', 1)
if '<version>1.0.23</version>' not in s:
    raise SystemExit('expected v1.0.22 pom baseline not found')
pom.write_text(s)

outbox = root / 'src/main/java/com/revature/project0/reliability/OutboxDispatcher.java'
text = outbox.read_text()
if 'public final class OutboxDispatcher' not in text:
    raise SystemExit('expected final OutboxDispatcher not found')
outbox.write_text(text.replace('public final class OutboxDispatcher', 'public class OutboxDispatcher', 1))

# Fail the patch if any class with @Transactional remains final under class-based Spring AOP.
violations = []
for java in (root / 'src/main/java/com/revature/project0').rglob('*.java'):
    body = java.read_text()
    if '@Transactional' in body and 'public final class ' in body:
        violations.append(str(java))
if violations:
    raise SystemExit('transactional final-class violations remain: ' + ','.join(violations))

print('V1_0_23_OUTBOX_PROXYABILITY_PATCH_APPLIED')
