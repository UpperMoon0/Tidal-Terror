# Release checklist

The source currently identifies itself as **1.0.0**. `CHANGELOG.md` keeps that version unreleased until a release is published. `CURSEFORGE.md` is the player-facing project description, ready to copy into the project page; creating it does not publish a CurseForge project or upload a file.

## Before publication

- Upload `src/main/resources/tidalterror-icon.png` as the project icon. This 500x500 image is already referenced by `logoFile` in `META-INF/mods.toml`; its generated original, biome screenshot, sandy Crusher model preview and prompt are retained in `art-source/project-icon`.
- Select and upload release screenshots of the actual biome and all four creatures. Development shader screenshots should identify the optional shader stack; local `art` galleries are not committed or hosted automatically.
- Create or confirm the CurseForge project, copy the description from `CURSEFORGE.md`, and mark TerraBlender (Forge) as a required dependency. Choose Minecraft 1.20.1 and Forge for the uploaded file.
- Build the final release source with Java 17 using `./gradlew.bat build`. Upload the ordinary jar from `build/libs`, without enabling test/preview properties. Confirm that metadata contains the intended version, author, URLs, and dependency ranges and no unresolved placeholders.
- Run the relevant native suites and a packaged client/server smoke test for that final source. Existing development test results do not certify a different release artifact.
- Record the release date in `CHANGELOG.md`, tag the published revision, and link the actual release/download page from the README. No download URL, project ID, or release tag should be invented before publication.

The normal jar contains the mod assets and registry data. Worlds, logs, generated development galleries, shader packs, and native test/preview classes are excluded. TerraBlender and optional shaders are separate downloads. The project uses the MIT License in `LICENSE.txt`; the preserved Forge MDK third-party notice is `LICENSE-Forge-MDK.txt`.
