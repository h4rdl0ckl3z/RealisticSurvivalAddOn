package org.atd.gameplay;

import org.atd.RealisticSurvivalAddOn;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CampfireSpawn implements Listener {

    // Store each player's campfire spawn location
    private final Map<Location, UUID> campfireOwners = new HashMap<>(); // Campfire -> Owner
    private final Map<UUID, Location> campfireSpawns = new HashMap<>(); // Player -> Campfire
    private final RealisticSurvivalAddOn plugin;
    private final int maxPlayersPerCampfire = 1; // Limit to 1 player

    public CampfireSpawn(RealisticSurvivalAddOn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Action action = event.getAction();

        if (block != null && action.isRightClick() && block.getType() == Material.CAMPFIRE) {
            Location campfireLocation = block.getLocation().add(0.5, 0, 0.5);

            if (!campfireOwners.containsKey(campfireLocation)) { // Check if campfire is already owned
                campfireOwners.put(campfireLocation, player.getUniqueId());
                campfireSpawns.put(player.getUniqueId(), campfireLocation);
                player.sendMessage("Your respawn point has been set at this campfire!");
            } else {
                player.sendMessage("This campfire is already claimed!");
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location campfireLocation = campfireSpawns.get(player.getUniqueId());

        if (campfireLocation != null) {
            // Ensure safe respawn
            Location safeLocation = findSafeLocation(campfireLocation);
            event.setRespawnLocation(safeLocation);
            player.sendMessage("You respawned at your campfire!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.CAMPFIRE) {
            Location brokenCampfireLocation = block.getLocation().add(0.5, 0, 0.5);
            UUID owner = campfireOwners.remove(brokenCampfireLocation); // Remove ownership

            if (owner != null) {
                campfireSpawns.remove(owner); // Remove respawn location
                // Notify the player who lost their spawn point (optional)
                Player ownerPlayer = plugin.getServer().getPlayer(owner);
                if (ownerPlayer != null) {
                    ownerPlayer.sendMessage("Your campfire respawn point has been removed!");
                }
            }
        }
    }

    private Location findSafeLocation(Location location) {
        // Example logic to ensure the player doesn't spawn inside the fire block
        Location safeLocation = location.clone();
        safeLocation.setY(location.getWorld().getHighestBlockYAt(location));
        return safeLocation;
    }
}
