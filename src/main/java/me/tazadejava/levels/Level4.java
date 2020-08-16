package me.tazadejava.levels;

import me.tazadejava.main.TeamChallengeMCGD;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class Level4 extends Level {

    private boolean isVictory;

    public Level4(TeamChallengeMCGD plugin, World currentWorld) {
        super(plugin, currentWorld);
    }

    @Override
    public void startLevel() {
        super.startLevel();

        isVictory = false;
    }

    @Override
    public boolean isVictory() {
        return isVictory;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if(event.getEntityType() == EntityType.RAVAGER) {
            if(event.getEntity().getCustomName() != null && event.getEntity().getCustomName().equals(ChatColor.LIGHT_PURPLE + "ur worst nightmare")) {
                if(event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    event.setCancelled(true);
                    Entity passenger = null;
                    if(!event.getEntity().getPassengers().isEmpty()) {
                        passenger = event.getEntity().getPassengers().get(0);
                        event.getEntity().removePassenger(passenger);
                        passenger.setFallDistance(0);
                        passenger.teleport(new Location(event.getEntity().getWorld(), 82, 50, 360));
                    }

                    event.getEntity().setFallDistance(0);
                    event.getEntity().teleport(new Location(event.getEntity().getWorld(), 82, 50, 360));

                    if(passenger != null) {
                        event.getEntity().addPassenger(passenger);
                    }

                    Bukkit.broadcastMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "The ravager revived itself!");
                } else {
                    if(event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityAttack(EntityDamageByEntityEvent event) {
        if(event.getEntityType() == EntityType.RAVAGER) {
            if (event.getEntity().getCustomName() != null && event.getEntity().getCustomName().equals(ChatColor.LIGHT_PURPLE + "ur worst nightmare")) {
                if(event.getDamager() instanceof Player) {
                    Player p = (Player) event.getDamager();

                    if(p.getInventory().getItemInMainHand().getType() != Material.FEATHER) {
                        if(p.getInventory().getItemInMainHand().getType() == Material.AIR) {
                            p.sendMessage(ChatColor.GRAY + "Hitting bare-handed seems to have no effect on the ravager...");
                        } else {
                            p.sendMessage(ChatColor.GRAY + "This item seems to have no effect on the ravager...");
                        }
                        event.setCancelled(true);
                    } else {
                        event.setDamage(event.getDamage() * 10d);
                        p.sendMessage(ChatColor.YELLOW + "Critical hit!");
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_HIT, 1f, 0.75f);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent event) {
        if(event.getEntityType() == EntityType.RAVAGER) {
            if (event.getEntity().getCustomName() != null && event.getEntity().getCustomName().equals(ChatColor.LIGHT_PURPLE + "ur worst nightmare")) {
                Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "The ravager died!");
                isVictory = true;
            }
        }
    }
}
