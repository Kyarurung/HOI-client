from pathlib import Path
import argparse
import json
import shutil
import xml.etree.ElementTree as ET
from PIL import Image
from compare_event_frames import captures, digest, pair


def tooltip_comparisons(run, output):
    output.mkdir(parents=True, exist_ok=True)
    after = captures(run / 'screenshots')
    source = Path('C:/Program Files (x86)/Steam/userdata/1398211522/760/remote/394360/screenshots')
    measurements = json.loads((run / 'event-tooltip-measurements.json').read_text('utf8'))
    selected = {}
    for name, paths in after.items():
        if name.startswith(('hoi-event-tooltip-', 'hoi-event-minimize-')):
            path = paths[0]
            shutil.copy2(path, output / name)
            selected[name] = path
    for key, row in measurements['renders'].items():
        if 'tooltipGui' not in row:
            continue
        screenshot = selected['hoi-event-tooltip-' + key + '-gui2.png']
        x, y, w, h = [value * 2 for value in row['tooltipGui']]
        with Image.open(screenshot) as image:
            crop = image.crop((x, y, x + w, y + h))
            crop.save(output / (key + '-tooltip.png'))
            crop.resize((crop.width * 3, crop.height * 3), Image.Resampling.NEAREST).save(output / (key + '-tooltip-nearest-3x.png'))
        if key.startswith('event-korea-21-'):
            with Image.open(source / '20261012212816_1.jpg') as image:
                original = image.crop((1393, 850, 1614, 898))
            pair(original, crop, output / (key + '-steam-vs-minecraft.png'), ('Steam 212816 / UI 1', 'Minecraft / GUI 2 / default font'))
            bx, by, bw, bh = [value * 2 for value in row['choiceGui']]
            with Image.open(screenshot) as image:
                current = image.crop((bx - 10, by - 10, max(bx + bw, x + w) + 10, max(by + bh, y + h) + 10))
            with Image.open(source / '20261012212816_1.jpg') as image:
                original = image.crop((1027, 780, 1630, 920))
            pair(original, current, output / (key + '-option-area-steam-vs-minecraft.png'), ('Steam 212816 / option + tooltip', 'Minecraft / actual option + tooltip'))
    for key, row in measurements['renders'].items():
        if 'eventRowGui' not in row:
            continue
        path = selected[key + '-hidden-gui2.png']
        x, y, w, h = [value * 2 for value in row['eventRowGui']]
        with Image.open(path) as image:
            current = image.crop((x, y, x + w, y + h))
        with Image.open(source / '20261012212814_1.jpg') as image:
            original = image.crop((10, 189, 516, 223))
        pair(original, current, output / (key + '-row-steam-vs-minecraft.png'), ('Steam / source row', 'Minecraft / remaining days unknown'))
    with Image.open(source / '20261012212834_1.jpg') as image:
        original = image.crop((1210, 644, 1572, 752))
    with Image.open(output / 'event-korea-504-choice-0-2560-tooltip.png') as image:
        pair(original, image, output / 'multiline-box-steam-vs-minecraft.png', ('Steam korea.2 / three lines', 'Minecraft korea.504 / three lines; different effects'))
    shutil.copy2(run / 'event-tooltip-measurements.json', output)
    return measurements


def compare(run, baseline, output):
    measurements = tooltip_comparisons(run, output)
    after, before = captures(run / 'screenshots'), captures(baseline / 'screenshots')
    frame_old = json.loads((baseline / 'event-frame-measurements.json').read_text('utf8'))
    frame_new = json.loads((run / 'event-frame-measurements.json').read_text('utf8'))
    key = 'national-long-body-1280'
    old = frame_old[key]['choicesGui'][0]
    new = frame_new[key]['choicesGui'][0]
    def button_crop(paths, bounds):
        x, y, w, h = [value * 2 for value in bounds]
        with Image.open(paths[0]) as image:
            return image.crop((x - 8, y - 8, x + w + 8, y + h + 8))
    filename = 'hoi-event-frame-national-long-body-1280-gui2.png'
    pair(button_crop(before[filename], old), button_crop(after[filename], new), output / 'two-line-option-before-after-1280.png', ('CMP-12 / before', 'CMP-15 / same font, added padding'))
    changes, added, missing = [], [], []
    for name in sorted(before.keys() | after.keys()):
        for index in range(max(len(before[name]), len(after[name]))):
            a = before[name][index] if index < len(before[name]) else None
            b = after[name][index] if index < len(after[name]) else None
            if a is None:
                added.append(b.name)
            elif b is None:
                missing.append(a.name)
            elif digest(a) != digest(b):
                changes.append(dict(name=name, occurrence=index + 1, before=a.name, after=b.name, before_sha256=digest(a), after_sha256=digest(b)))
    root = Path(__file__).resolve().parents[1]
    test_results = {}
    directories = {'client': root / 'client/build/test-results/test', 'protocol': root / 'protocol/build/test-results/test', 'producer': root / 'client/build/cmp15-server-source/fabric/build/test-results/exportClientRendererFixtures', 'manual': root / 'client/build/cmp15-server-source/fabric/build/test-results/cmp15ManualFixtures'}
    for label, directory in directories.items():
        counts = dict(tests=0, failures=0, errors=0, skipped=0)
        for file in directory.glob('TEST-*.xml'):
            suite = ET.parse(file).getroot()
            for field in counts:
                counts[field] += int(suite.attrib.get(field, 0))
        test_results[label] = counts
    jar = root / 'build/libs/hoi-client-1.0.0+26.3.jar'
    import hashlib
    result = dict(run=str(run), baseline=str(baseline), status=json.loads((run / 'hoi-run.json').read_text('utf8')), testResults=test_results,
                  captures=sum(map(len, after.values())), baselineCaptures=sum(map(len, before.values())), changedCaptures=changes, addedCaptures=added, missingCaptures=missing,
                  twoLineButton=dict(beforeGui=old, afterGui=new), measurements=measurements,
                  jar=dict(path=str(jar), sha1=hashlib.sha1(jar.read_bytes()).hexdigest(), bytes=jar.stat().st_size),
                  limits=['Source tooltips are authoritative server-snapshot-8 fixtures. News lifecycle uses the existing source frame catalog as a synthetic client fixture.',
                          'Mock request/Close/reset input tests and real client rendering do not establish live multiplayer, expiry timing, or audible sound verification.',
                          'Full screenshot hash differences include changing world backgrounds; no pixel equality is claimed for unrelated screens.'])
    (output / 'full-validation.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', 'utf8')
    shutil.copy2(run / 'event-tooltip-measurements.json', output)
    shutil.copy2(run / 'event-frame-measurements.json', output)
    print(json.dumps({key: result[key] for key in ['captures', 'baselineCaptures', 'testResults', 'twoLineButton', 'jar']}, indent=2))
    print(dict(changed=len(changes), added=len(added), missing=len(missing)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--run', type=Path, required=True)
    parser.add_argument('--baseline', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--tooltips-only', action='store_true')
    args = parser.parse_args()
    if args.tooltips_only:
        tooltip_comparisons(args.run, args.output)
    elif args.baseline is None:
        parser.error('--baseline is required for full comparison')
    else:
        compare(args.run, args.baseline, args.output)
