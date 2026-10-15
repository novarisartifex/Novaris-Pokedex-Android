#!/usr/bin/env python3
import argparse, gzip, json, os, re, sqlite3, sys
from lxml import html

WS = re.compile(r'[\t\r\f\v ]+')
MULTI_NL = re.compile(r'\n{3,}')

def clean_text(s):
    if s is None:
        return ''
    s = str(s).replace('\xa0', ' ')
    lines=[]
    for line in s.splitlines():
        line=WS.sub(' ', line).strip()
        if line:
            lines.append(line)
    return '\n'.join(lines)

def first(xs, default=''):
    return xs[0] if xs else default

def data_attrs(el):
    out={}
    if el is None: return out
    for k,v in el.attrib.items():
        if k.startswith('data-'):
            out[k[5:]] = v
    return out

def extract_characters(raw_text):
    marker='const BASE='
    pos=raw_text.find(marker)
    if pos < 0:
        return []
    start=pos+len(marker)
    # JS immediately continues with ; const...; scan JSON array robustly.
    depth=0; in_str=False; esc=False; end=None
    for i,ch in enumerate(raw_text[start:], start):
        if in_str:
            if esc: esc=False
            elif ch=='\\': esc=True
            elif ch=='"': in_str=False
            continue
        if ch=='"': in_str=True; continue
        if ch=='[': depth += 1
        elif ch==']':
            depth -= 1
            if depth==0:
                end=i+1; break
    if not end:
        return []
    try:
        arr=json.loads(raw_text[start:end])
        return arr if isinstance(arr,list) else []
    except Exception as e:
        print('Character JSON parse warning:', e, file=sys.stderr)
        return []


def extract_js_string_constant(raw_text, name):
    # Constants are JSON-compatible double-quoted JS strings in the Pokédex HTML.
    m=re.search(r'const\s+'+re.escape(name)+r'\s*=\s*("(?:\\.|[^"\\])*")\s*;', raw_text, re.S)
    if not m:
        return ''
    try:
        return json.loads(m.group(1))
    except Exception as e:
        print(f'Prompt constant parse warning {name}:', e, file=sys.stderr)
        return ''

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('html_file')
    ap.add_argument('out_gz')
    ap.add_argument('--version', default='V27.42')
    args=ap.parse_args()
    print('Parsing', args.html_file)
    raw=open(args.html_file,'r',encoding='utf-8',errors='replace').read()
    parser=html.HTMLParser(encoding='utf-8', recover=True, huge_tree=True)
    tree=html.parse(args.html_file, parser)
    root=tree.getroot()

    table = first(root.xpath("//table[.//tbody[@id='dexBody']]"), None)
    if table is None:
        raise SystemExit('dexBody table not found')
    headings=[clean_text(x.text_content()) for x in table.xpath('.//thead//th')]
    rows=root.xpath("//*[@id='dexBody']/tr[@data-dex]")
    print('Pokemon rows:', len(rows))

    db_path=args.out_gz[:-3] if args.out_gz.endswith('.gz') else args.out_gz+'.db'
    os.makedirs(os.path.dirname(os.path.abspath(args.out_gz)), exist_ok=True)
    if os.path.exists(db_path): os.remove(db_path)
    con=sqlite3.connect(db_path)
    cur=con.cursor()
    cur.executescript('''
    PRAGMA journal_mode=OFF;
    PRAGMA synchronous=OFF;
    CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL);
    CREATE TABLE pokemon (
      dex INTEGER PRIMARY KEY,
      label TEXT NOT NULL,
      en_name TEXT NOT NULL,
      de_name TEXT NOT NULL,
      generation INTEGER NOT NULL,
      artwork_url TEXT,
      career_role TEXT,
      career_category TEXT,
      career_tier TEXT,
      job_family TEXT,
      previous_career_role TEXT,
      changed INTEGER NOT NULL DEFAULT 0,
      attrs_json TEXT NOT NULL,
      cells_json TEXT NOT NULL,
      forms_json TEXT NOT NULL,
      shiny_json TEXT NOT NULL,
      search_text TEXT NOT NULL
    );
    CREATE INDEX idx_pokemon_generation ON pokemon(generation);
    CREATE INDEX idx_pokemon_role ON pokemon(career_role);
    CREATE TABLE characters (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      aliases TEXT,
      source TEXT,
      version TEXT,
      kind TEXT,
      category TEXT,
      generation_key TEXT,
      portrait_url TEXT,
      canon_text TEXT,
      novaris_text TEXT,
      scenes_json TEXT NOT NULL,
      raw_json TEXT NOT NULL,
      search_text TEXT NOT NULL
    );
    CREATE INDEX idx_char_category ON characters(category);
    CREATE INDEX idx_char_generation ON characters(generation_key);
    ''')
    cur.execute('INSERT INTO metadata(key,value) VALUES (?,?)',('source_version',args.version))
    cur.execute('INSERT INTO metadata(key,value) VALUES (?,?)',('source_file',os.path.basename(args.html_file)))
    cur.execute('INSERT INTO metadata(key,value) VALUES (?,?)',('schema_version','2'))
    for key,const_name in (
        ('prompt_core','NOVARIS_CORE_V117'),
        ('prompt_output_single','NOVARIS_OUTPUT_SINGLE'),
        ('prompt_output_triple','NOVARIS_OUTPUT_TRIPLE'),
        ('prompt_form_variant','NOVARIS_FORM_VARIANT_V111'),
    ):
        cur.execute('INSERT INTO metadata(key,value) VALUES (?,?)',(key,extract_js_string_constant(raw,const_name)))

    for idx,row in enumerate(rows,1):
        dex=int(row.get('data-dex') or 0)
        en=row.get('data-en-name') or ''
        de=row.get('data-de-name') or ''
        gen=int(row.get('data-generation') or 0)
        art=first(row.xpath("./td[contains(concat(' ',normalize-space(@class),' '),' art-cell ')]//*[contains(concat(' ',normalize-space(@class),' '),' art-wrap ')]"), None)
        if art is None:
            art=first(row.xpath(".//*[contains(concat(' ',normalize-space(@class),' '),' art-wrap ')]"), None)
        attrs=data_attrs(art)
        label=attrs.get('label') or f'#{dex:04d}.{en}'
        artwork_url=first(row.xpath("./td[1]//img/@src"), '')
        cells=[]
        tds=row.xpath('./td')
        for i,td in enumerate(tds):
            heading=headings[i] if i < len(headings) else f'Field {i+1}'
            txt=clean_text(td.text_content())
            cells.append({'heading':heading,'text':txt})
        forms=[]
        if len(tds)>1:
            for card in tds[1].xpath(".//*[contains(concat(' ',normalize-space(@class),' '),' form-card ')]"):
                name=clean_text(' '.join(card.xpath(".//*[contains(concat(' ',normalize-space(@class),' '),' form-label ')]//text()"))) or card.get('data-form-label') or ''
                url=first(card.xpath('.//img/@src'),'')
                blueprint=clean_text('\n'.join(card.xpath(".//*[contains(concat(' ',normalize-space(@class),' '),' form-blueprint-text ')]//text()")))
                filename=clean_text(' '.join(card.xpath(".//*[contains(concat(' ',normalize-space(@class),' '),' form-filename-copy-btn ')]//text()")))
                forms.append({'name':name,'url':url,'blueprint':blueprint,'filename':filename})
        shiny=[]
        if tds:
            for card in tds[-1].xpath(".//*[contains(concat(' ',normalize-space(@class),' '),' form-card ')]"):
                name=clean_text(' '.join(card.xpath(".//*[contains(concat(' ',normalize-space(@class),' '),' form-label ')]//text()"))) or card.get('data-form-label') or ''
                url=first(card.xpath('.//img/@src'),'')
                shiny.append({'name':name,'url':url})
            if not shiny:
                for img in tds[-1].xpath('.//img'):
                    shiny.append({'name':img.get('alt') or 'Shiny','url':img.get('src') or ''})
        career_role=attrs.get('career-role','')
        prev=attrs.get('previous-career-role','')
        changed=1 if prev and prev != career_role else 0
        search_parts=[label,en,de,str(dex),str(gen),career_role,attrs.get('career-category',''),attrs.get('career-tier',''),attrs.get('job-family','')]
        search_parts.extend(c['text'] for c in cells)
        search='\n'.join(search_parts).lower()
        cur.execute('''INSERT INTO pokemon VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)''',(
            dex,label,en,de,gen,artwork_url,career_role,attrs.get('career-category',''),attrs.get('career-tier',''),attrs.get('job-family',''),prev,changed,
            json.dumps(attrs,ensure_ascii=False,separators=(',',':')),
            json.dumps(cells,ensure_ascii=False,separators=(',',':')),
            json.dumps(forms,ensure_ascii=False,separators=(',',':')),
            json.dumps(shiny,ensure_ascii=False,separators=(',',':')),
            search
        ))
        if idx % 100 == 0: print(' converted', idx)

    chars=extract_characters(raw)
    print('Characters:', len(chars))
    for c in chars:
        cid=str(c.get('id') or c.get('name') or '')
        if not cid: continue
        name=str(c.get('name') or '')
        aliases=str(c.get('aliases') or '')
        source=str(c.get('source') or '')
        version=str(c.get('version') or '')
        kind=str(c.get('kind') or '')
        category=str(c.get('category') or '')
        # mirror JS generationOf() heuristics loosely, while preserving source fields
        blob=' '.join([source,version]).lower()
        if 'kanto' in blob or 'johto' in blob or 'original' in blob: g='original'
        elif 'hoenn' in blob or 'advanced' in blob: g='advanced'
        elif 'sinnoh' in blob or 'diamond' in blob or 'pearl' in blob: g='dp'
        elif 'unova' in blob or 'einall' in blob or 'best wishes' in blob: g='bw'
        elif 'kalos' in blob or 'xy' in blob or 'xyz' in blob: g='xy'
        elif 'alola' in blob or 'sun' in blob or 'moon' in blob: g='sm'
        elif 'journeys' in blob or 'reisen' in blob: g='journeys'
        elif 'horizon' in blob: g='horizons'
        else: g='other'
        scenes=c.get('scenes') or []
        search=' '.join([name,aliases,source,version,kind,category,str(c.get('canon') or ''),str(c.get('novaris') or ''),json.dumps(scenes,ensure_ascii=False)]).lower()
        cur.execute('''INSERT OR REPLACE INTO characters VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)''',(
            cid,name,aliases,source,version,kind,category,g,str(c.get('portrait') or ''),str(c.get('canon') or ''),str(c.get('novaris') or ''),
            json.dumps(scenes,ensure_ascii=False,separators=(',',':')),json.dumps(c,ensure_ascii=False,separators=(',',':')),search
        ))

    cur.execute('INSERT INTO metadata(key,value) VALUES (?,?)',('pokemon_count',str(len(rows))))
    cur.execute('INSERT INTO metadata(key,value) VALUES (?,?)',('character_count',str(len(chars))))
    con.commit()
    con.execute('VACUUM')
    con.close()
    with open(db_path,'rb') as src, gzip.open(args.out_gz,'wb',compresslevel=9) as dst:
        while True:
            b=src.read(1024*1024)
            if not b: break
            dst.write(b)
    print('DB:', os.path.getsize(db_path), 'GZ:', os.path.getsize(args.out_gz))
    os.remove(db_path)

if __name__=='__main__': main()
