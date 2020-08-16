package me.tazadejava.main;

import me.tazadejava.levels.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Binds the challenge command. Used for level 2 to start the challenge.
 */
public class ChallengeCommandHandler implements CommandExecutor {

    private Level currentLevel;

    public ChallengeCommandHandler(Level currentLevel) {
        this.currentLevel = currentLevel;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        return currentLevel.handleCommand(commandSender, args);
    }
}
