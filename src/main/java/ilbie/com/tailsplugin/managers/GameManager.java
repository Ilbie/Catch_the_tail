package ilbie.com.tailsplugin.managers;

import ilbie.com.tailsplugin.TailsPlugin;
import ilbie.com.tailsplugin.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team; // 올바른 임포트
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

public class GameManager {

    private final TailsPlugin plugin;
    private boolean isGameRunning = false;

    private final Map<Team, Team> teamTargets = new HashMap<>();
    private final List<Team> teams = new ArrayList<>();
    private final Map<Player, Team> playerTeams = new HashMap<>();
    private final Map<Team, Player> teamLeaders = new HashMap<>(); // 팀장 저장
    private final Map<Player, Player> slaveMasters = new HashMap<>(); // 노예와 주인 관계
    private final Map<Player, BukkitRunnable> slaveTasks = new HashMap<>(); // 노예의 작업 관리
    private final Set<Player> slaves = new HashSet<>(); // 노예 목록

    // '죽은' 플레이어를 추적
    private final Set<Player> deadPlayers = new HashSet<>();

    // 플레이어의 카운트다운 작업을 추적
    private final Map<Player, BukkitRunnable> countdownTasks = new HashMap<>();

    public GameManager(TailsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 카운트다운 작업을 추가합니다.
     *
     * @param player 대상 플레이어
     * @param task   카운트다운 작업
     */
    public void addCountdownTask(Player player, BukkitRunnable task) {
        countdownTasks.put(player, task);
    }

    /**
     * 카운트다운 작업을 제거합니다.
     *
     * @param player 대상 플레이어
     */
    public void removeCountdownTask(Player player) {
        countdownTasks.remove(player);
    }

    /**
     * 게임을 시작합니다.
     */
    public void startGame() {
        if (isGameRunning) {
            Bukkit.broadcast(Component.text("게임이 이미 시작되었습니다.").color(NamedTextColor.RED));
            return;
        }

        isGameRunning = true;
        setupTeamsAndTargets();
        Bukkit.broadcast(Component.text("꼬리잡기 게임이 시작되었습니다!").color(NamedTextColor.GREEN));

        // 플레이어들에게 팀과 타겟 정보 알림
        informPlayers();
    }

    /**
     * 게임을 중지하고 모든 데이터를 초기화합니다.
     */
    public void stopGame() {
        if (!isGameRunning) {
            Bukkit.broadcast(Component.text("게임이 진행 중이지 않습니다.").color(NamedTextColor.RED));
            return;
        }

        isGameRunning = false;
        teamTargets.clear();
        teams.clear();
        playerTeams.clear();
        teamLeaders.clear();

        // 노예 작업 중지 및 상태 해제
        for (Player slave : new HashSet<>(slaveMasters.keySet())) {
            removeSlaveEffects(slave);
        }
        slaveMasters.clear();
        slaveTasks.clear();
        slaves.clear();
        deadPlayers.clear();

        // 카운트다운 작업 중지
        for (BukkitRunnable task : countdownTasks.values()) {
            task.cancel();
        }
        countdownTasks.clear();

        resetTeams();
        Bukkit.broadcast(Component.text("꼬리잡기 게임이 종료되었습니다!").color(NamedTextColor.RED));
    }

    /**
     * 게임이 진행 중인지 확인합니다.
     *
     * @return 게임 진행 여부
     */
    public boolean isGameRunning() {
        return isGameRunning;
    }

    /**
     * 특정 팀의 타겟 팀을 반환합니다.
     *
     * @param team 대상 팀
     * @return 타겟 팀
     */
    public Team getTargetTeam(Team team) {
        return teamTargets.get(team);
    }

    /**
     * 특정 팀의 타겟 팀을 설정합니다.
     *
     * @param team      대상 팀
     * @param newTarget 새로운 타겟 팀
     */
    public void setNewTarget(Team team, Team newTarget) {
        teamTargets.put(team, newTarget);
    }

    /**
     * 팀과 타겟을 설정하고 플레이어를 팀에 할당합니다.
     */
    private void setupTeamsAndTargets() {
        TeamManager teamManager = plugin.getTeamManager();
        if (teamManager == null) return;

        // 기존 팀 초기화
        resetTeams();

        // 온라인 플레이어 목록 가져오기
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        // 플레이어 수 체크
        if (players.size() < 2) {
            Bukkit.broadcast(Component.text("게임을 시작하기 위해서는 최소 2명의 플레이어가 필요합니다.").color(NamedTextColor.RED));
            isGameRunning = false;
            return;
        }

        // 플레이어 리스트 셔플하여 랜덤화
        Collections.shuffle(players);

        // 팀 생성 및 플레이어 할당
        teamManager.setupTeams(players);
        teams.addAll(teamManager.getTeams());

        // 타겟 설정 (순서대로 타겟 지정)
        for (int i = 0; i < teams.size(); i++) {
            Team currentTeam = teams.get(i);
            Team targetTeam = teams.get((i + 1) % teams.size());
            teamTargets.put(currentTeam, targetTeam);
        }

        // 모든 플레이어에게 팀 정보 전송
        informPlayers();
    }

    /**
     * 모든 팀을 초기화하고 메인 스코어보드로 복원합니다.
     */
    private void resetTeams() {
        TeamManager teamManager = plugin.getTeamManager();
        if (teamManager == null) return;
        teamManager.resetTeams();
    }

    /**
     * 현재 모든 팀 목록을 반환합니다.
     *
     * @return 팀 목록
     */
    public List<Team> getTeams() {
        return teams;
    }

    /**
     * 특정 플레이어의 팀을 반환합니다.
     *
     * @param player 대상 플레이어
     * @return 팀
     */
    public Team getTeam(Player player) {
        return playerTeams.get(player);
    }

    /**
     * 플레이어들에게 팀과 타겟 정보를 알립니다.
     */
    private void informPlayers() {
        TeamManager teamManager = plugin.getTeamManager();
        if (teamManager == null) return;

        for (Player player : playerTeams.keySet()) {
            Team playerTeam = getTeam(player);
            Team targetTeam = getTargetTeam(playerTeam);

            NamedTextColor teamColor = teamManager.getColor(playerTeam);
            NamedTextColor targetColor = teamManager.getColor(targetTeam);

            player.sendMessage(
                    Component.text("당신의 팀: ", NamedTextColor.YELLOW)
                            .append(Component.text(playerTeam.getName(), teamColor))
            );
            player.sendMessage(
                    Component.text("당신의 타겟 팀: ", NamedTextColor.YELLOW)
                            .append(Component.text(targetTeam.getName(), targetColor))
            );
        }
    }

    /**
     * 특정 팀을 제거합니다.
     *
     * @param team 제거할 팀
     */
    public void removeTeam(Team team) {
        teams.remove(team);
        for (Iterator<Map.Entry<Player, Team>> it = playerTeams.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Player, Team> entry = it.next();
            if (entry.getValue().equals(team)) {
                it.remove();
            }
        }
        teamLeaders.remove(team);
        teamTargets.remove(team);

        // isRegistered() 제거 및 대체
        if (team != null) {
            try {
                team.unregister();
            } catch (Exception e) {
                plugin.getLogger().severe("팀을 해제하는 중 오류 발생: " + e.getMessage());
            }
        }
    }

    /**
     * 특정 플레이어의 팀을 설정합니다.
     *
     * @param player 대상 플레이어
     * @param team   설정할 팀
     */
    public void setPlayerTeam(Player player, Team team) {
        playerTeams.put(player, team);
    }

    /**
     * 노예와 주인 관계를 설정합니다.
     *
     * @param slave  노예 플레이어
     * @param master 주인 플레이어
     */
    public void setSlaveMaster(Player slave, Player master) {
        slaveMasters.put(slave, master);
        slaves.add(slave);
        startSlaveTask(slave, master);
    }

    /**
     * 특정 노예의 주인을 반환합니다.
     *
     * @param slave 노예 플레이어
     * @return 주인 플레이어
     */
    public Player getMaster(Player slave) {
        return slaveMasters.get(slave);
    }

    /**
     * 노예의 작업을 중지하고 노예 상태를 해제합니다.
     *
     * @param slave 노예 플레이어
     */
    public void removeSlaveEffects(Player slave) {
        BukkitRunnable task = slaveTasks.remove(slave);
        if (task != null) {
            task.cancel();
        }
        slaveMasters.remove(slave);
        setSlave(slave, false);
        deadPlayers.remove(slave);

        // 카운트다운 작업이 존재하면 취소
        BukkitRunnable countdownTask = countdownTasks.remove(slave);
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }

    /**
     * 팀장의 플레이어를 반환합니다.
     *
     * @param team 대상 팀
     * @return 팀장 플레이어
     */
    public Player getTeamLeader(Team team) {
        return teamLeaders.get(team);
    }

    /**
     * 노예 플레이어를 설정하거나 해제합니다.
     *
     * @param player   대상 플레이어
     * @param isSlave  노예 여부
     */
    public void setSlave(Player player, boolean isSlave) {
        if (isSlave) {
            slaves.add(player);
        } else {
            slaves.remove(player);
        }
    }

    /**
     * 플레이어가 노예인지 확인합니다.
     *
     * @param player 대상 플레이어
     * @return 노예 여부
     */
    public boolean isSlave(Player player) {
        return slaves.contains(player);
    }

    /**
     * 플레이어가 '죽은' 상태인지 확인합니다.
     *
     * @param player 대상 플레이어
     * @return '죽은' 상태 여부
     */
    public boolean isDead(Player player) {
        return deadPlayers.contains(player);
    }

    /**
     * 플레이어를 '죽은' 상태로 설정합니다.
     *
     * @param player 대상 플레이어
     */
    public void setDead(Player player) {
        deadPlayers.add(player);
    }

    /**
     * 플레이어를 '죽은' 상태에서 해제합니다.
     *
     * @param player 대상 플레이어
     */
    public void setAlive(Player player) {
        deadPlayers.remove(player);
    }

    /**
     * 노예 작업을 시작합니다. (액션바 메시지, 파티클 효과, 나침반 업데이트)
     *
     * @param slave  노예 플레이어
     * @param master 주인 플레이어
     */
    private void startSlaveTask(Player slave, Player master) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!slave.isOnline() || !master.isOnline()) {
                    removeSlaveEffects(slave);
                    this.cancel();
                    return;
                }

                try {
                    // 주인과의 거리 계산
                    double distance = slave.getLocation().distance(master.getLocation());
                    String message = "주인과의 거리: " + String.format("%.1f", distance) + "M";
                    slave.sendActionBar(Component.text(message, NamedTextColor.YELLOW));

                    // 파티클 효과 보여주기
                    slave.spawnParticle(Particle.FLAME, master.getLocation(), 10, 0.5, 1, 0.5, 0);

                    // 나침반이 주인을 가리키도록 설정
                    slave.setCompassTarget(master.getLocation());
                } catch (Exception e) {
                    Bukkit.getLogger().severe("노예 작업 중 오류 발생: " + e.getMessage());
                    removeSlaveEffects(slave);
                    this.cancel();
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 20L); // 1초마다 업데이트

        slaveTasks.put(slave, task);
    }

    /**
     * 플레이어를 부활시킵니다.
     *
     * @param player          부활시킬 플레이어
     * @param keepInventory   인벤토리 유지 여부
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
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 50, 1, 1, 1, 0.1);
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

            // 팀 및 타겟 정보 재전송
            TeamManager teamManager = plugin.getTeamManager();
            if (teamManager == null) return;

            Team team = teamManager.getTeam(player);
            if (team != null) {
                Team targetTeam = getTargetTeam(team);
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
            setAlive(player);
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
        setDead(player);

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
                    removeSlaveEffects(slave);
                    this.cancel();
                    removeCountdownTask(slave);
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
                    removeCountdownTask(slave);
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L); // 1초마다 실행

        // 플레이어의 카운트다운 작업을 추적
        addCountdownTask(slave, countdownTask);
    }
}
