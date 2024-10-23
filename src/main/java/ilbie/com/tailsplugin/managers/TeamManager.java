package ilbie.com.tailsplugin.managers;

import ilbie.com.tailsplugin.TailsPlugin;
import ilbie.com.tailsplugin.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team; // 올바른 임포트

// 추가된 임포트
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class TeamManager {

    private final TailsPlugin plugin;
    private final Scoreboard scoreboard;
    private final List<Team> teams = new ArrayList<>();

    public TeamManager(TailsPlugin plugin) {
        this.plugin = plugin;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    /**
     * 팀을 설정하고 플레이어를 할당합니다.
     *
     * @param players 온라인 플레이어 목록
     */
    public void setupTeams(List<Player> players) {
        // 팀 이름 및 색상 정의
        String[] teamNames = {"Red", "Orange", "Yellow", "Green", "Blue", "Indigo"};
        NamedTextColor[] teamColors = {
                NamedTextColor.RED,        // 빨강
                NamedTextColor.GOLD,       // 주황
                NamedTextColor.YELLOW,     // 노랑
                NamedTextColor.GREEN,      // 초록
                NamedTextColor.BLUE,       // 파랑
                NamedTextColor.DARK_PURPLE // 남색(인디고)
        };

        int teamCount = Math.min(players.size(), teamNames.length);

        // 팀 생성
        for (int i = 0; i < teamCount; i++) {
            Team team = scoreboard.getTeam(teamNames[i]);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamNames[i]);
                // Deprecated 메서드 대신 displayName을 설정
                team.displayName(Component.text(teamNames[i], teamColors[i]));
                // team.setColor(ChatColor.RED); // Deprecated, 제거
            }
            teams.add(team);
        }

        // 플레이어를 팀에 할당
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Team team = teams.get(i % teams.size());
            team.addEntry(player.getName());
            plugin.getGameManager().setPlayerTeam(player, team);
        }
    }

    /**
     * 모든 팀을 초기화하고 메인 스코어보드로 복원합니다.
     */
    public void resetTeams() {
        for (Team team : teams) {
            if (team != null) { // isRegistered() 대신 null 체크
                try {
                    team.unregister();
                } catch (Exception e) {
                    plugin.getLogger().severe("팀을 해제하는 중 오류 발생: " + e.getMessage());
                }
            }
        }
        teams.clear();
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
        for (Team team : teams) {
            if (team.hasEntry(player.getName())) {
                return team;
            }
        }
        return null;
    }

    /**
     * 팀의 색상을 반환합니다.
     *
     * @param team 대상 팀
     * @return 팀 색상
     */
    public NamedTextColor getColor(Team team) {
        // 팀의 이름에 따라 색상을 반환
        String teamName = team.getName().toLowerCase();
        switch (teamName) {
            case "red":
                return NamedTextColor.RED;
            case "orange":
                return NamedTextColor.GOLD;
            case "yellow":
                return NamedTextColor.YELLOW;
            case "green":
                return NamedTextColor.GREEN;
            case "blue":
                return NamedTextColor.BLUE;
            case "indigo":
                return NamedTextColor.DARK_PURPLE;
            default:
                return NamedTextColor.WHITE;
        }
    }
}
