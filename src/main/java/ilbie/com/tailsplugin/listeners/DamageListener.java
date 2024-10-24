package ilbie.com.tailsplugin.listeners;

import ilbie.com.tailsplugin.main.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public class DamageListener implements Listener {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public DamageListener(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /**
     * 모든 피해를 막고, 넉백을 방지하는 이벤트 핸들러
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (gameManager.isDead(player)) {
            event.setCancelled(true);
            player.setVelocity(new Vector(0, 0, 0)); // 넉백 방지
            player.sendActionBar(Component.text("당신은 현재 부활 대기 중입니다!", NamedTextColor.RED));
        }
    }

    /**
     * 피해를 처리하는 이벤트 핸들러
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!gameManager.isGameRunning()) return;

        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            // 사망 대기 중인 플레이어는 피해를 입지 않음
            if (gameManager.isDead(victim)) {
                event.setCancelled(true);
                attacker.sendMessage(Component.text("당신은 이미 타겟 팀을 공격했습니다!", NamedTextColor.RED));
                return;
            }

            Team attackerTeam = gameManager.getTeam(attacker);
            Team victimTeam = gameManager.getTeam(victim);

            if (attackerTeam == null || victimTeam == null) {
                event.setCancelled(true);
                return;
            }

            // 노예 공격 제한 강화
            if (gameManager.isSlave(victim)) {
                Player master = gameManager.getMaster(victim);
                if (master != null && attacker.equals(master)) {
                    // 주인이 자신의 노예를 공격하는 경우 허용
                    return;
                } else {
                    // 주인이 아닌 다른 플레이어가 노예를 공격하려는 경우 취소
                    attacker.sendMessage(Component.text("노예를 공격할 수 없습니다!", NamedTextColor.RED));
                    event.setCancelled(true);
                    return;
                }
            }

            // 노예는 공격할 수 없음 (노예가 공격자가 되는 경우)
            if (gameManager.isSlave(attacker)) {
                // 노예가 공격하는 경우: 자신의 주인의 타겟 팀만 공격할 수 있도록 설정
                Player master = gameManager.getMaster(attacker);
                if (master != null) {
                    Team masterTargetTeam = gameManager.getTargetTeam(gameManager.getTeam(master));
                    if (masterTargetTeam != null && masterTargetTeam.equals(victimTeam)) {
                        // 주인의 타겟 팀 공격 허용
                        return;
                    }
                }
                attacker.sendMessage(Component.text("노예는 타겟 팀만 공격할 수 있습니다!", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }

            Team targetTeam = gameManager.getTargetTeam(attackerTeam);

            // 타겟 팀 외 공격 시 제한
            if (!victimTeam.equals(targetTeam)) {
                attacker.sendMessage(Component.text("당신의 타겟 팀만 공격할 수 있습니다!", NamedTextColor.RED));
                event.setCancelled(true);
            }
        }
    }
}
