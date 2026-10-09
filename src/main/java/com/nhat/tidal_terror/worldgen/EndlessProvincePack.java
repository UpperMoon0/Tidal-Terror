package com.nhat.tidal_terror.worldgen;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.PathPackResources;

/** World types are optional data; the reef features and biome registries are shared. */
public final class EndlessProvincePack {
    public static final String ID = "tidalterror:endless_province";
    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !ModList.get().isLoaded("endless")) return;
        var path = ModList.get().getModFileById("tidalterror").getFile()
                .findResource("packs", "endless_province");
        event.addRepositorySource(consumer -> {
            var pack = Pack.readMetaAndCreate(ID, Component.literal("Tidal Terror Endless world types"),
                    true, name -> new PathPackResources(name, true, path),
                    PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) consumer.accept(pack);
        });
    }
    private EndlessProvincePack() {}
}
