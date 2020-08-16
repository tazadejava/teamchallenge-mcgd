package me.tazadejava.specialitems;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;

/**
 * Allows player to select range of blocks. Used in schematic saving/loading in OpCommandHandler
 */
public class SelectionWand extends SpecialItem {

    private HashMap<Player, Location[]> selections = new HashMap<>();

    public SelectionWand() {
        super(new SpecialItem.ItemEventHooks[] {ItemEventHooks.PLAYER_INTERACT});
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getHand() == EquipmentSlot.HAND && isItem(event.getPlayer().getInventory().getItemInMainHand())) {
            Player p = event.getPlayer();

            if(!selections.containsKey(p)) {
                selections.put(p, new Location[2]);
            }

            Location[] range = selections.get(p);

            if(event.getAction() == Action.LEFT_CLICK_BLOCK) {
                range[0] = event.getClickedBlock().getLocation();
                event.getPlayer().sendMessage(ChatColor.GRAY + "Added left-block range at (" + event.getClickedBlock().getX() + ", " + event.getClickedBlock().getZ() + ").");
                event.setCancelled(true);
            } else if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                range[1] = event.getClickedBlock().getLocation();
                event.getPlayer().sendMessage(ChatColor.GRAY + "Added right-block range at (" + event.getClickedBlock().getX() + ", " + event.getClickedBlock().getZ() + ").");
                event.setCancelled(true);
            }

            if(range[0] != null && range[1] != null) {
                event.getPlayer().sendMessage(ChatColor.GRAY + "A block range has been calculated: from " + range[0] + " to " + range[1]);
            }
        }
    }

    public Location[] getPlayerSelection(Player p) {
        return selections.getOrDefault(p, null);
    }

    public void clearPlayerSelection(Player p) {
        if (selections.containsKey(p)) {
            selections.remove(p);
        }
    }

    @Override
    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.WOODEN_SHOVEL, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Selection Wand");
        meta.setLore(formatLore(ChatColor.GRAY + "Left and right click this tool to select 2D rectangular regions where specific rooms reside!", ChatColor.GRAY + "Command: /mission add <mission name> <room name>"));
        item.setItemMeta(meta);

        return item;
    }
}
