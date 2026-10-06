# Release checklist

CurseForge project: **1726637**, [Tidal Terror](https://www.curseforge.com/minecraft/mc-mods/tidal-terror). Its description is maintained in `CURSEFORGE.md`. TerraBlender is a required dependency of every uploaded file.

## Version-driven publication

The workflows follow Endless's structure, adapted to Forge 1.20.1 and Java 17. To release:

1. Change `mod_version` in `gradle.properties` and add nonempty `changelogs/vVERSION.txt` notes.
2. Commit and push to `main`. The Release workflow compares the version with the previous revision. An untagged current version also runs, allowing the initial release and workflow repairs.
3. Preflight rejects missing notes and tags belonging to another commit. Validation runs 71 native GameTests, the terrain audit, five model checks, release tooling tests, and a clean packaging check.
4. A separate clean build produces the ordinary jar, verifies expanded metadata, dependencies, the icon and all resources, and rejects test/preview classes. The exact jar, SHA256SUMS, and source manifest are retained as an artifact and in a draft GitHub release.
5. The same jar uploads to CurseForge project 1726637 using `CURSEFORGE_API_TOKEN`, marked Forge 1.20.1, Java 17, client/server, release, with required TerraBlender. A receipt containing its file ID and checksum is saved before the GitHub release becomes public.

Normal PRs and non-main pushes use Validate without publishing. Manual Release runs must target `main`. Completed uploads with a recorded receipt are reused; differing bytes or reused version tags fail rather than overwrite a release. Archive ordering and timestamps are stable across rebuilds.

## Recovery and verification

Use **Re-run failed jobs** for a failed pipeline. A manual retry of the exact tagged commit is also allowed. If CurseForge accepted the upload but execution stopped before its receipt was recorded, inspect the author's Files page and reconcile that file before rerunning publication: the upload API cannot guarantee exactly-once delivery. Automatic upload retries are disabled for that reason. A newer source commit needs a new version after a version has been reserved.

Check the Actions run, the GitHub release's jar/checksum/manifest, and the CurseForge Files page separately. An accepted upload can remain pending moderation; green Actions does not mean a publicly downloadable CurseForge file. Client playtesting and shader screenshot review remain separate from automated native checks.

The normal jar excludes worlds, logs, development galleries, shader packs, and test/preview classes. TerraBlender and optional shaders are separate downloads. The mod is MIT licensed; the preserved third-party Forge MDK notice is `LICENSE-Forge-MDK.txt`.

## Verified initial release

On 2026-10-04, [Release run 37217889677](https://github.com/UpperMoon0/Tidal-Terror/actions/runs/37217889677) passed all 58 required native GameTests, four model checks, the native terrain audit, 13 release tooling tests, clean packaging, CurseForge upload and GitHub release publication. Tag `v0.0.1` points to `cc8e05ca7febed3c86be8d941bad7bcb7920cec0`. The jar SHA-256 is `20fb4e00093e6f5d384cbecb161e76fe765f616d0816dfa88577f423ada90da1`; the GitHub release retains the manifest, checksum and upload receipt. CurseForge file `9060762` was accepted and showed Under Review on the author page; the initial project also awaited moderator approval.
