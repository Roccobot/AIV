#!/usr/bin/env python3
"""Compares AIV's dependencies (docs/Dependencies.json) with their latest published versions,
and says which ones to read for what is worth bringing in (Rules.md, the dependencies; the
universal rule is in rules/Roccobot.md of Roccobot/tools).

    tools/check-dependencies.py

The version in use comes from where it is set: scripts/markdown/package.json for the npm
packages, Package.resolved for the Swift packages, the file an `in_use` entry names (a font's
Google Fonts version, written by the script that copies it), the register itself for what was
ported or followed. GitHub is read with `gh` (authenticated), npm with `npm view`, Google Fonts
with `curl`. Nothing is changed:
after reading a newer version, its number goes in the register (or the manifest) by hand, in
the commit that brings in what was worth taking.
Exits 1 when something is newer, 0 when everything is current.
Author: Rocco Casadei, a.k.a. Roccobot
"""
import base64
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def gh(path):
    return json.loads(subprocess.run(['gh', 'api', path], capture_output=True, text=True, check=True).stdout)


def npm_latest(package):
    # npm, not urllib: the python.org builds of Python on macOS have no certificates of their own.
    return subprocess.run(['npm', 'view', package, 'version'], capture_output=True, text=True, check=True).stdout.strip()


def file_version(repo, path):
    text = base64.b64decode(gh(f'repos/{repo}/contents/{path}')['content']).decode()
    if path.endswith('.json'):
        return json.loads(text)['version']
    match = re.search(r'@version\s+(\S+)', text)
    return match[1] if match else '?'


def google_fonts_latest(family):
    # A desktop Chrome, as tools/feedback-emoji.py asks: the version is in the files' addresses.
    agent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36'
    css = subprocess.run(['curl', '-s', '-A', agent, f'https://fonts.googleapis.com/css2?family={family}'], capture_output=True, text=True, check=True).stdout
    match = re.search(r'fonts\.gstatic\.com/s/[a-z]+/(v\d+)/', css)
    return match[1] if match else '?'


def in_use(dependency, manifests, resolved):
    if 'in_use' in dependency:
        match = re.search(dependency['in_use']['pattern'], (ROOT / dependency['in_use']['file']).read_text())
        return match[1] if match else '?'
    if dependency['source'] == 'npm':
        return manifests[dependency['manifest']]['dependencies'][dependency['package']]
    if dependency['source'] == 'github-release':
        return resolved[dependency['resolved']]
    return dependency['version']


def latest(dependency):
    source = dependency['source']
    if source == 'npm':
        return npm_latest(dependency['package'])
    if source == 'github-file':
        return file_version(dependency['repo'], dependency['path'])
    if source == 'github-release':
        return gh(f"repos/{dependency['repo']}/releases/latest")['tag_name'].lstrip('v')
    if source == 'github-commit':
        return gh(f"repos/{dependency['repo']}/commits?path={dependency['path']}&per_page=1")[0]['sha'][:7]
    if source == 'google-fonts':
        return google_fonts_latest(dependency['family'])
    raise SystemExit(f'unknown source: {source}')


def main():
    register = json.loads((ROOT / 'docs/Dependencies.json').read_text())['dependencies']
    manifests = {d['manifest']: json.loads((ROOT / d['manifest']).read_text()) for d in register if 'manifest' in d}
    pins = json.loads((ROOT / 'Package.resolved').read_text())['pins'] if (ROOT / 'Package.resolved').exists() else []
    resolved = {pin['identity']: pin['state'].get('version', '') for pin in pins}
    newer = []
    for dependency in register:
        current, published = in_use(dependency, manifests, resolved), latest(dependency)
        mark = 'ok' if current == published else 'NEW'
        print(f"{mark:4} {dependency['name']}: {current} -> {published}  ({dependency['into']})")
        if current != published:
            newer.append(dependency['name'])
    if newer:
        print('\nTo read for what is worth bringing in: ' + ', '.join(newer))
        sys.exit(1)


main()
