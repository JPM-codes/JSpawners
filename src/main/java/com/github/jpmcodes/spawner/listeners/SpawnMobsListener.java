package com.github.jpmcodes.spawner.listeners;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.Plugin;

import java.util.List;

@RequiredArgsConstructor
public class SpawnMobsListener implements Listener {

    private final JSpawnerPlugin plugin;

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        // Verifica se a opção de bloquear está ativada
        if (!plugin.getConfigs().getBoolean("bloquear-mobs-nascerem")) return;

        if (e.getSpawnReason() == SpawnReason.SPAWNER || e.getSpawnReason() == SpawnReason.SPAWNER_EGG) {
            return;
        }

        Location loc = e.getLocation();
        String worldName = loc.getWorld().getName();

        // 1. Verificar por Mundo
        List<String> blockedWorlds = plugin.getConfigs().getStringList("mundo-mobs");
        if (blockedWorlds != null) {
            for (String w : blockedWorlds) {
                if (w != null && !w.isEmpty() && w.equalsIgnoreCase(worldName)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        // 2. Verificar por Region (WorldGuard)
        List<String> blockedRegions = plugin.getConfigs().getStringList("region-mobs");
        if (blockedRegions != null && !blockedRegions.isEmpty()) {
            if (isInsideBlockedRegion(loc, blockedRegions)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private boolean isInsideBlockedRegion(Location loc, List<String> blockedRegions) {
        Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin instanceof WorldGuardPlugin) {
            WorldGuardPlugin wg = (WorldGuardPlugin) wgPlugin;
            RegionManager rm = wg.getRegionManager(loc.getWorld());
            if (rm != null) {
                ApplicableRegionSet set = rm.getApplicableRegions(loc);
                for (ProtectedRegion region : set) {
                    for (String blocked : blockedRegions) {
                        if (blocked != null && !blocked.isEmpty() && blocked.equalsIgnoreCase(region.getId())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
