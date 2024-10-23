package ilbie.com.tailsplugin.listeners;

import ilbie.com.tailsplugin.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GameListener implements Listener {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public GameListener(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /**
     * 플레이어가 피해를 입을 때, '죽은' 상태인 경우 피해를 막습니다.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (gameManager.isDead(player)) {
            event.setCancelled(true);
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0)); // 넉백 방지
            player.sendActionBar(Component.text("당신은 현재 부활 대기 중입니다!", NamedTextColor.RED));
        }
    }
}
