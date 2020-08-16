package me.tazadejava.main;

import me.tazadejava.levels.Level;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles general level restrictions.
 */
public class GeneralLevelListener implements Listener {

    private TeamChallengeMCGD plugin;
    private Level currentLevel;

    private int entitiesSpawned;

    public GeneralLevelListener(TeamChallengeMCGD plugin, Level currentLevel) {
        this.plugin = plugin;
        this.currentLevel = currentLevel;

        entitiesSpawned = 0;

        new BukkitRunnable() {
            @Override
            public void run() {
                if(currentLevel.isLevelInProgress()) {
                    if(currentLevel.isVictory()) {
                        activateLevelWinSequence();
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 10L);
    }

    private void activateLevelWinSequence() {
        new BukkitRunnable() {
            @Override
            public void run() {
                currentLevel.endLevel();
            }
        }.runTaskLater(plugin, 0L);

        plugin.markAsWinPlugin();
        Bukkit.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + "Your team beat the challenge!");
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 0.5f);

                    Firework firework = (Firework) p.getLocation().getWorld().spawnEntity(p.getLocation().clone().add(0, 3, 0), EntityType.FIREWORK);
                    FireworkMeta meta = firework.getFireworkMeta();
                    meta.setPower(2);
                    meta.addEffect(FireworkEffect.builder().flicker(true).trail(true).withColor(Color.YELLOW).build());
                    firework.setFireworkMeta(meta);
                }

                count++;
                if (count >= 3) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if(!plugin.isAllowedOnServer(event.getPlayer())) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, "You cannot join another group's server!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if(plugin.getRealName(event.getPlayer()).equals("")) {
            event.setJoinMessage(ChatColor.DARK_GREEN + "+ " + ChatColor.GRAY + event.getPlayer().getName());
        } else {
            event.setJoinMessage(ChatColor.DARK_GREEN + "+ " + ChatColor.GRAY + event.getPlayer().getName() + " (" + plugin.getRealName(event.getPlayer()) + ")");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setFormat(ChatColor.BLUE + event.getPlayer().getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + event.getMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage(ChatColor.DARK_RED + "- " + ChatColor.GRAY + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if(currentLevel.getSpawnPoint() != null) {
            event.setRespawnLocation(currentLevel.getSpawnPoint());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            LivingEntity ent = (LivingEntity) event.getEntity();

            for(PotionEffect potionEffect : ent.getActivePotionEffects()) {
                ent.removePotionEffect(potionEffect.getType());
            }

            event.setCancelled(true);
            ent.setHealth(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        if(!event.getCommand().isEmpty()) {
            if (event.getCommand().toLowerCase().startsWith("op ") || event.getCommand().toLowerCase().startsWith("gamemode ")) {
                event.setCancelled(true);
                event.getSender().sendMessage("You can't do that!");
            }
        }
    }
}
