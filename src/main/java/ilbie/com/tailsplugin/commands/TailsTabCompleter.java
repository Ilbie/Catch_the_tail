package ilbie.com.tailsplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class TailsTabCompleter implements TabCompleter {

    private final List<String> commands;

    public TailsTabCompleter() {
        commands = new ArrayList<>();
        commands.add("start");
        commands.add("stop");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String currentArg = args[0].toLowerCase();
            for (String cmd : commands) {
                if (cmd.startsWith(currentArg)) {
                    completions.add(cmd);
                }
            }
        }

        return completions;
    }
}
