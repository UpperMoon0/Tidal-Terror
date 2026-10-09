#!/usr/bin/env python3
"""Detect mod/datapack archives that replace Tidal Terror's minecraft:normal preset.

Usage: python tools/audit_world_preset_conflicts.py PATH [PATH ...]
Any jar/zip or directory tree can be scanned. No archives are extracted.
"""
from __future__ import annotations

import argparse
from pathlib import Path
from zipfile import BadZipFile, ZipFile

TARGET = 'data/minecraft/worldgen/world_preset/normal.json'
OWN_MARKER = 'data/tidalterror/worldgen/biome/sunken_wastes.json'


def find_conflicts(paths: list[Path]) -> list[str]:
    found: list[str] = []
    for supplied in paths:
        if supplied.is_file():
            files = [supplied]
        elif supplied.is_dir():
            files = [*supplied.rglob('*.jar'), *supplied.rglob('*.zip')]
            files.extend(p for p in supplied.rglob('normal.json')
                         if '/'.join(p.parts[-5:]) == TARGET)
        else:
            raise FileNotFoundError(supplied)
        for file in sorted(set(files)):
            if file.name == 'normal.json' and '/'.join(file.parts[-5:]) == TARGET:
                found.append(str(file))
            elif file.suffix.lower() in ('.jar', '.zip'):
                try:
                    with ZipFile(file) as archive:
                        if TARGET in archive.namelist() and OWN_MARKER not in archive.namelist():
                            found.append(str(file) + ':' + TARGET)
                except BadZipFile as exc:
                    raise RuntimeError('Invalid mod/datapack archive: ' + str(file)) from exc
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('paths', nargs='+', type=Path, help='Mod and datapack folders or jars/zips')
    args = parser.parse_args()
    found = find_conflicts(args.paths)
    if found:
        print('INCOMPATIBLE minecraft:normal preset collision detected:')
        for entry in found:
            print(' - ' + entry)
        print('Tidal Terror 0.0.4 also owns this preset. Supply a combined world preset; '
              'last-wins resource overrides silently discard one generator.')
        return 1
    print('No competing minecraft:normal preset files found in supplied inputs. '
          'This does not certify runtime compatibility.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
