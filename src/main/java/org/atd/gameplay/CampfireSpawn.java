package org.atd.gameplay;

import org.atd.RealisticSurvivalAddOn;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.sql.*;
import java.util.UUID;

public class CampfireSpawn implements Listener {

    private final RealisticSurvivalAddOn plugin;
    private final String databaseURL; // Path to your SQLite database file

    public CampfireSpawn(RealisticSurvivalAddOn plugin, String databaseURL) {
        this.plugin = plugin;
        this.databaseURL = databaseURL;
        createTable(); // Initialize the database table
    }

    private void createTable() {
        try (Connection connection = DriverManager.getConnection(databaseURL);
             Statement statement = connection.createStatement()) {

            statement.execute("CREATE TABLE IF NOT EXISTS campfire_spawns (" +
                    "player_uuid TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x INTEGER, " +
                    "y INTEGER, " +
                    "z INTEGER)");

        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating database table: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        World world = player.getWorld(); // Directly get world from player

        if (block != null && action.isRightClick() && block.getType() == Material.CAMPFIRE) { // No need to check world again, it's implied by the block
            Location campfireLocation = block.getLocation();

            // Check if the world is the target world.  This is CRUCIAL!
            if (!campfireLocation.getWorld().getName().equals("world")) {
                return; // Exit if not in the "world" world
            }

            if (!isCampfireOwned(campfireLocation)) {
                try (Connection connection = DriverManager.getConnection(databaseURL);
                     PreparedStatement statement = connection.prepareStatement(
                             "INSERT INTO campfire_spawns (player_uuid, world, x, y, z) VALUES (?, ?, ?, ?, ?)")) {

                    statement.setString(1, player.getUniqueId().toString());
                    statement.setString(2, campfireLocation.getWorld().getName()); // Use the actual world name
                    statement.setInt(3, campfireLocation.getBlockX());
                    statement.setInt(4, campfireLocation.getBlockY());
                    statement.setInt(5, campfireLocation.getBlockZ());
                    statement.executeUpdate();

                    player.sendMessage("Your respawn point has been set at this campfire!");
                } catch (SQLException e) {
                    plugin.getLogger().severe("Error saving campfire spawn to database: " + e.getMessage());
                    player.sendMessage("Error setting respawn point. Please try again.");
                    e.printStackTrace(); // Print the full stack trace for debugging
                }
            } else {
                player.sendMessage("This campfire is already claimed!");
            }
        }
    }

    private boolean isCampfireOwned(Location location) {
        try (Connection connection = DriverManager.getConnection(databaseURL);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM campfire_spawns WHERE world = ? AND x = ? AND y = ? AND z = ?")) {

            statement.setString(1, location.getWorld().getName());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next(); // Returns true if a row is found, false otherwise
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking campfire ownership: " + e.getMessage());
            e.printStackTrace(); // Important for debugging!
            return false; // Assume it's owned to prevent errors from letting players claim it
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
        try (Connection connection = DriverManager.getConnection(databaseURL);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT world, x, y, z FROM campfire_spawns WHERE player_uuid = ?")) {

            statement.setString(1, playerUUID.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String worldName = resultSet.getString("world");
                    int x = resultSet.getInt("x");
                    int y = resultSet.getInt("y");
                    int z = resultSet.getInt("z");
                    return new Location(plugin.getServer().getWorld(worldName), x + 0.5, y, z + 0.5); // Center the location
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

        if (block.getType() == Material.CAMPFIRE) {
            Location brokenCampfireLocation = block.getLocation();
            try (Connection connection = DriverManager.getConnection(databaseURL);
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM campfire_spawns WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                statement.setString(1, brokenCampfireLocation.getWorld().getName());
                statement.setInt(2, brokenCampfireLocation.getBlockX());
                statement.setInt(3, brokenCampfireLocation.getBlockY());
                statement.setInt(4, brokenCampfireLocation.getBlockZ());
                statement.executeUpdate();


                // Notify the player who lost their spawn point (optional)
                // (This requires another database query to get the player UUID)
                // ... (Implementation for notification)

            } catch (SQLException e) {
                plugin.getLogger().severe("Error deleting campfire spawn from database: " + e.getMessage());
            }
        }
    }

    private Location findSafeLocation(Location location) {
        Location safeLocation = location.clone();
        safeLocation.setY(location.getWorld().getHighestBlockYAt(location));
        return safeLocation;
    }
}