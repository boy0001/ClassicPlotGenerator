package com.empcraft.classic;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldname, String id) {
        return new ClassicGen(worldname);
    }
	
}
