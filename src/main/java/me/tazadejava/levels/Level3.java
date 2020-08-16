package me.tazadejava.levels;

import me.tazadejava.main.TeamChallengeMCGD;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Level3 extends Level {

    private HashMap<Player, Long> lastButtonPress = new HashMap<>();

    private int lastButtonLevel = 0;
    private int currentLevel = 0;

    private boolean isVictory = false;
    private boolean hasBeatenLevel1 = false;

    private Random random = new Random();

    private List<Location> pigmenLocations = new ArrayList<>();
    
    private HashMap<Location, Set<Entity>> livingPigmen = new HashMap<>();

    private Set<Player> playersInGoal = new HashSet<>();

    public Level3(TeamChallengeMCGD plugin, World currentWorld) {
        super(plugin, currentWorld);

        startOnEnable = true;

        pigmenLocations.add(new Location(currentWorld, 55, 50, 4));
        pigmenLocations.add(new Location(currentWorld, 55, 50, -3));
        pigmenLocations.add(new Location(currentWorld, 61, 50, 1.5));
        pigmenLocations.add(new Location(currentWorld, 61, 50, 1.5));
        pigmenLocations.add(new Location(currentWorld, 67, 50, 2));
        pigmenLocations.add(new Location(currentWorld, 67, 50, -3));
        pigmenLocations.add(new Location(currentWorld, 73, 50, -2));
        pigmenLocations.add(new Location(currentWorld, 73, 50, 3));
        pigmenLocations.add(new Location(currentWorld, 78.5, 50, 0.5));
        pigmenLocations.add(new Location(currentWorld, 82.5, 50, -3.5));
        pigmenLocations.add(new Location(currentWorld, 86.5, 50, 4.5));
        pigmenLocations.add(new Location(currentWorld, 92, 50, 0.5));
        pigmenLocations.add(new Location(currentWorld, 96, 50, 3.5));
        pigmenLocations.add(new Location(currentWorld, 96, 50, -3.5));
        pigmenLocations.add(new Location(currentWorld, 100, 50, 1.5));
        pigmenLocations.add(new Location(currentWorld, 100, 50, -2.5));
    }

    @Override
    public boolean isVictory() {
        return isVictory;
    }

    @Override
    public void startLevel() {
        super.startLevel();

        isVictory = false;
        hasBeatenLevel1 = false;

        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player p : Bukkit.getOnlinePlayers()) {
                    if(p.getInventory().getChestplate() != null && (p.getInventory().getChestplate().getType() == Material.ELYTRA || p.getInventory().getChestplate().getType() == Material.LEGACY_ELYTRA)) {
                        p.getInventory().setChestplate(null);
                        p.sendMessage(ChatColor.RED + "You're prohibited from flying in this level...");
                    }

                    if(currentLevel == 2) {
                        if(random.nextInt(2) == 0) {
                            List<Integer> possibilities = new ArrayList<>();
                            if(p.getInventory().getHelmet() != null) {
                                possibilities.add(0);
                            }
                            if(p.getInventory().getChestplate() != null) {
                                possibilities.add(1);
                            }
                            if(p.getInventory().getLeggings() != null) {
                                possibilities.add(2);
                            }
                            if(p.getInventory().getBoots() != null) {
                                possibilities.add(3);
                            }

                            if(!possibilities.isEmpty()) {
                                p.sendMessage(ChatColor.GRAY + "One of your armor pieces was taken away!");
                                switch(possibilities.get(random.nextInt(possibilities.size()))) {
                                    case 0:
                                        p.getInventory().setHelmet(null);
                                        break;
                                    case 1:
                                        p.getInventory().setChestplate(null);
                                        break;
                                    case 2:
                                        p.getInventory().setLeggings(null);
                                        break;
                                    case 3:
                                        p.getInventory().setBoots(null);
                                        break;
                                }
                            }
                        }
                    }

                    checkVictory(p);
                }

                switch(currentLevel) {
                    case 1:
                        //arrow locations:
                        // from -53 51 5 to -103 - -, facing north
                        // from - - -5 to -103 - -5, facing south

                        for(Player p : Bukkit.getOnlinePlayers()) {
                            Location loc = p.getLocation();

                            double minX = loc.getX() - 3;
                            double maxX = loc.getX() + 3;

                            if(minX < -103) {
                                minX = -103;
                            }
                            if(maxX > -53) {
                                maxX = -53;
                            }

                            for(int i = 0; i < 8 + random.nextInt(4); i++) {
                                loc = new Location(currentWorld, (Math.random() * (maxX - minX)) + minX, 51, random.nextInt(2) == 0 ? 5 : -5);
                                Vector dir = loc.getBlockZ() == -5 ? new Vector((Math.random() * 2) - 1, 0, 5 + (Math.random() * 4)) : new Vector((Math.random() * 2) - 1, 0, -(5 + (Math.random() * 4)));

                                Arrow arrow = currentWorld.spawnArrow(loc, dir, 1f + (float) (Math.random() * .4), 8);
                                arrow.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 3), true);
                            }
                        }
                        break;
                    case 2:
                        List<Location> spawnLocs = new ArrayList<>();

                        for(Location potentialLoc : pigmenLocations) {
                            if(livingPigmen.containsKey(potentialLoc) && livingPigmen.get(potentialLoc).size() > 3) {
                                continue;
                            }

                            for(Player p : Bukkit.getOnlinePlayers()) {
                                if(p.getLocation().distanceSquared(potentialLoc) <= 96) {
                                    spawnLocs.add(random.nextInt(spawnLocs.size() + 1), potentialLoc);
                                }
                            }
                        }

                        if(!spawnLocs.isEmpty()) {
                            for (int i = 0; i < random.nextInt(spawnLocs.size()); i++) {
                                Location loc = spawnLocs.get(i);

                                Piglin ent = (Piglin) loc.getWorld().spawnEntity(loc, EntityType.PIGLIN);
                                ent.setImmuneToZombification(true);
                                ent.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000, 2));
                                ent.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1000, 3));
                                ent.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));

                                ent.setMetadata("spawnLoc", new FixedMetadataValue(plugin, loc));
                                livingPigmen.putIfAbsent(loc, new HashSet<>());
                                livingPigmen.get(loc).add(ent);
                            }
                        }
                        break;
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private boolean withinWave1Bounds(Location loc) {
        if(loc.getBlockY() == 50 || loc.getBlockY() == 51) {
            if (loc.getBlockZ() >= -2 && loc.getBlockZ() <= 2) {
                if (loc.getBlockX() >= -108 && loc.getBlockX() <= -104) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkVictory(Player p) {
        Location loc = p.getLocation();

        boolean inGoal = false;
        if(loc.getBlockY() == 50 || loc.getBlockY() == 51) {
            if(loc.getBlockZ() >= -2 && loc.getBlockZ() <= 2) {
                if(loc.getBlockX() >= -108 && loc.getBlockX() <= -104) {
                    //wave 1
                    inGoal = true;
                }
                if(loc.getBlockX() >= 104 && loc.getBlockX() <= 108) {
                    //wave 2
                    inGoal = true;
                }
            }
        }

        if(!inGoal) {
            if(playersInGoal.contains(p)) {
                Bukkit.broadcastMessage(ChatColor.RED + p.getDisplayName() + " exited the goal area!");
                playersInGoal.remove(p);
            }
        } else if(!playersInGoal.contains(p)) {
            playersInGoal.add(p);
            int requiredPlayersCount = 0;
            for(Player pLoop : Bukkit.getOnlinePlayers()) {
                if(pLoop.isOp() && pLoop.getGameMode() == GameMode.CREATIVE) {
                    continue;
                }

                requiredPlayersCount++;
            }

            if(requiredPlayersCount == playersInGoal.size()) {
                if(currentLevel == 1) {
                    p.sendTitle(ChatColor.BLUE + "You beat level 1!", ChatColor.DARK_PURPLE + "You're done, but if you want an extra challenge, tackle level 2!", 10, 70, 20);
                    for(Player pLoop : Bukkit.getOnlinePlayers()) {
                        pLoop.teleport(getSpawnPoint().clone().add(0, 7, 0));
                    }
                    hasBeatenLevel1 = true;
                } else if(currentLevel == 2) {
                    p.sendTitle(ChatColor.GOLD + "You beat level 2!", ChatColor.DARK_PURPLE + "Congratulations!", 10, 70, 20);
                    isVictory = true;
                }
            } else {
                int remainingPlayers = (requiredPlayersCount - playersInGoal.size());
                Bukkit.broadcastMessage(ChatColor.AQUA + p.getDisplayName() + " entered the goal area! " + remainingPlayers + " player" + (remainingPlayers == 1 ? "" : "s") + " still need" + (remainingPlayers == 1 ? "s" : "") + " to enter the goal area to win!");
            }
        }
    }

    @Override
    public Location getSpawnPoint() {
        if(hasBeatenLevel1) {
            return super.getSpawnPoint().clone().setDirection(new Vector(-1, 0, 0)).add(0, 7, 0);
        } else {
            return super.getSpawnPoint().clone().setDirection(new Vector(-1, 0, 0));
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if(event.getEntity().hasMetadata("spawnLoc")) {
            livingPigmen.get((Location) event.getEntity().getMetadata("spawnLoc").get(0).value()).remove(event.getEntity());
        }
    }

    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {
        if(event.getEntity().getType() == EntityType.ARROW) {
            event.getEntity().remove();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.STONE_BUTTON) {
            Location loc = event.getClickedBlock().getLocation();
            if(loc.getBlockX() == -4 && loc.getBlockZ() == 0) {
                if(loc.getBlockY() == 51) {
                    if(System.currentTimeMillis() - lastButtonPress.getOrDefault(event.getPlayer(), 0L) <= 500) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "Please wait before clicking the button again!");
                        return;
                    }

                    lastButtonLevel = 1;
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "You pressed the button! Teleport the players between 1.5-2.0 seconds!");
                } else {
                    if(System.currentTimeMillis() - lastButtonPress.getOrDefault(event.getPlayer(), 0L) <= 500) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "Please wait before clicking the button again!");
                        return;
                    }

                    lastButtonLevel = 2;
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "You pressed the button! Teleport the players between 0.75-1.0 seconds!");
                }

                lastButtonPress.put(event.getPlayer(), System.currentTimeMillis());

                Chest chest1 = (Chest) new Location(loc.getWorld(), -52, 50, 2).getBlock().getState();
                Chest chest2 = (Chest) new Location(loc.getWorld(), 52, 50, -2).getBlock().getState();

                chest1.getBlockInventory().setContents(new ItemStack[0]);
                chest2.getBlockInventory().setContents(new ItemStack[0]);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().setGameMode(GameMode.ADVENTURE);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(isLevelInProgress && currentLevel != 0) {
            currentLevel = 0;
            for(Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(ChatColor.RED + "A player died!", ChatColor.DARK_PURPLE + "Your team failed the challenge. Click the button to retry when ready...", 10, 70, 20);
                if(event.getEntity() != p) {
                    p.teleport(getSpawnPoint());
                }
            }

            if(!livingPigmen.isEmpty()) {
                for(Collection<Entity> ents : livingPigmen.values()) {
                    for(Entity ent : ents) {
                        ent.remove();
                    }
                }

                livingPigmen.clear();
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if(event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }

        Player p = event.getPlayer();
        Location loc = event.getTo();

        long lastButtonPressTime = 0;
        if(lastButtonPress.containsKey(event.getPlayer())) {
            lastButtonPressTime = lastButtonPress.get(event.getPlayer());
        } else {
            if(!lastButtonPress.isEmpty()) {
                lastButtonPressTime = lastButtonPress.values().iterator().next();
            }
        }

        if(loc.getBlockX() == -50 && loc.getBlockY() == 50 && loc.getBlockZ() == 0 && lastButtonLevel == 1) { //wave 1
            long delay = System.currentTimeMillis() - lastButtonPressTime;
            currentLevel = 1;

            if(delay >= 1400 && delay <= 2100) {
                p.sendTitle(ChatColor.LIGHT_PURPLE + "Great timing!", ChatColor.DARK_PURPLE + "You teleported within the correct time delay!", 10, 70, 20);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
                p.getInventory().clear();
            } else {
                if(p.isOp() && p.getGameMode() == GameMode.CREATIVE) {
                    return;
                }

                event.setCancelled(true);
                giveTeleportPenalty(p, true);
            }
        } else if(loc.getBlockX() == 50 && loc.getBlockY() == 50 && loc.getBlockZ() == 0 && lastButtonLevel == 2) { //wave 2
            long delay = System.currentTimeMillis() - lastButtonPressTime;
            currentLevel = 2;

            if(delay >= 650 && delay <= 1100) {
                p.sendTitle(ChatColor.LIGHT_PURPLE + "Great timing!", ChatColor.DARK_PURPLE + "You teleported within the correct time delay!", 10, 70, 20);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
                p.getInventory().clear();
            } else {
                if(p.isOp() && p.getGameMode() == GameMode.CREATIVE) {
                    return;
                }

                event.setCancelled(true);
                giveTeleportPenalty(p, true);
            }
        } else {
            if(p.isOp() && p.getGameMode() == GameMode.CREATIVE) {
                return;
            }

            if(currentLevel == 1 && withinWave1Bounds(event.getFrom())) {
                return;
            }

            event.setCancelled(true);
            giveTeleportPenalty(p, false);
        }
    }

    private void giveTeleportPenalty(Player p, boolean wrongTiming) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100000, 1000, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100000, 1000, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100000, 1000, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100000, 1000, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 100000, 1000, false));

        if(wrongTiming) {
            p.sendTitle(ChatColor.LIGHT_PURPLE + "Wrong!", ChatColor.DARK_PURPLE + "You teleported in the wrong timeframe (too early/too late)...", 10, 50, 20);
        } else {
            p.sendTitle(ChatColor.LIGHT_PURPLE + "Wrong!", ChatColor.DARK_PURPLE + "You teleported to an incorrect location...", 10, 50, 20);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                p.setVelocity(new Vector(0, 100, 0));
            }
        }.runTaskLater(plugin, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                p.setHealth(0);
            }
        }.runTaskLater(plugin, 15L);
    }
}
