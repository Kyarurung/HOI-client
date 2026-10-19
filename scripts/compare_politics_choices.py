from pathlib import Path
import argparse
import hashlib
import json
from PIL import Image, ImageDraw, ImageFont


SCENES = [
    ('육군참모총장 · 장교단', '20261011144045_1.jpg', 'army_chief-officer_corps'),
    ('해군참모총장 · 장교단', '20261011144047_1.jpg', 'navy_chief-officer_corps'),
    ('공군참모총장 · 장교단', '20261011144049_1.jpg', 'air_chief-officer_corps'),
    ('최고사령부 · 장교단', '20261011144526_1.jpg', 'high_command-officer_corps'),
    ('육군참모총장 · 정치', '20261011144053_1.jpg', 'army_chief-politics'),
    ('해군참모총장 · 정치', '20261011144054_1.jpg', 'navy_chief-politics'),
    ('공군참모총장 · 정치', '20261011144055_1.jpg', 'air_chief-politics'),
    ('최고사령부 · 정치', '20261011144528_1.jpg', 'high_command-politics'),
    ('안보법 · 정치', '20261011144106_1.jpg', 'law-politics'),
    ('정부수반 · 정치', '20261011144059_1.jpg', 'minister-politics'),
    ('정보부장 · 정치', '20261011144101_1.jpg', 'intelligence-politics'),
    ('국방장관 · 정치', '20261011144102_1.jpg', 'defense-politics'),
]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def screenshot(directory, name, state='ready'):
    found = list(directory.glob('*hoi-prc-choice-' + name + '-' + state + '-gui2.png'))
    if len(found) != 1:
        raise ValueError(f'Expected one actual render for {name}: {found}')
    return found[0]


def compare(originals, renders, output, font_path, fixtures):
    output.mkdir(parents=True, exist_ok=True)
    font = ImageFont.truetype(str(font_path), 18)
    small = ImageFont.truetype(str(font_path), 14)
    records = []
    for index, (label, original, name) in enumerate(SCENES):
        source = originals / original
        render = screenshot(renders, name)
        sheet = Image.new('RGB', (1040, 718), '#18191d')
        draw = ImageDraw.Draw(sheet)
        draw.text((12, 8), label + ' / 같은 픽셀 배율 1:1 · 리사이즈 없음', font=font, fill='white')
        draw.text((12, 37), '원본 TFR · ' + original, font=small, fill='#e5d399')
        draw.text((530, 37), '새 Minecraft 실제 렌더 · GUI 2 · PRC', font=small, fill='#e5d399')
        box = (710, 80, 1210, 680)
        with Image.open(source) as native, Image.open(render) as actual:
            if native.size != (2560, 1440) or actual.size != (2560, 1440):
                raise ValueError(f'Reference captures must be 2560x1440: {source}, {render}')
            sheet.paste(native.crop(box), (12, 65))
            sheet.paste(actual.crop(box), (530, 65))
        draw.text((12, 673), '원본 세이브와 2020 시험 캠페인은 진행·비용·후보 조건이 다릅니다.', font=small, fill='white')
        draw.text((12, 695), '보드 결정 유지: 기본 글꼴/크기 · 회색 특성 글씨 · 2줄 말줄임. 세이브 차이는 미조정.', font=small, fill='#ffcc88')
        target = output / f'{index + 1:02d}-{name}.png'
        sheet.save(target)
        fixture = fixtures / (name.rsplit('-', 1)[0] + '-ready.json')
        view = json.loads(fixture.read_text(encoding='utf8'))
        records.append(dict(scene=label, original=source.as_posix(), original_sha256=digest(source),
                            render=render.as_posix(), render_sha256=digest(render), crop=box,
                            fixture=fixture.as_posix(), fixture_sha256=digest(fixture),
                            candidates=[dict(name=tile['name'], overlay=tile['overlay'], cost=tile['value'].split('\n')[0]) for tile in view['tiles']],
                            output=target.name, output_sha256=digest(target), resized=False))
    for name in ('army_chief', 'navy_chief', 'air_chief', 'high_command'):
        with Image.open(screenshot(renders, name + '-politics')) as political, Image.open(screenshot(renders, name + '-officer_corps')) as officer:
            box = (720, 85, 1200, 660)
            if political.crop(box).tobytes() != officer.crop(box).tobytes():
                raise AssertionError(f'Choice pixels differ between parent tabs: {name}')
    with Image.open(screenshot(renders, 'army_chief-politics')) as before, Image.open(screenshot(renders, 'army_chief-politics', 'reloaded')) as after:
        box = (720, 85, 1200, 660)
        if before.crop(box).tobytes() != after.crop(box).tobytes():
            raise AssertionError('Grayscale choice pixels changed after releasing and recreating the runtime textures')
    (output / 'comparison-manifest.json').write_text(json.dumps(records, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    ministers = Image.new('RGB', (1536, 804), '#18191d')
    draw = ImageDraw.Draw(ministers)
    draw.text((12, 8), '정부수반·장관 6종 / 실제 PRC 렌더 · GUI 2 · 픽셀 배율 1:1', font=font, fill='white')
    for index, (name, label) in enumerate([('minister', '정부수반'), ('foreign', '외무장관'), ('finance', '재무장관'),
                                            ('interior', '내무장관'), ('intelligence', '정보부장'), ('defense', '국방장관')]):
        x, y = 12 + index % 3 * 512, 46 + index // 3 * 376
        draw.text((x, y), label + ' · 잠김: 흑백 · X 없음', font=small, fill='#e5d399')
        with Image.open(screenshot(renders, name + '-politics')) as actual:
            ministers.paste(actual.crop((710, 80, 1210, 420)), (x, y + 24))
    ministers.save(output / '13-all-ministers.png')
    states = Image.new('RGB', (1040, 2660), '#18191d')
    draw = ImageDraw.Draw(states)
    draw.text((12, 8), '실제 PRC 상태 검증 / 현재 인물 녹색 · 조건 미충족 X · 정치력 부족 빨강', font=font, fill='white')
    for index, (name, label) in enumerate([('army_chief', '육군참모총장'), ('navy_chief', '해군참모총장'),
                                           ('air_chief', '공군참모총장'), ('high_command', '최고사령부')]):
        y = 46 + index * 650
        for column, state in enumerate(('ready', 'poor')):
            x = 12 + column * 518
            draw.text((x, y), label + (' · 정치력 2000' if state == 'ready' else ' · 정치력 0'), font=small, fill='#e5d399')
            with Image.open(screenshot(renders, name + '-politics', state)) as actual:
                states.paste(actual.crop((710, 80, 1210, 680)), (x, y + 24))
    states.save(output / '14-candidate-states.png')
    locked = Image.new('RGB', (1040, 1400), '#18191d')
    draw = ImageDraw.Draw(locked)
    draw.text((12, 8), '잠긴 장관 / 조건 미충족 법률 · 실제 렌더 · GUI 2 · 픽셀 배율 1:1', font=font, fill='white')
    for index, (name, label) in enumerate([('defense', '국방장관 · 현재/공석 흑백 · X 없음'), ('law', '안보법 · 안보 없음: 흑백/X/회색 비용')]):
        y = 46 + index * 650
        for column, state in enumerate(('ready', 'poor')):
            x = 12 + column * 518
            draw.text((x, y), label, font=small, fill='#e5d399')
            draw.text((x, y + 20), '정치력 2000 · 비용 노랑(X칸은 회색)' if state == 'ready' else '정치력 0 · 부족 비용 빨강(X칸은 회색)', font=small, fill='white')
            with Image.open(screenshot(renders, name + '-politics', state)) as actual:
                locked.paste(actual.crop((710, 80, 1210, 680)), (x, y + 42))
    locked.save(output / '15-locked-law-states.png')
    print(f'Created {len(records)} lossless native/actual-render comparison sheets and three state sheets; four parent-tab and one resource-reload pixel comparisons passed')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--originals', type=Path, required=True)
    parser.add_argument('--renders', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--fixtures', type=Path, required=True)
    parser.add_argument('--font', type=Path, default=Path('C:/Windows/Fonts/malgun.ttf'))
    args = parser.parse_args()
    compare(args.originals, args.renders, args.output, args.font, args.fixtures)
