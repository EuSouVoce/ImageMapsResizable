package net.craftcitizen.imagemaps;

import org.bukkit.block.BlockFace;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class ImagePlaceEvent extends Event implements Cancellable
{
    private static final HandlerList handlers;
    private final Player player;
    private final Block block;
    private final BlockFace widthDirection;
    private final BlockFace heightDirection;
    private final int width;
    private final int height;
    private final PlacementData cache;
    private boolean cancelled;
    
    public ImagePlaceEvent(final Player player, final Block block, final BlockFace widthDirection, final BlockFace heightDirection, final int width, final int height, final PlacementData cache) {
        this.player = player;
        this.block = block;
        this.widthDirection = widthDirection;
        this.heightDirection = heightDirection;
        this.width = width;
        this.height = height;
        this.cache = cache;
    }
    
    public Player getPlayer() {
        return this.player;
    }
    
    public Block getBlock() {
        return this.block;
    }
    
    public BlockFace getHeightDirection() {
        return this.heightDirection;
    }
    
    public BlockFace getWidthDirection() {
        return this.widthDirection;
    }
    
    public int getWidth() {
        return this.width;
    }
    
    public int getHeight() {
        return this.height;
    }
    
    public PlacementData getCacheEntry() {
        return this.cache;
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }
    
    public void setCancelled(final boolean cancel) {
        this.cancelled = cancel;
    }
    
    public HandlerList getHandlers() {
        return getHandlerList();
    }
    
    public static HandlerList getHandlerList() {
        return ImagePlaceEvent.handlers;
    }
    
    static {
        handlers = new HandlerList();
    }
}
