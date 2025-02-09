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
        // Plugin startup logic
        getLogger().info("RealisticSurvivalAddOn has been enabled!");

        // Save the default config if it doesn't exist
        this.saveDefaultConfig();

        // Load the configuration
        loadConfig();

        // Register the event listener
        getServer().getPluginManager().registerEvents(new FarmProtection(this), this);
        getServer().getPluginManager().registerEvents(new CampfireSpawn(this), this);

        // Register the command executor
        this.getCommand("rsaoreload").setExecutor(this);
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