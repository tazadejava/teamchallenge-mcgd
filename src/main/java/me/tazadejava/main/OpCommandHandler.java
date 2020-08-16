package me.tazadejava.main;

import me.tazadejava.specialitems.SelectionWand;
import me.tazadejava.specialitems.SpecialItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;

/**
 * Helper commands for general challenges. Not needed for normal level testing.
 */
public class OpCommandHandler implements CommandExecutor {

    private JavaPlugin plugin;
    private HashMap<String, SpecialItem> specialItems;

    public OpCommandHandler(JavaPlugin plugin, HashMap<String, SpecialItem> specialItems) {
        this.plugin = plugin;
        this.specialItems = specialItems;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if(args.length == 0) {
            return false;
        }
        if(!commandSender.isOp()) {
            return false;
        }

        switch(args[0].toLowerCase()) {
            case "despawn":
                for(LivingEntity ent : Bukkit.getWorlds().get(0).getLivingEntities()) {
                    if(ent instanceof Player) {
                        continue;
                    }

                    ent.remove();
                }
                commandSender.sendMessage("Despawned all living entities.");
                break;
            case "sign":
                if(args.length < 3) {
                    commandSender.sendMessage("/mcgd sign <line> <text>");
                    break;
                }
                if(!(commandSender instanceof Player)) {
                    break;
                }

                Player p = (Player) commandSender;

                int line = Integer.parseInt(args[1]) - 1;

                Block target = p.getTargetBlock(null, 10);

                if(target != null && target.getState() instanceof Sign) {
                    Sign sign = (Sign) target.getState();

                    String restOfArgs = "";
                    for(int i = 2; i < args.length; i++) {
                        restOfArgs += args[i] + " ";
                    }

                    restOfArgs = restOfArgs.substring(0, restOfArgs.length() - 1);

                    sign.setLine(line, restOfArgs);
                    sign.update();
                    p.sendMessage("Set line " + (line + 1) + " of sign to " + restOfArgs);
                }
                break;
            case "schematic":
                if(args.length < 2) {
                    commandSender.sendMessage("/mcgd schematic <get/save <name>/load <name>>");
                    break;
                }

                switch(args[1].toLowerCase()) {
                    case "get":
                        if(commandSender instanceof Player) {
                            ((Player) commandSender).getInventory().addItem(specialItems.get("wand").getItem());
                        }
                        break;
                    case "save":
                        if(args.length < 3) {
                            commandSender.sendMessage("/mcgd schematic <get/save <name>/load <name>>");
                            break;
                        }
                        SelectionWand wand = (SelectionWand) specialItems.get("wand");
                        Location[] selection = wand.getPlayerSelection((Player) commandSender);

                        if(selection == null || selection[0] == null || selection[1] == null) {
                            commandSender.sendMessage("You must define the bounds first.");
                            break;
                        }

                        saveBlocks(args[2], selection);
                        commandSender.sendMessage("Done!");
                        break;
                    case "load":
                        if(args.length < 3) {
                            commandSender.sendMessage("/mcgd schematic <get/save <name>/load <name>>");
                            break;
                        }
                        commandSender.sendMessage("Block loading success? " + loadBlocks(args[2]));
                        break;
                    default:
                        commandSender.sendMessage("Unknown command.");
                        break;
                }
                break;
            case "spawnlevel4":
                if(!(commandSender instanceof Player)) {
                    break;
                }

                p = (Player) commandSender;

                LivingEntity entity = (LivingEntity) p.getWorld().spawnEntity(p.getLocation(), EntityType.RAVAGER);
                entity.setCustomName(ChatColor.LIGHT_PURPLE + "ur worst nightmare");
                entity.setCustomNameVisible(true);

                entity.setRemoveWhenFarAway(false);

                LivingEntity ent = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.EVOKER);
                ent.setCustomName(ChatColor.BLUE + "ur worst nightmare");
                ent.setRemoveWhenFarAway(false);
                ent = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.EVOKER);
                ent.setCustomName(ChatColor.BLUE + "ur worst nightmare");
                ent.setRemoveWhenFarAway(false);

//                entity.addPassenger(ent);
                break;
            case "loadworld":
                if(args.length == 2) {
                    WorldCreator creator = new WorldCreator(args[1]);
                    Bukkit.createWorld(creator);
                    commandSender.sendMessage("Done.");
                }
            case "tpworld":
                if(commandSender instanceof Player) {
                    if (args.length == 2) {
                        if (Bukkit.getWorld(args[1]) != null) {
                            ((Player) commandSender).teleport(Bukkit.getWorld(args[1]).getSpawnLocation());
                            commandSender.sendMessage("Done.");
                        }
                    }
                }
                break;
            default:
                commandSender.sendMessage("Unknown command.");
                break;
        }

        return true;
    }

    private void saveBlocks(String name, Location[] selection) {
        int[] xs = selection[0].getBlockX() < selection[1].getBlockX() ? new int[] {selection[0].getBlockX(), selection[1].getBlockX()} : new int[] {selection[1].getBlockX(), selection[0].getBlockX()};
        int[] ys = selection[0].getBlockY() < selection[1].getBlockY() ? new int[] {selection[0].getBlockY(), selection[1].getBlockY()} : new int[] {selection[1].getBlockY(), selection[0].getBlockY()};
        int[] zs = selection[0].getBlockZ() < selection[1].getBlockZ() ? new int[] {selection[0].getBlockZ(), selection[1].getBlockZ()} : new int[] {selection[1].getBlockZ(), selection[0].getBlockZ()};

        try {
            File schematicsFolder = new File(plugin.getDataFolder() + "/schematics/");
            if(!schematicsFolder.exists()) {
                schematicsFolder.mkdirs();
            }

            File dataFile = new File(schematicsFolder.getAbsolutePath() + "/" + name + ".schematic");
            if(!dataFile.exists()) {
                dataFile.createNewFile();
            }

            FileWriter writer = new FileWriter(dataFile);

            writer.append(xs[0] + " " + xs[1] + " " + ys[0] + " " + ys[1] + " " + zs[0] + " " + zs[1] + "\n");

            for(int x = xs[0]; x <= xs[1]; x++) {
                for(int y = ys[0]; y <= ys[1]; y++) {
                    for(int z = zs[0]; z <= zs[1]; z++) {
                        Block block = new Location(selection[0].getWorld(), x, y, z).getBlock();
                        if(block.getType() != Material.AIR) {
                            writer.append(block.getType() + " " + x + " " + y + " " + z);
                            writer.append("\n");
                        }
                    }
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean loadBlocks(String name) {
        return loadBlocksToMaterial(name, null);
    }

    public boolean loadBlocksToMaterial(String name, Material overrideMat) {
        try {
            File schematicsFolder = new File(plugin.getDataFolder() + "/schematics/");
            File dataFile = new File(schematicsFolder.getAbsolutePath() + "/" + name + ".schematic");
            if(!dataFile.exists()) {
                return false;
            }

            BufferedReader reader = new BufferedReader(new FileReader(dataFile));

            String[] bounds = reader.readLine().split(" ");

            HashMap<Integer, HashMap<Integer, HashMap<Integer, Material>>> blocks = new HashMap<>();

            String read;
            while((read = reader.readLine()) != null) {
                String[] split = read.split(" ");

                int x = Integer.parseInt(split[1]);
                int y = Integer.parseInt(split[2]);
                int z = Integer.parseInt(split[3]);

                Material mat;
                if(overrideMat != null) {
                    mat = overrideMat;
                } else {
                    mat = Material.valueOf(split[0]);
                }

                blocks.putIfAbsent(x, new HashMap<>());
                blocks.get(x).putIfAbsent(y, new HashMap<>());
                blocks.get(x).get(y).put(z, mat);
            }

            for(int x = Integer.parseInt(bounds[0]); x <= Integer.parseInt(bounds[1]); x++) {
                for(int y = Integer.parseInt(bounds[2]); y <= Integer.parseInt(bounds[3]); y++) {
                    for(int z = Integer.parseInt(bounds[4]); z <= Integer.parseInt(bounds[5]); z++) {
                        Block block = new Location(Bukkit.getWorlds().get(0), x, y, z).getBlock();
                        if(blocks.containsKey(x)) {
                            Material mat = null;
                            if(blocks.get(x).containsKey(y)) {
                                if(blocks.get(x).get(y).containsKey(z)) {
                                    mat = blocks.get(x).get(y).get(z);
                                    if(block.getType() != mat) {
                                        block.setType(mat);
                                    }
                                }
                            }

                            if(mat == null) {
                                if(block.getType() != Material.AIR) {
                                    block.setType(Material.AIR);
                                }
                            }
                        }
                    }
                }
            }

            reader.close();

            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
