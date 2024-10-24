package ilbie.com.tailsplugin.listeners;

import ilbie.com.tailsplugin.main.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import ilbie.com.tailsplugin.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scoreboard.Team;

public class DeathListener implements Listener {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public DeathListener(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameManager.isGameRunning()) return;

        Player deceased = event.getEntity();
        Player killer = deceased.getKiller();

        // 사망 원인 확인
        EntityDamageEvent lastDamage = deceased.getLastDamageCause();

        // 인벤토리 유지 여부 결정
        boolean keepInventory = false;

        if (lastDamage == null || lastDamage.getCause() == DamageCause.SUICIDE) {
            // 자살한 경우
            keepInventory = true;

            // 부활 처리
            gameManager.resurrectPlayer(deceased, true);
            event.setKeepInventory(true);
            event.setDeathMessage(""); // deprecated, 대신 빈 문자열 설정
            event.setCancelled(true);
            return;
        } else if (killer != null && killer instanceof Player) {
            // 다른 플레이어에게 살해된 경우
            Team deceasedTeam = gameManager.getTeam(deceased);
            Team killerTeam = gameManager.getTeam(killer);

            if (deceasedTeam == null || killerTeam == null) return;

            // 노예는 주인을 만들 수 없음
            if (gameManager.getMaster(deceased) != null) return;

            // 노예화 로직
            for (Player member : Bukkit.getOnlinePlayers()) {
                if (gameManager.getTeam(member).equals(deceasedTeam)) {
                    killerTeam.addEntry(member.getName());
                    gameManager.setPlayerTeam(member, killerTeam);

                    // 노예와 주인 설정
                    gameManager.setSlaveMaster(member, killer);

                    member.sendMessage(Component.text("당신은 이제 " + killer.getName() + "의 노예가 되었습니다.", NamedTextColor.YELLOW));

                    // 인터랙션 불가능하게 설정
                    gameManager.setSlave(member, true);

                    // 나침반 지급
                    member.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.COMPASS));

                    // 부활 및 주인 근처로 텔레포트 (1분 후)
                    gameManager.handleDeath(member, killer);
                }
            }

            // 타겟 승계
            Team deceasedTarget = gameManager.getTargetTeam(deceasedTeam);
            gameManager.setNewTarget(killerTeam, deceasedTarget);

            // 팀 제거
            gameManager.removeTeam(deceasedTeam);

            // 게임 종료 조건 확인
            if (gameManager.getTeams().size() == 1) {
                Team winningTeam = gameManager.getTeams().get(0);
                NamedTextColor teamColor = ColorUtils.toNamedTextColor(winningTeam.getColor());

                Bukkit.broadcast(
                        Component.text("게임이 종료되었습니다! 승리 팀: ", NamedTextColor.GOLD)
                                .append(Component.text(winningTeam.getName(), teamColor))
                );
                gameManager.stopGame();
            } else {
                // 남은 팀들에게 새로운 타겟 정보 알림
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (gameManager.getMaster(player) != null) continue; // 노예는 제외
                    Team playerTeam = gameManager.getTeam(player);
                    if (playerTeam != null) {
                        Team targetTeam = gameManager.getTargetTeam(playerTeam);
                        NamedTextColor targetColor = ColorUtils.toNamedTextColor(targetTeam.getColor());

                        player.sendMessage(
                                Component.text("새로운 타겟 팀: ", NamedTextColor.YELLOW)
                                        .append(Component.text(targetTeam.getName(), targetColor))
                        );
                    }
                }
            }

            // 사망 메시지 숨기기
            event.setDeathMessage(""); // deprecated, 대신 빈 문자열 설정
            event.setCancelled(true);
        } else {
            // 기타 원인으로 사망한 경우 (낙사 등)
            // 부활 처리
            gameManager.resurrectPlayer(deceased, true);
            event.setKeepInventory(true);
            event.setDeathMessage(""); // deprecated, 대신 빈 문자열 설정
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        gameManager.handlePostRespawnTeleport(player);
    }

    /**
     * 부활 대기 중인 노예 플레이어가 피해를 입지 못하도록 방지합니다.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (gameManager.isDead(player)) {
            // 사망 대기 중인 노예는 피해를 입지 못하도록 방지
            event.setCancelled(true);
        }
    }

    /**
     * 부활 대기 중인 노예 플레이어가 공격하지 못하도록 방지합니다.
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isGameRunning()) return;

        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            // 부활 대기 중인 노예는 공격 불가
            if (plugin.getGameManager().isDead(attacker)) {
                attacker.sendMessage(Component.text("부활 대기 상태에서는 공격할 수 없습니다!", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }

            // 노예는 주인의 타겟을 공격할 수 있음
            if (plugin.getGameManager().isSlave(attacker)) {
                Player master = plugin.getGameManager().getMaster(attacker);
                if (master != null) {
                    Team masterTargetTeam = plugin.getGameManager().getTargetTeam(plugin.getGameManager().getTeam(master));
                    Team victimTeam = plugin.getGameManager().getTeam(victim);

                    if (masterTargetTeam != null && masterTargetTeam.equals(victimTeam)) {
                        // 주인의 타겟 팀을 공격할 수 있음
                        return;
                    }
                }
                attacker.sendMessage(Component.text("당신은 주인의 타겟 팀만 공격할 수 있습니다!", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
        }
    }
}


