package ilbie.com.tailsplugin.listeners;

import ilbie.com.tailsplugin.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import ilbie.com.tailsplugin.utils.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerInteractListener implements Listener {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public PlayerInteractListener(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /**
     * 플레이어의 인터랙션 제한 (아이템 사용 등)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isSlave(player) || gameManager.isDead(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatUtil.prefix().append(
                    Component.text("당신은 현재 아무 것도 할 수 없습니다!", NamedTextColor.RED)
            ));
        }
    }
}
