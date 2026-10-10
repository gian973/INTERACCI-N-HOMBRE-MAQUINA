from pathlib import Path
import zipfile,hashlib
root=Path(__file__).resolve().parents[1]
folders={'src','docs','scripts','prototype','.mvn'}
files={'README.md','pom.xml','compose.yaml','Dockerfile','.dockerignore','.gitignore','.env.example','mvnw','mvnw.cmd'}
output=root/'ImperioGym-proyecto.zip'
members=[]
for path in root.rglob('*'):
    if not path.is_file():continue
    relative=path.relative_to(root)
    if relative.parts[0] not in folders and str(relative) not in files:continue
    if '__pycache__' in relative.parts:continue
    members.append((path,relative))
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED,compresslevel=8) as archive:
    for path,relative in sorted(members,key=lambda x:str(x[1])):
        info=zipfile.ZipInfo.from_file(path,'ImperioGym/'+relative.as_posix())
        info.compress_type=zipfile.ZIP_DEFLATED
        if relative.as_posix()=='mvnw':info.create_system=3;info.external_attr=0o100755<<16
        archive.writestr(info,path.read_bytes())
with zipfile.ZipFile(output) as archive:
    assert archive.testzip() is None
    assert not any('/.tools/' in n or '/.data/' in n or n.endswith('/.env') for n in archive.namelist())
digest=hashlib.sha256(output.read_bytes()).hexdigest()
(root/'ImperioGym-proyecto.sha256').write_text(digest+'  '+output.name+'\n',encoding='ascii')
print(f'ZIP verified: {len(members)} files, {output.stat().st_size} bytes')
print(output)
