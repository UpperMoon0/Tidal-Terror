"""Capture an explicitly selected Forge dev server for the serial benchmark.

Requires psutil. Exports only MOD_CLASSES, never the complete environment.
Stop the selected server normally before starting benchmark_chunks.py.
"""
import argparse
import json
from pathlib import Path

import psutil


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--pid', type=int, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    process = psutil.Process(args.pid)
    command = process.cmdline()
    if not command or Path(command[0]).name.lower() != 'java.exe':
        raise RuntimeError('Select the actual Java server process, not Gradle')
    if 'forgeserveruserdev' not in command:
        raise RuntimeError('Selected process is not a Forge dev server')
    environment = process.environ()
    if not environment.get('MOD_CLASSES'):
        raise RuntimeError('Server is missing its development mod paths')
    spec = {'executable': command[0], 'args': command[1:],
            'environment': {'MOD_CLASSES': environment['MOD_CLASSES']}}
    with args.output.open('x') as output:
        json.dump(spec, output, indent=2)
    print(f'Captured Forge server PID {args.pid}: {args.output}')


if __name__ == '__main__':
    main()
