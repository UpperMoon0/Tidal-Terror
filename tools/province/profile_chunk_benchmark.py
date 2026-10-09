"""Summarize JFR samples inside the recorded workload intervals only."""
import argparse
from collections import Counter
from datetime import datetime
import json
from pathlib import Path
import subprocess

JFR='C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot/bin/jfr.exe'
def nanos(text): return int(datetime.fromisoformat(text.replace('Z','+00:00')).timestamp()*1e9)
def main():
    parser=argparse.ArgumentParser();parser.add_argument('directory',type=Path)
    parser.add_argument('--jfr',default=JFR);args=parser.parse_args()
    result=json.loads((args.directory/'results.json').read_text());profiles=[]
    for run in result['runs']:
        recording=args.directory/run['directory']/(run['phase']+'.jfr')
        if not recording.exists():continue
        events=json.loads(subprocess.check_output([args.jfr,'print','--json','--events',
            'jdk.ExecutionSample,jdk.JavaMonitorEnter,jdk.GCPhasePause',str(recording)],text=True))['recording']['events']
        for workload in run['workloads']:
            intervals=[(b['start_epoch_ns'],b['start_epoch_ns']+int(b['ms']*1e6)) for b in workload['batches']]
            samples=Counter();owners=Counter();paths=Counter();monitor=Counter();threads=Counter();gc=[];count=0;truncated=0;examples=[]
            for event in events:
                values=event['values'];at=nanos(values['startTime'])
                if not any(start<=at<=end for start,end in intervals):continue
                frames=(values.get('stackTrace') or {}).get('frames',[])
                methods=[]
                for frame in frames:
                    method=frame.get('method') or {};kind=method.get('type') or {}
                    methods.append((kind.get('name','').replace('/','.')+'.'+method.get('name','')))
                if event['type']=='jdk.ExecutionSample':
                    count+=1
                    truncated+=bool((values.get('stackTrace') or {}).get('truncated'))
                    thread=values.get('sampledThread') or {};threads[thread.get('javaName','unknown')]+=1
                    if methods:samples[methods[0]]+=1
                    for marker in ('NoiseBasedChunkGenerator.getBaseHeight','NoiseBasedChunkGenerator.iterateNoiseColumn',
                                   'NoiseChunk.<init>','ReefTerrain.lambda$originalFloor$0'):
                        if any(m.endswith(marker) for m in methods):paths[marker]+=1
                    if len(examples)<2 and any(m.endswith('ReefTerrain.lambda$originalFloor$0') for m in methods):
                        examples.append(methods)
                    for prefix in ('com.nhat.tidal_terror.','com.nstut.endless.'):
                        match=next((m for m in methods if m.startswith(prefix)),None)
                        if match:owners[match]+=1
                elif event['type']=='jdk.JavaMonitorEnter':
                    monitor[(values.get('monitorClass') or {}).get('name','unknown')]+=1
                else:gc.append(values.get('duration'))
            profiles.append({'variant':run['variant'],'replicate':run['replicate'],'phase':run['phase'],
                'workload':workload['label'],'execution_samples':count,'top_frames':samples.most_common(12),
                'mod_frames':owners.most_common(15),'sampled_threads':threads.most_common(),
                'inclusive_path_samples':dict(paths),'truncated_samples':truncated,'height_query_stack_examples':examples,
                'contended_monitors':monitor.most_common(10),'gc_pauses':gc})
    (args.directory/'profiles.json').write_text(json.dumps(profiles,indent=2))
    print(json.dumps(profiles,indent=2))
if __name__=='__main__':main()
