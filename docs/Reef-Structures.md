# Reef structure placement — Minecraft 1.20.1 / Forge 47.2.0

The reef basin excavates the ocean during RAW_GENERATION. Vanilla ruined
portal starts are calculated earlier from noise columns. Vanilla shipwreck
pieces read WORLD_SURFACE_WG / OCEAN_FLOOR_WG during postProcess; these are
generation heightmaps, not the final sediment height. Neither accounts for
the reef's later seabed. Giant coral crowns must not become structure floors.

ReefShipwreckPlacementMixin substitutes coordinate-stable reef seabed height
samples during native postProcess. Vanilla averaging, beached burial, template
rotation, clipping, and loot handling remain in place. Non-reef height samples
use the original heightmap call.

ReefRuinedPortalPlacementMixin moves the template and bounding box to the
lowest sediment column in its footprint before native placement. Its native
netherrack spreading also samples the reef seabed. Where random drip columns
would end in water, only the bottom of the existing netherrack foundation is
extended through water to sediment or intervening solid ground. Portal pieces
whose center lies outside the reef retain vanilla placement.

The height comes from ReefTerrain.anchorFloor, which is independent of chunk
decoration order and does not mistake already placed structures or coral for
the seabed. Template movement is idempotent across intersecting chunks and
saved piece reloads.

Local decompiled vanilla references inspected:
  ShipwreckPieces.ShipwreckPiece.postProcess
  RuinedPortalStructure.findGenerationPoint / findSuitableY
  RuinedPortalPiece.postProcess / spreadNetherrack / getSurfaceY
  TemplateStructurePiece.postProcess / move
  ChunkGenerator.applyBiomeDecoration
Mapped Forge jar signatures were checked before defining injection targets.

Native regression:
  gradlew.bat -PreefTests runServer --offline
ReefStructureAudit uses actual vanilla templates and postProcess, all four
shipwreck rotations inside/outside the reef, portal foundation support,
full-height chunk clipping, and serialized/reloaded pieces. Fixture code is
excluded from the release jar.

This changes future structure generation. It does not automatically move
existing structures or edit a player's already generated chunks. The running
development client uses its launch snapshot and needs a restart for changes.
