"""Install the checksum-pinned Forge shader stack verified in Celestial Nail."""
from pathlib import Path
import hashlib
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ARTIFACTS = [
    (".dependencies/shaders/oculus-mc1.20.1-1.8.0.jar",
     "https://cdn.modrinth.com/data/GchcoXML/versions/iQ1SwGc3/oculus-mc1.20.1-1.8.0.jar",
     "1bb4ac77400d6684347988ed298a692c2cb15cf7923693607eb8739b171a20fef7412259e9e157111d9ce21779badab386029956f7d2283a9e611722a373e9d5"),
    (".dependencies/shaders/embeddium-0.3.31+mc1.20.1.jar",
     "https://cdn.modrinth.com/data/sk9rgfiA/versions/UTbfe5d1/embeddium-0.3.31%2Bmc1.20.1.jar",
     "ffbf2da4685260a4d5c14c621708bd20722563f084f042d3dfb0a7b87f048e39299648c854a93939129da0d23a15a91ec628560d601e76074b08e275f6e132e9"),
    ("run/shaderpacks/ComplementaryReimagined_r5.9.3.zip",
     "https://cdn.modrinth.com/data/HVnmMxH1/versions/Bqen1mJX/ComplementaryReimagined_r5.9.3.zip",
     "45304b1d7862afdb2177b7e6226fc9c6737911d17a4ad46cc9c519ca4deb935f05072cd337cd8b19a15bb8062b57b54eceec7bdee4cd572d94858e2946d37d85"),
]

for relative, url, expected in ARTIFACTS:
    destination = ROOT / relative
    data = destination.read_bytes() if destination.exists() else urllib.request.urlopen(url, timeout=60).read()
    if hashlib.sha512(data).hexdigest() != expected:
        raise RuntimeError(f"Checksum mismatch: {relative}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    if not destination.exists():
        destination.write_bytes(data)
    print(f"Verified {destination.name}")

config = ROOT / "run/config/oculus.properties"
config.parent.mkdir(parents=True, exist_ok=True)
if not config.exists():
    config.write_text("shaderPack=ComplementaryReimagined_r5.9.3.zip\nenableShaders=true\n", encoding="utf-8")
print("ForgeGradle loads Oculus's bundled jcpp directly; no extra library or Loom mixin workaround is needed.")
