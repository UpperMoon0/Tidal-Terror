"""Serial vanilla/modded fresh-generation and cold-load RCON benchmark.

forceload synchronously obtains FULL chunks in 1.20.1; then an all-four
entity-ready guard verifies the stronger completion boundary.
Worlds are disposable children of a fresh output directory. Never deletes saves.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import socket
import statistics
import struct
import subprocess
import time

ROOT=Path(__file__).resolve().parents[2]
JAVA=Path('C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot/bin/java.exe')
WORKLOADS={'outside':(0,2048),'cathedral':(-55689,43535),'inner_wastes':(-53755,43535),'blend_edge':(-52689,43535)}

class Rcon:
    def __init__(self,port):
        self.sock=socket.create_connection(('127.0.0.1',port),timeout=600)
        self.sock.setsockopt(socket.IPPROTO_TCP,socket.TCP_NODELAY,1)
        self.ident=0
        self.command('chunk-benchmark-local',3)
    def read(self,n):
        result=b''
        while len(result)<n:
            data=self.sock.recv(n-len(result))
            if not data: raise RuntimeError('RCON disconnected')
            result+=data
        return result
    def command(self,text,kind=2):
        self.ident+=1
        body=struct.pack('<ii',self.ident,kind)+text.encode()+b'\0\0'
        self.sock.sendall(struct.pack('<i',len(body))+body)
        while True:
            size=struct.unpack('<i',self.read(4))[0]
            packet=self.read(size)
            ident,response_type=struct.unpack('<ii',packet[:8])
            if ident==-1: raise RuntimeError('RCON authentication rejected')
            if ident==self.ident and (kind!=3 or response_type==2):
                return packet[8:-2].decode(errors='replace')
    def close(self): self.sock.close()

def receipt(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def prepare(directory,variant,server_port,rcon_port):
    directory.mkdir(parents=True,exist_ok=False)
    (directory/'eula.txt').write_text('eula=true\n')
    props={'level-name':'world','level-seed':'0','level-type':'tidalterror:reef_province_deep' if variant=='deep' else 'minecraft:normal',
           'online-mode':'false','server-ip':'127.0.0.1','server-port':server_port,'enable-rcon':'true','rcon.port':rcon_port,
           'rcon.password':'chunk-benchmark-local','broadcast-rcon-to-ops':'false','max-tick-time':'0',
           'view-distance':'4','simulation-distance':'5','generate-structures':'true','difficulty':'easy','spawn-protection':'0'}
    (directory/'server.properties').write_text(''.join(f'{k}={v}\n' for k,v in props.items()))
    (directory/'config').mkdir()
    (directory/'config/endless.json').write_text('{"buildHeight":{"minBuildHeight":-1024,"maxBuildHeight":1024}}')

def start(directory,variant,phase,spec,vanilla):
    env=os.environ.copy()
    base=['-Xms2G','-Xmx2G','-XX:ActiveProcessorCount=4',
          f'-XX:StartFlightRecording=name=chunks,settings=profile,dumponexit=true,filename={phase}.jfr']
    if variant=='vanilla': command=[str(JAVA),*base,'-jar',str(vanilla),'nogui']
    else:
        args=[a for a in spec['args'] if not a.startswith(('-Xms','-Xmx','-XX:ActiveProcessorCount','-XX:StartFlightRecording'))]
        command=[str(JAVA),*base,*args]
        env.update(spec['environment'])
    logpath=directory/f'{phase}.log'
    log=logpath.open('w')
    process=subprocess.Popen(command,cwd=directory,env=env,stdout=log,stderr=subprocess.STDOUT,
                             creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
    begin=time.perf_counter()
    while time.perf_counter()-begin<600:
        if process.poll() is not None: raise RuntimeError(f'Server exited: {logpath}')
        content=logpath.read_text(errors='replace')
        if 'Done (' in content and 'RCON running on' in content: return process,log,time.perf_counter()-begin
        time.sleep(.5)
    raise RuntimeError(f'Server startup timed out: {logpath}')

def measure(rcon,label,x,z):
    x=(x//16)*16; z=(z//16)*16
    samples=[]
    for dx,dz in ((0,0),(32,0),(0,32),(32,32)):
        command=f'forceload add {x+dx} {z+dz} {x+dx+31} {z+dz+31}'
        epoch=time.time_ns();begin=time.perf_counter_ns()
        response=rcon.command(command)
        ack_ms=(time.perf_counter_ns()-begin)/1e6
        if 'Marked' not in response: raise RuntimeError(f'Unexpected completion: {response}')
        condition='execute '+ ' '.join(f'if loaded {x+dx+cx} 64 {z+dz+cz}' for cx,cz in ((0,0),(16,0),(0,16),(16,16)))
        deadline=time.monotonic()+120
        while True:
            check=rcon.command(condition)
            if 'Test passed' in check: break
            if time.monotonic()>deadline: raise RuntimeError(f'Entity-ready chunks missing: {check}')
            time.sleep(.01)
        elapsed=(time.perf_counter_ns()-begin)/1e6
        samples.append({'command':command,'chunks':4,'ms':elapsed,'full_ack_ms':ack_ms,'start_epoch_ns':epoch,'response':response})
    total=sum(s['ms'] for s in samples)
    return {'label':label,'chunks':16,'total_ms':total,'full_ack_total_ms':sum(s['full_ack_ms'] for s in samples),
            'ms_per_chunk':total/16,'chunks_per_second':16000/total,'batches':samples}

def stop(rcon,process,log):
    try:
        rcon.command('forceload remove all')
        rcon.command('save-all flush')
        rcon.command('stop')
    except (OSError,RuntimeError):
        if process.poll() is None: raise
    finally: rcon.close()
    process.wait(timeout=180);log.close()
    if process.returncode: raise RuntimeError(f'Server shutdown exit {process.returncode}')

def main():
    global JAVA
    parser=argparse.ArgumentParser()
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--launch',type=Path,default=ROOT/'build/chunk-bench-launch.json')
    parser.add_argument('--vanilla',type=Path,default=Path('D:/DevCaches/.gradle/caches/neoformruntime/artifacts/minecraft_1.20.1_server.jar'))
    parser.add_argument('--java',type=Path,default=JAVA)
    args=parser.parse_args()
    JAVA=args.java.resolve()
    output=args.output.resolve();output.mkdir(parents=True,exist_ok=False)
    spec=json.loads(args.launch.read_text());vanilla=args.vanilla.resolve()
    result={'method':'serial RCON FULL acknowledgement plus all-four entity-ready guard, 4 batches of 4 chunks per workload',
            'minecraft':'1.20.1','java':str(JAVA),'heap_gib':2,'active_processors':4,'seed':0,
            'logical_range':[-1024,1024],'structures':True,'platform':platform.platform(),
            'vanilla_sha256':receipt(vanilla),'forge_launch_sha256':receipt(args.launch),
            'tidal_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
            'endless_pin':json.loads((ROOT/'tools/endless-source.json').read_text()),'runs':[]}
    # Reverse order in the second replicate. Each case uses an independent JVM
    # for generation, then another JVM with only its own persisted chunks.
    for index,variant in enumerate(('vanilla','default','deep','deep','default','vanilla')):
        directory=output/f'{index+1}-{variant}';port=25641+index;rcon_port=25741+index
        prepare(directory,variant,port,rcon_port)
        for phase in ('fresh','cold'):
            process,log,ready=start(directory,variant,phase,spec,vanilla)
            rcon=Rcon(rcon_port)
            try:
                rcon.command('gamerule doMobSpawning false')
                rcon.command('gamerule doDaylightCycle false')
                # Different warmup area from every measured footprint.
                warmup=measure(rcon,'warmup',768,2048)
                record={'variant':variant,'replicate':1 if index<3 else 2,'phase':phase,
                        'jvm_to_ready_seconds':ready,'directory':directory.name,'warmup':warmup,'workloads':[]}
                for label,(x,z) in WORKLOADS.items():
                    value=measure(rcon,label,x,z);record['workloads'].append(value)
                    print(json.dumps({'variant':variant,'replicate':record['replicate'],'phase':phase,
                                      'workload':label,'ms':round(value['total_ms'],2)}),flush=True)
                result['runs'].append(record)
                (output/'results.json').write_text(json.dumps(result,indent=2))
            finally: stop(rcon,process,log)
    summary={}
    for variant in ('vanilla','default','deep'):
        summary[variant]={}
        for phase in ('fresh','cold'):
            summary[variant][phase]={label:statistics.mean(w['total_ms'] for r in result['runs']
                if r['variant']==variant and r['phase']==phase for w in r['workloads'] if w['label']==label) for label in WORKLOADS}
    result['summary_mean_batch_totals_ms']=summary
    (output/'results.json').write_text(json.dumps(result,indent=2))
    print('CHUNK_BENCH_COMPLETE '+json.dumps(summary),flush=True)

if __name__=='__main__': main()
