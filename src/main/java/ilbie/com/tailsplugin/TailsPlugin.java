package ilbie.com.tailsplugin;

import ilbie.com.tailsplugin.listeners.GameListener;
import ilbie.com.tailsplugin.listeners.PlayerDeathListener;
import ilbie.com.tailsplugin.managers.GameManager;
import ilbie.com.tailsplugin.managers.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TailsPlugin extends JavaPlugin {

    private GameManager gameManager;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        // 매니저 초기화
        gameManager = new GameManager(this);
        teamManager = new TeamManager(this);

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        // 기타 초기화 작업...
        getLogger().info("TailsPlugin이 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        // 플러그인 비활성화 시 처리할 작업...
        getLogger().info("TailsPlugin이 비활성화되었습니다.");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}
