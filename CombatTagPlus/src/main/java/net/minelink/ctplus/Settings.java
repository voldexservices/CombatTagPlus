package net.minelink.ctplus;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

public final class Settings {

    private final CombatTagPlus plugin;

    Settings(CombatTagPlus plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        Configuration defaults = plugin.getConfig().getDefaults();
        if(defaults != null) {
            defaults.set("disabled-worlds", new ArrayList<>());
            defaults.set("command-blacklist", new ArrayList<>());
            defaults.set("command-whitelist", new ArrayList<>());
        }
        reload();
    }

    public void update() {
        // Initialize the new config cache
        List<Map<String, Object>> config = new ArrayList<>();

        // Default config path
        Path path = Paths.get(plugin.getDataFolder().getAbsolutePath() + File.separator + "config.yml");

        // Iterate through new config
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("config.yml")));
        for (String key : defaultConfig.getKeys(true)) {
            // Convert key to correct format for a single line
            String oneLineKey = StringUtils.repeat("  ", key.split(".").length) + key + ": ";

            // Generate new config section
            Map<String, Object> section = new HashMap<>();
            section.put("key", oneLineKey);

            // Attempt to save value from old configuration
            if (key.equals("config-version")) {
                section.put("value", defaultConfig.get(key));
            } else if (plugin.getConfig().get(key) != null) {
                section.put("value", plugin.getConfig().get(key));
            } else {
                section.put("value", defaultConfig.get(key));
            }

            // Save section to cache
            config.add(section);
        }

        // Attempt to open the default config
        try (BufferedReader br = new BufferedReader(new InputStreamReader(plugin.getResource("config.yml")))) {
            // Iterate through all lines in this config
            String current;
            String previous = null;
            List<String> comments = new ArrayList<>();
            while ((current = br.readLine()) != null) {
                // If previous line is a comment add it, else clear the comments
                if (previous != null && previous.matches("(| +)#.*")) {
                    comments.add(previous);
                } else {
                    comments.clear();
                }

                // Iterate through current config cache
                for (Map<String, Object> section : config) {
                    // Do nothing if key is not valid
                    if (section.get("key") == null) continue;

                    // Do nothing if there are no comments to assign
                    if (comments.isEmpty()) continue;

                    // Do nothing if current line doesn't start with this key
                    String key = section.get("key").toString();
                    if (!current.startsWith(key.substring(0, key.length() - 1))) continue;

                    // Add comment to config cache
                    section.put("comments", new ArrayList<>(comments));
                }

                // Set the previous line
                previous = current;
            }
        } catch (IOException e) {
            // Failed to read from default config within plugin jar
            plugin.getLogger().severe("**CONFIG ERROR**");
            plugin.getLogger().severe("Failed to read from default config within plugin jar.");

            // Leave a stack trace in console
            e.printStackTrace();
        }

        // Attempt to open the new config
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // Iterate through new cached config
            for (int i = 0; i < config.size(); i++) {
                // Get section of the config at this index
                Map<String, Object> section = config.get(i);

                // Do nothing if key is invalid
                if (section.get("key") == null) continue;

                // Do nothing if value is invalid
                if (section.get("value") == null) continue;

                // Write the comments if they are valid
                Object comments = section.get("comments");
                if (comments != null && comments instanceof List) {
                    for (Object o : (List) comments) {
                        writer.write(o.toString());
                        writer.newLine();
                    }
                }

                // Write the key
                String key = section.get("key").toString();
                writer.write(key);

                // Write the value
                Object value = section.get("value");

                if (value instanceof String) {
                    writer.write("'" + value.toString() + "'");
                } else if (value instanceof List) {
                    List list = (List) value;
                    int indent = key.length() - key.replace(" ", "").length() - 1;
                    for (Object s : list) {
                        writer.newLine();
                        writer.write(StringUtils.repeat(" ", indent) + "  - '" + s.toString() + "'");
                    }
                } else {
                    writer.write(value.toString());
                }

                // Write a couple more lines for extra space ;-)
                if (config.size() > i + 1) {
                    writer.newLine();
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            // Failed to write new config file to the disk
            plugin.getLogger().severe("**CONFIG ERROR**");
            plugin.getLogger().severe("Failed to write an updated config to the disk.");

            // Leave a stack trace in console
            e.printStackTrace();
        }

        // Reload the updated configuration
        plugin.reloadConfig();
        load();
    }

    private int configVersion, latestConfigVersion, tagDuration, logoutWaitTime, npcDespawnTime, forceFieldRadius;
    private byte forceFieldMaterialDamage;
    private boolean resetTagOnPearl, playEffect, alwaysSpawn, mobTagging, instantlyKill, spawnNPC, untagOnKick, onlyTagAttacker,
            disableSelfTagging, disableBlockEdit, disableStorageAccess, disableCreativeTags, disableEnderpearls, disableFlying,
            disableTeleportation, disableCrafting, resetDespawnTimeOnHit, generateRandomName, useBarApi, denySafezone,
            denySafezoneEnderpearl, useForceFields, untagOnPluginTeleport;
    private String tagMessage, tagUnknownMessage, untagMessage, logoutCancelledMessage, logoutSuccessMessage, logoutPendingMessage,
            disableBlockEditMessage, disableStorageAccessMessage, disableEnderpearlsMessage, disableFlyingMessage,
            disableTeleportationMessage, disableCraftingMessage, randomNamePrefix, killMessage, killMessageItem, barApiEndedMessage,
            barApiCountdownMessage, forceFieldMaterial, disabledCommandMessage, commandUntagMessage, commandTagMessage;
    private List<String> commandWhitelist, commandBlacklist, untagOnKickBlacklist, disabledWorlds;

    private void reload() {
        this.configVersion = plugin.getConfig().getInt("config-version", 0);
        this.latestConfigVersion = plugin.getConfig().getDefaults().getInt("config-version", 0);
        this.tagDuration = plugin.getConfig().getInt("tag-duration", 15);
        this.logoutWaitTime = plugin.getConfig().getInt("logout-wait-time", 10);
        this.npcDespawnTime = plugin.getConfig().getInt("npc-despawn-time", 60);
        this.forceFieldRadius = plugin.getConfig().getInt("force-field-radius");
        this.forceFieldMaterialDamage = (byte) plugin.getConfig().getInt("force-field-material-damage");

        this.resetTagOnPearl = plugin.getConfig().getBoolean("reset-tag-on-pearl");
        this.playEffect = plugin.getConfig().getBoolean("play-effect");
        this.alwaysSpawn = plugin.getConfig().getBoolean("always-spawn");
        this.mobTagging = plugin.getConfig().getBoolean("mob-tagging");
        this.instantlyKill = plugin.getConfig().getBoolean("instantly-kill");
        this.spawnNPC = plugin.getConfig().getBoolean("spawn-npc", false);
        this.untagOnKick = plugin.getConfig().getBoolean("untag-on-kick");
        this.onlyTagAttacker = plugin.getConfig().getBoolean("only-tag-attacker");
        this.disableSelfTagging = plugin.getConfig().getBoolean("disable-self-tagging");
        this.disableBlockEdit = plugin.getConfig().getBoolean("disable-block-edit");
        this.disableStorageAccess = plugin.getConfig().getBoolean("disable-storage-access");
        this.disableCreativeTags = plugin.getConfig().getBoolean("disable-creative-tags");
        this.disableEnderpearls = plugin.getConfig().getBoolean("disable-enderpearls");
        this.disableFlying = plugin.getConfig().getBoolean("disable-flying");
        this.disableTeleportation = plugin.getConfig().getBoolean("disable-teleportation");
        this.disableCrafting =plugin.getConfig().getBoolean("disable-crafting");
        this.resetDespawnTimeOnHit = plugin.getConfig().getBoolean("reset-despawn-time-on-hit");
        this.generateRandomName = plugin.getConfig().getBoolean("generate-random-name");
        this.useBarApi = plugin.getConfig().getBoolean("barapi");
        this.denySafezone = plugin.getConfig().getBoolean("deny-safezone");
        this.denySafezoneEnderpearl = plugin.getConfig().getBoolean("deny-safezone-enderpearl");
        this.useForceFields = plugin.getConfig().getBoolean("force-fields");
        this.untagOnPluginTeleport = plugin.getConfig().getBoolean("untag-on-plugin-teleport");

        this.tagMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("tag-message", ""));
        this.tagUnknownMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("tag-unknown-message", ""));
        this.untagMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("untag-message", ""));
        this.logoutCancelledMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("logout-cancelled-message", ""));
        this.logoutSuccessMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("logout-success-message", ""));
        this.logoutPendingMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("logout-pending-message", ""));

        this.disableBlockEditMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("disable-block-edit-message", ""));
        this.disableStorageAccessMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("disable-storage-access-message", ""));
        this.disableEnderpearlsMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("disable-enderpearls-message", ""));
        this.disableFlyingMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("disable-flying-message", ""));
        this.disableTeleportationMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("disable-teleportation-message", ""));
        this.disableCraftingMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("disable-crafting-message", ""));
        this.randomNamePrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("random-name-prefix", ""));
        this.killMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("kill-message", ""));
        this.killMessageItem = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("kill-message-item", ""));
        this.barApiEndedMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("barapi-ended-message", "&aYou are no longer in combat!"));
        this.barApiCountdownMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("barapi-countdown-message", "&eCombatTag: &f{remaining}"));
        this.forceFieldMaterial = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("force-field-material"));
        this.disabledCommandMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("disabled-command-message", ""));
        this.commandUntagMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("command-untag-message"));
        this.commandTagMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("command-tag-message"));

        this.commandWhitelist = plugin.getConfig().getStringList("command-whitelist");
        this.commandBlacklist = plugin.getConfig().getStringList("command-blacklist");
        this.untagOnKickBlacklist = plugin.getConfig().getStringList("untag-on-kick-blacklist");
        this.disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public int getLatestConfigVersion() {
        return latestConfigVersion;
    }

    public boolean isOutdated() {
        return getConfigVersion() < getLatestConfigVersion();
    }

    public int getTagDuration() {
        return tagDuration;
    }

    public String getTagMessage() {
        return tagMessage;
    }

    public String getTagUnknownMessage() {
        return tagUnknownMessage;
    }

    public String getUntagMessage() {
        return untagMessage;
    }

    public boolean resetTagOnPearl() {
        return resetTagOnPearl;
    }

    public boolean playEffect() {
        return playEffect;
    }

    public boolean alwaysSpawn() {
        return alwaysSpawn;
    }

    public boolean mobTagging() {
        return mobTagging;
    }

    public int getLogoutWaitTime() {
        return logoutWaitTime;
    }

    public String getLogoutCancelledMessage() {
        return logoutCancelledMessage;
    }

    public String getLogoutSuccessMessage() {
        return logoutSuccessMessage;
    }

    public String getLogoutPendingMessage() {
        return logoutPendingMessage;
    }

    public boolean instantlyKill() {
        return instantlyKill;
    }

    public boolean spawnNPC() {
        return spawnNPC;
    }

    public boolean untagOnKick() {
        return untagOnKick;
    }

    public List<String> getUntagOnKickBlacklist() {
        return untagOnKickBlacklist;
    }

    public boolean onlyTagAttacker() {
        return onlyTagAttacker;
    }

    public boolean disableSelfTagging() {
        return disableSelfTagging;
    }

    public boolean disableBlockEdit() {
        return disableBlockEdit;
    }

    public String getDisableBlockEditMessage() {
        return disableBlockEditMessage;
    }

    public boolean disableStorageAccess() {
        return disableStorageAccess;
    }

    public String getDisableStorageAccessMessage() {
        return disableStorageAccessMessage;
    }

    public boolean disableCreativeTags() {
        return disableCreativeTags;
    }

    public boolean disableEnderpearls() {
        return disableEnderpearls;
    }

    public String getDisableEnderpearlsMessage() {
        return disableEnderpearlsMessage;
    }

    public boolean disableFlying() {
        return disableFlying;
    }

    public String getDisableFlyingMessage() {
        return disableFlyingMessage;
    }

    public boolean disableTeleportation() {
        return disableTeleportation;
    }

    public String getDisableTeleportationMessage() {
        return disableTeleportationMessage;
    }

    public boolean disableCrafting() {
        return disableCrafting;
    }

    public String getDisableCraftingMessage() {
        return disableCraftingMessage;
    }

    public int getNpcDespawnTime() {
        return npcDespawnTime;
    }

    public int getNpcDespawnMillis() {
        return getNpcDespawnTime() * 1000;
    }

    public boolean resetDespawnTimeOnHit() {
        return resetDespawnTimeOnHit;
    }

    public boolean generateRandomName() {
        return generateRandomName;
    }

    public String getRandomNamePrefix() {
        return randomNamePrefix;
    }

    public String getKillMessage() {
        return killMessage;
    }

    public String getKillMessageItem() {
        return killMessageItem;
    }

    public boolean useBarApi() {
        return useBarApi;
    }

    public String getBarApiEndedMessage() {
        return barApiEndedMessage;
    }

    public String getBarApiCountdownMessage() {
        return barApiCountdownMessage;
    }

    public boolean denySafezone() {
        return denySafezone;
    }

    public boolean denySafezoneEnderpearl() {
        return denySafezoneEnderpearl;
    }

    public boolean useForceFields() {
        return useForceFields;
    }

    public int getForceFieldRadius() {
        return forceFieldRadius;
    }

    public String getForceFieldMaterial() {
        return forceFieldMaterial;
    }

    public byte getForceFieldMaterialDamage() {
        return forceFieldMaterialDamage;
    }

    public boolean useFactions() {
        return plugin.getConfig().getBoolean("factions", false);
    }

    public boolean useTowny() {
        return plugin.getConfig().getBoolean("towny", false);
    }

    public boolean useWorldGuard() {
        return plugin.getConfig().getBoolean("worldguard", false);
    }

    public boolean useArchonGuard() {
        return plugin.getConfig().getBoolean("archonguard", true);
    }

    public List<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public String getDisabledCommandMessage() {
        return disabledCommandMessage;
    }

    public boolean isCommandBlacklisted(String message) {
        if (message.charAt(0) == '/') {
            message = message.substring(1);
        }

        message = message.toLowerCase();

        for (String command : commandWhitelist) {
            if (command.equals("*") || message.equals(command) || message.startsWith(command + " ")) {
                return false;
            }
        }

        for (String command : commandBlacklist) {
            if (command.equals("*") || message.equals(command) || message.startsWith(command + " ")) {
                return true;
            }
        }

        return false;
    }

    public boolean untagOnPluginTeleport() {
        return untagOnPluginTeleport;
    }

    public String getCommandUntagMessage() {
        return commandUntagMessage;
    }
    
    public String getCommandTagMessage() {
        return commandTagMessage;
    }

    public String formatDuration(long seconds) {
        List<String> parts = new ArrayList<>();
        for (TimeUnit timeUnit : new TimeUnit[] { DAYS, HOURS, MINUTES, SECONDS }) {
            long duration = seconds / SECONDS.convert(1, timeUnit);
            if (duration > 0) {
                seconds -= SECONDS.convert(duration, timeUnit);
                String englishWord = timeUnit.name().toLowerCase(Locale.ENGLISH);
                String durationWord = plugin.getConfig().getString("duration-words." + englishWord, englishWord);
                parts.add(duration + " " + durationWord);
            }
        }
        String formatted = StringUtils.join(parts, ", ");
        if (formatted.contains(", ")) {
            int index = formatted.lastIndexOf(", ");
            StringBuilder builder = new StringBuilder(formatted);
            formatted = builder.replace(index, index + 2, " and ").toString();
        }

        return formatted;
    }
}
