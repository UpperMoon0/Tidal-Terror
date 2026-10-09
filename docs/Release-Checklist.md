# Release checklist

CurseForge project: **1726637**, [Tidal Terror](https://www.curseforge.com/minecraft/mc-mods/tidal-terror). Its description is maintained in `CURSEFORGE.md`. TerraBlender is no longer a dependency of any target; Endless is optional for Forge 1.20.1.

## Version-driven publication

The version-driven workflow publishes all five loader/version targets. Merging a version change to `main` starts publication automatically; it is not a merge-only check. Before releasing 0.0.4:

1. Confirm `mod_version=0.0.4` in `gradle.properties`, nonempty `changelogs/v0.0.4.txt` notes, and no `v0.0.4` tag reserved by another commit. Update the current README, CurseForge description and changelog while preserving historical benchmark data and reports.
2. Publish Endless 0.9.3 first and verify that its Forge 1.20.1 jar is publicly downloadable. Tidal's optional runtime range is `[0.9.3,0.10)`; older Endless releases cannot supply the supported deep adapter. Deep users also need Architectury API 9.2.14+ on the server and clients. Ordinary Tidal Forge worlds need neither companion.
3. Check `tools/endless-source.json`: CI builds that exact reviewed Endless commit and shares its unbundled Forge 0.9.3 jar with the validation and release builds. A local Forge build also requires this jar in `.dependencies/endless/`, even when Endless is absent at runtime. The pin need not change for an Endless docs-only merge whose runtime code is unchanged.
4. Confirm the latest exact-head Validate run: Forge native gameplay/model/terrain suites, normal worlds with and without Endless and reload after removing it, deep fresh/cold generation, four port native/client suites, tooling regressions, and clean five-target packaging. Passing these checks does not resolve the documented deep cave/structure, preset-collision or performance limitations.
5. Commit and push to `main`. The Release workflow detects the changed or untagged version, rejects missing notes or a tag belonging to another commit, reruns Validate, then clean-builds and verifies all five production jars. Archive metadata/resources, required companions, optional Endless, and fixture exclusion must pass.
6. The complete immutable bundle (five jars, SHA256SUMS and source/version manifest) is retained as an artifact and reserved in a draft GitHub release. `CURSEFORGE_API_TOKEN` publishes the original Forge jar first, then all four ports with the matching loader, Minecraft and Java metadata. Forge declares optional `nstut-endless`; Fabric declares required Architectury/Fabric API; NeoForge declares required Architectury. Fabric 1.20.1 bundles Reach Entity Attributes 2.4.0.
7. Every CurseForge upload must produce a receipt matching its artifact checksum and source commit. Only after all five receipts are verified does the GitHub release become public. Check that the CurseForge project description matches `CURSEFORGE.md`; this workflow uploads file notes and does not update the project description.

The five artifacts are Forge 1.20.1 (Java 17), Fabric 1.20.1 (Java 17), Fabric 1.21.1 (Java 21), NeoForge 1.21.1 (Java 21) and NeoForge 26.1.2 (Java 25). Loader and companion minimums are listed in the README; Fabric 1.21.1 declares Loader 0.16.14 and is tested with 0.17.2.

Normal PRs and non-main pushes use Validate without publishing. Manual Release runs must target `main`. Completed uploads with a recorded receipt are reused; differing bytes or reused version tags fail rather than overwrite a release. Archive ordering and timestamps are stable across rebuilds.

## Recovery and verification

Use **Re-run failed jobs** for a failed pipeline. A manual retry of the exact tagged commit is also allowed. If CurseForge accepted the upload but execution stopped before its receipt was recorded, inspect the author's Files page and reconcile that file before rerunning publication: the upload API cannot guarantee exactly-once delivery. Automatic upload retries are disabled for that reason. A newer source commit needs a new version after a version has been reserved.

Check the Actions run, the GitHub release's jar/checksum/manifest, and the CurseForge Files page separately. An accepted upload can remain pending moderation; green Actions does not mean a publicly downloadable CurseForge file. Client playtesting and shader screenshot review remain separate from automated native checks.

The normal jar excludes worlds, logs, development galleries, shader packs, and test/preview classes. Endless and optional shaders are separate downloads. The mod is MIT licensed; the preserved third-party Forge MDK notice is `LICENSE-Forge-MDK.txt`.

## Verified initial release

On 2026-10-04, [Release run 37217889677](https://github.com/UpperMoon0/Tidal-Terror/actions/runs/37217889677) passed all 58 required native GameTests, four model checks, the native terrain audit, 13 release tooling tests, clean packaging, CurseForge upload and GitHub release publication. Tag `v0.0.1` points to `cc8e05ca7febed3c86be8d941bad7bcb7920cec0`. The jar SHA-256 is `20fb4e00093e6f5d384cbecb161e76fe765f616d0816dfa88577f423ada90da1`; the GitHub release retains the manifest, checksum and upload receipt. CurseForge file `9060762` was accepted and showed Under Review on the author page; the initial project also awaited moderator approval.
