package net.craftcitizen.imagemaps.clcore.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public abstract class SubCommand {
    private String permission = "";
    protected Plugin plugin;
    private final boolean console;

    public SubCommand(final String permission, final Plugin plugin, final boolean console) {
        this.permission = permission;
        this.plugin = plugin;
        this.console = console;
    }

    public boolean checkSender(final CommandSender sender) {
        if (!(sender instanceof Player) && this.isConsoleCommand()) {
            return true;
        }
        return this.getPermission().equals("") || sender.hasPermission(this.getPermission());
    }

    public Plugin getPlugin() { return this.plugin; }

    public boolean isConsoleCommand() { return this.console; }

    public String getPermission() { return this.permission; }

    protected abstract String execute(CommandSender var1, Command var2, String var3, String[] var4);

    protected List<String> onTabComplete(final CommandSender sender, final String[] args) { return Collections.emptyList(); }

    public abstract void help(CommandSender var1);
}
