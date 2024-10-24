package ilbie.com.tailsplugin.listeners;

import ilbie.com.tailsplugin.main.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class InteractionListener implements Listener {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public InteractionListener(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /**
     * 플레이어의 인터랙션 제한 (블록 파괴, 배치, 아이템 사용 등)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isSlave(player) || gameManager.isDead(player)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("당신은 현재 아무 것도 할 수 없습니다!", NamedTextColor.RED));
        }
    }

    /**
     * 노예의 블록 파괴 제한
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isSlave(player) || gameManager.isDead(player)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("당신은 현재 아무 것도 할 수 없습니다!", NamedTextColor.RED));
        }
    }

    /**
     * 노예의 블록 배치 제한
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isSlave(player) || gameManager.isDead(player)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("당신은 현재 아무 것도 할 수 없습니다!", NamedTextColor.RED));
        }
    }
}
