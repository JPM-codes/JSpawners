package com.github.jpmcodes.spawner.listeners;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.config.PluginConfigSnapshot;
import com.github.jpmcodes.spawner.tasks.SpawnerTask;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.util.List;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;

@Getter
public class SpawnMobsListener implements Listener {
    private final JSpawnerPlugin plugin;
    private WorldGuardPlugin wgPlugin;

    public SpawnMobsListener(JSpawnerPlugin plugin) {
        this.plugin = plugin;
        Plugin wg = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (wg instanceof WorldGuardPlugin) {
            this.wgPlugin = (WorldGuardPlugin) wg;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        PluginConfigSnapshot cfg = this.plugin.getConfigCache().getPlugin();
        if (!cfg.isBloquearMobsNascerem()) {
            return;
        }
        if (SpawnerTask.isSpawning) {
            e.setCancelled(true); // cancela para o PlotSquared não processar
            e.setCancelled(false); // mas deixa spawnar mesmo assim
        }
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER
                || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }

        Location loc = e.getLocation();
        String worldName = loc.getWorld().getName();

        List<String> blockedWorlds = cfg.getBlockedWorlds();
        for (String w : blockedWorlds) {
            if (w.equalsIgnoreCase(worldName)) {
                e.setCancelled(true);
                return;
            }
        }

        List<String> blockedRegions = cfg.getBlockedRegions();
        if (!blockedRegions.isEmpty() && isInsideBlockedRegion(loc, blockedRegions)) {
            e.setCancelled(true);
        }
    }

    private boolean isInsideBlockedRegion(Location loc, List<String> blockedRegions) {
        if (this.wgPlugin == null) {
            return false;
        }

        RegionManager regionManager = this.wgPlugin.getRegionManager(loc.getWorld());
        if (regionManager == null) {
            return false;
        }

        for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : regionManager.getApplicableRegions(loc)) {
            for (String blocked : blockedRegions) {
                if (blocked.equalsIgnoreCase(region.getId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
