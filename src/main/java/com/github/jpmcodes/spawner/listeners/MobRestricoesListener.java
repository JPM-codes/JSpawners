package com.github.jpmcodes.spawner.listeners;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.utils.Debug;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

@RequiredArgsConstructor
@Getter
public class MobRestricoesListener implements Listener {
    private final JSpawnerPlugin plugin;

    private boolean isSpawnerMob(Entity entity) {
        return entity.hasMetadata("stack-spawner");
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!isSpawnerMob(e.getEntity())) {
            return;
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.CUSTOM
                || e.getCause() == EntityDamageEvent.DamageCause.MAGIC) {
            e.setCancelled(true);
            return;
        }

        if (e.getEntity().getType() == EntityType.ENDERMAN
                && (e.getCause() == EntityDamageEvent.DamageCause.DROWNING
                || e.getCause() == EntityDamageEvent.DamageCause.CONTACT
                || e.getCause() == EntityDamageEvent.DamageCause.MELTING
                || e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobAttack(EntityDamageByEntityEvent e) {
        if (e.getEntity().getType() == EntityType.ENDERMAN && isSpawnerMob(e.getEntity()) &&
                e.getDamager() instanceof org.bukkit.entity.Projectile) {
            e.setCancelled(true);
            return;
        }

        if (isSpawnerMob(e.getDamager())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobTarget(EntityTargetEvent e) {
        if (isSpawnerMob(e.getEntity())) {
            e.setCancelled(true);
            e.setTarget(null);
        }
    }

    @EventHandler
    public void onCreeperExplode(ExplosionPrimeEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.Creeper && isSpawnerMob(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEndermanTeleport(EntityTeleportEvent e) {
        if (e.getEntityType() == EntityType.ENDERMAN && isSpawnerMob(e.getEntity())) {
            e.setCancelled(true);
        }
    }
}
