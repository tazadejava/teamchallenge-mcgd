package me.tazadejava.levels;

import me.tazadejava.main.TeamChallengeMCGD;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class Level1 extends Level {

    private static final int[] BRIDGE_BOUNDS = new int[] {
            -2, 2,
            49, 49,
            -54, -4
    };

    private static final int[] SPAWN_BOUNDS = new int[] {
            -3, 3,
            49, 49,
            -3, 3
    };

    private static final int[] GOAL_BOUNDS = new int[] {
            -2, 2,
            49, 49,
            -59, -55
    };

    private LinkedList<Block> brokenBridgeBlocks;
    private Set<String> membersInGoal = new HashSet<>();

    public Level1(TeamChallengeMCGD plugin, World currentWorld) {
        super(plugin, currentWorld);

        brokenBridgeBlocks = new LinkedList<>();
    }

    @Override
    public void startLevel() {
        super.startLevel();

        new BukkitRunnable() {
            @Override
            public void run() {
                for(int i = 0; i < Math.min(brokenBridgeBlocks.size(), 5); i++) {
                    Block block = brokenBridgeBlocks.poll();

                    block.setType(Material.OAK_PLANKS);
                    block.getWorld().spawnParticle(Particle.FLAME, block.getLocation(), 5);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void onDisable() {
        for(int i = 0; i < brokenBridgeBlocks.size(); i++) {
            Block block = brokenBridgeBlocks.poll();

            block.setType(Material.OAK_PLANKS);
            block.getWorld().spawnParticle(Particle.FLAME, block.getLocation(), 5);
        }
    }

    public boolean isVictory() {
        if(!plugin.isOnline()) {

        } else {
            String[] allMembers = plugin.getAllGroupMemberMinecraftUsernames();

            for (Player p : currentWorld.getPlayers()) {
                if (isInGame(p) && !p.isOp()) {
                    Location loc = p.getLocation();
                    loc.add(0, -1, 0);

                    String name = p.getName();

                    if (isInGoal(loc) && !p.isDead()) {
                        if (!membersInGoal.contains(name)) {
                            membersInGoal.add(name);

                            int remainingPlayers = 0;
                            for (String allMember : allMembers) {
                                if (!membersInGoal.contains(allMember)) {
                                    remainingPlayers++;
                                }
                            }

                            if (remainingPlayers == 0) {
                                return true;
                            } else {
                                Bukkit.broadcastMessage(ChatColor.GOLD + name + " has entered the goal! " + remainingPlayers + " player" + (remainingPlayers == 1 ? "" : "s") + " still need" + (remainingPlayers != 1 ? "" : "s") + " to enter the goal to win!");

                                for (Player pLoop : Bukkit.getOnlinePlayers()) {
                                    pLoop.getWorld().playSound(pLoop.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
                                }
                            }
                        }
                    } else {
                        if (membersInGoal.contains(name)) {
                            membersInGoal.remove(name);

                            Bukkit.broadcastMessage(ChatColor.RED + name + " has exited the goal!");
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean isCrossingBridge(Location loc) {
        return loc.getBlockZ() >= BRIDGE_BOUNDS[4] && loc.getBlockZ() <= BRIDGE_BOUNDS[5];
    }

    private boolean isInBounds(Location loc, int[] bounds) {
        return loc.getBlockX() >= bounds[0] && loc.getBlockX() <= bounds[1]
                && loc.getBlockY() >= bounds[2] && loc.getBlockY() <= bounds[3]
                && loc.getBlockZ() >= bounds[4] && loc.getBlockZ() <= bounds[5];
    }

    private boolean isInBridge(Location loc) {
        return isInBounds(loc, BRIDGE_BOUNDS);
    }

    private boolean isInSpawn(Location loc) {
        return isInBounds(loc, SPAWN_BOUNDS);
    }

    private boolean isInGoal(Location loc) {
        return isInBounds(loc, GOAL_BOUNDS);
    }

    private boolean isInGame(Player p) {
        if(p.isOp() && p.getGameMode() == GameMode.CREATIVE) {
            return false;
        }

        return true;
    }

    private void createExplosion(Location loc) {
        loc.getWorld().createExplosion(loc, 4, false, true);
        loc.getWorld().spawnParticle(Particle.BARRIER, loc, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if(isInGame(event.getPlayer()) && (isCrossingBridge(event.getBlock().getLocation()) || isCrossingBridge(event.getBlockAgainst().getLocation()))) {
            createExplosion(event.getBlock().getLocation());
            event.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        if(isInBridge(event.getBlock().getLocation())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    brokenBridgeBlocks.add(event.getBlock());
                }
            }.runTaskLater(plugin, (long) (Math.random() * 10) + 5);
        } else {
            if(isInGame(event.getPlayer()) && (isInSpawn(event.getBlock().getLocation()) || isInGoal(event.getBlock().getLocation()))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if(event.getTo() != null && isInGame(event.getPlayer()) && isCrossingBridge(event.getTo().getBlock().getLocation().add(0, -1, 0)) && event.getTo().getY() == 50) {
            boolean shouldExplode = true;
            for(Entity ent : currentWorld.getNearbyEntities(event.getTo(), 5, 5, 5)) {
                if(ent.getPassengers().contains(event.getPlayer())) {
                    shouldExplode = false;
                }
            }

            if(shouldExplode) {
                createExplosion(event.getTo());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if(event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            if(event.getDamage() > 10) {
                event.setDamage(10);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplode(BlockExplodeEvent event) {
        Iterator<Block> blocks = event.blockList().iterator();
        while(blocks.hasNext()) {
            Block block = blocks.next();

            if(isInSpawn(block.getLocation()) || isInGoal(block.getLocation())) {
                blocks.remove();
            } else if (isInBridge(block.getLocation())) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        brokenBridgeBlocks.add(block);
                    }
                }.runTaskLater(plugin, (long) (Math.random() * 10) + 5);
            }
        }
    }
}
