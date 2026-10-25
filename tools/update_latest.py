#!/usr/bin/env python3
"""Replace dist/latest with the current complete installable mod set, without archives."""
import hashlib
import argparse
import json
import os
from pathlib import Path
import shutil
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'dist/latest'


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def properties(path):
    return dict(line.strip().split('=', 1) for line in path.read_text().splitlines()
                if '=' in line and not line.lstrip().startswith('#'))


def latest(directory, pattern):
    candidates = [p for p in directory.glob(pattern) if not p.name.endswith('-sources.jar')]
    if not candidates:
        raise SystemExit(f'Build artifact missing: {directory / pattern}')
    return max(candidates, key=lambda p: p.stat().st_mtime_ns)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fabric-api', type=Path, help='Publish the explicitly verified Fabric API JAR instead of the compile baseline.')
    args = parser.parse_args()
    config = properties(ROOT / 'gradle.properties')
    api_version = properties(ROOT.parent / 'veil/gradle.properties')['fabric_api_version']
    cache = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle'))
    sources = {
        'create': latest(ROOT.parent / '26_3_porting/build/libs', 'create-fabric-*.jar'),
        'sable': latest(ROOT.parent / 'sable/build/libs', 'sable-fabric-*.jar'),
        'aeronautics_bundle': latest(ROOT / 'build/libs', 'aeronautics-bundle-fabric-*.jar'),
        'fabric-api': args.fabric_api.resolve() if args.fabric_api else latest(cache / 'caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api' / api_version,
                             '*/*.jar'),
    }
    records = []
    (ROOT / 'dist').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.latest-', dir=ROOT / 'dist') as temporary:
        stage = Path(temporary)
        for mod_id, src in sources.items():
            target = stage / src.name
            shutil.copy2(src, target)
            with zipfile.ZipFile(target) as jar:
                assert jar.testzip() is None, f'Broken JAR: {src}'
                metadata = json.loads(jar.read('fabric.mod.json'))
                assert metadata['id'] == mod_id, (src, metadata['id'])
                for nested in metadata.get('jars', []):
                    assert nested['file'] in jar.namelist(), (src, nested)
            records.append({'id': mod_id, 'version': metadata['version'], 'file': target.name,
                            'sha256': sha(target), 'source': str(src.relative_to(ROOT)) if src.is_relative_to(ROOT) else str(src),
                            'nestedJars': [n['file'] for n in metadata.get('jars', [])]})
        manifest = {'name': 'latest', 'minecraft': config['minecraft_version'], 'java': 25,
                    'fabricLoader': config['fabric_loader_version'], 'artifacts': records,
                    'policy': 'Current full set only. Updates replace this folder; historical builds and revision ZIPs are not retained.',
                    'verification': 'Native gameplay checks and exact input hashes: GAMEPLAY_AUDIT.md in Aeronautics and the sibling Sable, Veil and ImGuiMC projects. Historical scopes and hashes remain historical.',
                    'auditReports': {
                        'aeronautics': 'https://github.com/nedogimov-m/Simulated-Fabric/blob/26.3/GAMEPLAY_AUDIT.md',
                        'sable': 'https://github.com/nedogimov-m/sable-fabric/blob/26.3/GAMEPLAY_AUDIT.md',
                        'veil': 'https://github.com/nedogimov-m/Veil-Fabric/blob/26.3/GAMEPLAY_AUDIT.md',
                        'imguimc': 'https://github.com/nedogimov-m/imguimc-fabric/blob/26.3/GAMEPLAY_AUDIT.md'}}
        schematic = ROOT / 'build/porting/airshipbase/airshipbase.compat.nbt'
        if schematic.is_file():
            shutil.copy2(schematic, stage / 'airshipbase.nbt')
            manifest['schematic'] = {'file': 'airshipbase.nbt', 'sha256': sha(schematic),
                                     'change': 'One unavailable Aeroscapes Redstone Link channel uses paper/paper at both ends. Original block layout, inventories and other NBT are unchanged.'}
        (stage / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
        readme = '''# Актуальный комплект Fabric 26.3

В этой папке лежат четыре устанавливаемых JAR: Create, Sable, Aeronautics Bundle и Fabric API.
Simulated, Aeronautics и Offroad входят в один Aeronautics Bundle; отдельно устанавливать их не нужно.
Перед установкой bundle убери прежние отдельные simulated-fabric, aeronautics-fabric и offroad-fabric JAR из mods.
Скопируй четыре JAR в папку mods, заменив прежние версии. Дубликатов оставаться не должно.
Minecraft 26.3 / Java 25 / Fabric Loader 0.19.5. Fabric API включён; Loader устанавливается отдельно.
Veil, ImGuiMC, Sable Companion и остальные встроенные зависимости отдельно добавлять не нужно.

Обновление этой папки после сборки: python3 tools/update_latest.py из корня проекта.
Новых папок ревизий и ZIP-архивов не создаём. Отчёты проверок остаются в проекте.

Результаты проверки использования предметов и отображения: GAMEPLAY_AUDIT.md в проекте Aeronautics.
Проверки Sable, Veil и ImGuiMC, включая исправления инспектора, переходов между измерениями,
ориентации и обрезки интерфейса: GAMEPLAY_AUDIT.md в соответствующих проектах; ссылки есть в manifest.json.
'''
        if schematic.is_file():
            readme += '''

airshipbase.nbt — совместимая копия схемы: частота отсутствующего предмета Aeroscapes заменена на бумагу в двух слотах одной пары Redstone Link. Пользовательский оригинал не изменён.
Для уже построенного корабля выставьте бумагу в обоих слотах пары: передатчик (18,24,71), приёмник (19,37,52), координаты относительно начала схемы. Два остальных пустых приёмника не настраивайте этой частотой.
Перед взлётом нужны топливо для переносных двигателей и горелок, работающая подача воды в котлы и ненулевой выход пара. Простое включение всех рычагов этого не обеспечивает.

## Проверенный первый запуск airshipbase

1. Исправьте радиоканал бумагой на уже построенном корабле либо используйте приложенную схему для новой постройки.
2. Заполните водой два бассейна 2×2 под насосами. Обычная схемопушка не печатает эти источники. Относительно начала схемы: y=26, слева x=17/18 и z=53/54, справа x=22/23 и z=53/54.
3. Заправьте углём оба красных переносных двигателя и горелки под котлами. В тесте было по 64 угля в двух двигателях и по одному в 20 горелках; для долгого полёта нужна дальнейшая подача топлива.
4. Обычные рычаги оставьте в исходных состояниях схемы: ВКЛ — (23,13,70), (20,17,80), (22,24,72); ВЫКЛ — (18,23,74), (22,23,74), (19,27,51). Координаты относительные.
5. Выставьте 15 на всех пяти регуляторах пульта. Для изменения зажмите ПКМ на ручке и двигайте мышь вверх/вниз, затем отпустите.
6. Дождитесь устойчивой подачи воды и нагрева обоих котлов, работы всех четырёх паровых выходов и наполнения оболочек паром. В проверенном запуске прогрев занял около 28 игровых секунд. Прогревайте корабль до физической сборки.
7. На физическом сборщике зажмите ПКМ, поднимите ручку до упора и отпустите. После подъёма регулируйте пар для нужной высоты.
8. Для движения включите два главных пропеллерных подшипника пустой рукой: (6,19,78) и (34,19,78). Крайний правый регулятор пульта (23,24,74) управляет их передачами: выход 15 останавливает винты, выход 0 даёт полную тягу. Два регулятора пара (18,24,72) и (19,24,72) при этом оставьте открытыми. Все пять регуляторов на 15 нужны для этапа прогрева, затем тяга настраивается отдельно.

Этот порядок проверен на исходной конструкции: подъём на 16,15 м выше старта без просадки за 112 тиков. Все восемь источников воды сохранились после сборки; оба котла и четыре паровых выхода продолжали работать. Без принудительной тяги, телепортации или удерживающего корабль ограничения.
'''
        (stage / 'README_RU.md').write_text(readme)
        (stage / 'SHA256SUMS').write_text(''.join(f'{sha(p)}  {p.name}\n' for p in sorted(stage.iterdir()) if p.is_file()))
        DEST.mkdir(exist_ok=True)
        expected = {p.name for p in stage.iterdir()}
        for p in stage.iterdir():
            os.replace(p, DEST / p.name)
        for p in DEST.iterdir():
            if p.is_file() and (p.suffix == '.jar' or p.name == 'airshipbase.nbt') and p.name not in expected:
                p.unlink()
    print(json.dumps({'directory': str(DEST), 'mods': len(records), 'bytes': sum((DEST / a['file']).stat().st_size for a in records)}))


if __name__ == '__main__':
    main()
