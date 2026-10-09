"""Compare old/new saved terrain under one frozen runtime; never changes source saves."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import benchmark_chunks as bench


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--old',type=Path,required=True,help='Server directory containing the old world')
    parser.add_argument('--new',type=Path,required=True,help='Server directory containing the new world')
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--launch',type=Path,required=True)
    parser.add_argument('--java',type=Path,default=bench.JAVA)
    args=parser.parse_args();bench.JAVA=args.java.resolve()
    output=args.output.resolve();output.mkdir(parents=True,exist_ok=False)
    spec=json.loads(args.launch.read_text())
    result={'method':'serial old/new/new/old saved-world clones; same runtime and entity-ready boundary',
            'old_source':str(args.old.resolve()),'new_source':str(args.new.resolve()),
            'tidal_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=bench.ROOT,text=True).strip(),
            'endless_pin':json.loads((bench.ROOT/'tools/endless-source.json').read_text()),'runs':[]}
    for index,label in enumerate(('old','new','new','old')):
        directory=output/f'{index+1}-{label}';port=25661+index;rcon_port=25761+index
        bench.prepare(directory,'deep',port,rcon_port)
        source=args.old if label=='old' else args.new
        shutil.copytree(source.resolve()/'world',directory/'world')
        process,log,ready=bench.start(directory,'deep','cold',spec,None)
        rcon=bench.Rcon(rcon_port)
        try:
            rcon.command('gamerule doMobSpawning false');rcon.command('gamerule doDaylightCycle false')
            warmup=bench.measure(rcon,'warmup',768,2048)
            record={'saved_terrain':label,'replicate':1 if index<2 else 2,'warmup':warmup,
                    'jvm_to_ready_seconds':ready,'directory':directory.name,'workloads':[]}
            for name,(x,z) in bench.WORKLOADS.items():
                value=bench.measure(rcon,name,x,z);record['workloads'].append(value)
                print(json.dumps({'saved_terrain':label,'workload':name,'ms':value['total_ms']}),flush=True)
            result['runs'].append(record)
            (output/'results.json').write_text(json.dumps(result,indent=2))
        finally:bench.stop(rcon,process,log)
    print('RELOAD_PAIR_COMPLETE',flush=True)


if __name__=='__main__':main()
