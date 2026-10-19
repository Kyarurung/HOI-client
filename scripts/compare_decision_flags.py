import argparse
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
from compare_event_frames import captures, digest


def compare(run, baseline, output):
    output.mkdir(parents=True, exist_ok=True)
    after, before = captures(run / 'screenshots'), captures(baseline / 'screenshots')
    changes, added, missing = [], [], []
    for name in sorted(before.keys() | after.keys()):
        for index in range(max(len(before[name]), len(after[name]))):
            old = before[name][index] if index < len(before[name]) else None
            new = after[name][index] if index < len(after[name]) else None
            if old is None:
                added.append(new.name)
            elif new is None:
                missing.append(old.name)
            elif digest(old) != digest(new):
                changes.append(dict(name=name, occurrence=index, before=str(old), after=str(new),
                                    beforeSha256=digest(old), afterSha256=digest(new)))
    evidence_path = run / 'screenshots/hoi-decisions-source-evidence.json'
    evidence = json.loads(evidence_path.read_text('utf-8'))
    root = Path(__file__).resolve().parents[1]
    pixel_checks = []
    for row in evidence:
        if not row['flagRows']:
            continue
        screenshot = Path(row['screenshots']['flags'])
        name = f"{row['fixture']}-{row['width']}x{row['height']}-gui2"
        shutil.copy2(screenshot, output / (name + '.png'))
        with Image.open(screenshot) as image:
            for ident, bounds in row['flagRows'].items():
                x, y, width, height = [value * 2 for value in bounds]
                crop = image.crop((x, y, x + width, y + height))
                crop.resize((crop.width * 3, crop.height * 3), Image.Resampling.NEAREST).save(output / (name + '-' + ident + '-nearest-3x.png'))
                if row['width'] == 2560:
                    tag = 'kor' if row['fixture'] == 'kor-kfxx' else 'prc'
                    path = root.parent / f'HOI-resourcepack/pack/assets/hoi/textures/gui/unit_hud/flags/{tag}.png'
                    with Image.open(path) as flag:
                        original = flag.convert('RGB').resize((20, 14), Image.Resampling.NEAREST)
                    pixels = image.convert('RGB')
                    expected = original.tobytes()
                    matches = [[xx, yy] for yy in range(y, y + height - 13) for xx in range(x, x + width - 19)
                               if pixels.getpixel((xx, yy)) == original.getpixel((0, 0))
                               and pixels.crop((xx, yy, xx + 20, yy + 14)).tobytes() == expected]
                    assert len(matches) == 1, f'Expected one exact 20x14 original flag: {name}'
                    pixel_checks.append(dict(fixture=row['fixture'], source=str(path), renderedPixels=[20, 14], matches=matches))
        for key, path in row['screenshots'].items():
            if key not in ('top', 'bottom', 'flags'):
                shutil.copy2(path, output / (name + '-' + key + '.png'))
    tests = {}
    for label, directory in {'client': root / 'client/build/test-results/test', 'protocol': root / 'protocol/build/test-results/test',
                             'producer': root / 'client/build/cmp16-server-source/fabric/build/test-results/exportClientRendererFixtures'}.items():
        totals = dict.fromkeys(('tests', 'failures', 'errors', 'skipped'), 0)
        for path in directory.glob('TEST-*.xml'):
            suite = ET.parse(path).getroot()
            for key in totals:
                totals[key] += int(suite.get(key, 0))
        tests[label] = totals
    jar = root / 'build/libs/hoi-client-1.0.0+26.3.jar'
    data = jar.read_bytes()
    metrics = {}
    for filename in ('event-frame-measurements.json', 'event-tooltip-measurements.json'):
        if (run / filename).is_file() and (baseline / filename).is_file():
            metrics[filename] = json.loads((run / filename).read_text('utf-8')) == json.loads((baseline / filename).read_text('utf-8'))
    result = dict(run=str(run), baseline=str(baseline), status=json.loads((run / 'hoi-run.json').read_text('utf-8')),
                  captures=sum(map(len, after.values())), baselineCaptures=sum(map(len, before.values())),
                  changedCaptures=changes, addedCaptures=added, missingCaptures=missing, testResults=tests,
                  eventMeasurementsEqual=metrics, decisionEvidence=evidence, originalFlagPixelChecks=pixel_checks,
                  jar=dict(path=str(jar), bytes=len(data), sha1=hashlib.sha1(data).hexdigest(), sha256=hashlib.sha256(data).hexdigest()),
                  resourcePacks=json.loads((root.parent / 'HOI-resourcepack/build/resource-packs.json').read_text('utf-8')),
                  limits=['GUI2 renderer fixtures; no operational multiplayer check.',
                          'No supplied original decision row with a flag; inline size/baseline/spacing remain assumptions.',
                          'Raw capture differences include dynamic backgrounds; every changed full image has not been visually reviewed.'])
    (output / 'render-verification.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', 'utf-8')
    shutil.copy2(evidence_path, output)
    print(json.dumps({key: result[key] for key in ('captures', 'baselineCaptures', 'testResults', 'jar', 'eventMeasurementsEqual')}, indent=2))
    print('Changed/additional/missing captures:', len(changes), len(added), len(missing))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--run', type=Path, required=True)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    compare(args.run, args.baseline, args.output)
