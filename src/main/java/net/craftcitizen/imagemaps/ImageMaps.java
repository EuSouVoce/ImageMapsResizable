package net.craftcitizen.imagemaps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.io.Files;

import net.craftcitizen.imagemaps.clcore.LambdaRunnable;
import net.craftcitizen.imagemaps.clcore.SemanticVersion;
import net.craftcitizen.imagemaps.clcore.Utils;
import net.craftcitizen.imagemaps.clcore.util.MessageLevel;
import net.craftcitizen.imagemaps.clcore.util.MessageUtil;
import net.craftcitizen.imagemaps.clcore.util.Tuple;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

public class ImageMaps extends JavaPlugin implements Listener {
    private static final String MAPS_YML = "maps.yml";
    private static final String CONFIG_VERSION_KEY = "storageVersion";
    private static final int CONFIG_VERSION = 1;
    private static final long AUTOSAVE_PERIOD = 18000L;
    public static final String PLACEMENT_METADATA = "imagemaps.place";
    public static final int MAP_WIDTH = 128;
    public static final int MAP_HEIGHT = 128;
    private static final String IMAGES_DIR = "images";
    private final Map<String, BufferedImage> imageCache = new HashMap<String, BufferedImage>();
    private final Map<ImageMap, Integer> maps = new HashMap<ImageMap, Integer>();
    private Material toggleItem;

    @Override
    public void onEnable() {
        final BaseComponent prefix = new TextComponent(new ComponentBuilder("[").color(ChatColor.GRAY).append("ImageMaps")
                .color(ChatColor.AQUA).append("]").color(ChatColor.GRAY).create());
        MessageUtil.registerPlugin(this, prefix, ChatColor.GRAY, ChatColor.YELLOW, ChatColor.RED, ChatColor.DARK_RED, ChatColor.DARK_AQUA);
        if (!new File(this.getDataFolder(), ImageMaps.IMAGES_DIR).exists()) {
            new File(this.getDataFolder(), ImageMaps.IMAGES_DIR).mkdirs();
        }

        this.saveDefaultConfig();
        this.toggleItem = Material.matchMaterial(this.getConfig().getString("toggleItem", Material.WOODEN_HOE.name()));
        if (this.toggleItem == null) {
            this.toggleItem = Material.WOODEN_HOE;
            this.getLogger().warning("Given toggleItem is invalid, defaulting to WOODEN_HOE");
        }

        this.getCommand("imagemap").setExecutor(new ImageMapCommandHandler(this));
        this.getServer().getPluginManager().registerEvents(this, this);
        this.loadMaps();
        new LambdaRunnable(this::saveMaps).runTaskTimer(this, ImageMaps.AUTOSAVE_PERIOD, ImageMaps.AUTOSAVE_PERIOD);
    }

    @Override
    public void onDisable() { this.saveMaps(); }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFrameProperty(final PlayerInteractEntityEvent event) {
        if (this.isInvisibilitySupported()) {
            if (event.getRightClicked().getType() == EntityType.ITEM_FRAME
                    || this.isGlowingSupported() && event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME) {
                final ItemFrame frame = (ItemFrame) event.getRightClicked();
                final Player p = event.getPlayer();
                if (p.getInventory().getItemInMainHand().getType() == this.toggleItem) {
                    if (p.isSneaking()) {
                        if (p.hasPermission("imagemaps.toggleFixed")) {
                            event.setCancelled(true);
                            frame.setFixed(!frame.isFixed());
                            MessageUtil.sendMessage(this, p, MessageLevel.INFO,
                                    String.format("Frame set to %s.", frame.isFixed() ? "fixed" : "unfixed"));
                        }
                    } else if (p.hasPermission("imagemaps.toggleVisible")) {
                        event.setCancelled(true);
                        frame.setVisible(!frame.isVisible());
                        MessageUtil.sendMessage(this, p, MessageLevel.INFO,
                                String.format("Frame set to %s.", frame.isVisible() ? "visible" : "invisible"));
                    }
                }
            }
        }
    }

    public boolean isInvisibilitySupported() {
        final SemanticVersion version = Utils.getMCVersion();
        return version.getMajor() >= 1 && version.getMinor() >= 16;
    }

    public boolean isGlowingSupported() {
        final SemanticVersion version = Utils.getMCVersion();
        return version.getMajor() >= 1 && version.getMinor() >= 17;
    }

    public boolean isUpDownFaceSupported() {
        final SemanticVersion version = Utils.getMCVersion();
        return version.getMajor() >= 1
                && (version.getMajor() == 1 && version.getMinor() == 14 && version.getRevision() >= 4 || version.getMinor() > 14);
    }

    public boolean isSetTrackingSupported() {
        final SemanticVersion version = Utils.getMCVersion();
        return version.getMajor() >= 1 && version.getMinor() >= 14;
    }

    private void saveMaps() {
        final FileConfiguration config = new YamlConfiguration();
        config.set(ImageMaps.CONFIG_VERSION_KEY, 1);
        config.set("maps", this.maps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));
        final BukkitRunnable saveTask = new LambdaRunnable(() -> {
            try {
                config.save(new File(this.getDataFolder(), ImageMaps.MAPS_YML));
            } catch (final IOException var3) {
                var3.printStackTrace();
            }
        });
        if (this.isEnabled()) {
            saveTask.runTaskAsynchronously(this);
        } else {
            saveTask.run();
        }
    }

    private void loadMaps() {
        final File configFile = new File(this.getDataFolder(), ImageMaps.MAPS_YML);
        if (configFile.exists()) {
            Configuration config = YamlConfiguration.loadConfiguration(configFile);
            final int version = config.getInt(ImageMaps.CONFIG_VERSION_KEY, -1);
            if (version == -1) {
                config = this.convertLegacyMaps(config);
            }

            final ConfigurationSection section = config.getConfigurationSection("maps");
            if (section != null) {
                section.getValues(false).forEach((a, b) -> {
                    final int id = Integer.parseInt(a);
                    final ImageMap imageMap = (ImageMap) b;
                    final MapView map = Bukkit.getMap(id);
                    final BufferedImage image = this.getImage(imageMap.getFilename());
                    this.maps.put(imageMap, id);
                    if (image == null) {
                        this.getLogger().warning(() -> "Image file " + imageMap.getFilename() + " not found!");
                    } else if (map == null) {
                        this.getLogger().warning(() -> "Map " + id + " referenced but does not exist!");
                    } else {
                        if (this.isSetTrackingSupported()) {
                            map.setTrackingPosition(false);
                        }

                        map.getRenderers().forEach(map::removeRenderer);
                        map.addRenderer(new ImageMapRenderer(this, image, imageMap.getX(), imageMap.getY(), imageMap.getScale()));
                    }
                });
            }
        }
    }

    public int cleanupMaps() {
        final int start = this.maps.size();
        this.maps.entrySet().removeIf(a -> {
            final MapView map = Bukkit.getMap((Integer) a.getValue());
            final BufferedImage image = this.getImage(((ImageMap) a.getKey()).getFilename());
            return map == null || image == null;
        });
        return start - this.maps.size();
    }

    private Configuration convertLegacyMaps(final Configuration config) {
        this.getLogger().info("Converting maps from Version <1.0");

        try {
            Files.copy(new File(this.getDataFolder(), ImageMaps.MAPS_YML), new File(this.getDataFolder(), "maps.yml.backup"));
        } catch (final IOException var11) {
            this.getLogger().severe("Failed to backup maps.yml!");
            var11.printStackTrace();
        }

        final Map<Integer, ImageMap> map = new HashMap<Integer, ImageMap>();

        for (final String key : config.getKeys(false)) {
            final int id = Integer.parseInt(key);
            final String image = config.getString(key + ".image");
            final int x = config.getInt(key + ".x") / 128;
            final int y = config.getInt(key + ".y") / 128;
            final double scale = config.getDouble(key + ".scale", 1.0);
            map.put(id, new ImageMap(image, x, y, scale));
        }

        final Configuration var12 = new YamlConfiguration();
        var12.set(ImageMaps.CONFIG_VERSION_KEY, 1);
        var12.createSection("maps", map);
        return var12;
    }

    public boolean hasImage(final String filename) {
        if (this.imageCache.containsKey(filename.toLowerCase())) {
            return true;
        } else {
            final File file = new File(this.getDataFolder(), ImageMaps.IMAGES_DIR + File.separatorChar + filename);
            return file.exists() && this.getImage(filename) != null;
        }
    }

    public BufferedImage getImage(final String filename) {
        if (!filename.contains("/") && !filename.contains("\\") && !filename.contains(":")) {
            if (this.imageCache.containsKey(filename.toLowerCase())) {
                return (BufferedImage) this.imageCache.get(filename.toLowerCase());
            } else {
                final File file = new File(this.getDataFolder(), ImageMaps.IMAGES_DIR + File.separatorChar + filename);
                BufferedImage image = null;
                if (!file.exists()) {
                    return null;
                } else {
                    try {
                        image = ImageIO.read(file);
                        this.imageCache.put(filename.toLowerCase(), image);
                    } catch (final IOException var5) {
                        this.getLogger().log(Level.SEVERE, String.format("Error while trying to read image %s.", file.getName()), var5);
                    }

                    if (image == null) {
                        this.getLogger().log(Level.WARNING, () -> String.format("Failed to read file as image %s.", file.getName()));
                    }

                    return image;
                }
            }
        } else {
            this.getLogger().warning("Someone tried to get image with illegal characters in file name.");
            return null;
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (player.hasMetadata("imagemaps.place")) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                player.removeMetadata("imagemaps.place", this);
                MessageUtil.sendMessage(this, player, MessageLevel.NORMAL, "Image placement cancelled.");
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                final PlacementData data = (PlacementData) ((MetadataValue) player.getMetadata("imagemaps.place").get(0)).value();
                final PlacementResult result = this.placeImage(player, event.getClickedBlock(), event.getBlockFace(), data);
                switch (result) {
                case INVALID_FACING:
                    MessageUtil.sendMessage(this, player, MessageLevel.WARNING, "You can't place an image on this block face.");
                    break;
                case INVALID_DIRECTION:
                    MessageUtil.sendMessage(this, player, MessageLevel.WARNING, "Couldn't calculate how to place the map.");
                    break;
                case EVENT_CANCELLED:
                    MessageUtil.sendMessage(this, player, MessageLevel.NORMAL, "Image placement cancelled by another plugin.");
                    break;
                case INSUFFICIENT_SPACE:
                    MessageUtil.sendMessage(this, player, MessageLevel.NORMAL, "Map couldn't be placed, the space is blocked.");
                    break;
                case INSUFFICIENT_WALL:
                    MessageUtil.sendMessage(this, player, MessageLevel.NORMAL, "Map couldn't be placed, the supporting wall is too small.");
                    break;
                case OVERLAPPING_ENTITY:
                    MessageUtil.sendMessage(this, player, MessageLevel.NORMAL,
                            "Map couldn't be placed, there is another entity in the way.");
                case SUCCESS:
                }

                player.removeMetadata("imagemaps.place", this);
                event.setCancelled(true);
            }
        }
    }

    private PlacementResult placeImage(final Player player, final Block block, final BlockFace face, final PlacementData data) {
        if (!ImageMaps.isAxisAligned(face)) {
            this.getLogger().severe("Someone tried to create an image with an invalid block facing");
            return PlacementResult.INVALID_FACING;
        } else if (face.getModY() != 0 && !this.isUpDownFaceSupported()) {
            return PlacementResult.INVALID_FACING;
        } else {
            final Block b = block.getRelative(face);
            final BufferedImage image = this.getImage(data.getFilename());
            final Tuple<Integer, Integer> size = this.getImageSize(data.getFilename(), data.getSize());
            final BlockFace widthDirection = ImageMaps.calculateWidthDirection(player, face);
            final BlockFace heightDirection = ImageMaps.calculateHeightDirection(player, face);
            if (widthDirection != null && heightDirection != null) {
                for (int x = 0; x < size.getKey(); x++) {
                    for (int y = 0; y < size.getValue(); y++) {
                        final Block frameBlock = b.getRelative(widthDirection, x).getRelative(heightDirection, y);
                        if (!block.getRelative(widthDirection, x).getRelative(heightDirection, y).getType().isSolid()) {
                            return PlacementResult.INSUFFICIENT_WALL;
                        }

                        if (frameBlock.getType().isSolid()) {
                            return PlacementResult.INSUFFICIENT_SPACE;
                        }

                        if (!b.getWorld()
                                .getNearbyEntities(frameBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, Hanging.class::isInstance)
                                .isEmpty()) {
                            return PlacementResult.OVERLAPPING_ENTITY;
                        }
                    }
                }

                final ImagePlaceEvent event = new ImagePlaceEvent(player, block, widthDirection, heightDirection, size.getKey(),
                        size.getValue(), data);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return PlacementResult.EVENT_CANCELLED;
                } else {
                    for (int x = 0; x < size.getKey(); x++) {
                        for (int y = 0; y < size.getValue(); y++) {
                            final Class<? extends ItemFrame> itemFrameClass = data.isGlowing() ? GlowItemFrame.class : ItemFrame.class;
                            final ItemFrame frame = (ItemFrame) block.getWorld()
                                    .spawn(b.getRelative(widthDirection, x).getRelative(heightDirection, y).getLocation(), itemFrameClass);
                            frame.setFacingDirection(face);
                            frame.setItem(this.getMapItem(image, x, y, data));
                            frame.setRotation(ImageMaps.facingToRotation(heightDirection, widthDirection));
                            if (this.isInvisibilitySupported()) {
                                frame.setFixed(data.isFixed());
                                frame.setVisible(!data.isInvisible());
                            }
                        }
                    }

                    return PlacementResult.SUCCESS;
                }
            } else {
                return PlacementResult.INVALID_DIRECTION;
            }
        }
    }

    public boolean deleteImage(final String filename) {
        final File file = new File(this.getDataFolder(), ImageMaps.IMAGES_DIR + File.separatorChar + filename);
        boolean fileDeleted = false;
        if (file.exists()) {
            fileDeleted = file.delete();
        }

        this.imageCache.remove(filename.toLowerCase());
        final Iterator<Map.Entry<ImageMap, Integer>> it = this.maps.entrySet().iterator();

        while (it.hasNext()) {
            final Map.Entry<ImageMap, Integer> entry = (Map.Entry<ImageMap, Integer>) it.next();
            final ImageMap imageMap = entry.getKey();
            if (imageMap.getFilename().equalsIgnoreCase(filename)) {
                final MapView map = Bukkit.getMap(entry.getValue());
                if (map != null) {
                    map.getRenderers().forEach(map::removeRenderer);
                    it.remove();
                }
            }
        }

        this.saveMaps();
        return fileDeleted;
    }

    public boolean reloadImage(final String filename) {
        if (!this.imageCache.containsKey(filename.toLowerCase())) {
            return false;
        } else {
            this.imageCache.remove(filename.toLowerCase());
            final BufferedImage image = this.getImage(filename);
            if (image == null) {
                this.getLogger().warning(() -> "Failed to reload image: " + filename);
                return false;
            } else {
                this.maps.entrySet().stream().filter(a -> ((ImageMap) a.getKey()).getFilename().equalsIgnoreCase(filename))
                        .map(a -> Bukkit.getMap((Integer) a.getValue())).flatMap(a -> a.getRenderers().stream())
                        .filter(ImageMapRenderer.class::isInstance).forEach(a -> ((ImageMapRenderer) a).recalculateInput(image));
                return true;
            }
        }
    }

    private ItemStack getMapItem(final BufferedImage image, final int x, final int y, final PlacementData data) {
        final ItemStack item = new ItemStack(Material.FILLED_MAP);
        final ImageMap imageMap = new ImageMap(data.getFilename(), x, y, this.getScale(image, data.getSize()));
        if (this.maps.containsKey(imageMap)) {
            final MapMeta meta = (MapMeta) item.getItemMeta();
            meta.setMapId((Integer) this.maps.get(imageMap));
            item.setItemMeta(meta);
            return item;
        } else {
            final MapView map = this.getServer().createMap((World) this.getServer().getWorlds().get(0));
            map.getRenderers().forEach(map::removeRenderer);
            map.addRenderer(new ImageMapRenderer(this, image, x, y, this.getScale(image, data.getSize())));
            if (this.isSetTrackingSupported()) {
                map.setTrackingPosition(false);
            }

            final MapMeta meta = (MapMeta) item.getItemMeta();
            meta.setMapView(map);
            item.setItemMeta(meta);
            this.maps.put(imageMap, map.getId());
            return item;
        }
    }

    public Tuple<Integer, Integer> getImageSize(final String filename, final Tuple<Integer, Integer> size) {
        final BufferedImage image = this.getImage(filename);
        if (image == null) {
            return new Tuple<Integer, Integer>(0, 0);
        } else {
            final double finalScale = this.getScale(image, size);
            final int finalX = (int) ((127.0 + Math.ceil((double) image.getWidth() * finalScale)) / 128.0);
            final int finalY = (int) ((127.0 + Math.ceil((double) image.getHeight() * finalScale)) / 128.0);
            return new Tuple<Integer, Integer>(finalX, finalY);
        }
    }

    public double getScale(final String filename, final Tuple<Integer, Integer> size) {
        return this.getScale(this.getImage(filename), size);
    }

    public double getScale(final BufferedImage image, final Tuple<Integer, Integer> size) {
        if (image == null) {
            return 1.0;
        } else {
            final int baseX = image.getWidth();
            final int baseY = image.getHeight();
            double finalScale = 1.0;
            if (size != null) {
                final int targetX = size.getKey() * 128;
                final int targetY = size.getValue() * 128;
                final double scaleX = size.getKey() > 0 ? (double) targetX / (double) baseX : Double.MAX_VALUE;
                final double scaleY = size.getValue() > 0 ? (double) targetY / (double) baseY : Double.MAX_VALUE;
                finalScale = Math.min(scaleX, scaleY);
                if (finalScale >= Double.MAX_VALUE) {
                    finalScale = 1.0;
                }
            }

            return finalScale;
        }
    }

    private static Rotation facingToRotation(final BlockFace heightDirection, final BlockFace widthDirection) {
        return switch (heightDirection) {
        case WEST -> Rotation.CLOCKWISE_45;
        case NORTH -> widthDirection == BlockFace.WEST ? Rotation.CLOCKWISE : Rotation.NONE;
        case EAST -> Rotation.CLOCKWISE_135;
        case SOUTH -> widthDirection == BlockFace.WEST ? Rotation.CLOCKWISE : Rotation.NONE;
        default -> Rotation.NONE;
        };
    }

    private static BlockFace calculateWidthDirection(final Player player, final BlockFace face) {
        final float yaw = (360.0F + player.getLocation().getYaw()) % 360.0F;
        switch (face) {
        case WEST:
            return BlockFace.SOUTH;
        case NORTH:
            return BlockFace.WEST;
        case EAST:
            return BlockFace.NORTH;
        case SOUTH:
            return BlockFace.EAST;
        case UP:
        case DOWN:
            if (Utils.isBetween((double) yaw, 45.0, 135.0)) {
                return BlockFace.NORTH;
            } else if (Utils.isBetween((double) yaw, 135.0, 225.0)) {
                return BlockFace.EAST;
            } else {
                if (Utils.isBetween((double) yaw, 225.0, 315.0)) {
                    return BlockFace.SOUTH;
                }

                return BlockFace.WEST;
            }
        default:
            return null;
        }
    }

    private static BlockFace calculateHeightDirection(final Player player, final BlockFace face) {
        final float yaw = (360.0F + player.getLocation().getYaw()) % 360.0F;
        switch (face) {
        case WEST:
        case NORTH:
        case EAST:
        case SOUTH:
            return BlockFace.DOWN;
        case UP:
            if (Utils.isBetween((double) yaw, 45.0, 135.0)) {
                return BlockFace.EAST;
            } else if (Utils.isBetween((double) yaw, 135.0, 225.0)) {
                return BlockFace.SOUTH;
            } else {
                if (Utils.isBetween((double) yaw, 225.0, 315.0)) {
                    return BlockFace.WEST;
                }

                return BlockFace.NORTH;
            }
        case DOWN:
            if (Utils.isBetween((double) yaw, 45.0, 135.0)) {
                return BlockFace.WEST;
            } else if (Utils.isBetween((double) yaw, 135.0, 225.0)) {
                return BlockFace.NORTH;
            } else {
                if (Utils.isBetween((double) yaw, 225.0, 315.0)) {
                    return BlockFace.EAST;
                }

                return BlockFace.SOUTH;
            }
        default:
            return null;
        }
    }

    private static boolean isAxisAligned(final BlockFace face) {
        return switch (face) {
        case WEST, NORTH, EAST, SOUTH, UP, DOWN -> true;
        default -> false;
        };
    }

    static {
        ConfigurationSerialization.registerClass(ImageMap.class);
    }
}
