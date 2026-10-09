package com.nhat.tidal_terror.worldgen;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.BooleanSupplier;

/** Nearest-center search over bounded deterministic candidates; never asks for chunks. */
public final class ProvinceLocatorSearch {
    public static final int RANGE=65536;
    public static ReefProvinceLayout.Center find(long seed,int x,int z,
            Predicate<ReefProvinceLayout.Center> accepted,BooleanSupplier cancelled) {
        return find(seed,x,z,2,accepted,cancelled);
    }
    public static ReefProvinceLayout.Center find(long seed,int x,int z,int version,
            Predicate<ReefProvinceLayout.Center> accepted,BooleanSupplier cancelled) {
        int spacing=ReefProvinceLayout.spacing(version);
        var candidates=new ArrayList<ReefProvinceLayout.Center>();
        for(int cx=Math.floorDiv(x-RANGE,spacing);cx<=Math.floorDiv(x+RANGE,spacing);cx++)
            for(int cz=Math.floorDiv(z-RANGE,spacing);cz<=Math.floorDiv(z+RANGE,spacing);cz++) {
                var c=ReefProvinceLayout.center(seed,cx,cz,version);
                if(distanceSquared(c,x,z)<=(double)RANGE*RANGE)candidates.add(c);
            }
        candidates.sort(Comparator.comparingDouble(c->distanceSquared(c,x,z)));
        for(var c:candidates) {
            if(cancelled.getAsBoolean())return null;
            if(accepted.test(c))return c;
        }
        return null;
    }
    private static double distanceSquared(ReefProvinceLayout.Center c,int x,int z) {
        double dx=(double)c.x()-x,dz=(double)c.z()-z;return dx*dx+dz*dz;
    }
    private ProvinceLocatorSearch(){}
}
