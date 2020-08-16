package me.tazadejava.levels;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.tazadejava.main.TeamChallengeMCGD;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileReader;

public class Level5 extends Level {

    public Location[] sheepLocations;

    public boolean isVictory = false;
    public boolean hasKilledVindicators = false;

    private Gson gson = new Gson();
    private long lastModifiedTime;

    private long lastVindicatorDeath;

    public Level5(TeamChallengeMCGD plugin, World currentWorld) {
        super(plugin, currentWorld);

        sheepLocations = new Location[] {new Location(currentWorld, -3901, 50, 2000), new Location(currentWorld, 2951, 50, -300)};
    }

    @Override
    public boolean isVictory() {
        return isVictory;
    }

    @Override
    public void startLevel() {
        super.startLevel();

        isVictory = false;
        hasKilledVindicators = false;
        lastVindicatorDeath = -1;

        new BukkitRunnable() {
            @Override
            public void run() {
                File answerFile = new File(plugin.getDataFolder().getParentFile().getAbsolutePath() + "/TeamPlugin/solution.json");

                if(answerFile.exists()) {
                    if(answerFile.lastModified() != lastModifiedTime) {
                        Bukkit.broadcastMessage(ChatColor.GRAY + "Processing your solution.json file...");
                        lastModifiedTime = answerFile.lastModified();
                        try {
                            FileReader reader = new FileReader(answerFile);

                            JsonObject data = gson.fromJson(reader, JsonObject.class);

                            if(!data.has("sheep")) {
                                Bukkit.broadcastMessage(ChatColor.RED + "Your JSON file is missing one or more attributes!");
                                return;
                            }
                            if(!data.has("action")) {
                                Bukkit.broadcastMessage(ChatColor.RED + "Your JSON file is missing one or more attributes!");
                                return;
                            }
                            if(!data.has("invincible")) {
                                Bukkit.broadcastMessage(ChatColor.RED + "Your JSON file is missing one or more attributes!");
                                return;
                            }

                            if(data.get("sheep").getAsInt() != 10) {
                                Bukkit.broadcastMessage(ChatColor.RED + "Your sheep value is incorrect!");
                            }

                            if(!data.get("action").getAsString().equals("explode")) {
                                Bukkit.broadcastMessage(ChatColor.RED + "Your sheep value is incorrect!");
                            }

                            if(data.get("invincible").getAsBoolean()) {
                                Bukkit.broadcastMessage(ChatColor.RED + "Your sheep value is incorrect!");
                            }

                            reader.close();

                            Bukkit.broadcastMessage(ChatColor.GREEN + "Your solution.json file was processed successfully!");

                            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Something is happening to the sheeps' aura...!");
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    int time = 0;
                                    for(Entity ent : currentWorld.getEntitiesByClasses(Sheep.class)) {
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                ent.getWorld().createExplosion(ent.getLocation(), 4F, true, false);
                                            }
                                        }.runTaskLater(plugin, time);
                                        time += 10;

                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                isVictory = true;
                                            }
                                        }.runTaskLater(plugin, time + 20L);
                                    }
                                }
                            }.runTaskLater(plugin, 40L);
                        } catch(Exception ex) {
                            ex.printStackTrace();

                            Bukkit.broadcastMessage(ChatColor.RED + "There was an error with your solution.json file! " + ex.getMessage());
                            Bukkit.broadcastMessage(ChatColor.RED + "View the output log for details...");
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    private void playSoundToAll(Sound sound, float pitch) {
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), sound, 1, pitch);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if(isVictory) {
            return;
        }
        if(event.getBlock().getType() == Material.REDSTONE_WIRE || event.getBlock().getType() == Material.REPEATER) {
            return;
        }
        if(event.getPlayer().isOp() && event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        int maxDistance = (int) Math.pow(300, 2);
        for (Location loc : sheepLocations) {
            if (loc.distanceSquared(event.getBlock().getLocation()) <= maxDistance) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "The mystical sheeps' aura prevents you from breaking this block!");
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if(isVictory) {
            return;
        }
        if(event.getBlock().getType() == Material.REDSTONE_WIRE || event.getBlock().getType() == Material.REPEATER) {
            return;
        }
        if(event.getPlayer().isOp() && event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        int maxDistance = (int) Math.pow(300, 2);
        for (Location loc : sheepLocations) {
            if (loc.distanceSquared(event.getBlock().getLocation()) <= maxDistance) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "The mystical sheeps' aura prevents you from placing this block!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShear(PlayerShearEntityEvent event) {
        if(event.getEntity().getType() == EntityType.SHEEP) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ((Sheep) event.getEntity()).setSheared(false);
                    event.getEntity().getWorld().playEffect(event.getEntity().getLocation(), Effect.CHORUS_FLOWER_GROW, 0);
                    event.getEntity().getWorld().playEffect(event.getEntity().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                    event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1f, 0.5f);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if(event.getEntityType() == EntityType.VINDICATOR) {
            long deathTime = System.currentTimeMillis();
            lastVindicatorDeath = deathTime;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if(lastVindicatorDeath == deathTime) {
                        hasKilledVindicators = true;
                        playVindicatorText();
                    }
                }
            }.runTaskLater(plugin, 50L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(hasKilledVindicators) {
            playVindicatorText();
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if(event.getEntityType() == EntityType.SHEEP) {
            int maxDistance = (int) Math.pow(300, 2);
            for(Location loc : sheepLocations) {
                if(loc.distanceSquared(event.getEntity().getLocation()) <= maxDistance) {
                    event.setCancelled(true);
                    event.getEntity().getWorld().playEffect(event.getEntity().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                    if(event.getDamager() instanceof Player) {
                        event.getDamager().sendMessage(ChatColor.RED + "The mystical sheep's aura prevents you from punching it!");
                    }
                }
            }
        }
    }

    private void playVindicatorText() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "The vindicators have been removed from this world.");
                playSoundToAll(Sound.AMBIENT_CAVE, 0.5f);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "They leave a message:");
                        Bukkit.broadcastMessage(ChatColor.GOLD + "\"sheep\": 10");
                        Bukkit.broadcastMessage(ChatColor.GOLD + "\"action\": \"explode\"");
                        Bukkit.broadcastMessage(ChatColor.GOLD + "\"invincible\": false");
                        playSoundToAll(Sound.ENTITY_SHEEP_DEATH, 0.5f);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.broadcastMessage(ChatColor.RED + "They want you to clear the island of the sheep's aura!");
                                Bukkit.broadcastMessage(ChatColor.GREEN + "Save the message in a file in the data folder. Call the file solution.json to beat the sheep!");
                                playSoundToAll(Sound.AMBIENT_CAVE, 0.5f);
                            }
                        }.runTaskLater(plugin, 60L);
                    }
                }.runTaskLater(plugin, 20L);
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if(isVictory) {
            return;
        }

        String command = event.getMessage();

        if(command.toLowerCase().startsWith("/teamchallenge") || command.toLowerCase().startsWith("/tc")) {
            int maxDistance = (int) Math.pow(150, 2);
            for(Location loc : sheepLocations) {
                if(loc.distanceSquared(event.getPlayer().getLocation()) <= maxDistance) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "The mystical sheeps' aura prevents you from executing commands here!");
                }
            }
        }
    }
}
