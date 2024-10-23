package ilbie.com.tailsplugin.listeners;

import ilbie.com.tailsplugin.TailsPlugin;
import ilbie.com.tailsplugin.managers.GameManager;
import ilbie.com.tailsplugin.managers.TeamManager;
import ilbie.com.tailsplugin.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scoreboard.Team; // 올바른 임포트

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerDeathListener implements Listener {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public PlayerDeathListener(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /**
     * 플레이어 사망 이벤트 처리
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameManager.isGameRunning()) return;

        Player deceased = event.getEntity();
        Player killer = deceased.getKiller();

        // 사망 원인 확인
        org.bukkit.event.entity.EntityDamageEvent lastDamage = deceased.getLastDamageCause();

        // 사망 원인이 null이거나 자살인 경우
        if (lastDamage == null || lastDamage.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.SUICIDE) {
            // 자살한 경우
            gameManager.resurrectPlayer(deceased, true);
            event.setKeepInventory(true);
            event.setDeathMessage(""); // 수정: Component.text("") → ""
            event.setDroppedExp(0); // 수정: setDroppedExperience(int) → setDroppedExp(int)
            event.getDrops().clear(); // 수정: drops(new ArrayList<>()) → getDrops().clear()
            return;
        }

        if (killer != null && killer instanceof Player) {
            TeamManager teamManager = plugin.getTeamManager();
            if (teamManager == null) return;

            Team deceasedTeam = teamManager.getTeam(deceased);
            Team killerTeam = teamManager.getTeam(killer);

            if (deceasedTeam == null || killerTeam == null) {
                event.setCancelled(true);
                return;
            }

            // 노예는 주인을 만들 수 없음
            if (gameManager.getMaster(deceased) != null) return;

            // 노예화 로직
            for (Player member : Bukkit.getOnlinePlayers()) {
                if (teamManager.getTeam(member).equals(deceasedTeam)) {
                    killerTeam.addEntry(member.getName());
                    gameManager.setPlayerTeam(member, killerTeam);

                    // 노예와 주인 설정
                    gameManager.setSlaveMaster(member, killer);

                    member.sendMessage(ChatUtil.prefix().append(
                            Component.text("당신은 이제 " + killer.getName() + "의 노예가 되었습니다.", NamedTextColor.YELLOW)
                    ));

                    // 인터랙션 불가능하게 설정
                    gameManager.setSlave(member, true);

                    // 나침반 지급
                    member.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS));

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
                NamedTextColor teamColor = teamManager.getColor(winningTeam);

                Bukkit.broadcast(
                        ChatUtil.prefix().append(
                                Component.text("게임이 종료되었습니다! 승리 팀: ", NamedTextColor.GOLD)
                                        .append(Component.text(winningTeam.getName(), teamColor))
                        )
                );
                gameManager.stopGame();
            } else {
                // 남은 팀들에게 새로운 타겟 정보 알림
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (gameManager.getMaster(player) != null) continue; // 노예는 제외
                    Team playerTeam = teamManager.getTeam(player);
                    if (playerTeam != null) {
                        Team targetTeam = gameManager.getTargetTeam(playerTeam);
                        NamedTextColor targetColor = teamManager.getColor(targetTeam);

                        player.sendMessage(ChatUtil.prefix().append(
                                Component.text("새로운 타겟 팀: ", NamedTextColor.YELLOW)
                                        .append(Component.text(targetTeam.getName(), targetColor))
                        ));
                    }
                }
            }

            // 사망 메시지 숨기기
            event.setDeathMessage(""); // 수정
            event.setDroppedExp(0); // 수정
            event.getDrops().clear(); // 아이템 드랍 방지
            return;
        } else {
            // 기타 원인으로 사망한 경우 (낙사 등)
            // 부활 처리
            gameManager.resurrectPlayer(deceased, true);
            event.setKeepInventory(true);
            event.setDeathMessage(""); // 수정
            event.setDroppedExp(0); // 수정
            event.getDrops().clear(); // 아이템 드랍 방지
            return;
        }
    }
}
