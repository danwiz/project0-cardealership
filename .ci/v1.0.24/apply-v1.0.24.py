from pathlib import Path

root = Path('modern')

pom = root / 'pom.xml'
s = pom.read_text().replace('<version>1.0.23</version>', '<version>1.0.24</version>', 1)
if '<version>1.0.24</version>' not in s:
    raise SystemExit('expected v1.0.23 pom baseline not found')
pom.write_text(s)

writer = root / 'src/main/java/com/revature/project0/security/SecurityProblemWriter.java'
text = writer.read_text()
old_import = 'import com.fasterxml.jackson.databind.ObjectMapper;'
if old_import not in text:
    raise SystemExit('expected Jackson 2 ObjectMapper import not found')
text = text.replace(old_import, 'import tools.jackson.databind.json.JsonMapper;', 1)
text = text.replace('ObjectMapper mapper', 'JsonMapper mapper')
writer.write_text(text)

if 'com.fasterxml.jackson.databind.ObjectMapper' in writer.read_text():
    raise SystemExit('legacy ObjectMapper reference remains')
if 'JsonMapper mapper' not in writer.read_text():
    raise SystemExit('Jackson 3 JsonMapper constructor field not established')

print('V1_0_24_JACKSON3_SECURITY_WRITER_PATCH_APPLIED')
