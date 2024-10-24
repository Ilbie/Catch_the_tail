package ilbie.com.tailsplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class TailsTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if ("start".startsWith(args[0].toLowerCase())) {
                completions.add("start");
            }
            if ("stop".startsWith(args[0].toLowerCase())) {
                completions.add("stop");
            }
            if ("reset".startsWith(args[0].toLowerCase())) {
                completions.add("reset");
            }
        }

        return completions;
    }
}
