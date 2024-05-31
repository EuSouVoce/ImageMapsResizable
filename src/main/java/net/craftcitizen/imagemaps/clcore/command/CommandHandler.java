package net.craftcitizen.imagemaps.clcore.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;

public abstract class CommandHandler implements TabExecutor {
    private final Map<String, SubCommand> commands = new HashMap<String, SubCommand>();
    private final Plugin plugin;

    public CommandHandler(final Plugin plugin) { this.plugin = plugin; }

    /*
     * Enabled force condition propagation Lifted jumps to return sites
     */
    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, String[] args) {
        String message;
        if ((args = CommandUtils.parseArgumentStrings(args)).length == 0 || !this.commands.containsKey(args[0])) {
            if (!this.commands.containsKey("help"))
                return false;
            message = this.commands.get("help").execute(sender, cmd, label, args);
        } else {
            message = this.commands.get(args[0]).execute(sender, cmd, label, args);
        }
        if (message == null)
            return true;
        sender.sendMessage(message);
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        switch (args.length) {
        case 0: {
            return Collections.emptyList();
        }
        case 1: {
            return this.commands.keySet().stream().filter(a -> a.startsWith(args[0])).filter(
                    a -> this.commands.get(a).getPermission().isEmpty() || sender.hasPermission(this.commands.get(a).getPermission()))
                    .collect(Collectors.toList());
        }
        }
        if (!this.commands.containsKey(args[0])) {
            return Collections.emptyList();
        }
        return this.commands.get(args[0]).onTabComplete(sender, args);
    }

    public void registerSubCommand(final String name, final SubCommand command, final String... alias) {
        Validate.notNull((Object) command, (String) "Command can't be null!");
        Validate.notEmpty((String) name, (String) "Commandname can't be empty!");
        Validate.isTrue((!this.commands.containsKey(name) ? 1 : 0) != 0, (String) ("Command " + name + " is already defined"));
        for (final String a : alias) {
            Validate.isTrue((!this.commands.containsKey(a) ? 1 : 0) != 0, (String) ("Command " + a + " is already defined"));
        }
        if (command instanceof SubCommandHandler) {
            Validate.isTrue((((SubCommandHandler) command).getDepth() == 1 ? 1 : 0) != 0,
                    (String) "The depth of a SubCommandHandler must be the depth of the previous Handler + 1!");
        }
        this.commands.put(name, command);
        for (final String s : alias) {
            this.commands.put(s, command);
        }
    }

    protected Plugin getPlugin() { return this.plugin; }

    protected Map<String, SubCommand> getCommands() { return this.commands; }
}
