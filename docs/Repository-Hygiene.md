# Repository files

Track production sources, game-ready textures/models/data, native test fixtures and checks, build/release tooling, licenses, the public README banner and documented native screenshots. Keep benchmark reports and their JSON measurements together so recorded results remain auditable and reproducible.

Documentation and versioned release notes use Markdown. README.md is the single project README; avoid duplicate pointer files and unused Forge MDK template credits. Preserve third-party legal notices in LICENSE-Forge-MDK.md and the project license in LICENSE.md.

Ignore worlds, downloaded development mods/shaders, build/cache directories, runtime logs, undocumented screenshots and local artwork studies/source images. Optional art-export scripts require local source images; they are not part of build or CI. Art workflow documents and generation prompts are local-only and ignored, alongside source artwork. Build and verification scripts remain tracked. Untracked artwork remains on the developer machine. Deletions remove files from the branch tip after pushing; existing Git history remains intact.

The obsolete 2D Reef Spear icon is removed. Its native model renders held, dropped, framed and in inventory/JEI contexts using the UV atlas.
