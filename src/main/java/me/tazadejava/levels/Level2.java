package me.tazadejava.levels;

import me.tazadejava.main.OpCommandHandler;
import me.tazadejava.main.TeamChallengeMCGD;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class Level2 extends Level {

    private List<Location> possibleSignLocations = new ArrayList<>();
    private HashMap<Location, BlockFace> signOrientations = new HashMap<>();
    private List<String> possibleWords;

    private boolean hasLost = false;
    private int currentWave;
    private boolean waveInProgress = false;
    private boolean hasWon = false;

    private BossBar currentBossBar;
    private OpCommandHandler cmdHandler;

    private Random random = new Random();
    private int spawnedMonsters;

    public Level2(TeamChallengeMCGD plugin, World world) {
        super(plugin, world);

        startOnEnable = false;

        possibleWords = new ArrayList<>();

        possibleWords.addAll(Arrays.asList(nouns.split("\n")));

        notifyToStartLevel();

        possibleSignLocations.add(new Location(world, 11, 50, 0));
        possibleSignLocations.add(new Location(world, -11, 50, 0));
        possibleSignLocations.add(new Location(world, 0, 50, 11));
        possibleSignLocations.add(new Location(world, 0, 50, -11));

        signOrientations.put(possibleSignLocations.get(0), BlockFace.EAST);
        signOrientations.put(possibleSignLocations.get(1), BlockFace.WEST);
        signOrientations.put(possibleSignLocations.get(2), BlockFace.SOUTH);
        signOrientations.put(possibleSignLocations.get(3), BlockFace.NORTH);
    }

    private void clearOldBossBar() {
        if(currentBossBar != null) {
            currentBossBar.removeAll();
            currentBossBar = null;
        }
    }

    private void defineBossBar(String title) {
        currentBossBar = Bukkit.createBossBar(title, BarColor.PURPLE, BarStyle.SEGMENTED_10);

        for(Player p : Bukkit.getOnlinePlayers()) {
            currentBossBar.addPlayer(p);
        }
    }

    private void notifyToStartLevel() {
        clearOldBossBar();
        defineBossBar("Type " + ChatColor.LIGHT_PURPLE + "/challenge start" + ChatColor.WHITE + " to start the challenge.");
    }

    @Override
    public void startLevel() {
        super.startLevel();

        hasLost = false;
        currentWave = 1;
        waveInProgress = true;
        hasWon = false;

        removeMatchFile();
        cleanMap();
        clearOldBossBar();
        defineBossBar("Type " + ChatColor.LIGHT_PURPLE + "/wave verify <word1> <word2>" + ChatColor.WHITE + " to beat the wave!");

        for(Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1, 0.75f);
            p.sendTitle(ChatColor.LIGHT_PURPLE + "FIRST WAVE", ChatColor.DARK_PURPLE + "The challenge has started! Here comes the zombies...", 10, 70, 20);
        }

        new BukkitRunnable() {

            private int count = 0;

            @Override
            public void run() {
                if(!isLevelInProgress) {
                    cancel();
                    return;
                }

                if(count > 6) {
                    if(matchFileExists() && waveInProgress) {
                        waveInProgress = false;

                        if(currentWave == 2) {
                            hasWon = true;
                            cancel();
                            for(Player p : Bukkit.getOnlinePlayers()) {
                                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0.5f);
                                p.sendTitle(ChatColor.GREEN + "You survived wave " + currentWave + "!", ChatColor.YELLOW + "Congrats, you have beat the challenge!", 10, 70, 20);
                            }
                            return;
                        }

                        for(Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0.5f);
                            p.sendTitle(ChatColor.GREEN + "You survived wave " + currentWave + "!", ChatColor.YELLOW + "The final wave will start in 10 seconds, if you want to attempt it.", 10, 70, 20);
                            p.sendMessage(ChatColor.GOLD + "You have completed the challenge! If you would like, you may continue on to complete wave 2. Otherwise, you are all done!");
                        }

                        clearOldBossBar();
                        defineBossBar("The next wave will start shortly... Be prepared!");

                        cleanMap();

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                currentWave++;
                                waveInProgress = true;
                                count = -3;

                                removeMatchFile();
                                cleanMap();
                                clearOldBossBar();
                                defineBossBar("Type " + ChatColor.LIGHT_PURPLE + "/wave verify <word1> <word2>" + ChatColor.WHITE + " to beat the wave!");

//                                if(currentWave == 2) {
//                                    for(Player p : Bukkit.getOnlinePlayers()) {
//                                        p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.25f, 0.75f);
//                                        p.sendTitle(ChatColor.LIGHT_PURPLE + "WAVE 2", ChatColor.DARK_PURPLE + "The challenge has started! Let the arrow storm begin...", 10, 70, 20);
//                                    }
//                                } else {
                                    for(Player p : Bukkit.getOnlinePlayers()) {
                                        p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.5f, 0.75f);
                                        p.sendTitle(ChatColor.LIGHT_PURPLE + "FINAL WAVE", ChatColor.DARK_PURPLE + "The challenge has started! The fires have been ignited...", 10, 70, 20);
                                    }
//                                }
                            }
                        }.runTaskLater(plugin, 200L);
                    }
                }

                if(!waveInProgress) {
                    return;
                }

                if(count < 0) {
                    count++;
                    return;
                }

                if(count == 6) {
                    spawnSigns();
                    for(Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 2f);
                        p.sendTitle(ChatColor.GREEN + "The signs have spawned!", ChatColor.YELLOW + "To beat the wave, read the signs and verify the words (see above)!", 10, 50, 20);
                    }
                }

                switch(currentWave) {
                    case 1:
                        if(count == 0) {
                            spawnedMonsters = 0;
                        }

                        if(spawnedMonsters < 40) {
                            if(spawnedMonsters > 20) {
                                if(random.nextInt(2) == 0) {
                                    break;
                                }
                            } else if(spawnedMonsters > 15) {
                                if(random.nextInt(3) == 0) {
                                    break;
                                }
                            }

                            for (int i = 0; i < 2 + random.nextInt(3); i++) {
                                Location loc = possibleSignLocations.get(random.nextInt(4)).clone();
                                loc.add((Math.random() * 2) - 1, 0, (Math.random() * 2) - 1);
                                LivingEntity ent = (LivingEntity) currentWorld.spawnEntity(loc, EntityType.ZOMBIE);
                                ent.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
                                ent.setMetadata("wavemonster", new FixedMetadataValue(plugin, true));
                                spawnedMonsters++;
                            }
                        }
                        break;
//                    case 2:
//                        List<Player> candidates = new ArrayList<>();
//
//                        for(Player p : Bukkit.getOnlinePlayers()) {
////                            if(!p.isOp()) {
//                                candidates.add(p);
////                            }
//                        }
//
//                        for(Location loc : possibleSignLocations) {
//                            boolean changeX = signOrientations.get(loc) == BlockFace.NORTH || signOrientations.get(loc) == BlockFace.SOUTH;
//
//                            for(int i = 0; i < 2 + random.nextInt(3); i++) {
//                                Location arrowLoc;
//                                if(changeX) {
//                                    arrowLoc = loc.clone().add((Math.random() * 4) - 2, 1, 0);
//                                } else {
//                                    arrowLoc = loc.clone().add(0, 1, (Math.random() * 4) - 2);
//                                }
//
//                                Arrow ent = (Arrow) currentWorld.spawnEntity(arrowLoc, EntityType.ARROW);
//                                ent.setMetadata("wavemonster", new FixedMetadataValue(plugin, true));
//                                ent.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1), true);
//                                ent.setFireTicks(100);
//
//                                if(!candidates.isEmpty()) {
//                                    Vector vector = candidates.get(random.nextInt(candidates.size())).getEyeLocation().toVector().subtract(ent.getLocation().toVector()).normalize().multiply(1.5f);
//                                    ent.setVelocity(vector);
//                                }
//                            }
//                        }
//                        break;
                    case 2:
                        if(count == 0) {
                            currentWorld.setGameRule(GameRule.DO_FIRE_TICK, true);
                            cmdHandler.loadBlocksToMaterial("maplevel2", Material.RED_WOOL);

                            for(Location loc : possibleSignLocations) {
                                loc.clone().add(0, -1, 0).getBlock().setType(Material.COBBLESTONE);

                                for(int dx = -3; dx <= 3; dx++) {
                                    for(int dy = -1; dy <= 0; dy++) {
                                        for(int dz = -3; dz <= 3; dz++) {
                                            if(random.nextInt(4) == 0) {
                                                continue;
                                            }

                                            Location burn = loc.clone().add(dx, dy, dz);

                                            if(burn.getBlock().getType() != Material.AIR) {
                                                if(burn.clone().add(0, -1, 0).getBlock().getType() == Material.AIR) {
                                                    burn.clone().add(0, -1, 0).getBlock().setType(Material.FIRE);
                                                }

                                                if(burn.clone().add(1, 0, 0).getBlock().getType() == Material.AIR) {
                                                    burn.clone().add(1, 0, 0).getBlock().setType(Material.FIRE);
                                                }

                                                if(burn.clone().add(0, 0, 1).getBlock().getType() == Material.AIR) {
                                                    burn.clone().add(0, 0, 1).getBlock().setType(Material.FIRE);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                }

                count++;
            }
        }.runTaskTimer(plugin, 80L, 20L);
    }

    private void spawnSigns() {
        Collections.shuffle(possibleSignLocations);
        Collections.shuffle(possibleWords);

        for(int i = 0; i < 2; i++) {
            Location loc = possibleSignLocations.get(i);

            loc.getBlock().setType(Material.OAK_SIGN);
            Sign sign = (Sign) loc.getBlock().getState();

            Rotatable data = (Rotatable) loc.getBlock().getBlockData();
            data.setRotation(signOrientations.get(loc));
            sign.setBlockData(data);

            sign.setLine(0, possibleWords.get(i));
            sign.update();
        }

        for(int i = 2; i < 4; i++) {
            possibleSignLocations.get(i).getBlock().setType(Material.AIR);
        }
    }

    @Override
    public void endLevel() {
        super.endLevel();

        cleanMap();
        currentWorld.setGameRule(GameRule.DO_FIRE_TICK, false);

        clearOldBossBar();
    }

    @Override
    public void onDisable() {
        cleanMap();
        clearOldBossBar();
    }

    private boolean matchFileExists() {
        return new File(plugin.getDataFolder() + "/matched").exists();
    }

    private void removeMatchFile() {
        File matchFile = new File(plugin.getDataFolder() + "/matched");
        if(matchFile.exists()) {
            matchFile.delete();
        }
    }

    public void addOpCommandHandler(OpCommandHandler cmdHandler) {
        this.cmdHandler = cmdHandler;
    }

    private void cleanMap() {
        cmdHandler.loadBlocks("maplevel2");

        for(Location loc : possibleSignLocations) {
            if(loc.getBlock().getType() != Material.AIR) {
                loc.getBlock().setType(Material.AIR);
            }
        }

        for(Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();

            for(PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
        }

        for(Entity ent : currentWorld.getEntities()) {
            if(ent instanceof Player) {
                continue;
            }

            ent.remove();
        }
    }

    @Override
    public boolean isVictory() {
        if(hasLost) {
            return false;
        }

        return hasWon;
    }

    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage("Unknown command. Try /challenge start");
            return true;
        }

        switch(args[0].toLowerCase()) {
            case "start":
                //TODO: ADD A CHECKER TO SEE IF THE CHALLENGE IS CURRENTLY ACTIVE
                if(isLevelInProgress) {
                    sender.sendMessage("You can't do this while the challenge is in progress!");
                    break;
                }

                startLevel();

                if(sender.isOp() && args.length > 1) {
                    currentWave = Integer.parseInt(args[1]);
                }
                break;
            default:
                sender.sendMessage("Unknown command. Try /challenge start");
                break;
        }

        return true;
    }

//    @EventHandler
//    public void onArrowLand(ProjectileHitEvent event) {
//        if(event.getEntity().hasMetadata("wavemonster")) {
//            event.getEntity().remove();
//        }
//    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();

        if(command.toLowerCase().startsWith("/wave")) {
            if(command.contains(" ")) {
                String[] split = command.split(" ");

                switch(split[1].toLowerCase()) {
                    case "zombie":
                        if(!isLevelInProgress || !waveInProgress || currentWave != 1) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(ChatColor.RED + "You can only call this command during wave 1!");
                        }
                        break;
//                    case "arrow":
//                        if(!isLevelInProgress || !waveInProgress || currentWave != 2) {
//                            event.setCancelled(true);
//                            event.getPlayer().sendMessage(ChatColor.RED + "You can only call this command during wave 2!");
//                        }
//                        break;
                    case "fire":
                        if(!isLevelInProgress || !waveInProgress || currentWave != 2) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(ChatColor.RED + "You can only call this command during wave 2!");
                        }
                        break;
                    case "verify":
                        if(!isLevelInProgress || !waveInProgress) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(ChatColor.RED + "You can only call this command during a wave!");
                        }
                        break;
                    default:
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "You cannot define any other commands other than /wave zombie/fire/verify for this challenge!");
                        break;
                }
            }
        }
    }

    @EventHandler
    public void onSpread(BlockSpreadEvent event) {
        if(currentWave == 2 && isLevelInProgress && random.nextInt(5) == 0) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        Location loc = event.getSource().getLocation().add(dx, dy, dz);

                        if (loc.getBlock().getType() == Material.AIR) {
                            loc.getBlock().setType(Material.FIRE);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(currentBossBar != null) {
            currentBossBar.addPlayer(event.getPlayer());
        }

        event.getPlayer().setGameMode(GameMode.ADVENTURE);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if(event.getEntity().hasMetadata("wavemonster")) {
            spawnedMonsters--;
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(isLevelInProgress) {
            for(Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(ChatColor.RED + "A player died!", ChatColor.DARK_PURPLE + "Your team failed the challenge. Type " + ChatColor.LIGHT_PURPLE + "/challenge start " + ChatColor.DARK_PURPLE + "to retry when ready.", 10, 70, 20);
            }

            endLevel();

            notifyToStartLevel();
        }
    }

    private String nouns = "manager\n" +
            "birthday\n" +
            "wedding\n" +
            "apple\n" +
            "contract\n" +
            "sister\n" +
            "speaker\n" +
            "database\n" +
            "country\n" +
            "property\n" +
            "committee\n" +
            "camera\n" +
            "sympathy\n" +
            "wealth\n" +
            "drawing\n" +
            "intention\n" +
            "bird\n" +
            "health\n" +
            "entry\n" +
            "homework\n" +
            "member\n" +
            "security\n" +
            "recording\n" +
            "hair\n" +
            "movie\n" +
            "marketing\n" +
            "ear\n" +
            "basket\n" +
            "moment\n" +
            "steak\n" +
            "system\n" +
            "arrival\n" +
            "error\n" +
            "video\n" +
            "gate\n" +
            "weakness\n" +
            "economics\n" +
            "writer\n" +
            "death\n" +
            "feedback\n" +
            "passenger\n" +
            "tongue\n" +
            "truth\n" +
            "flight\n" +
            "poet\n" +
            "movie\n" +
            "area\n" +
            "newspaper\n" +
            "winner\n" +
            "response\n" +
            "insect\n" +
            "safety\n" +
            "cabinet\n" +
            "painting\n" +
            "son\n" +
            "outcome\n" +
            "theory\n" +
            "meal\n" +
            "magazine\n" +
            "selection\n" +
            "dinner\n" +
            "moment\n" +
            "passenger\n" +
            "food\n" +
            "disk\n" +
            "client\n" +
            "agreement\n" +
            "two\n" +
            "wedding\n" +
            "storage\n" +
            "system\n" +
            "gene\n" +
            "paper\n" +
            "math\n" +
            "reaction\n" +
            "reality\n" +
            "hall\n" +
            "classroom\n" +
            "actor\n" +
            "revenue\n" +
            "agency\n" +
            "family\n" +
            "medicine\n" +
            "housing\n" +
            "speech\n" +
            "mode\n" +
            "piano\n" +
            "climate\n" +
            "awareness";
}
