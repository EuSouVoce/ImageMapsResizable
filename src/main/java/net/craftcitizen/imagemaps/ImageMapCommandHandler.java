package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.clcore.command.CommandHandler;

public class ImageMapCommandHandler extends CommandHandler {
    public ImageMapCommandHandler(final ImageMaps plugin) {
        super(plugin);
        this.registerSubCommand("download", new ImageMapDownloadCommand(plugin), new String[0]);
        this.registerSubCommand("delete", new ImageMapDeleteCommand(plugin), new String[0]);
        this.registerSubCommand("place", new ImageMapPlaceCommand(plugin), new String[0]);
        this.registerSubCommand("info", new ImageMapInfoCommand(plugin), new String[0]);
        this.registerSubCommand("list", new ImageMapListCommand(plugin), new String[0]);
        this.registerSubCommand("reload", new ImageMapReloadCommand(plugin), new String[0]);
        this.registerSubCommand("cleanup", new ImageMapCleanupCommand(plugin), new String[0]);
        this.registerSubCommand("debuginfo", new ImageMapDebugInfoCommand(plugin), new String[0]);
        this.registerSubCommand("help", new ImageMapHelpCommand(plugin, this.getCommands()), "?");
    }
}
