package ilbie.com.tailsplugin.commands;

import ilbie.com.tailsplugin.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import ilbie.com.tailsplugin.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TailsCommand implements CommandExecutor {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public TailsCommand(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp()) {
            sender.sendMessage(ChatUtil.prefix().append(
                    Component.text("이 명령어를 사용할 권한이 없습니다.", NamedTextColor.RED)
            ));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtil.prefix().append(
                    Component.text("사용법: /tails <start|stop>", NamedTextColor.YELLOW)
            ));
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (gameManager.isGameRunning()) {
                sender.sendMessage(ChatUtil.prefix().append(
                        Component.text("게임이 이미 시작되었습니다.", NamedTextColor.RED)
                ));
                return true;
            }
            gameManager.startGame();
            sender.sendMessage(ChatUtil.prefix().append(
                    Component.text("게임을 시작했습니다!", NamedTextColor.GREEN)
            ));
        } else if (args[0].equalsIgnoreCase("stop")) {
            if (!gameManager.isGameRunning()) {
                sender.sendMessage(ChatUtil.prefix().append(
                        Component.text("게임이 진행 중이지 않습니다.", NamedTextColor.RED)
                ));
                return true;
            }
            gameManager.stopGame();
            sender.sendMessage(ChatUtil.prefix().append(
                    Component.text("게임을 중지했습니다!", NamedTextColor.GREEN)
            ));
        } else {
            sender.sendMessage(ChatUtil.prefix().append(
                    Component.text("사용법: /tails <start|stop>", NamedTextColor.YELLOW)
            ));
        }

        return true;
    }
}
