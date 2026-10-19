from pathlib import Path
import argparse
import hashlib
import json

def export(catalog, output):
    data = json.loads(catalog.read_text('utf8'))
    events = data['events']
    cases = [('national-korea-21', events['korea.21']),
             ('national-korea-1', events['korea.1']),
             ('national-korea-2', events['korea.2']),
             ('national-one', events['korea.79']),
             ('national-korea-23', events['korea.23']),
             ('national-korea-54', events['korea.54']),
             ('national-three', next(e for e in events.values() if e['kind'] == 'NATIONAL' and len(e['options']) == 3 and len({o['id'] for o in e['options']}) == 3 and e['image'])),
             ('national-four', next(e for e in events.values() if e['kind'] == 'NATIONAL' and len(e['options']) == 4 and len({o['id'] for o in e['options']}) == 4 and e['image'])),
             ('national-max', events['russia.454']),
             ('national-no-image', next(e for e in events.values() if e['kind'] == 'NATIONAL' and not e['image'] and e['options'])),
             ('national-long-body', events['Australia.0']),
             ('news-coronavirus', events['news.113']),
             ('news-taliban', events['news.131']),
             ('news-one', next(e for e in events.values() if e['kind'] == 'GLOBAL' and len(e['options']) == 1 and e['image'])),
             ('news-four', next(e for e in events.values() if e['kind'] == 'GLOBAL' and len(e['options']) == 4 and len({o['id'] for o in e['options']}) == 4 and e['image'])),
             ('news-long-body', events['gma_news.5'])]
    rows = []
    for name, event in cases:
        choices = event['options'][:1] if event['id'] == 'news.131' else event['options']
        rows.append(dict(name=name, id=event['id'], titleKey=event['titleKey'], bodyKey=event['bodyKey'],
                         choiceScope='Steam screenshot non-USA visible option news.131.a; TFR_events_ZZZ_news.txt trigger NOT tag=USA' if event['id'] == 'news.131' else 'Full source catalog options',
                         view=dict(token='', revision=0, kind='GLOBAL_EVENT' if event['kind'] == 'GLOBAL' else 'EVENT',
                                   title=event['title'], subtitle='', body=event['body'], flag='', image=event['image'],
                                   sound='', tiles=[], choices=choices, presentation='')))
    for name, key in [('super-tfr', 'SAU_civil_war_super_event'), ('super-tfrkp', 'tfr_se_kor_martial_law_yoon'),
                      ('super-long-quote', 'PRC_eadi_victory_super_event')]:
        event = data['superEvents'][key]
        rows.append(dict(name=name, id=key, titleKey=event['titleKey'], bodyKey=event['bodyKey'],
                         view=dict(token='', revision=0, kind='SUPER_EVENT', title=event['title'], subtitle='', body=event['body'],
                                   flag='', image=event['image'], sound=event['sound'], tiles=[],
                                   choices=[dict(id='ack', label=event['button'])], presentation='')))
    result = dict(source=str(catalog), source_sha256=hashlib.sha256(catalog.read_bytes()).hexdigest(),
                  scope='Source catalog renderer examples; not proof that all source options/effects are executable server gameplay.', cases=rows)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', 'utf8')
    print('Exported', len(rows), 'original source examples')

if __name__ == '__main__':
    cli = argparse.ArgumentParser()
    cli.add_argument('--catalog', type=Path, required=True)
    cli.add_argument('--output', type=Path, required=True)
    args = cli.parse_args()
    export(args.catalog, args.output)
