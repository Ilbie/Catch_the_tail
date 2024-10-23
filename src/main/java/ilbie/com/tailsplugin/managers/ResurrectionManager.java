package ilbie.com.tailsplugin.managers;

import ilbie.com.tailsplugin.TailsPlugin;
import ilbie.com.tailsplugin.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team; // 추가된 임포트
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ResurrectionManager {

    private final TailsPlugin plugin;
    private final GameManager gameManager;

    public ResurrectionManager(TailsPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /**
     * 플레이어를 부활시키는 메서드 (부활 시 모든 상호작용 가능)
     *
     * @param player        부활시킬 플레이어
     * @param keepInventory 인벤토리 유지 여부
     */
    public void resurrectPlayer(Player player, boolean keepInventory) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.spigot().respawn();

            if (keepInventory) {
                player.sendMessage(ChatUtil.prefix().append(
                        Component.text("당신은 부활했습니다!", NamedTextColor.GREEN)
                ));
            } else {
                player.getInventory().clear();
                player.sendMessage(ChatUtil.prefix().append(
                        Component.text("당신은 부활했지만 아이템을 잃었습니다.", NamedTextColor.RED)
                ));
            }

            // 체력 및 상태 회복
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20);

            // 불사의 토템 효과 (재생 및 흡수)
            if (keepInventory) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1)); // 45초간 재생
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1)); // 5초간 흡수
            }

            // 부활 시 파티클 및 사운드 효과 적용 (불사의 토템과 유사)
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 50, 1, 1, 1, 0.1); // 수정된 파티클
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

            // 팀 및 타겟 정보 재전송
            TeamManager teamManager = plugin.getTeamManager();
            if (teamManager == null) return;

            Team team = teamManager.getTeam(player);
            if (team != null) {
                Team targetTeam = gameManager.getTargetTeam(team);
                NamedTextColor teamColor = teamManager.getColor(team);
                NamedTextColor targetColor = teamManager.getColor(targetTeam);

                player.sendMessage(
                        Component.text("당신의 팀: ", NamedTextColor.YELLOW)
                                .append(Component.text(team.getName(), teamColor))
                );
                player.sendMessage(
                        Component.text("당신의 타겟 팀: ", NamedTextColor.YELLOW)
                                .append(Component.text(targetTeam.getName(), targetColor))
                );
            }

            // '죽은' 상태 해제
            gameManager.setAlive(player);
        }, 1L); // 다음 틱에 실행
    }

    /**
     * 플레이어를 '죽은' 상태로 설정하고 1분 후 주인에게 텔레포트 및 부활을 스케줄링합니다.
     *
     * @param player  죽은 플레이어
     * @param master 주인 플레이어
     */
    public void handleDeath(Player player, Player master) {
        // 플레이어를 '죽은' 상태로 설정
        gameManager.setDead(player);

        // 텔레포트 및 부활을 1분 후에 처리
        teleportToMaster(player, master);
    }

    /**
     * 플레이어를 주인 근처로 텔레포트하고 부활을 준비합니다.
     *
     * @param slave  노예 플레이어
     * @param master 주인 플레이어
     */
    private void teleportToMaster(Player slave, Player master) {
        // 플레이어를 사망 위치로 텔레포트 (현재 위치 유지)
        Location deathLocation = slave.getLocation();
        slave.teleport(deathLocation);
        slave.sendMessage(ChatUtil.prefix().append(
                Component.text("당신은 사망 위치에 텔레포트되었습니다.", NamedTextColor.GREEN)
        ));

        // 플레이어를 주인에게 1분 후 텔레포트 및 부활시키는 작업 시작
        BukkitRunnable countdownTask = new BukkitRunnable() {
            int timeLeft = 60; // 60초

            @Override
            public void run() {
                if (!slave.isOnline() || !master.isOnline()) {
                    gameManager.removeSlaveEffects(slave);
                    this.cancel();
                    gameManager.removeCountdownTask(slave);
                    return;
                }

                if (timeLeft > 0) {
                    // 남은 시간을 액션바로 표시
                    String message = "부활까지 남은 시간: " + timeLeft + "초";
                    slave.sendActionBar(Component.text(message, NamedTextColor.YELLOW));
                    timeLeft--;
                } else {
                    // 1분 후 주인에게 텔레포트 및 부활
                    Location masterLocation = master.getLocation();
                    Location teleportLocation = masterLocation.clone().add(2, 0, 2); // 주인 근처로 이동
                    slave.teleport(teleportLocation);
                    slave.sendMessage(ChatUtil.prefix().append(
                            Component.text("주인님 근처로 텔레포트되었습니다.", NamedTextColor.GREEN)
                    ));

                    // 부활 처리
                    resurrectPlayer(slave, false);

                    // 카운트다운 작업 종료
                    this.cancel();
                    gameManager.removeCountdownTask(slave);
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L); // 1초마다 실행

        // 플레이어의 카운트다운 작업을 추적
        gameManager.addCountdownTask(slave, countdownTask);
    }
}
