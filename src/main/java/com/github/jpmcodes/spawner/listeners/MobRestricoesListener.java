package com.github.jpmcodes.spawner.listeners;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.utils.Debug;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

@RequiredArgsConstructor
public class MobRestricoesListener implements Listener {

    private final JSpawnerPlugin plugin;

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity().hasMetadata("mob_spawner")) {

            Debug.info(e.getCause().name());
            Debug.info(e.getCause().toString());
            Debug.info(e.getEntity().toString());

            // Impedir dano de água/chuva/projéteis para Enderman
            if (e.getEntity().getType() == EntityType.ENDERMAN) {
                // Bloqueia dano de água (DROWNING/MELTING), contato (blocos), e PROJÉTEIS (flechas fazem ele teleportar)
                if (e.getCause() == EntityDamageEvent.DamageCause.DROWNING ||
                        e.getCause() == EntityDamageEvent.DamageCause.CONTACT ||
                        e.getCause() == EntityDamageEvent.DamageCause.MELTING ||
                        e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                    e.setCancelled(true);
                    return;
                }
            }

            // Cancela o knockback resetando a velocidade no próximo tick se o movimento estiver desativado
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (e.getEntity().isValid()) {
                    e.getEntity().setVelocity(new Vector(0, 0, 0));
                }
            });
        }
    }

    // 1. Desativar Ataque (Dano) e Teleporte por Flecha
    @EventHandler
    public void onMobAttack(EntityDamageByEntityEvent e) {
        // Se o Enderman é o ALVO de uma flecha, cancelamos para ele não teleportar antes do dano
        if (e.getEntity().getType() == EntityType.ENDERMAN && e.getEntity().hasMetadata("mob_spawner")) {
            if (e.getDamager() instanceof org.bukkit.entity.Projectile) {
                e.setCancelled(true);
                return;
            }
        }

        // Verifica se quem está batendo é o nosso mob
        if (e.getDamager().hasMetadata("mob_spawner")) {
            e.setCancelled(true); // Cancela o dano
        }
    }

    // 2. Desativar Foco (Ajuda muito no disable-movement e disable-attack)
    // Isso impede que o mob fique tentando andar na direção do jogador
    @EventHandler
    public void onMobTarget(EntityTargetEvent e) {
        if (e.getEntity().hasMetadata("mob_spawner")) {
            e.setCancelled(true);
        }
    }

    // 3. Desativar Explosão do Creeper
    // Usamos ExplosionPrimeEvent que é chamado um segundo antes dele explodir
    @EventHandler
    public void onCreeperExplode(ExplosionPrimeEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.Creeper && e.getEntity().hasMetadata("mob_spawner")) {
            e.setCancelled(true); // Impede ele de explodir
        }
    }

    // 4. Desativar Teleporte do Enderman (Genérico)
    @EventHandler
    public void onEndermanTeleport(EntityTeleportEvent e) {
        if (e.getEntityType() == EntityType.ENDERMAN &&
                e.getEntity().hasMetadata("mob_spawner") &&
                !e.getEntity().hasMetadata("returning")) {

            e.setCancelled(true); // Tenta cancelar (pode ser ignorado pelo NMS)

            final Entity enderman = e.getEntity();
            final Location origin = e.getFrom().clone();

            enderman.setMetadata("returning", new FixedMetadataValue(JSpawnerPlugin.getInstance(), true));

            Bukkit.getScheduler().scheduleSyncDelayedTask(JSpawnerPlugin.getInstance(), () -> {
                enderman.teleport(origin);
                enderman.removeMetadata("returning", JSpawnerPlugin.getInstance());
            }, 1L);
        }
    }
}