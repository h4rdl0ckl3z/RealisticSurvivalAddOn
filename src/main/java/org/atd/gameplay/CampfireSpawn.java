package org.atd.gameplay;

import org.atd.RealisticSurvivalAddOn;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.sql.*;
import java.util.UUID;

public class CampfireSpawn implements Listener, CommandExecutor {

    private final RealisticSurvivalAddOn plugin;
    private final String databaseURL;

    public CampfireSpawn(RealisticSurvivalAddOn plugin, String databaseURL) {
        this.plugin = plugin;
        this.databaseURL = databaseURL;
        createTable();
        plugin.getCommand("home").setExecutor(this);
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS campfire_spawns (" +
                "player_uuid TEXT PRIMARY KEY, " +
                "world TEXT, " +
                "x INTEGER, " +
                "y INTEGER, " +
                "z INTEGER)";

        try (Connection connection = DriverManager.getConnection(databaseURL);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating database table: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CAMPFIRE) return;

        Player player = event.getPlayer();
        Location campfireLocation = block.getLocation();

        if (!campfireLocation.getWorld().getName().equals("world")) {
            player.sendMessage("You can only set a respawn point in the 'world' world.");
            return;
        }

        if (isCampfireOwned(campfireLocation)) {
            player.sendMessage("This campfire is already claimed!");
            return;
        }

        setCampfireSpawn(player, campfireLocation);
    }

    private void setCampfireSpawn(Player player, Location location) {
        String sql = "INSERT INTO campfire_spawns (player_uuid, world, x, y, z) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(databaseURL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, location.getWorld().getName());
            statement.setInt(3, location.getBlockX());
            statement.setInt(4, location.getBlockY());
            statement.setInt(5, location.getBlockZ());
            statement.executeUpdate();

            player.sendMessage("Your respawn point has been set at this campfire!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving campfire spawn to database: " + e.getMessage());
            player.sendMessage("Error setting respawn point. Please try again.");
        }
    }

    private boolean isCampfireOwned(Location location) {
        String sql = "SELECT 1 FROM campfire_spawns WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (Connection connection = DriverManager.getConnection(databaseURL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, location.getWorld().getName());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking campfire ownership: " + e.getMessage());
            return true; // Assume it's owned to prevent errors from letting players claim it
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location campfireLocation = getCampfireLocation(player.getUniqueId());

        if (campfireLocation != null) {
            Location safeLocation = findSafeLocation(campfireLocation);
            event.setRespawnLocation(safeLocation);
            player.sendMessage("You respawned at your campfire!");
        }
    }

    private Location getCampfireLocation(UUID playerUUID) {
        String sql = "SELECT world, x, y, z FROM campfire_spawns WHERE player_uuid = ?";

        try (Connection connection = DriverManager.getConnection(databaseURL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, playerUUID.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String worldName = resultSet.getString("world");
                    int x = resultSet.getInt("x");
                    int y = resultSet.getInt("y");
                    int z = resultSet.getInt("z");
                    return new Location(plugin.getServer().getWorld(worldName), x + 0.5, y, z + 0.5);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving campfire location: " + e.getMessage());
        }
        return null;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CAMPFIRE) return;

        Location brokenCampfireLocation = block.getLocation();
        String sql = "DELETE FROM campfire_spawns WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (Connection connection = DriverManager.getConnection(databaseURL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, brokenCampfireLocation.getWorld().getName());
            statement.setInt(2, brokenCampfireLocation.getBlockX());
            statement.setInt(3, brokenCampfireLocation.getBlockY());
            statement.setInt(4, brokenCampfireLocation.getBlockZ());
            statement.executeUpdate();

            // Optional: Notify the player who lost their spawn point
            // (Implementation for notification can be added here)

        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting campfire spawn from database: " + e.getMessage());
        }
    }

    private Location findSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Invalid location provided to findSafeLocation: " + location);
            return null; // Or handle the error appropriately
        }
        Location safeLocation = location.clone();
        World world = safeLocation.getWorld();
        int highestY = world.getHighestBlockYAt(safeLocation);
        safeLocation.setY(highestY);

        //Check for obstructions above the campfire
        for(int y = highestY; y < world.getMaxHeight(); y++){
            Block block = world.getBlockAt(safeLocation.getBlockX(), y, safeLocation.getBlockZ());
            if(!block.getType().isAir()){
                safeLocation.setY(y+1);
                break;
            }
        }
        return safeLocation;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("home")) {
            Location homeLocation = getCampfireLocation(player.getUniqueId());
            if (homeLocation != null) {
                Location safeLocation = findSafeLocation(homeLocation);
                player.teleport(safeLocation);
                player.sendMessage("You have been teleported to your campfire home.");
            } else {
                player.sendMessage("You have not set a campfire home yet.");
            }
            return true;
        }
        return false;
    }
}