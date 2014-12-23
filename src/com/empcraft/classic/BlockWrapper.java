package com.empcraft.classic;

public class BlockWrapper {
    public short x;
    public short y;
    public short z;
    public short id;
    public byte data;
    
    public BlockWrapper(short x, short y, short z, short id, byte data) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = id;
        this.data = data;
    }
}
