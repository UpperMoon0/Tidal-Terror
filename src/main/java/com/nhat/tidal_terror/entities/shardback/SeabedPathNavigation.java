package com.nhat.tidal_terror.entities.shardback;

import com.nhat.tidal_terror.entities.ReefNavigation;
import net.minecraft.world.phys.Vec3;
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

    @Override public boolean canCutCorner(BlockPathTypes type) {
        // Native lookahead also has a geometric fallback after the raycast.
        // A seabed walker must visit its supported waypoints instead.
        return !mob.isInWater() && super.canCutCorner(type);
    }

    @Override protected boolean canMoveDirectly(Vec3 from, Vec3 to) {
        if (!mob.isInWater()) return super.canMoveDirectly(from, to);
        // Native amphibious shortcuts raycast through water but do not check
        // the seabed or the crab's wide body between waypoints.
        Vec3 feet = new Vec3(from.x, mob.getY(), from.z);
        int steps = Math.max(1, (int)Math.ceil(feet.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++)
            if (!ReefNavigation.clear(mob, feet.lerp(to, i / (double)steps), true)) return false;
        return super.canMoveDirectly(from, to);
    }

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
                            level.getBlockState(below).isFaceSturdy(SeabedPathNavigation.this.level, below, Direction.UP)
                            && (!mob.isInWater() || ReefNavigation.clear(mob,
                                new Vec3(candidate.x + offset, candidate.y, candidate.z + offset), true)))
                        neighbors[supported++] = candidate;
                }
                return supported;
            }
        };
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, maximumVisitedNodes);
    }
}
