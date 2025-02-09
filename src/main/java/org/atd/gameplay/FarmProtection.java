package org.atd.gameplay;

import org.atd.RealisticSurvivalAddOn;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;

public class FarmProtection implements Listener {

    private final RealisticSurvivalAddOn plugin;

    public FarmProtection(RealisticSurvivalAddOn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityInteract(EntityInteractEvent event) {
        if (!plugin.isProtectionEnabled()) return;

        Block block = event.getBlock();
        if(block.getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isProtectionEnabled()) return;

        if(event.getAction() == Action.PHYSICAL) {
            if(Objects.requireNonNull(event.getClickedBlock()).getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
        }
    }
}