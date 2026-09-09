from pathlib import Path

root = Path('modern')
pom = root / 'pom.xml'
text = pom.read_text()
text = text.replace('<version>1.0.25</version>', '<version>1.0.26</version>', 1)
old = '<dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>'
new = '<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-flyway</artifactId></dependency>'
if old not in text:
    raise SystemExit('expected direct flyway-core dependency not found')
text = text.replace(old, new, 1)
if '<artifactId>spring-boot-starter-flyway</artifactId>' not in text:
    raise SystemExit('Spring Boot Flyway starter not established')
if '<artifactId>flyway-database-postgresql</artifactId>' not in text:
    raise SystemExit('PostgreSQL Flyway database module missing')
pom.write_text(text)

print('V1_0_26_BOOT4_FLYWAY_STARTER_PATCH_APPLIED')
