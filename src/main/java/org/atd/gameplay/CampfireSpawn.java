package org.atd.gameplay;

import org.atd.RealisticSurvivalAddOn;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CampfireSpawn implements Listener {

    // Store each player's campfire spawn location
    private final Map<UUID, Location> campfireSpawns = new HashMap<>();
    private final RealisticSurvivalAddOn plugin;

    public CampfireSpawn(RealisticSurvivalAddOn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Ensure the action is a right-click and block isn't null
        if (block != null && event.getAction().isRightClick()
                && block.getType() == Material.CAMPFIRE) {
            Location campfireLocation = block.getLocation().add(0.5, 0, 0.5); // Center the spawn point
            campfireSpawns.put(player.getUniqueId(), campfireLocation);
            player.sendMessage("Your respawn point has been set at this campfire!");
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

        // Remove any campfire respawn point if the campfire is broken
        if (block.getType() == Material.CAMPFIRE) {
            Location brokenCampfireLocation = block.getLocation().add(0.5, 0, 0.5);
            campfireSpawns.values().removeIf(location -> location.equals(brokenCampfireLocation));
            event.getPlayer().sendMessage("A campfire respawn point has been removed!");
        }
    }

    private Location findSafeLocation(Location location) {
        // Example logic to ensure the player doesn't spawn inside the fire block
        Location safeLocation = location.clone();
        safeLocation.setY(location.getWorld().getHighestBlockYAt(location));
        return safeLocation;
    }
}
