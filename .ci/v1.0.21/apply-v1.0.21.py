from pathlib import Path

root = Path('modern')

p = root / 'pom.xml'
s = p.read_text().replace('<version>1.0.19</version>', '<version>1.0.21</version>', 1)
if '<version>1.0.21</version>' not in s:
    raise SystemExit('expected v1.0.19 pom baseline not found')
p.write_text(s)

targets = [
    'src/main/java/com/revature/project0/workflow/TransactionalOfferWorkflow.java',
    'src/main/java/com/revature/project0/springjdbc/SpringJdbcIdempotencyRepository.java',
    'src/main/java/com/revature/project0/springjdbc/SpringJdbcOutboxRepository.java',
    'src/main/java/com/revature/project0/springjdbc/SpringJdbcOfferRepository.java',
    'src/main/java/com/revature/project0/springjdbc/SpringJdbcOfferLockRepository.java',
    'src/main/java/com/revature/project0/springjdbc/SpringJdbcAuditRepository.java',
]

for rel in targets:
    f = root / rel
    text = f.read_text()
    if 'public final class ' not in text:
        raise SystemExit(f'expected final class not found: {rel}')
    f.write_text(text.replace('public final class ', 'public class ', 1))

print('V1_0_21_MODIFIER_ONLY_PATCH_APPLIED')
