from pathlib import Path
import argparse
import hashlib
import json
import re
import shutil
import sys
from collections import defaultdict
from PIL import Image, ImageChops, ImageDraw

REFERENCES = {
    'national-korea-21': ('win', '20261012212814_1', (900, 270)),
    'national-korea-1': ('win', '20261012212828_1', (900, 270)),
    'national-korea-2': ('win', '20261012212834_1', (900, 270)),
    'national-one': ('win', '20261012215309_1', (900, 270)),
    'national-korea-23': ('win', '20261012215331_1', (900, 270)),
    'national-korea-54': ('win', '20261012215336_1', (900, 270)),
    'news-coronavirus': ('news', '20261012215258_1', (900, 320)),
    'news-taliban': ('news', '20261012215316_1', (900, 320)),
    'super-tfr': ('super', '20261012215721_1', (770, 400))
}

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def captures(directory):
    result = defaultdict(list)
    for path in sorted(directory.glob('*.png')):
        result[re.sub(r'^\d+_', '', path.name)].append(path)
    return result

def pair(left, right, output, labels):
    canvas = Image.new('RGB', (left.width + right.width, max(left.height, right.height) + 28), '#171b24')
    canvas.paste(left.convert('RGB'), (0, 28))
    canvas.paste(right.convert('RGB'), (left.width, 28))
    draw = ImageDraw.Draw(canvas)
    draw.text((10, 8), labels[0], fill='white')
    draw.text((left.width + 10, 8), labels[1], fill='white')
    canvas.save(output)

def glyph_bounds(image, box, bright=False):
    x, y, width, height = box
    crop = image.convert('RGB').crop((x, y, x + width, y + height))
    mask = Image.new('L', crop.size)
    values = []
    for yy in range(height):
        for xx in range(width):
            pixel = crop.getpixel((xx, yy))
            values.append(255 if (min(pixel) > 180 and max(pixel) - min(pixel) < 50 if bright else max(pixel) < 32) else 0)
    mask.putdata(values)
    bounds = mask.getbbox()
    return [x + bounds[0], y + bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1]] if bounds else None

def compare(run, baseline, shots, output, pack, steam):
    output.mkdir(parents=True, exist_ok=True)
    measurements = json.loads((run / 'event-frame-measurements.json').read_text('utf8'))
    after, before = captures(run / 'screenshots'), captures(baseline / 'screenshots')
    focused = all(name.startswith('hoi-event-frame-') for name in after)
    files, changed, added, missing = [], [], [], []
    for name in sorted(before.keys() | after.keys()):
        for index in range(max(len(before[name]), len(after[name]))):
            old = before[name][index] if index < len(before[name]) else None
            new = after[name][index] if index < len(after[name]) else None
            if old is None:
                added.append(new.name)
            elif new is None:
                missing.append(old.name)
            elif digest(old) != digest(new):
                with Image.open(old) as a, Image.open(new) as b:
                    bounds = ImageChops.difference(a.convert('RGB'), b.convert('RGB')).getbbox() if a.size == b.size else None
                    changed.append(dict(name=name, occurrence=index + 1, before=old.name, after=new.name,
                                        before_sha256=digest(old), after_sha256=digest(new), changed_bounds=bounds, size_changed=a.size != b.size))
    for name in sorted(after):
        if name.startswith('hoi-event-frame-'):
            for path in after[name]:
                target = output / 'cases' / path.name
                target.parent.mkdir(exist_ok=True)
                shutil.copy2(path, target)
    table = []
    for case, (kind, filename, origin) in REFERENCES.items():
        key = case + '-2560'
        row = measurements[key]
        original = shots / (kind + '-' + filename + '.png')
        cropped = original.is_file()
        if not cropped:
            original = steam / (filename + '.jpg')
        actual = after['hoi-event-frame-' + case + '-2560-gui2.png'][0]
        with Image.open(original) as reference, Image.open(actual) as full:
            if not cropped:
                width, height = (1040, 680) if kind == 'super' else (720, 860) if kind == 'news' else (720, 1000)
                reference = reference.crop((origin[0], origin[1], origin[0] + width, origin[1] + height))
            current = full.crop((origin[0], origin[1], origin[0] + reference.width, origin[1] + reference.height))
            pair(reference, current, output / (case + '-steam-vs-minecraft.png'), ('Steam TFR / UI 1', 'Actual Minecraft / GUI 2 / default font'))
        pixels = {key: [value * 2 for value in row[key]] for key in ('panelGui', 'titleGui', 'bodyGui', 'pictureGui')}
        pixels['choices'] = [[value * 2 for value in bounds] for bounds in row['choicesGui']]
        with Image.open(actual) as full:
            pixels['titleGlyphScreen'] = glyph_bounds(full, pixels['titleGui'], kind == 'super')
            pixels['choiceGlyphScreen'] = glyph_bounds(full, pixels['choices'][0], True)
            if kind == 'super':
                pixels['quoteFirstLineGlyphScreen'] = glyph_bounds(full, pixels['bodyGui'][:3] + [18], True)
        choice = pixels['choices'][0]
        pixels['visibleChoiceTopApprox'] = choice[1] + (3 if kind == 'super' else 2)
        table.append(dict(id=row['id'], original_file=filename + '.jpg', reference_source_file=original.name, source_sha256=digest(original), actual_sha256=digest(actual),
                          body_lines=row['bodyLines'], body_scroll_screen_pixels=row['bodyScrollPixels'] * 2, **pixels,
                          visible_choice_gap_from_body=pixels['visibleChoiceTopApprox'] - pixels['bodyGui'][1] - pixels['bodyGui'][3],
                          image_gap_from_visible_choice=pixels['pictureGui'][1] - pixels['visibleChoiceTopApprox'],
                          minimize='Answerable national/news events minimize locally and reopen from the pending event list',
                          coordinate_kind='GUI rectangles converted to screen pixels; visible choice inset from original sprite alpha; text boxes are not glyph bounds'))
    for case, name in [('national-one', 'national'), ('news-coronavirus', 'news'), ('super-tfr', 'super')]:
        for width in (2560, 1280):
            source = after[f'hoi-event-frame-{case}-{width}-gui2-window.png'][0]
            shutil.copy2(source, output / f'{name}-{width}-gui2-window.png')
        row = measurements[case + '-2560']
        full = after[f'hoi-event-frame-{case}-2560-gui2.png'][0]
        with Image.open(full) as image:
            x, y, width, height = [v * 2 for v in row['panelGui']]
            tx, ty, tw, th = [v * 2 for v in row['titleGui']]
            crops = {'top-edge': (x, y, x + width, y + 60), 'title': (tx, ty, tx + tw, ty + th)}
            if name == 'national':
                by = (row['bodyGui'][1] + row['bodyGui'][3]) * 2 + 43
                crops['top-middle-seam'] = (x, y + 110, x + width, y + 136)
                crops['middle-bottom-seam'] = (x, by - 16, x + width, by + 16)
            for label, bounds in crops.items():
                crop = image.crop(bounds)
                crop.resize((crop.width * 3, crop.height * 3), Image.Resampling.NEAREST).save(output / f'{name}-{label}-nearest-3x.png')
    sprite_check = []
    sys.path.insert(0, str(pack / 'scripts'))
    from import_assets import read_image
    report = json.loads((pack / 'event-frame-import-report.json').read_text('utf8'))
    for row in report['sprites']:
        source = Path(row['source'])
        target = pack / 'pack/assets/hoi/textures/gui/events/frame' / (row['target'] + '.png')
        with read_image(source) as a, Image.open(target) as b:
            identical = a.size == b.size and a.convert('RGBA').tobytes() == b.convert('RGBA').tobytes()
        if not identical or digest(source) != row['source_sha256']:
            raise ValueError('Original sprite pixels or input hash differ: ' + str(source))
        sprite_check.append(dict(sprite=row['sprite'], size=[row['width'], row['height']], pixels_identical=identical))
    result = dict(runDirectory=str(run), runStatus=json.loads((run / 'hoi-run.json').read_text('utf8'))['status'],
                  comparisonScope='Focused event-frames suite; missing baseline captures are outside this suite' if focused else 'Full client suite',
                  baselineRun=str(baseline), coordinateComparison=table,
                  originalSprites=sprite_check, measurements=measurements, baselinePngCount=sum(map(len, before.values())),
                  currentPngCount=sum(map(len, after.values())), changedCaptures=changed, addedCaptures=added, missingCaptures=missing,
                  verificationLimits=['Original screenshots show one option only; multiple/no-image/long-body behavior is a bounded client assumption.',
                                      'Minecraft default font and 12 GUI-pixel line spacing retained; original font equivalence is not claimed.',
                                      'Fixtures use source text/images; they do not establish live server execution, multiplayer, or audible sound checks.'])
    (output / 'render-verification.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', 'utf8')
    shutil.copy2(run / 'event-frame-measurements.json', output / 'event-frame-measurements.json')
    for path in sorted(output.rglob('*')):
        if path.is_file():
            files.append(dict(path=path.relative_to(output).as_posix(), bytes=path.stat().st_size, sha256=digest(path)))
    (output / 'manifest.json').write_text(json.dumps(files, indent=2) + '\n', 'utf8')
    print(json.dumps(dict(originalComparisons=len(table), changed=len(changed), added=len(added), missing=len(missing), originalSprites=sprite_check), indent=2))

if __name__ == '__main__':
    cli = argparse.ArgumentParser()
    cli.add_argument('--run', type=Path, required=True)
    cli.add_argument('--baseline', type=Path, required=True)
    cli.add_argument('--shots', type=Path, required=True)
    cli.add_argument('--output', type=Path, required=True)
    cli.add_argument('--pack', type=Path, required=True)
    cli.add_argument('--steam', type=Path, default=Path('C:/Program Files (x86)/Steam/userdata/1398211522/760/remote/394360/screenshots'))
    args = cli.parse_args()
    compare(args.run, args.baseline, args.shots, args.output, args.pack, args.steam)
