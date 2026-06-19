package me.msicraft.consumefood2;

import de.tr7zw.changeme.nbtapi.NBT;
import me.msicraft.consumefood2.Command.MainCommand;
import me.msicraft.consumefood2.Command.MainTabComplete;
import me.msicraft.consumefood2.CustomFood.CustomFoodManager;
import me.msicraft.consumefood2.CustomFood.Event.CustomFoodRelatedEvent;
import me.msicraft.consumefood2.CustomFood.Menu.Event.CustomFoodEditEvent;
import me.msicraft.consumefood2.File.MessageData;
import me.msicraft.consumefood2.PlayerData.Event.PlayerDataRelatedEvent;
import me.msicraft.consumefood2.PlayerData.PlayerDataManager;
import me.msicraft.consumefood2.VanillaFood.Event.VanillaFoodRelatedEvent;
import me.msicraft.consumefood2.VanillaFood.Menu.Event.VanillaFoodEditEvent;
import me.msicraft.consumefood2.VanillaFood.VanillaFoodManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class ConsumeFood2 extends JavaPlugin {

    private static ConsumeFood2 plugin;

    public static ConsumeFood2 getPlugin() {
        return plugin;
    }

    public static final Component PREFIX = Component.text("[ConsumeFood2] ", NamedTextColor.GREEN);
    private boolean usePlaceHolderAPI = false;
    private boolean useFoodComponent = false;
    private int bukkitVersion = 11601;

    private MessageData messageData;

    private PlayerDataManager playerDataManager;
    private VanillaFoodManager vanillaFoodManager;
    private CustomFoodManager customFoodManager;

    @Override
    public void onEnable() {
        plugin = this;
        createConfigFile();

        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly, disabling the plugin");
            Bukkit.getPluginManager().disablePlugin(this); // <-- Updated here
            return;
        }

        if (getConfig().getBoolean("Compatibility.PlaceholderAPI")) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                usePlaceHolderAPI = true;
                Bukkit.getConsoleSender().sendMessage(PREFIX.append(Component.text("Detect PlaceholderAPI plugin", NamedTextColor.GRAY)));
            }
        }

        BukkitChecker bukkitChecker = new BukkitChecker(this, 119951);
        String bukkitVersionS = bukkitChecker.getBukkitVersion();
        if (bukkitVersionS == null) {
            getServer().getConsoleSender().sendMessage(PREFIX.append(Component.text("Bukkit version not found", NamedTextColor.RED)));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        } else {
            bukkitVersion = bukkitChecker.parseVersion(bukkitVersionS);
            if (bukkitVersion >= 12005) { // 1.20.5 evaluates as 12005
                useFoodComponent = true;
            }
            getServer().getConsoleSender().sendMessage(PREFIX.append(Component.text("Bukkit Version: " + bukkitVersionS + " (" + bukkitVersion + ")", NamedTextColor.GREEN)));
        }

        messageData = new MessageData(this);

        playerDataManager = new PlayerDataManager(this);
        vanillaFoodManager = new VanillaFoodManager(this);
        customFoodManager = new CustomFoodManager(this);

        final int configVersion = plugin.getConfig().contains("config-version", true) ? plugin.getConfig().getInt("config-version") : -1;
        if (configVersion != 1) {
            getServer().getConsoleSender().sendMessage(PREFIX.append(Component.text("You are using the old config", NamedTextColor.RED)));
            getServer().getConsoleSender().sendMessage(PREFIX.append(Component.text("Created the latest config.yml after replacing the old config.yml with config_old.yml", NamedTextColor.RED)));
            replaceConfig();
            createConfigFile();
        } else {
            getServer().getConsoleSender().sendMessage(PREFIX.append(Component.text("You are using the latest version of config.yml", NamedTextColor.GRAY)));
        }

        eventRegister();
        commandRegister();

        reloadVariables();

        getServer().getConsoleSender().sendMessage(PREFIX.append(Component.text("Plugin Enable", NamedTextColor.GREEN)));

        bukkitChecker.getPluginUpdateCheck(version -> {
            String versionS = this.getDescription().getVersion();
            if (versionS.contains("dev")) {
                getLogger().info("Running the development version");
            }
            if (versionS.equals(version)) {
                getLogger().info("There is not a new update available.");
            } else {
                getLogger().info("A new version of the plugin is available: (v." + version + "), Current: v." + versionS);
            }
        });
        Metrics metrics = new Metrics(this, 23298);
        getLogger().info("Enabled metrics. You may opt-out by changing plugins/bStats/config.yml");
    }

    @Override
    public void onDisable() {
        vanillaFoodManager.saveVanillaFood();
        customFoodManager.saveCustomFood();

        playerDataManager.saveAll();

        getServer().getConsoleSender().sendMessage(PREFIX.append(Component.text("Plugin Disable", NamedTextColor.RED)));
    }

    private void eventRegister() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new PlayerDataRelatedEvent(this), this);
        pluginManager.registerEvents(new VanillaFoodRelatedEvent(this), this);
        pluginManager.registerEvents(new CustomFoodRelatedEvent(this), this);
        pluginManager.registerEvents(new CustomFoodEditEvent(this), this);
        pluginManager.registerEvents(new VanillaFoodEditEvent(this), this);
    }

    private void commandRegister() {
        getServer().getPluginCommand("consumefood2").setExecutor(new MainCommand(this));
        getServer().getPluginCommand("consumefood2").setTabCompleter(new MainTabComplete(this));
    }

    public void reloadVariables() {
        reloadConfig();
        messageData.reloadConfig();
        vanillaFoodManager.reloadVariables();
        customFoodManager.reloadVariables();
    }

    protected FileConfiguration config;

    private void createConfigFile() {
        File configf = new File(getDataFolder(), "config.yml");
        if (!configf.exists()) {
            configf.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(configf);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void replaceConfig() {
        File file = new File(getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        File config_old = new File(getDataFolder(), "config_old-" + dateFormat.format(date) + ".yml");
        file.renameTo(config_old);
    }

    public boolean isUseFoodComponent() {
        return useFoodComponent;
    }

    public boolean isUsePlaceHolderAPI() {
        return usePlaceHolderAPI;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public VanillaFoodManager getVanillaFoodManager() {
        return vanillaFoodManager;
    }

    public CustomFoodManager getCustomFoodManager() {
        return customFoodManager;
    }

    public MessageData getMessageData() {
        return messageData;
    }

    public int getBukkitVersion() {
        return bukkitVersion;
    }
}