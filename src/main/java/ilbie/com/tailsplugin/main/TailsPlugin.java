package ilbie.com.tailsplugin.main;

import ilbie.com.tailsplugin.commands.TailsCommand;
import ilbie.com.tailsplugin.commands.TailsTabCompleter;
import ilbie.com.tailsplugin.listeners.*;
import ilbie.com.tailsplugin.managers.GameManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TailsPlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        gameManager = new GameManager(this);

        // 명령어 등록
        TailsCommand tailsCommand = new TailsCommand(this);
        this.getCommand("tails").setExecutor(tailsCommand);
        this.getCommand("tails").setTabCompleter(new TailsTabCompleter());

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new QuitListener(this), this);
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
