package me.tazadejava.main;

import com.google.gson.*;
import me.tazadejava.levels.*;
import me.tazadejava.specialitems.SelectionWand;
import me.tazadejava.specialitems.SpecialItem;
import me.tazadejava.specialitems.SpecialItemEventListener;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TeamChallengeMCGD extends JavaPlugin {

    private HashMap<String, String> playerNames = new HashMap<>();
    private int groupConfig;

    private Level currentLevel;

    private boolean isOnline = false;

    @Override
    public void onEnable() {
        World currentWorld = getServer().getWorlds().get(0);

        //check if API backend is online to connect to
        checkIfOnline();

        if(!isOnline) {
            getLogger().info("The backend server is currently offline, so any requests to it will be silenced. The whitelist is not active.");
        }

        saveDefaultConfig();

        int levelConfig = getConfig().getInt("current-level");
        groupConfig = getConfig().getInt("current-group");
        switch(levelConfig) {
            case 1:
                currentLevel = new Level1(this, currentWorld);
                break;
            case 2:
                currentLevel = new Level2(this, currentWorld);
                break;
            case 3:
                currentLevel = new Level3(this, currentWorld);
                break;
            case 4:
                currentLevel = new Level4(this, currentWorld);
                break;
            case 5:
                currentLevel = new Level5(this, currentWorld);
                break;
            case 6:
                currentLevel = new Level6(this, currentWorld, groupConfig);
                break;
            default:
                getLogger().severe("The current-level attribute " + levelConfig + " does not exist! THIS PLUGIN CANNOT BE ENABLED WITHOUT A PROPER LEVEL!");
                getServer().getPluginManager().disablePlugin(this);
                return;
        }

        getLogger().info("[TeamChallengeMCGD] Current level: " + levelConfig + ". Current group: " + groupConfig + ".");

        getServer().getPluginManager().registerEvents(currentLevel, this);
        getServer().getPluginManager().registerEvents(new GeneralLevelListener(this, currentLevel), this);

        HashMap<String, SpecialItem> specialItems = new HashMap<>();
        specialItems.put("wand", new SelectionWand());

        getServer().getPluginManager().registerEvents(new SpecialItemEventListener(specialItems.values()), this);

        OpCommandHandler opCommandHandler;
        getCommand("mcgd").setExecutor(opCommandHandler = new OpCommandHandler(this, specialItems));

        ChallengeCommandHandler challengeCommandHandler = new ChallengeCommandHandler(currentLevel);
        getCommand("challenge").setExecutor(challengeCommandHandler);

        switch(levelConfig) {
            case 2:
                ((Level2) currentLevel).addOpCommandHandler(opCommandHandler);
        }

        if(currentLevel.startOnEnable()) {
            currentLevel.startLevel();
        }
    }

    /**
     * marks the most recent code upload as the winning upload
     */
    public void markAsWinPlugin() {
        if(!isOnline) {
            return;
        }

        try {
            URL url = new URL("http://mcgamedev.port0.org:3075/api/submitvictoryplugin");
            URLConnection connection = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) connection;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            byte[] out = ("{\"groupNumber\":" + groupConfig + ",\"key\":\"b25692cc-5bac-49b3-a90c-f05035d6644b\"}").getBytes(StandardCharsets.UTF_8);
            http.setFixedLengthStreamingMode(out.length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.connect();

            try(OutputStream os = http.getOutputStream()) {
                os.write(out);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long lastGroupObtain = -1;
    private String[] groupUsernames;

    /**
     * Requires backend online status. Gets all group member usernames for whitelist.
     * @return
     */
    public String[] getAllGroupMemberMinecraftUsernames() {
        if(lastGroupObtain != -1) {
            return groupUsernames;
        } else {
            lastGroupObtain = System.currentTimeMillis();

            JsonObject groupData = getGroupData();
            JsonObject users = getUserData();

            playerNames.clear();

            if(groupData == null || users == null) {
                getLogger().severe("Invalid group/user data! Is the server offline?");
                return null;
            } else {
                JsonArray membersArray = groupData.getAsJsonObject("group" + groupConfig).get("members").getAsJsonArray();
                List<String> usernames = new ArrayList<>();

                for(JsonElement member : membersArray) { //TODO: potential choke point: if invalid username, it crashes
                    String username = users.getAsJsonObject(member.getAsString()).get("minecraftUsername").getAsString();
                    if(!username.equals("UNKNOWN")) {
                        usernames.add(username);
                        playerNames.put(username, users.getAsJsonObject(member.getAsString()).get("name").getAsString());
                    }
                }

                groupUsernames = usernames.toArray(new String[0]);
                return groupUsernames;
            }
        }
    }

    /**
     * Checks if backend is online, and sets isOnline to appropiate variable.
     */
    private void checkIfOnline() {
        try {
            URL url = new URL("http://mcgamedev.port0.org:3075/api/onlinetest");
            URLConnection req = url.openConnection();
            req.connect();

            isOnline = true;
        } catch(ConnectException ex) {
            isOnline = false;
        } catch (MalformedURLException e) {
            isOnline = false;
        } catch (IOException e) {
            isOnline = false;
        }
    }

    /**
     * Requires backend online status. Will get group data.
     * @return
     */
    private JsonObject getGroupData() {
        try {
            URL url = new URL("http://mcgamedev.port0.org:3075/api/localdatabase/groupdata");
            URLConnection req = url.openConnection();
            req.setRequestProperty("key", "ece4a377-2077-406c-b6d3-71aea2284fd7");
            req.connect();

            return new JsonParser().parse(new InputStreamReader((InputStream) req.getContent())).getAsJsonObject();
        } catch(ConnectException ex) {
            return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Requires backend online status. Will get individual user data.
     * @return
     */
    private JsonObject getUserData() {
        try {
            URL url = new URL("http://mcgamedev.port0.org:3075/api/localdatabase/userdata");
            URLConnection req = url.openConnection();
            req.setRequestProperty("key", "a166da74-d919-4eb2-8076-ee5a33115cf4");
            req.connect();

            return new JsonParser().parse(new InputStreamReader((InputStream) req.getContent())).getAsJsonObject();
        } catch(ConnectException ex) {
            return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Returns real name of a Minecraft username.
     * @param p
     * @return
     */
    public String getRealName(Player p) {
        if(playerNames.containsKey(p.getName())) {
            return playerNames.get(p.getName());
        } else {
            return "";
        }
    }

    /**
     * Cross checks with group usernames to allow player onto server. Only works when online backend is up.
     * @param p
     * @return
     */
    public boolean isAllowedOnServer(Player p) {
        if(p.isOp()) {
            return true;
        }
        if(!isOnline) {
            return true;
        }

        for(String username : getAllGroupMemberMinecraftUsernames()) {
            if(p.getName().equals(username)) {
                return true;
            }
        }

        return false;
    }

    public boolean isOnline() {
        return isOnline;
    }

    @Override
    public void onDisable() {
        currentLevel.onDisable();
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if(label.equalsIgnoreCase("gm")) {
            if(sender instanceof Player) {
                Player p = (Player) sender;

                if(p.getGameMode() == GameMode.CREATIVE) {
                    p.setGameMode(GameMode.SURVIVAL);
                } else {
                    p.setGameMode(GameMode.CREATIVE);
                }
            }

            return true;
        }

        return super.onCommand(sender, cmd, label, args);
    }
}
