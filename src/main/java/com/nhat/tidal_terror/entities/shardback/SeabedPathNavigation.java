package com.nhat.tidal_terror.entities.shardback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;

/** Native amphibious paths constrained to supported feet, for a walking crab. */
final class SeabedPathNavigation extends AmphibiousPathNavigation {
    SeabedPathNavigation(Mob mob, Level level) { super(mob, level); }

    @Override protected PathFinder createPathFinder(int maximumVisitedNodes) {
        nodeEvaluator = new AmphibiousNodeEvaluator(false) {
            @Override public void prepare(PathNavigationRegion region, Mob creature) {
                super.prepare(region, creature);
                // Vanilla amphibious evaluation penalizes water beside sediment,
                // preferring unsupported swimming nodes one block above it.
                creature.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0);
            }
            @Override public int getNeighbors(Node[] neighbors, Node current) {
                int count = super.getNeighbors(neighbors, current), supported = 0;
                double offset = (int)(mob.getBbWidth() + 1) * .5;
                for (int i = 0; i < count; i++) {
                    Node candidate = neighbors[i];
                    BlockPos below = BlockPos.containing(candidate.x + offset, candidate.y - 1, candidate.z + offset);
                    if (SeabedPathNavigation.this.level.hasChunkAt(below) &&
                            level.getBlockState(below).isFaceSturdy(SeabedPathNavigation.this.level, below, Direction.UP))
                        neighbors[supported++] = candidate;
                }
                return supported;
            }
        };
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, maximumVisitedNodes);
    }
}
