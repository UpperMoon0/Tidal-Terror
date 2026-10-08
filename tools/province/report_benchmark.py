"""Validate the four independent native tours and summarize per-run metrics.

Usage: python tools/province/report_benchmark.py RESULT_ROOT --output summary.json
The input directories must be baseline-e, optimized-e, optimized-f, baseline-f.
"""
import argparse
import json
import struct
from pathlib import Path
from statistics import mean


def report(root):
    runs = {}
    fixtures = set()
    manifests = []
    dimensions = set()
    for label in ("baseline-e", "optimized-e", "optimized-f", "baseline-f"):
        directory = root / label
        metrics = json.loads((directory / "performance-metrics.json").read_text())
        receipt = json.loads((directory / "receipt.json").read_text())
        manifest = json.loads((directory / "manifest.json").read_text())
        variant = label.split("-")[0]
        assert receipt["variant"] == variant
        assert len(manifest) == len(metrics["moves"]) == 5
        for capture in manifest:
            with (directory / capture["file"]).open("rb") as stream:
                header = stream.read(24)
            assert header[:8] == b"\x89PNG\r\n\x1a\n"
            dimensions.add(struct.unpack(">II", header[16:24]))
        assert [move["pose"] for move in metrics["moves"]] == [
            "outer-wastes", "inner-wastes", "rim-passage", "cathedral", "cathedral-surface"
        ]
        assert (metrics["logicalMin"], metrics["logicalMax"]) == (-1024, 1024)
        assert metrics["elapsedMs"] > 120000
        for key in ("frames", "serverTicks", "serverTickIntervals"):
            assert metrics[key]["count"] > 1000
        if variant == "optimized":
            assert metrics["admissionBuilds"] == 0
        fixtures.add(receipt["fixtureSha256"])
        manifests.append(manifest)
        runs[label] = {"metrics": metrics, "receipt": receipt, "manifest": manifest}
    assert len(fixtures) == 1, "Measurement fixture changed between runs"
    assert len(dimensions) == 1, "Framebuffer size changed between captures"
    assert all(manifest == manifests[0] for manifest in manifests), "Terrain probes changed"
    for variant in ("baseline", "optimized"):
        paired = [run for label, run in runs.items() if label.startswith(variant)]
        for key in ("sourceSha256", "dependencySha256", "buildConfigurationSha256"):
            assert len({run["receipt"][key] for run in paired}) == 1, key

    rows = {}
    for variant in ("baseline", "optimized"):
        metrics = [run["metrics"] for label, run in runs.items() if label.startswith(variant)]
        rows[variant] = {
            # These are means of two per-run statistics, NOT pooled percentiles.
            "frameP95MeanMs": mean(m["frames"]["p95Ms"] for m in metrics),
            "frameP99MeanMs": mean(m["frames"]["p99Ms"] for m in metrics),
            "frameWorstMs": max(m["frames"]["maxMs"] for m in metrics),
            "tickP95MeanMs": mean(m["serverTicks"]["p95Ms"] for m in metrics),
            "tickWorstMs": max(m["serverTicks"]["maxMs"] for m in metrics),
            "heartbeatP95MeanMs": mean(m["serverTickIntervals"]["p95Ms"] for m in metrics),
            "heartbeatWorstMs": max(m["serverTickIntervals"]["maxMs"] for m in metrics),
            "renderCpuCoresMean": mean(m["cpuMs"]["Render thread"] / m["elapsedMs"] for m in metrics),
            "serverCpuCoresMean": mean(m["cpuMs"]["Server thread"] / m["elapsedMs"] for m in metrics),
            "workerCpuCoresMean": mean(sum(v for k, v in m["cpuMs"].items() if k.startswith("Worker-Main")) / m["elapsedMs"] for m in metrics),
        }
    changes = {key: 100 * (rows["optimized"][key] / value - 1) for key, value in rows["baseline"].items()}
    return {"aggregation": "Mean of two per-run p95/p99/CPU values; maximum of per-run maxima. Negative change means lower.",
            "order": list(runs), "framebufferPixels": list(next(iter(dimensions))),
            "summary": rows, "changePercent": changes, "runs": runs}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = report(args.root)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({key: result[key] for key in ("aggregation", "summary", "changePercent")}, indent=2))
