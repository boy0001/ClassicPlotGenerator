////////////////////////////////////////////////////////////////////////////////////////////////////
// PlotSquared - A plot manager and world generator for the Bukkit API                             /
// Copyright (c) 2014 IntellectualSites/IntellectualCrafters                                       /
//                                                                                                 /
// This program is free software; you can redistribute it and/or modify                            /
// it under the terms of the GNU General Public License as published by                            /
// the Free Software Foundation; either version 3 of the License, or                               /
// (at your option) any later version.                                                             /
//                                                                                                 /
// This program is distributed in the hope that it will be useful,                                 /
// but WITHOUT ANY WARRANTY; without even the implied warranty of                                  /
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                   /
// GNU General Public License for more details.                                                    /
//                                                                                                 /
// You should have received a copy of the GNU General Public License                               /
// along with this program; if not, write to the Free Software Foundation,                         /
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA                               /
//                                                                                                 /
// You can contact us via: support@intellectualsites.com                                           /
////////////////////////////////////////////////////////////////////////////////////////////////////

package com.empcraft.classic;

import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.object.PlotBlock;
import com.intellectualcrafters.plot.object.PlotGenerator;
import com.intellectualcrafters.plot.object.PlotManager;
import com.intellectualcrafters.plot.object.PlotWorld;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The default generator is very messy, as we have decided to try externalize all calculations from within the loop. -
 * You will see a lot of slower implementations have a single for loop. - This is perfectly fine to do, it will just
 * mean world generation may take somewhat longer
 *
 * @author Citymonstret
 * @author Empire92
 */
public class ClassicGen extends PlotGenerator {
    /**
     * Set to static to re-use the same managet for all Default World Generators
     */
    private static PlotManager manager = null;
    /**
     * Some generator specific variables (implementation dependent)
     */
    final int plotsize;
    final int pathsize;
    final PlotBlock wall;
    final PlotBlock wallfilling;
    final PlotBlock floor1;
    final PlotBlock floor2;
    final int size;
    final Biome biome;
    final int roadheight;
    final int wallheight;
    final int plotheight;
    final PlotBlock[] plotfloors;
    final PlotBlock[] filling;
    /**
     * result object is returned for each generated chunk, do stuff to it
     */
    short[][] result;
    /**
     * plotworld object
     */
    ClassicPlotWorld plotworld = null;
    /**
     * Faster sudo-random number generator than java.util.random
     */
    private long state;

    /**
     * Initialize variables, and create plotworld object used in calculations
     */
    public ClassicGen(final String world) {
        super(world);

        if (this.plotworld == null) {
            this.plotworld = (ClassicPlotWorld) PlotMain.getWorldSettings(world);
        }
        this.plotsize = this.plotworld.PLOT_WIDTH;

        this.pathsize = this.plotworld.ROAD_WIDTH;

        this.floor1 = this.plotworld.ROAD_BLOCK;
        this.floor2 = this.plotworld.ROAD_STRIPES;

        this.wallfilling = this.plotworld.WALL_FILLING;
        this.size = this.pathsize + this.plotsize;
        this.wall = this.plotworld.WALL_BLOCK;

        this.plotfloors = this.plotworld.TOP_BLOCK;
        this.filling = this.plotworld.MAIN_BLOCK;
        this.wallheight = this.plotworld.WALL_HEIGHT;
        this.roadheight = this.plotworld.ROAD_HEIGHT;
        this.plotheight = this.plotworld.PLOT_HEIGHT;

        this.biome = this.plotworld.PLOT_BIOME;
    }

    /**
     * Return the plot manager for this type of generator, or create one For square plots you may as well use the
     * default plot manager which comes with PlotSquared
     */
    @Override
    public PlotManager getPlotManager() {
        if (manager == null) {
            manager = new ClassicPlotManager();
        }
        return manager;
    }

    /**
     * Allow spawning everywhere
     */
    @Override
    public boolean canSpawn(final World world, final int x, final int z) {
        return true;
    }

    /**
     * Get a new plotworld class For square plots you can use the DefaultPlotWorld class which comes with PlotSquared
     */
    @Override
    public PlotWorld getNewPlotWorld(final String world) {
        this.plotworld = new ClassicPlotWorld(world);
        return this.plotworld;
    }

    public final long nextLong() {
        final long a = this.state;
        this.state = xorShift64(a);
        return a;
    }

    public final long xorShift64(long a) {
        a ^= (a << 21);
        a ^= (a >>> 35);
        a ^= (a << 4);
        return a;
    }

    public final int random(final int n) {
        final long r = ((nextLong() >>> 32) * n) >> 32;
        return (int) r;
    }

    /**
     * Cuboid based plot generation is quick, as it requires no calculations inside the loop - You don't have to use
     * this this method, but you may find it useful.
     */
    public void setCuboidRegion(final int x1, final int x2, final int y1, final int y2, final int z1, final int z2, final PlotBlock block) {
        for (int x = x1; x < x2; x++) {
            for (int z = z1; z < z2; z++) {
                for (int y = y1; y < y2; y++) {
                    setBlock(this.result, x, y, z, block.id);
                }
            }
        }
    }

    private void setCuboidRegion(final int x1, final int x2, final int y1, final int y2, final int z1, final int z2, final PlotBlock[] blocks) {
        if (blocks.length == 1) {
            setCuboidRegion(x1, x2, y1, y2, z1, z2, blocks[0]);
        } else {
            for (int x = x1; x < x2; x++) {
                for (int z = z1; z < z2; z++) {
                    for (int y = y1; y < y2; y++) {
                        final int i = random(blocks.length);
                        setBlock(this.result, x, y, z, blocks[i].id);
                    }
                }
            }
        }
    }

    /**
     * Standard setblock method for world generation
     */
    private void setBlock(final short[][] result, final int x, final int y, final int z, final short blkid) {
        if (result[y >> 4] == null) {
            result[y >> 4] = new short[4096];
        }
        result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
    }

    /**
     * Return the block populator
     */
    @Override
    public List<BlockPopulator> getDefaultPopulators(final World world) {
        // disabling spawning for this world
        if (!this.plotworld.MOB_SPAWNING) {
            world.setSpawnFlags(false, false);
            world.setAmbientSpawnLimit(0);
            world.setAnimalSpawnLimit(0);
            world.setMonsterSpawnLimit(0);
            world.setWaterAnimalSpawnLimit(0);
        }
        // You can have as many populators as you would like, e.g. tree
        // populator, ore populator
        return Arrays.asList((BlockPopulator) new ClassicPop(this.plotworld));
    }

    /**
     * Return the default spawn location for this world
     */
    @Override
    public Location getFixedSpawnLocation(final World world, final Random random) {
        return new Location(world, 0, this.plotworld.ROAD_HEIGHT + 2, 0);
    }

    /**
     * This part is a fucking mess. - Refer to a proper tutorial if you would like to learn how to make a world
     * generator
     */
    @Override
    public short[][] generateExtBlockSections(final World world, final Random random, int cx, int cz, final BiomeGrid biomes) {

        final int maxY = world.getMaxHeight();
        this.result = new short[maxY / 16][];

        final int prime = 31;
        int h = 1;
        h = (prime * h) + cx;
        h = (prime * h) + cz;
        this.state = h;

        double pathWidthLower;
        if ((this.pathsize % 2) == 0) {
            pathWidthLower = Math.floor(this.pathsize / 2) - 1;
        } else {
            pathWidthLower = Math.floor(this.pathsize / 2);
        }
        cx = (cx % this.size) + (8 * this.size);
        cz = (cz % this.size) + (8 * this.size);
        final int absX = (int) ((((cx * 16) + 16) - pathWidthLower - 1) + (8 * this.size));
        final int absZ = (int) ((((cz * 16) + 16) - pathWidthLower - 1) + (8 * this.size));
        final int plotMinX = (((absX) % this.size));
        final int plotMinZ = (((absZ) % this.size));
        int roadStartX = (plotMinX + this.pathsize);
        int roadStartZ = (plotMinZ + this.pathsize);
        if (roadStartX >= this.size) {
            roadStartX -= this.size;
        }
        if (roadStartZ >= this.size) {
            roadStartZ -= this.size;
        }

        // BOTTOM (1/1 cuboids)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                setBlock(this.result, x, 0, z, (short) 7);
                biomes.setBiome(x, z, this.biome);
            }
        }
        // ROAD (0/24) The following is an inefficient placeholder as it is too
        // much work to finish it
        if ((this.pathsize > 16) && ((plotMinX > roadStartX) || (plotMinZ > roadStartZ)) && !((roadStartX < 16) && (roadStartZ < 16)) && (((roadStartX > 16) && (roadStartZ > 16)) || ((plotMinX > roadStartX) && (plotMinZ > roadStartZ)))) {
            setCuboidRegion(0, 16, 1, this.roadheight + 1, 0, 16, this.floor1);
            return this.result;
        }
        if (((plotMinZ + 1) <= 16) || ((roadStartZ <= 16) && (roadStartZ > 0))) {
            final int start = Math.max((16 - plotMinZ - this.pathsize) + 1, (16 - roadStartZ) + 1);
            int end = Math.min(16 - plotMinZ - 1, (16 - roadStartZ) + this.pathsize);
            if ((start >= 0) && (start <= 16) && (end < 0)) {
                end = 16;
            }
            setCuboidRegion(0, 16, 1, this.roadheight + 1, Math.max(start, 0), Math.min(16, end), this.floor1);
        }
        if (((plotMinX + 1) <= 16) || ((roadStartX <= 16) && (roadStartX > 0))) {
            final int start = Math.max((16 - plotMinX - this.pathsize) + 1, (16 - roadStartX) + 1);
            int end = Math.min(16 - plotMinX - 1, (16 - roadStartX) + this.pathsize);
            if ((start >= 0) && (start <= 16) && (end < 0)) {
                end = 16;
            }
            setCuboidRegion(Math.max(start, 0), Math.min(16, end), 1, this.roadheight + 1, 0, 16, this.floor1);
        }

        // ROAD STRIPES
        if ((this.pathsize > 4) && this.plotworld.ROAD_STRIPES_ENABLED) {
            if ((plotMinZ + 2) <= 16) {
                final int value = (plotMinZ + 2);
                int start, end;
                if ((plotMinX + 2) <= 16) {
                    start = 16 - plotMinX - 1;
                } else {
                    start = 16;
                }
                if ((roadStartX - 1) <= 16) {
                    end = (16 - roadStartX) + 1;
                } else {
                    end = 0;
                }
                if (!(((plotMinX + 2) <= 16) || ((roadStartX - 1) <= 16))) {
                    start = 0;
                }
                setCuboidRegion(0, end, this.wallheight, this.wallheight + 1, 16 - value, (16 - value) + 1, this.floor2); //
                setCuboidRegion(start, 16, this.wallheight, this.wallheight + 1, 16 - value, (16 - value) + 1, this.floor2); //
            }
            if ((plotMinX + 2) <= 16) {
                final int value = (plotMinX + 2);
                int start, end;
                if ((plotMinZ + 2) <= 16) {
                    start = 16 - plotMinZ - 1;
                } else {
                    start = 16;
                }
                if ((roadStartZ - 1) <= 16) {
                    end = (16 - roadStartZ) + 1;
                } else {
                    end = 0;
                }
                if (!(((plotMinZ + 2) <= 16) || ((roadStartZ - 1) <= 16))) {
                    start = 0;
                }
                setCuboidRegion(16 - value, (16 - value) + 1, this.wallheight, this.wallheight + 1, 0, end, this.floor2); //
                setCuboidRegion(16 - value, (16 - value) + 1, this.wallheight, this.wallheight + 1, start, 16, this.floor2); //
            }
            if ((roadStartZ <= 16) && (roadStartZ > 1)) {
                int start, end;
                if ((plotMinX + 2) <= 16) {
                    start = 16 - plotMinX - 1;
                } else {
                    start = 16;
                }
                if ((roadStartX - 1) <= 16) {
                    end = (16 - roadStartX) + 1;
                } else {
                    end = 0;
                }
                if (!(((plotMinX + 2) <= 16) || ((roadStartX - 1) <= 16))) {
                    start = 0;
                }
                setCuboidRegion(0, end, this.wallheight, this.wallheight + 1, (16 - roadStartZ) + 1, (16 - roadStartZ) + 2, this.floor2);
                setCuboidRegion(start, 16, this.wallheight, this.wallheight + 1, (16 - roadStartZ) + 1, (16 - roadStartZ) + 2, this.floor2);
            }
            if ((roadStartX <= 16) && (roadStartX > 1)) {
                int start, end;
                if ((plotMinZ + 2) <= 16) {
                    start = 16 - plotMinZ - 1;
                } else {
                    start = 16;
                }
                if ((roadStartZ - 1) <= 16) {
                    end = (16 - roadStartZ) + 1;
                } else {
                    end = 0;
                }
                if (!(((plotMinZ + 2) <= 16) || ((roadStartZ - 1) <= 16))) {
                    start = 0;
                }
                setCuboidRegion((16 - roadStartX) + 1, (16 - roadStartX) + 2, this.wallheight, this.wallheight + 1, 0, end, this.floor2); //
                setCuboidRegion((16 - roadStartX) + 1, (16 - roadStartX) + 2, this.wallheight, this.wallheight + 1, start, 16, this.floor2); //
            }
        }

        // Plot filling (28/28 cuboids) (10x2 + 4x2)
        if (this.plotsize > 16) {
            if (roadStartX <= 16) {
                if (roadStartZ <= 16) {
                    setCuboidRegion(0, 16 - roadStartX, 1, this.plotheight, 0, 16 - roadStartZ, this.filling);
                    setCuboidRegion(0, 16 - roadStartX, this.plotheight, this.plotheight + 1, 0, 16 - roadStartZ, this.plotfloors);
                }
                if (plotMinZ <= 16) {
                    setCuboidRegion(0, 16 - roadStartX, 1, this.plotheight, 16 - plotMinZ, 16, this.filling);
                    setCuboidRegion(0, 16 - roadStartX, this.plotheight, this.plotheight + 1, 16 - plotMinZ, 16, this.plotfloors);
                }
            } else {
                if (roadStartZ <= 16) {
                    if (plotMinX > 16) {
                        setCuboidRegion(0, 16, 1, this.plotheight, 0, 16 - roadStartZ, this.filling);
                        setCuboidRegion(0, 16, this.plotheight, this.plotheight + 1, 0, 16 - roadStartZ, this.plotfloors);
                    }
                }
            }
            if (plotMinX <= 16) {
                if (plotMinZ <= 16) {
                    setCuboidRegion(16 - plotMinX, 16, 1, this.plotheight, 16 - plotMinZ, 16, this.filling);
                    setCuboidRegion(16 - plotMinX, 16, this.plotheight, this.plotheight + 1, 16 - plotMinZ, 16, this.plotfloors);
                } else {
                    int z = 16 - roadStartZ;
                    if (z < 0) {
                        z = 16;
                    }
                    setCuboidRegion(16 - plotMinX, 16, 1, this.plotheight, 0, z, this.filling);
                    setCuboidRegion(16 - plotMinX, 16, this.plotheight, this.plotheight + 1, 0, z, this.plotfloors);
                }
                if (roadStartZ <= 16) {
                    setCuboidRegion(16 - plotMinX, 16, 1, this.plotheight, 0, 16 - roadStartZ, this.filling);
                    setCuboidRegion(16 - plotMinX, 16, this.plotheight, this.plotheight + 1, 0, 16 - roadStartZ, this.plotfloors);
                } else {
                    if (roadStartX <= 16) {
                        if (plotMinZ > 16) {
                            int x = 16 - roadStartX;
                            if (x < 0) {
                                x = 16;
                            }
                            setCuboidRegion(0, x, 1, this.plotheight, 0, 16, this.filling);
                            setCuboidRegion(0, x, this.plotheight, this.plotheight + 1, 0, 16, this.plotfloors);
                        }
                    }
                }
            } else {
                if (plotMinZ <= 16) {
                    if (roadStartX > 16) {
                        int x = 16 - roadStartX;
                        if (x < 0) {
                            x = 16;
                        }
                        setCuboidRegion(0, x, 1, this.plotheight, 16 - plotMinZ, 16, this.filling);
                        setCuboidRegion(0, x, this.plotheight, this.plotheight + 1, 16 - plotMinZ, 16, this.plotfloors);
                    }
                } else {
                    if (roadStartZ > 16) {
                        int x = 16 - roadStartX;
                        if (x < 0) {
                            x = 16;
                        }
                        int z = 16 - roadStartZ;
                        if (z < 0) {
                            z = 16;
                        }
                        if (roadStartX > 16) {
                            setCuboidRegion(0, x, 1, this.plotheight, 0, z, this.filling);
                            setCuboidRegion(0, x, this.plotheight, this.plotheight + 1, 0, z, this.plotfloors);
                        } else {
                            setCuboidRegion(0, x, 1, this.plotheight, 0, z, this.filling);
                            setCuboidRegion(0, x, this.plotheight, this.plotheight + 1, 0, z, this.plotfloors);
                        }
                    }
                }
            }
        } else {
            if (roadStartX <= 16) {
                if (roadStartZ <= 16) {
                    setCuboidRegion(0, 16 - roadStartX, 1, this.plotheight, 0, 16 - roadStartZ, this.filling);
                    setCuboidRegion(0, 16 - roadStartX, this.plotheight, this.plotheight + 1, 0, 16 - roadStartZ, this.plotfloors);
                }
                if (plotMinZ <= 16) {
                    setCuboidRegion(0, 16 - roadStartX, 1, this.plotheight, 16 - plotMinZ, 16, this.filling);
                    setCuboidRegion(0, 16 - roadStartX, this.plotheight, this.plotheight + 1, 16 - plotMinZ, 16, this.plotfloors);
                }
            }
            if (plotMinX <= 16) {
                if (plotMinZ <= 16) {
                    setCuboidRegion(16 - plotMinX, 16, 1, this.plotheight, 16 - plotMinZ, 16, this.filling);
                    setCuboidRegion(16 - plotMinX, 16, this.plotheight, this.plotheight + 1, 16 - plotMinZ, 16, this.plotfloors);
                }
                if (roadStartZ <= 16) {
                    setCuboidRegion(16 - plotMinX, 16, 1, this.plotheight, 0, 16 - roadStartZ, this.filling);
                    setCuboidRegion(16 - plotMinX, 16, this.plotheight, this.plotheight + 1, 0, 16 - roadStartZ, this.plotfloors);
                }
            }
        }

        // WALLS (16/16 cuboids)
        if (this.pathsize > 0) {
            if ((plotMinZ + 1) <= 16) {
                int start, end;
                if ((plotMinX + 2) <= 16) {
                    start = 16 - plotMinX - 1;
                } else {
                    start = 16;
                }
                if ((roadStartX - 1) <= 16) {
                    end = (16 - roadStartX) + 1;
                } else {
                    end = 0;
                }
                if (!(((plotMinX + 2) <= 16) || ((roadStartX - 1) <= 16))) {
                    start = 0;
                }
                setCuboidRegion(0, end, 1, this.wallheight + 1, 16 - plotMinZ - 1, 16 - plotMinZ, this.wallfilling);
                setCuboidRegion(0, end, this.wallheight + 1, this.wallheight + 2, 16 - plotMinZ - 1, 16 - plotMinZ, this.wall);
                setCuboidRegion(start, 16, 1, this.wallheight + 1, 16 - plotMinZ - 1, 16 - plotMinZ, this.wallfilling);
                setCuboidRegion(start, 16, this.wallheight + 1, this.wallheight + 2, 16 - plotMinZ - 1, 16 - plotMinZ, this.wall);
            }
            if ((plotMinX + 1) <= 16) {
                int start, end;
                if ((plotMinZ + 2) <= 16) {
                    start = 16 - plotMinZ - 1;
                } else {
                    start = 16;
                }
                if ((roadStartZ - 1) <= 16) {
                    end = (16 - roadStartZ) + 1;
                } else {
                    end = 0;
                }
                if (!(((plotMinZ + 2) <= 16) || ((roadStartZ - 1) <= 16))) {
                    start = 0;
                }
                setCuboidRegion(16 - plotMinX - 1, 16 - plotMinX, 1, this.wallheight + 1, 0, end, this.wallfilling);
                setCuboidRegion(16 - plotMinX - 1, 16 - plotMinX, this.wallheight + 1, this.wallheight + 2, 0, end, this.wall);
                setCuboidRegion(16 - plotMinX - 1, 16 - plotMinX, 1, this.wallheight + 1, start, 16, this.wallfilling);
                setCuboidRegion(16 - plotMinX - 1, 16 - plotMinX, this.wallheight + 1, this.wallheight + 2, start, 16, this.wall);
            }
            if ((roadStartZ <= 16) && (roadStartZ > 0)) {
                int start, end;
                if ((plotMinX + 1) <= 16) {
                    start = 16 - plotMinX;
                } else {
                    start = 16;
                }
                if ((roadStartX + 1) <= 16) {
                    end = (16 - roadStartX) + 1;
                } else {
                    end = 0;
                }
                if (!(((plotMinX + 1) <= 16) || (roadStartX <= 16))) {
                    start = 0;
                }
                setCuboidRegion(0, end, 1, this.wallheight + 1, 16 - roadStartZ, (16 - roadStartZ) + 1, this.wallfilling);
                setCuboidRegion(0, end, this.wallheight + 1, this.wallheight + 2, 16 - roadStartZ, (16 - roadStartZ) + 1, this.wall);
                setCuboidRegion(start, 16, 1, this.wallheight + 1, 16 - roadStartZ, (16 - roadStartZ) + 1, this.wallfilling);
                setCuboidRegion(start, 16, this.wallheight + 1, this.wallheight + 2, 16 - roadStartZ, (16 - roadStartZ) + 1, this.wall);
            }
            if ((roadStartX <= 16) && (roadStartX > 0)) {
                int start, end;
                if ((plotMinZ + 1) <= 16) {
                    start = 16 - plotMinZ;
                } else {
                    start = 16;
                }
                if ((roadStartZ + 1) <= 16) {
                    end = (16 - roadStartZ) + 1;
                } else {
                    end = 0;
                }
                if (!(((plotMinZ + 1) <= 16) || ((roadStartZ + 1) <= 16))) {
                    start = 0;
                }
                setCuboidRegion(16 - roadStartX, (16 - roadStartX) + 1, 1, this.wallheight + 1, 0, end, this.wallfilling);
                setCuboidRegion(16 - roadStartX, (16 - roadStartX) + 1, this.wallheight + 1, this.roadheight + 2, 0, end, this.wall);
                setCuboidRegion(16 - roadStartX, (16 - roadStartX) + 1, 1, this.wallheight + 1, start, 16, this.wallfilling);
                setCuboidRegion(16 - roadStartX, (16 - roadStartX) + 1, this.wallheight + 1, this.wallheight + 2, start, 16, this.wall);
            }
        }
        // Return the chunk
        return this.result;
    }

}
