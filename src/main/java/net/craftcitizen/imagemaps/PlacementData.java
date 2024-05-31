package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.clcore.util.Tuple;

public class PlacementData
{
    private final String filename;
    private final boolean isInvisible;
    private final boolean isFixed;
    private final boolean isGlowing;
    private final Tuple<Integer, Integer> scale;
    
    public PlacementData(final String filename, final boolean isInvisible, final boolean isFixed, final boolean isGlowing, final Tuple<Integer, Integer> scale) {
        this.filename = filename;
        this.isInvisible = isInvisible;
        this.isFixed = isFixed;
        this.isGlowing = isGlowing;
        this.scale = scale;
    }
    
    public String getFilename() {
        return this.filename;
    }
    
    public boolean isFixed() {
        return this.isFixed;
    }
    
    public boolean isInvisible() {
        return this.isInvisible;
    }
    
    public boolean isGlowing() {
        return this.isGlowing;
    }
    
    public Tuple<Integer, Integer> getSize() {
        return this.scale;
    }
}
