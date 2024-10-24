package ilbie.com.tailsplugin.utils;

import org.bukkit.scoreboard.Team;

public class TeamUtils {

    /**
     * 특정 팀의 색상을 반환합니다.
     *
     * @param team 대상 팀
     * @return 팀 색상
     */
    public static String getTeamColor(Team team) {
        // 팀 색상에 따라 문자열 반환
        switch (team.getName().toLowerCase()) {
            case "red":
                return "§c"; // 빨강
            case "orange":
                return "§6"; // 주황
            case "yellow":
                return "§e"; // 노랑
            case "green":
                return "§a"; // 초록
            case "blue":
                return "§9"; // 파랑
            case "indigo":
                return "§5"; // 남색
            default:
                return "§f"; // 기본 흰색
        }
    }

    // 추가적인 팀 관련 유틸리티 메서드 구현 가능
}
