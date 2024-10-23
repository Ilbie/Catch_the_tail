package ilbie.com.tailsplugin.listeners;

import ilbie.com.tailsplugin.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public PlayerQuitListener(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /**
     * 플레이어가 서버를 떠날 때 '죽은' 상태에서 제거
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isDead(player)) {
            gameManager.setAlive(player);
        }
    }
}
