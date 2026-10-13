#!/usr/bin/env python3
"""Brings the DF's emoji font up to the latest Noto Color Emoji on Google Fonts.

Rocco's request of 2026-10-13: the DF draws emoji with Google's Noto Color Emoji, latest
version, and the system's only where it cannot. The page's security policy takes fonts from
its own address only, so the font is copied here: the slices Google serves, each with its
unicode-range, which a browser downloads only when the page shows one of its characters. The
slices are COLRv1, and each source says so with tech(): a browser without COLRv1 (Safari)
skips them and keeps the system's emoji.

Run from the repository root; it rewrites publish/assets/noto-emoji/ and the block between the
noto-emoji markers in publish/feedback.css. Author: Rocco Casadei, a.k.a. Roccobot.
"""
import re
import shutil
import sys
import urllib.request
from pathlib import Path

CSS_URL = 'https://fonts.googleapis.com/css2?family=Noto+Color+Emoji&display=swap'
LICENSE_URL = 'https://raw.githubusercontent.com/google/fonts/main/ofl/notocoloremoji/OFL.txt'
# A desktop Chrome: Google picks the format by the browser asking, and Chrome gets COLRv1.
AGENT = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36'
FOLDER = Path('publish/assets/noto-emoji')
STYLESHEET = Path('publish/feedback.css')
BEGIN, END = '/* noto-emoji:begin', '/* noto-emoji:end */'


def fetch(url):
    request = urllib.request.Request(url, headers={'User-Agent': AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


def main():
    css = fetch(CSS_URL).decode()
    faces = re.findall(r'src: url\((https://[^)]+)\) format\(\'woff2\'\);\s*unicode-range: ([^;]+);', css)
    if not faces:
        sys.exit('Google Fonts did not answer with the expected @font-face rules.')
    version = re.search(r'/notocoloremoji/(v\d+)/', faces[0][0]).group(1)
    shutil.rmtree(FOLDER, ignore_errors=True)
    FOLDER.mkdir(parents=True)
    blocks = []
    for url, ranges in faces:
        name = url.rsplit('.', 2)[-2] + '.woff2'
        (FOLDER / name).write_bytes(fetch(url))
        blocks.append(
            '@font-face {\n'
            '  font-family: "Noto Color Emoji";\n'
            '  font-display: swap;\n'
            f'  src: url("assets/noto-emoji/{name}") format("woff2") tech(color-COLRv1);\n'
            f'  unicode-range: {ranges};\n'
            '}'
        )
    (FOLDER / 'OFL.txt').write_bytes(fetch(LICENSE_URL))
    block = f'{BEGIN}: Noto Color Emoji {version} from Google Fonts, written by tools/feedback-emoji.py */\n' + '\n'.join(blocks) + '\n' + END
    text = STYLESHEET.read_text()
    if text.count(BEGIN) != 1 or text.count(END) != 1:
        sys.exit('publish/feedback.css needs one noto-emoji block, between its two markers.')
    start, end = text.index(BEGIN), text.index(END) + len(END)
    STYLESHEET.write_text(text[:start] + block + text[end:])
    print(f'Noto Color Emoji {version}: {len(faces)} parti in {FOLDER}.')


if __name__ == '__main__':
    main()
