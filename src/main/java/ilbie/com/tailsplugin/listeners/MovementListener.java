package ilbie.com.tailsplugin.listeners;

import ilbie.com.tailsplugin.main.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public MovementListener(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /**
     * '죽은' 플레이어의 이동 제한
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isDead(player)) {
            // '죽은' 플레이어는 자유롭게 이동할 수 있습니다.
            // 상호작용은 다른 이벤트에서 제한됩니다.
            // 필요한 경우 특정 지역으로의 이동을 제한하려면 추가 로직을 구현할 수 있습니다.
        }
    }
}
