package ilbie.com.tailsplugin.commands;

import ilbie.com.tailsplugin.main.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
            sender.sendMessage(Component.text("이 명령어를 사용할 권한이 없습니다.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("사용법: /tails <start|stop|reset>", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (gameManager.isGameRunning()) {
                sender.sendMessage(Component.text("게임이 이미 시작되었습니다.", NamedTextColor.RED));
                return true;
            }
            gameManager.startGame();
            sender.sendMessage(Component.text("게임이 시작되었습니다.", NamedTextColor.GREEN));

        } else if (args[0].equalsIgnoreCase("stop")) {
            if (!gameManager.isGameRunning()) {
                sender.sendMessage(Component.text("게임이 진행 중이지 않습니다.", NamedTextColor.RED));
                return true;
            }
            gameManager.stopGame();
            sender.sendMessage(Component.text("게임이 중지되었습니다.", NamedTextColor.GREEN));

        } else if (args[0].equalsIgnoreCase("reset")) {
            if (gameManager.isGameRunning()) {
                gameManager.stopGame();
                sender.sendMessage(Component.text("게임이 종료되고 리셋되었습니다.", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("진행 중인 게임이 없으므로 리셋되지 않았습니다.", NamedTextColor.RED));
            }
            gameManager.resetTeams(); // 팀 데이터 초기화
            gameManager.resetGameData(); // 게임 관련 데이터 초기화

        } else {
            sender.sendMessage(Component.text("사용법: /tails <start|stop|reset>", NamedTextColor.YELLOW));
        }

        return true;
    }
}
