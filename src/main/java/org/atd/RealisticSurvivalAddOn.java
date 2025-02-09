package org.atd;

import org.atd.gameplay.CampfireSpawn;
import org.atd.gameplay.FarmProtection;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

public final class RealisticSurvivalAddOn extends JavaPlugin {

    private boolean protectionEnabled;

    @Override
    public void onLoad() {
        Plugin realisticSurvivalPlugin = getServer().getPluginManager().getPlugin("RealisticSurvival");

        // Enable features that depend on RealisticSurvival
        getLogger().info(ChatColor.GREEN + "RealisticSurvival found! Full features enabled.");
    }

    @Override
    public void onEnable() {
        getLogger().info("RealisticSurvivalAddOn has been enabled!");

        this.saveDefaultConfig(); // Save default config if it doesn't exist
        loadConfig();            // Load the configuration

        // Database setup:  Better to do this *after* config load in case you need config values
        String databaseURL = "jdbc:sqlite:plugins/RealisticSurvivalAddOn/database.db"; // Or get from config
        try {
            Class.forName("org.sqlite.JDBC"); // Important: Load the JDBC driver
            CampfireSpawn campfireSpawnListener = new CampfireSpawn(this, databaseURL);
            getServer().getPluginManager().registerEvents(campfireSpawnListener, this);
        } catch (ClassNotFoundException e) {
            getLogger().severe("SQLite JDBC driver not found! Disabling CampfireSpawn feature.");
            // Consider disabling the plugin or parts of it if the DB is critical.
            return; // Stop further initialization related to CampfireSpawn.
        }

        getServer().getPluginManager().registerEvents(new FarmProtection(this), this);  // Other listeners

        this.getCommand("rsaoreload").setExecutor(this); // Command registration
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("RealisticSurvivalAddOn has been disabled!");
    }

    public boolean isProtectionEnabled() {
        return protectionEnabled;
    }

    private void loadConfig() {
        // Reload the config from disk
        this.reloadConfig();

        // Load the configuration values
        this.protectionEnabled = this.getConfig().getBoolean("protection-enabled", true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rsaoreload")) {
            // Check if the sender has OP status
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            // Reload the configuration
            loadConfig();

            // Notify the sender that the config has been reloaded
            sender.sendMessage(ChatColor.GREEN + "RealisticSurvivalAddOn configuration reloaded!");
            return true;
        }
        return false;
    }
}