from pathlib import Path

root = Path('modern')

pom = root / 'pom.xml'
s = pom.read_text().replace('<version>1.0.24</version>', '<version>1.0.25</version>', 1)
if '<version>1.0.25</version>' not in s:
    raise SystemExit('expected v1.0.24 pom baseline not found')
pom.write_text(s)

test = root / 'src/test/java/com/revature/project0/springjdbc/SpringTransactionalOfferWorkflowIntegrationTest.java'
text = test.read_text()
text = text.replace('webEnvironment = SpringBootTest.WebEnvironment.NONE,', 'webEnvironment = SpringBootTest.WebEnvironment.MOCK,', 1)
text = text.replace('            "spring.main.web-application-type=none",\n', '', 1)
if 'WebEnvironment.MOCK' not in text:
    raise SystemExit('mock servlet context not established')
if 'spring.main.web-application-type=none' in text:
    raise SystemExit('non-web override remains in transaction test')
test.write_text(text)

print('V1_0_25_SPRING_TEST_CONTEXT_PATCH_APPLIED')
