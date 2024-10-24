package ilbie.com.tailsplugin.managers;

import ilbie.com.tailsplugin.main.TailsPlugin;
import ilbie.com.tailsplugin.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

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

    private final Scoreboard scoreboard;

    // 플레이어가 부활 후 텔레포트해야 할 주인 정보를 저장
    private final Map<Player, Player> pendingTeleports = new HashMap<>();

    // '죽은' 플레이어를 추적
    private final Set<Player> deadPlayers = new HashSet<>();

    // 플레이어의 카운트다운 작업을 추적
    private final Map<Player, BukkitRunnable> countdownTasks = new HashMap<>();

    // 팀의 색상을 저장하는 맵
    private final Map<Team, NamedTextColor> teamColorsMap = new HashMap<>();

    public GameManager(TailsPlugin plugin) {
        this.plugin = plugin;
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager != null) {
            this.scoreboard = scoreboardManager.getNewScoreboard();
        } else {
            throw new IllegalStateException("ScoreboardManager is null!");
        }
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

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("꼬리잡기 게임이 시작되었습니다!");

            // 타이틀 메시지
            player.sendTitle("게임 시작!", "타겟 팀을 잡으세요!", 10, 70, 20);

            // 탭 리스트 및 이름 설정
            Team playerTeam = playerTeams.get(player);
            if (playerTeam != null) {
                NamedTextColor teamColor = teamColorsMap.getOrDefault(playerTeam, NamedTextColor.WHITE);
                player.displayName(Component.text("[" + getColorNameInKorean(teamColor) + "] " + player.getName(), teamColor));
                player.playerListName(Component.text("[" + getColorNameInKorean(teamColor) + "] " + player.getName(), teamColor));
            } else {
                // 팀이 없을 경우 기본 값으로 설정
                player.displayName(Component.text(player.getName(), NamedTextColor.WHITE));
                player.playerListName(Component.text(player.getName(), NamedTextColor.WHITE));
            }
        }

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

        // 게임이 종료될 때 승리한 팀 확인
        if (teams.size() == 1) {
            Team winningTeam = teams.get(0);
            NamedTextColor teamColor = teamColorsMap.getOrDefault(winningTeam, NamedTextColor.WHITE);

            // 승리한 팀의 모든 플레이어에게 메시지와 이름표 색상 업데이트
            for (String playerName : winningTeam.getEntries()) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null) {
                    player.sendMessage("축하합니다! 당신의 팀이 승리했습니다!");

                    // 타이틀 메시지
                    player.sendTitle("승리!", "축하합니다! 당신의 팀이 승리했습니다!", 10, 70, 20);

                    // 이름표 및 탭 리스트 이름 색상 업데이트
                    player.displayName(Component.text("[" + getColorNameInKorean(teamColor) + "] " + player.getName(), teamColor));
                    player.playerListName(Component.text("[" + getColorNameInKorean(teamColor) + "] " + player.getName(), teamColor));
                }
            }
        }

        // 모든 플레이어의 탭 리스트 및 이름 색상을 초기화
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.displayName(Component.text(player.getName(), NamedTextColor.WHITE));
            player.playerListName(Component.text(player.getName(), NamedTextColor.WHITE));
        }

        // 기존 상태 초기화
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
        // 기존 팀 삭제
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }

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
        String[] teamNames = {"빨강", "주황", "노랑", "초록", "파랑", "남색"};
        NamedTextColor[] teamColors = {
                NamedTextColor.RED,           // 빨강
                NamedTextColor.GOLD,          // 주황
                NamedTextColor.YELLOW,        // 노랑
                NamedTextColor.GREEN,         // 초록
                NamedTextColor.BLUE,          // 파랑
                NamedTextColor.DARK_PURPLE    // 남색(인디고)
        };

        int teamCount = Math.min(players.size(), teamNames.length);

        for (int i = 0; i < teamCount; i++) {
            Team team = scoreboard.registerNewTeam(teamNames[i]);

            // 팀 색상 설정
            NamedTextColor teamColor = teamColors[i];
            teamColorsMap.put(team, teamColor);

            // 팀 프리픽스 설정 (색상과 함께 팀 이름 표시)
            team.prefix(Component.text("[" + team.getName() + "] ").color(teamColor));

            teams.add(team);
        }

        // 플레이어를 팀에 할당하고 팀장 지정
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Team team = teams.get(i % teams.size());
            team.addEntry(player.getName());
            playerTeams.put(player, team);

            // 팀의 첫 번째 플레이어를 팀장으로 지정
            if (!teamLeaders.containsKey(team)) {
                teamLeaders.put(team, player);
            }
        }

        // 타겟 설정 (순서대로 타겟 지정)
        for (int i = 0; i < teams.size(); i++) {
            Team currentTeam = teams.get(i);
            Team targetTeam = teams.get((i + 1) % teams.size());
            teamTargets.put(currentTeam, targetTeam);
        }

        // 모든 플레이어에게 새로운 스코어보드 적용
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    /**
     * 모든 팀을 초기화하고 메인 스코어보드로 복원합니다.
     */
    public void resetTeams() {
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }

        // 메인 스코어보드로 복원
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
    public void resetGameData() {
        // 진행 중인 게임 상태 초기화
        isGameRunning = false;

        // 죽은 플레이어 및 카운트다운 작업 초기화
        deadPlayers.clear();
        countdownTasks.clear();

        // 노예 및 작업 초기화
        slaveMasters.clear();
        slaveTasks.clear();
        pendingTeleports.clear();

        // 기타 게임 관련 데이터 초기화
        teams.clear();
        teamTargets.clear();
        playerTeams.clear();
        teamLeaders.clear();
        slaves.clear();
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
        for (Player player : playerTeams.keySet()) {
            Team playerTeam = getTeam(player);
            Team targetTeam = getTargetTeam(playerTeam);

            // 플레이어 팀 색상 가져오기
            NamedTextColor playerTeamColor = teamColorsMap.getOrDefault(playerTeam, NamedTextColor.WHITE);
            NamedTextColor targetTeamColor = teamColorsMap.getOrDefault(targetTeam, NamedTextColor.WHITE);

            // 팀 이름과 타겟 팀 이름에 각각 색상 적용
            String playerTeamColorName = getColorNameInKorean(playerTeamColor);
            String targetTeamColorName = getColorNameInKorean(targetTeamColor);

            // 플레이어에게 자신의 팀과 타겟 팀 정보 표시
            player.sendMessage(
                    Component.text("당신의 팀: ", NamedTextColor.YELLOW)
                            .append(Component.text(playerTeamColorName, playerTeamColor))
            );
            player.sendMessage(
                    Component.text("당신의 타겟 팀: ", NamedTextColor.YELLOW)
                            .append(Component.text(targetTeamColorName, targetTeamColor))
            );
        }
    }
    private String getColorNameInKorean(NamedTextColor color) {
        if (color == NamedTextColor.RED) {
            return "빨강";
        } else if (color == NamedTextColor.GOLD) {
            return "주황";
        } else if (color == NamedTextColor.YELLOW) {
            return "노랑";
        } else if (color == NamedTextColor.GREEN) {
            return "초록";
        } else if (color == NamedTextColor.BLUE) {
            return "파랑";
        } else if (color == NamedTextColor.DARK_PURPLE) {
            return "보라";
        } else if (color == NamedTextColor.WHITE) {
            return "하양";
        } else if (color == NamedTextColor.BLACK) {
            return "검정";
        } else if (color == NamedTextColor.GRAY) {
            return "회색";
        } else if (color == NamedTextColor.DARK_AQUA) {
            return "청록";
        } else if (color == NamedTextColor.DARK_GREEN) {
            return "짙은 초록";
        } else if (color == NamedTextColor.LIGHT_PURPLE) {
            return "연보라";
        } else if (color == NamedTextColor.AQUA) {
            return "하늘색";
        } else if (color == NamedTextColor.DARK_RED) {
            return "짙은 빨강";
        } else if (color == NamedTextColor.DARK_GRAY) {
            return "짙은 회색";
        } else {
            return "알 수 없는 색";
        }
    }
    /**
     * 특정 팀을 제거합니다.
     *
     * @param team 제거할 팀
     */
    public void removeTeam(Team team) {
        teams.remove(team);
        teamColorsMap.remove(team);
        for (Iterator<Map.Entry<Player, Team>> it = playerTeams.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Player, Team> entry = it.next();
            if (entry.getValue().equals(team)) {
                it.remove();
            }
        }
        teamLeaders.remove(team);
        team.unregister();
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
        pendingTeleports.remove(slave);
        deadPlayers.remove(slave);

        // 카운트다운 작업이 존재하면 취소
        BukkitRunnable countdownTask = countdownTasks.remove(slave);
        if (countdownTask != null) {
            countdownTask.cancel();
        }

        // 부활 대기 효과 제거
        removeResurrectionEffects(slave);
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

                // 주인과의 거리 계산
                double distance = slave.getLocation().distance(master.getLocation());
                String message = "주인과의 거리: " + String.format("%.1f", distance) + "M";
                slave.sendActionBar(Component.text(message, NamedTextColor.YELLOW));

                // 파티클 효과 보여주기
                slave.spawnParticle(Particle.FLAME, master.getLocation(), 10, 0.5, 1, 0.5, 0);

                // 나침반이 주인을 가리키도록 설정
                slave.setCompassTarget(master.getLocation());
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
                player.sendMessage(Component.text("당신은 부활했습니다!").color(NamedTextColor.GREEN));
            } else {
                player.getInventory().clear();
                player.sendMessage(Component.text("당신은 부활했지만 아이템을 잃었습니다.", NamedTextColor.RED));
            }

            // 체력 및 상태 회복
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20);

            // 불사의 토템 효과 (재생 및 흡수)
            if (keepInventory) {
                PotionEffect regeneration = new PotionEffect(
                        PotionEffectType.REGENERATION, 900, 1, false, false, false
                );
                player.addPotionEffect(regeneration);
                PotionEffect absorption = new PotionEffect(
                        PotionEffectType.ABSORPTION, 100, 1, false, false, false
                );
                player.addPotionEffect(absorption);
            }
            // 부활 시 파티클 및 사운드 효과 적용 (불사의 토템과 유사)
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 50, 1, 1, 1, 0.1);
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

            // 팀 및 타겟 정보 재전송
            Team playerTeam = getTeam(player);
            if (playerTeam != null) {
                Team targetTeam = getTargetTeam(playerTeam);
                NamedTextColor teamColor = teamColorsMap.getOrDefault(playerTeam, NamedTextColor.WHITE);
                NamedTextColor targetColor = teamColorsMap.getOrDefault(targetTeam, NamedTextColor.WHITE);

                player.sendMessage(
                        Component.text("당신의 팀: ", NamedTextColor.YELLOW)
                                .append(Component.text(playerTeam.getName(), teamColor))
                );
                player.sendMessage(
                        Component.text("당신의 타겟 팀: ", NamedTextColor.YELLOW)
                                .append(Component.text(targetTeam.getName(), targetColor))
                );
            }

            // 노예의 무적과 투명화 효과 제거
            removeResurrectionEffects(player);

            // 상호작용 재활성화는 각 리스너에서 처리됩니다.
        }, 1L); // 다음 틱에 실행
    }

    /**
     * 플레이어가 죽었을 때 부활을 처리하는 메서드
     *
     * @param player  죽은 플레이어
     * @param master  주인 플레이어
     */
    public void handleDeath(Player player, Player master) {
        // 플레이어가 이미 부활 대기 상태인 경우
        if (deadPlayers.contains(player)) {
            // 기존 카운트다운 작업 취소
            BukkitRunnable existingTask = countdownTasks.get(player);
            if (existingTask != null) {
                existingTask.cancel();
            }
        }

        // 플레이어를 '죽은' 상태로 설정
        setDead(player);

        // 무적 및 투명화 효과 적용
        applyResurrectionEffects(player);

        // 1분 후 부활과 텔레포트를 스케줄링
        teleportToMaster(player, master);

        // 노예로 죽었을 때 항상 다시 부활
        if (isSlave(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                resurrectPlayer(player, false); // 부활
            }, 1200L); // 60초 후 부활
        }
    }
    /**
     * 플레이어를 주인 근처로 텔레포트하고 부활을 준비합니다.
     *
     * @param slave  노예 플레이어
     * @param master 주인 플레이어
     */
    private void teleportToMaster(Player slave, Player master) {
        // 플레이어를 사망 위치로 텔레포트
        Location deathLocation = slave.getLocation();
        slave.teleport(deathLocation);
        slave.sendMessage(Component.text("당신은 사망 위치에 텔레포트되었습니다.", NamedTextColor.GREEN));

        // 플레이어를 주인에게 1분 후 텔레포트 및 부활시키는 작업 시작
        BukkitRunnable countdownTask = new BukkitRunnable() {
            int timeLeft = 60; // 60초

            @Override
            public void run() {
                if (!slave.isOnline() || !master.isOnline()) {
                    removeSlaveEffects(slave);
                    this.cancel();
                    countdownTasks.remove(slave);
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
                    slave.sendMessage(Component.text("주인님 근처로 텔레포트되었습니다.", NamedTextColor.GREEN));

                    // 부활 처리
                    resurrectPlayer(slave, false);

                    // '죽은' 상태 해제
                    setAlive(slave);

                    // 카운트다운 작업 종료
                    this.cancel();
                    countdownTasks.remove(slave);
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L); // 1초마다 실행

        // 플레이어의 카운트다운 작업을 추적
        countdownTasks.put(slave, countdownTask);
    }

    /**
     * 플레이어의 부활 후 텔레포트를 처리합니다.
     *
     * @param player 부활한 플레이어
     */
    public void handlePostRespawnTeleport(Player player) {
        if (pendingTeleports.containsKey(player)) {
            Player master = pendingTeleports.get(player);
            if (master != null && master.isOnline()) {
                // 이미 teleportToMaster에서 처리하므로 여기서는 필요 없음
                // 만약 별도의 처리가 필요하다면 추가
            }
            pendingTeleports.remove(player);
        }
    }

    /**
     * 노예 플레이어에게 무적과 투명화 효과를 적용합니다.
     *
     * @param player 대상 플레이어
     */
    public void applyResurrectionEffects(Player player) {
        // 기존 포션 효과 제거 후 재적용
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        PotionEffect resistance = new PotionEffect(
                PotionEffectType.RESISTANCE, 1200, 255, false, false, false
        );
        player.addPotionEffect(resistance); // 60초 동안 무적

        PotionEffect invisibility = new PotionEffect(
                PotionEffectType.INVISIBILITY, 1200, 1, false, false, false
        );
        player.addPotionEffect(invisibility); // 60초 동안 투명화
    }


    /**
     * 노예 플레이어의 무적과 투명화 효과를 제거합니다.
     *
     * @param player 대상 플레이어
     */
    public void removeResurrectionEffects(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }
}
