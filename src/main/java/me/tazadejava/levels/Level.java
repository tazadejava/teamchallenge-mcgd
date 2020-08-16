package me.tazadejava.levels;

import me.tazadejava.main.TeamChallengeMCGD;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Holds the general structure for a challenge level.
 */
public abstract class Level implements Listener {

    protected TeamChallengeMCGD plugin;
    protected World currentWorld;

    protected boolean startOnEnable = true;
    protected boolean isLevelInProgress = false;

    public Level(TeamChallengeMCGD plugin, World currentWorld) {
        this.plugin = plugin;
        this.currentWorld = currentWorld;
    }

    public Location getSpawnPoint() {
        return new Location(currentWorld, 0.5, 50, 0.5);
    }

    public void startLevel() {
        isLevelInProgress = true;
    }

    public void endLevel() {
        isLevelInProgress = false;
    }

    public abstract boolean isVictory();

    public boolean isLevelInProgress() {
        return isLevelInProgress;
    }

    public boolean startOnEnable() {
        return startOnEnable;
    }

    public boolean handleCommand(CommandSender sender, String[] args) {return false;}

    public void onDisable() {}

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHunger(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);
    }
}
