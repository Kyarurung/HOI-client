from pathlib import Path
import argparse
import hashlib
import json
from PIL import Image, ImageChops, ImageDraw, ImageFont

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def find(folder, name):
    return next(folder.glob('*' + name + '.png'))

def compare(before, after, measurements, output, allow_event_text_change=False, allow_event_choice_change=False):
    output.mkdir(parents=True, exist_ok=True)
    data = json.loads(measurements.read_text('utf8'))
    font = ImageFont.truetype('C:/Windows/Fonts/consola.ttf', 18)
    records = []
    for name in ('china-705', 'china-182'):
        original = find(after, 'hoi-leader-event-source-' + name + '-2560-gui2-window')
        capture = Image.open(original).convert('RGB')
        sheet = Image.new('RGB', (1160, max(900, capture.height + 100)), '#20232a')
        draw = ImageDraw.Draw(sheet)
        draw.text((20, 15), 'Original GUI definition / sprite sizes', font=font, fill='white')
        draw.text((20, 43), 'No matching original in-game screenshot found', font=font, fill='#e5b74b')
        lines = ['EventWindow_leader / TFR', 'Root: CENTER(-282,-310)', '', 'GUI source units:', 'Title: (14,70), 551x32, centered', 'Middle: (0,121), declared 605x235', 'Portrait: (43,125), 156x210 * 0.85', 'Frame: (35,116), 172x232 * 0.85', 'Body: (198,121), max 350x350', 'Bottom: authored y=221; runtime not observed', 'Choices: x=115, y=10 in bottom', 'Slots: 300x47, one column', '', 'Native sprite pixels:', 'top 605x123; middle/options 605x80', 'end 605x68; choice 352x48', 'minimize 33x33 (not drawn)', '', 'Minecraft capture, framebuffer pixels:']
        m = data[name + '-2560']
        lines.append('Panel: ' + str([x * 2 for x in m['panelGui']]))
        for key in ('titleGui', 'portraitGui', 'frameGui'):
            lines.append(key.replace('Gui', '') + ': ' + str([x * 2 for x in m[key]]))
        lines.append('Choices: ' + str([[x * 2 for x in box] for box in m['choicesGui']]))
        lines.extend(['', 'Current event font retained: 9 GUI px', 'Source fonts: typewriter22 / Electro(16)', 'Bottom follows actual body + portrait height'])
        draw.multiline_text((20, 86), '\n'.join(lines), font=font, fill='#cbd3df', spacing=5)
        draw.text((540, 15), name + ': actual Minecraft / GUI scale 2', font=font, fill='white')
        sheet.paste(capture, (540, 80))
        target = output / (name + '-definition-vs-render.png')
        sheet.save(target)
        records.append(dict(file=target.name, capture=str(original), capture_sha256=digest(original), sha256=digest(target)))
    regression = []
    for kind in ('event', 'global_event', 'super_event', 'completion'):
        name = 'hoi-leader-regression-' + kind + '-gui2'
        previous, current = find(before, name), find(after, name)
        a, b = Image.open(previous).convert('RGB'), Image.open(current).convert('RGB')
        bounds = (970, 320, 1590, 1120) if kind in ('event', 'global_event') else (780, 400, 1780, 1040) if kind == 'super_event' else (1030, 570, 1530, 870)
        left, right = a.crop(bounds), b.crop(bounds)
        difference = ImageChops.difference(left, right)
        changed = sum(pixel != (0, 0, 0) for pixel in difference.get_flattened_data())
        if changed and not (kind == 'event' and (allow_event_text_change or allow_event_choice_change)):
            raise AssertionError(f'{kind}: {changed} changed pixels inside the popup')
        outside_text = difference.copy()
        if kind == 'event' and allow_event_text_change:
            outside_text.paste((0, 0, 0), (24, 70, 594, 118))
            outside_text.paste((0, 0, 0), (30, 130, 590, 470))
        if kind == 'event' and allow_event_choice_change:
            outside_text.paste((0, 0, 0), (24, 470, 594, 790))
        if kind == 'event' and (allow_event_text_change or allow_event_choice_change) and outside_text.getbbox():
            raise AssertionError('Ordinary event changed outside its permitted text and choice regions')
        sheet = Image.new('RGB', (left.width * 2 + 60, left.height + 80), '#20232a')
        draw = ImageDraw.Draw(sheet)
        draw.text((20, 15), kind + ': before / external pack loaded', font=font, fill='white')
        draw.text((left.width + 40, 15), 'after / ' + str(changed) + ' changed popup pixels', font=font, fill='white')
        sheet.paste(left, (20, 60)); sheet.paste(right, (left.width + 40, 60))
        target = output / (kind + '-before-after.png'); sheet.save(target)
        regression.append(dict(kind=kind, bounds=bounds, changed_popup_pixels=changed, changed_outside_text_pixels=sum(pixel != (0, 0, 0) for pixel in outside_text.get_flattened_data()), before_sha256=digest(previous), after_sha256=digest(current), comparison=target.name))
    (output / 'comparison-manifest.json').write_text(json.dumps(dict(measurements=data, measurements_sha256=digest(measurements), comparisons=records, regressions=regression), ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print('Two definition/render comparisons and four measured popup comparisons generated')

if __name__ == '__main__':
    cli = argparse.ArgumentParser()
    cli.add_argument('--before', type=Path, required=True)
    cli.add_argument('--after', type=Path, required=True)
    cli.add_argument('--measurements', type=Path, required=True)
    cli.add_argument('--output', type=Path, required=True)
    cli.add_argument('--allow-event-text-change', action='store_true')
    cli.add_argument('--allow-event-choice-change', action='store_true')
    args = cli.parse_args()
    compare(args.before, args.after, args.measurements, args.output, args.allow_event_text_change, args.allow_event_choice_change)
