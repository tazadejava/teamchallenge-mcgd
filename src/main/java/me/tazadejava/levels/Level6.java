package me.tazadejava.levels;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tazadejava.main.TeamChallengeMCGD;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Level6 extends Level {

    /*
    Spawn monsters between:

    -20 50 -20
    20 50 20

    but don't spawn between

    -5 50 -5
    5 50 5
     */

    public static class GroupScore {
        public int groupNumber;
        public int groupScore;
        public long lastScoreMilli;

        public GroupScore(int groupNumber, int groupScore, long lastScoreMilli) {
            this.groupNumber = groupNumber;
            this.groupScore = groupScore;
            this.lastScoreMilli = lastScoreMilli;
        }
    }

    private static final String MONSTER_METADATA_LABEL = "12583e05-c9c7-424c-9f9a-4c2e2ad40d25";

    public static final String SCORE_UPLOAD_TOKEN = "0eff6db3-4178-4bc2-b5e0-826ce44c47dd";

    private boolean gotHighestScore, removedScoreboard, isTimeUp, isRavagerAlive, warnedFiveMinutes, warnedOneMinute;
    private int spawnedMonsters;
    private List<LivingEntity> spawnedMonstersList;

    private int killedMonstersCount, lastKilledMonstersCount;
    private int groupNumber;
    private List<GroupScore> sortedGroupScores;

    private long endTime;

    private BossBar timeLeftBossBar;
    private Scoreboard killedMonstersScoreboard;
    private Objective sidebar;


    public Level6(TeamChallengeMCGD plugin, World currentWorld, int groupNumber) {
        super(plugin, currentWorld);

        this.groupNumber = groupNumber;

        currentWorld.setSpawnLocation(getSpawnPoint());

        if(!plugin.isOnline()) {
            //35 minutes
            endTime = System.currentTimeMillis() + (1000 * 60 * 35);
        }
    }

    @Override
    public void startLevel() {
        super.startLevel();

        gotHighestScore = false;
        removedScoreboard = false;
        isTimeUp = false;
        isRavagerAlive = false;
        spawnedMonsters = 0;
        killedMonstersCount = 0;
        spawnedMonstersList = new ArrayList<>();

        warnedFiveMinutes = false;
        warnedOneMinute = false;

        checkChallengeEndTime();

        sortedGroupScores = getSortedGroupScores();

        for(GroupScore score : sortedGroupScores) {
            if(score.groupNumber == groupNumber) {
                killedMonstersCount = score.groupScore;
            }
        }

        timeLeftBossBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + "Time left: " + getTimeLeftFormatted(), BarColor.PURPLE, BarStyle.SEGMENTED_10);
        killedMonstersScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        for(Player p : Bukkit.getOnlinePlayers()) {
            timeLeftBossBar.addPlayer(p);
            p.setScoreboard(killedMonstersScoreboard);
        }

        sidebar = killedMonstersScoreboard.registerNewObjective("killedCount", "dummy", ChatColor.GREEN + "Group Kills (you are bold)");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        new BukkitRunnable() {
            @Override
            public void run() {
                if(!spawnedMonstersList.isEmpty()) {
                    Iterator<LivingEntity> iterator = spawnedMonstersList.iterator();

                    while(iterator.hasNext()) {
                        LivingEntity ent = iterator.next();

                        if(!ent.isValid() || ent.isDead()) {
                            spawnedMonsters--;
                            iterator.remove();
                        }
                    }
                }

                spawnMonsters();

                if(sortedGroupScores != null) {
                    updateScoreboard();
                }

                timeLeftBossBar.setProgress(getTimeLeftProgress());
                timeLeftBossBar.setTitle("Time left: " + getTimeLeftFormatted());

                if(getSecondsLeft() <= 60 * 5 && !warnedFiveMinutes) {
                    warnedFiveMinutes = true;
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "There are 5 minutes left in the challenge!");
                    timeLeftBossBar.setColor(BarColor.PINK);
                }

                if(getSecondsLeft() <= 60 && !warnedOneMinute) {
                    warnedOneMinute = true;
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "There is 1 minute left in the challenge!");
                    timeLeftBossBar.setColor(BarColor.RED);
                }

                if(getSecondsLeft() < 60 && !removedScoreboard) {
                    removedScoreboard = true;
                    for(Player p : Bukkit.getOnlinePlayers()) {
                        p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    }
                }
                if(getSecondsLeft() == 0) {
                    isTimeUp = true;
                    Bukkit.broadcastMessage("" + ChatColor.DARK_RED + ChatColor.BOLD + "TIME IS UP!");
                    playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f);
                    lastKilledMonstersCount = -1;

                    cancel();

                    for(LivingEntity ent : spawnedMonstersList) {
                        ent.remove();
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Calculating final scores. . .");
                        }
                    }.runTaskLater(plugin, 20L);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                            Objective sidebar = scoreboard.registerNewObjective("finalplaces", "dummy", ChatColor.BLUE + "Final Monster Kills (you are bold)");
                            sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

                            for(int i = 0; i < sortedGroupScores.size(); i++) {
                                GroupScore groupScore = sortedGroupScores.get(i);

                                if(groupScore.groupNumber == groupNumber) {
                                    if(i == 0) {
                                        playSoundToAll(Sound.ENTITY_PLAYER_LEVELUP, 0.5f);
                                        Bukkit.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + "Congratulations! Your team got the highest score and won 1st place with " + killedMonstersCount + " kills total!");
                                        gotHighestScore = true;
                                    } else {
                                        playSoundToAll(Sound.ENTITY_PLAYER_LEVELUP, 2f);
                                        String place = "";
                                        switch(i) {
                                            case 1:
                                                place = "2nd";
                                                break;
                                            case 2:
                                                place = "3rd";
                                                break;
                                            default:
                                                place = (i + 1) + "th";
                                                break;
                                        }
                                        Bukkit.broadcastMessage(ChatColor.GREEN + "Your team made " + place + " place with " + killedMonstersCount + " kills total!");
                                    }
                                }

                                switch(i) {
                                    case 0:
                                        sidebar.getScore("" + ChatColor.GOLD + (groupScore.groupNumber == groupNumber ? ChatColor.BOLD : "") + "Group " + groupScore.groupNumber).setScore(groupScore.groupScore);
                                        break;
                                    case 1:
                                        sidebar.getScore("" + ChatColor.DARK_GREEN + (groupScore.groupNumber == groupNumber ? ChatColor.BOLD : "") + "Group " + groupScore.groupNumber).setScore(groupScore.groupScore);
                                        break;
                                    case 2:
                                        sidebar.getScore("" + ChatColor.AQUA + (groupScore.groupNumber == groupNumber ? ChatColor.BOLD : "") + "Group " + groupScore.groupNumber).setScore(groupScore.groupScore);
                                        break;
                                    default:
                                        sidebar.getScore("" + ChatColor.GRAY + (groupScore.groupNumber == groupNumber ? ChatColor.BOLD : "") + "Group " + groupScore.groupNumber).setScore(groupScore.groupScore);
                                        break;
                                }
                            }

                            for(Player p : Bukkit.getOnlinePlayers()) {
                                p.setScoreboard(scoreboard);
                            }
                        }
                    }.runTaskLater(plugin, 100L);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        new BukkitRunnable() {

            int count = 0;

            @Override
            public void run() {
                if(isTimeUp) {
                    count++;
                    if(count > 3) {
                        cancel();
                        updateGroupScore();
                        return;
                    }
                }

                if(lastKilledMonstersCount != killedMonstersCount) {
                    lastKilledMonstersCount = killedMonstersCount;
                    updateGroupScore();
                } else {
                    checkChallengeEndTime();
                }

                List<GroupScore> scores = getSortedGroupScores();
                if(scores != null) {
                    sortedGroupScores = scores;
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 40L);
    }

    private void updateScoreboard() {
        if(killedMonstersScoreboard.getObjective("killedCount") != null) {
            killedMonstersScoreboard.getObjective("killedCount").unregister();
        }
        sidebar = killedMonstersScoreboard.registerNewObjective("killedCount", "dummy", ChatColor.BLUE + "Monster Kills (you are bold)");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        for(int i = 0; i < sortedGroupScores.size(); i++) {
            GroupScore groupScore = sortedGroupScores.get(i);

            if(groupScore.groupNumber == groupNumber) {
                groupScore.groupScore = killedMonstersCount;
            }

            if(groupScore.groupScore == 0) {
                continue;
            }

            switch(i) {
                case 0:
                    sidebar.getScore("" + ChatColor.GOLD + (groupScore.groupNumber == groupNumber ? ChatColor.BOLD : "") + "Group " + groupScore.groupNumber).setScore(groupScore.groupScore);
                    break;
                case 1:
                    sidebar.getScore("" + ChatColor.DARK_GREEN + (groupScore.groupNumber == groupNumber ? ChatColor.BOLD : "") + "Group " + groupScore.groupNumber).setScore(groupScore.groupScore);
                    break;
                case 2:
                    sidebar.getScore("" + ChatColor.AQUA + (groupScore.groupNumber == groupNumber ? ChatColor.BOLD : "") + "Group " + groupScore.groupNumber).setScore(groupScore.groupScore);
                    break;
                default:
                    sidebar.getScore("" + ChatColor.GRAY + (groupScore.groupNumber == groupNumber ? ChatColor.BOLD : "") + "Group " + groupScore.groupNumber).setScore(groupScore.groupScore);
                    break;
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if(spawnedMonstersList != null) {
            for(LivingEntity ent : spawnedMonstersList) {
                ent.remove();
            }
            timeLeftBossBar.removeAll();
        }
    }

    private void playSoundToAll(Sound sound, float pitch) {
        for(Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), sound, 1, pitch);
        }
    }

    //RUN THIS METHOD ASYNC
    private void updateGroupScore() {
        if(!plugin.isOnline()) {
            return;
        }

        boolean passed = false;
        try {
            URL url = new URL("http://mcgamedev.port0.org:3075/api/localdatabase/challenge6scores");
            URLConnection connection = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) connection;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            String val = "{\"groupNumber\":" + groupNumber + ",\"score\":" + killedMonstersCount + ",\"key\":\"" + Level6.SCORE_UPLOAD_TOKEN + "\"}";
            byte[] out = (val).getBytes(StandardCharsets.UTF_8);
            http.setFixedLengthStreamingMode(out.length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.connect();

            try(OutputStream os = http.getOutputStream()) {
                os.write(out);
            }

            passed = true;
            return;
        } catch(ConnectException ex) {
            System.out.println("Failed connection for post.");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(!passed) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updateGroupScore();
                    }
                }.runTaskLater(plugin, 2L + (int) (Math.random() * 5));
            }
        }
    }

    private void checkChallengeEndTime() {
        if(!plugin.isOnline()) {
            return;
        }

        try {
            URL url = new URL("http://mcgamedev.port0.org:3075/api/localdatabase/challenge6endtime");
            URLConnection req = url.openConnection();
            req.setRequestProperty("key", Level6.SCORE_UPLOAD_TOKEN);
            req.connect();

            JsonObject obj = new JsonParser().parse(new InputStreamReader((InputStream) req.getContent())).getAsJsonObject();

            String time = obj.get("endTime").getAsString();

            String[] timeAMPM = time.split(" ");

            String[] hourMinute = timeAMPM[0].split(":");

            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("PST"));

            cal.set(Calendar.HOUR, Integer.parseInt(hourMinute[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(hourMinute[1]));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.AM_PM, timeAMPM[1].equals("AM") ? Calendar.AM : Calendar.PM);

            Date date = cal.getTime();

            endTime = date.getTime();
        } catch(ConnectException ex) {
            System.out.println("Failed connection for challenge end time.");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //RUN THIS ASYNC
    private List<Level6.GroupScore> getSortedGroupScores() {
        if(!plugin.isOnline()) {
            GroupScore score = new GroupScore(1, killedMonstersCount, 0);
            return new ArrayList<>(Arrays.asList(score));
        }

        try {
            URL url = new URL("http://mcgamedev.port0.org:3075/api/localdatabase/challenge6scores");
            URLConnection req = url.openConnection();
            req.setRequestProperty("key", Level6.SCORE_UPLOAD_TOKEN);
            req.connect();

            JsonObject obj = new JsonParser().parse(new InputStreamReader((InputStream) req.getContent())).getAsJsonObject();

            List<Level6.GroupScore> scores = new ArrayList<>();

            for(Map.Entry<String, JsonElement> element : obj.entrySet()) {
                JsonObject val = element.getValue().getAsJsonObject();
                scores.add(new Level6.GroupScore(Integer.parseInt(element.getKey()), val.get("score").getAsInt(), val.get("lastscoremilli").getAsLong()));
            }

            scores.sort(new Comparator<Level6.GroupScore>() {
                @Override
                public int compare(Level6.GroupScore o1, Level6.GroupScore o2) {
                    int result = Integer.compare(o2.groupScore, o1.groupScore);

                    if(result == 0) {
                        return Long.compare(o1.lastScoreMilli, o2.lastScoreMilli);
                    } else {
                        return result;
                    }
                }
            });

            return scores;
        } catch(ConnectException ex) {
            System.out.println("Failed connection for sorted group retrieval.");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private int getSecondsLeft() {
        long now = new Date().getTime();

        long subtraction = endTime - now;

        if(subtraction <= 0) {
            return 0;
        } else {
            return (int) (subtraction / 1000);
        }
    }

    /*

    TYPES OF MONSTERS SPAWNED:

    There will always be one ravager in the arena

    Main enemies:
    - Vindicator, zombie, skeleton, guardian (always levitation), blaze, wither skeleton, witch, some will have:
        - fire resistance (30%)
        - resistance 1-5 (60%)
        - strength 1-4 (50%)
        - absorption 1-5 (30%)
        - levitation 1-2 (20%)
        - REGEN 1-5 (10%)
        - SPEED 1-5 (20%)
        - WATER BREATHING (30%)

     */

    private static final int MAX_MONSTERS = 50;
    private static final EntityType[] ENTITY_TYPES = new EntityType[] {EntityType.VINDICATOR, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.GUARDIAN, EntityType.BLAZE, EntityType.WITCH, EntityType.WITHER_SKELETON};

    private void spawnMonsters() {
        if(spawnedMonsters >= MAX_MONSTERS) {
            return;
        }

        Random random = new Random();
        int toSpawn = (MAX_MONSTERS - spawnedMonsters);
        toSpawn = random.nextInt((toSpawn / 4 * 3) + 1) + (toSpawn / 4);

        spawnedMonsters += toSpawn;

        for(int i = 0; i < toSpawn; i++) {
            EntityType chosen = ENTITY_TYPES[random.nextInt(ENTITY_TYPES.length)];

            if(!isRavagerAlive) {
                isRavagerAlive = true;
                chosen = EntityType.RAVAGER;
            }

            LivingEntity spawned = (LivingEntity) currentWorld.spawnEntity(getRandomArenaSpawnLocation(), chosen);
            spawned.setMetadata(MONSTER_METADATA_LABEL, new FixedMetadataValue(plugin, null));

            spawnedMonstersList.add(spawned);

            switch(chosen) {
                case GUARDIAN:
                    if(Math.random() < .5) {
                        spawned.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 30, random.nextInt(2) + 1, false));
                        spawned.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 35, 1, false));
                    }
                    break;
                case RAVAGER:
                    LivingEntity passenger = (LivingEntity) currentWorld.spawnEntity(spawned.getLocation(), ENTITY_TYPES[random.nextInt(ENTITY_TYPES.length)]);
                    passenger.setMetadata(MONSTER_METADATA_LABEL, new FixedMetadataValue(plugin, null));
                    spawned.addPassenger(passenger);

                    spawnedMonstersList.add(passenger);
                    spawnedMonsters++;
                    break;
            }

            if(Math.random() < .3) {
                spawned.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 10000, 0, false));
            }
            if(Math.random() < .6) {
                spawned.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 10000, (int) (Math.random() * 3) + 1, false));
            }
            if(Math.random() < .5) {
                spawned.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 10000, (int) (Math.random() * 5) + 1, false));
            }
            if(Math.random() < .3) {
                spawned.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 10000, (int) (Math.random() * 6) + 1, false));
            }
            if(Math.random() < .1) {
                spawned.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 30, (int) (Math.random() * 3) + 1, false));
                spawned.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 35, 1, false));
            }
            if(Math.random() < .1) {
                spawned.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10000, (int) (Math.random() * 6) + 1, false));
            }
            if(Math.random() < .2) {
                spawned.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10000, (int) (Math.random() * 6) + 1, false));
            }
            if(Math.random() < .3) {
                spawned.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 10000, 0, false));
            }
        }
    }

    private Location getRandomArenaSpawnLocation() {
        Location loc = new Location(currentWorld, (int) (Math.random() * 41) - 20, 50, (int) (Math.random() * 41) - 20);

        if(loc.getBlock().getType() == Material.AIR) {
            return loc.getBlock().getLocation();
        } else {
            if(loc.getBlock().getRelative(0, 1, 0).getType() == Material.AIR) {
                return loc.add(0, 1, 0).getBlock().getLocation();
            } else {
                return getRandomArenaSpawnLocation();
            }
        }
    }

    private double getTimeLeftProgress() {
        //calculate time left
        int secondsLeft = getSecondsLeft();

        if(secondsLeft >= 60) {
            return (secondsLeft % 60) / 60d;
        } else {
            return secondsLeft / 60d;
        }
    }

    private String getTimeLeftFormatted() {
        int secondsLeft = getSecondsLeft();

        if(secondsLeft <= 60) {
            return "" + ChatColor.RED + getSecondsLeft() + " second" + (getSecondsLeft() == 1 ? "" : "s") + " left";
        } else {
            int minutesLeft = getSecondsLeft() / 60;
            return minutesLeft + " minute" + (minutesLeft == 1 ? "" : "s") + " left";
        }
    }

    @Override
    public boolean isVictory() {
        return gotHighestScore;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(timeLeftBossBar != null) {
            timeLeftBossBar.addPlayer(event.getPlayer());
        }
        if(killedMonstersScoreboard != null && getSecondsLeft() > 60) {
            event.getPlayer().setScoreboard(killedMonstersScoreboard);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        if(isExemptFromRules(event.getPlayer())) {
            return;
        }

        if(event.getBlock().getLocation().getBlockY() == 49) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplode(BlockExplodeEvent event) {
        Iterator<Block> blocks = event.blockList().iterator();
        while(blocks.hasNext()) {
            Block block = blocks.next();

            if(block.getLocation().getBlockY() == 49) {
                blocks.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> blocks = event.blockList().iterator();
        while(blocks.hasNext()) {
            Block block = blocks.next();

            if(block.getLocation().getBlockY() == 49) {
                blocks.remove();
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().hasMetadata(MONSTER_METADATA_LABEL)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if(!event.getEntity().isValid() || event.getEntity().isDead()) {
                        return;
                    }

                    LivingEntity ent = (LivingEntity) event.getEntity();
                    ent.setCustomName(ChatColor.WHITE + "HP: " + ChatColor.RED + ((Math.round(ent.getHealth() * 10d) / 10d)) + ChatColor.GRAY + " / " + ChatColor.RED + ent.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if(event.getEntity().hasMetadata(MONSTER_METADATA_LABEL)) {
            event.getDrops().clear();
            spawnedMonsters--;
            spawnedMonstersList.remove(event.getEntity());
            if(!isTimeUp) {
                killedMonstersCount++;
                updateScoreboard();
            }
        }
    }

    @Override
    public Location getSpawnPoint() {
        return new Location(currentWorld, 0.5, 53, 0.5);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                event.getEntity().spigot().respawn();
            }
        }.runTaskLater(plugin, 5L);
    }


    private boolean isExemptFromRules(Player p) {
        return p.isOp() && p.getGameMode() == GameMode.CREATIVE;
    }
}
