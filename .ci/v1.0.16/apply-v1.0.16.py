from pathlib import Path

root = Path('modern')

pom = root / 'pom.xml'
text = pom.read_text(encoding='utf-8')
text = text.replace('<version>1.0.15</version>', '<version>1.0.16</version>', 1)
pom.write_text(text, encoding='utf-8')

auth = root / 'src/test/java/com/revature/project0/app/AuthenticationServiceTest.java'
text = auth.read_text(encoding='utf-8')
if 'import com.revature.project0.app.error.InvalidCredentialsException;' not in text:
    text = text.replace(
        'import com.revature.project0.domain.Role;',
        'import com.revature.project0.domain.Role;\nimport com.revature.project0.app.error.InvalidCredentialsException;'
    )
old = 'assertThrows(IllegalArgumentException.class, () -> service.authenticate("dane","wrong"));'
new = 'assertThrows(InvalidCredentialsException.class, () -> service.authenticate("dane","wrong"));'
if old not in text:
    raise SystemExit('AuthenticationServiceTest expected v1.0.15 assertion not found')
auth.write_text(text.replace(old, new, 1), encoding='utf-8')

arch = root / 'src/test/java/com/revature/project0/architecture/ArchitectureTest.java'
text = arch.read_text(encoding='utf-8')
if 'import com.tngtech.archunit.core.importer.ImportOption;' not in text:
    text = text.replace(
        'import com.tngtech.archunit.junit.AnalyzeClasses;',
        'import com.tngtech.archunit.junit.AnalyzeClasses;\nimport com.tngtech.archunit.core.importer.ImportOption;'
    )
old = '@AnalyzeClasses(packages = "com.revature.project0")'
new = '@AnalyzeClasses(packages = "com.revature.project0", importOptions = ImportOption.DoNotIncludeTests.class)'
if old not in text:
    raise SystemExit('ArchitectureTest expected v1.0.15 AnalyzeClasses annotation not found')
text = text.replace(old, new, 1)
if text.count('@ArchTest') != 7:
    raise SystemExit('Architecture rule count changed unexpectedly')
arch.write_text(text, encoding='utf-8')

print('V1_0_16_PATCH_APPLIED')
