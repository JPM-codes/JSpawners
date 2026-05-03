package com.github.jpmcodes.spawner.listeners;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

@RequiredArgsConstructor
public class MobRestricoesListener implements Listener {

    private final JSpawnerPlugin plugin;

    // 1. Desativar Ataque (Dano)
    @EventHandler
    public void onMobAttack(EntityDamageByEntityEvent e) {
        // Verifica se quem está batendo é o nosso mob
        if (e.getDamager().hasMetadata("mob_spawner")) {
            if (plugin.getConfig().getBoolean("mobs.disable-attack")) {
                e.setCancelled(true); // Cancela o dano
            }
        }
    }

    // 2. Desativar Foco (Ajuda muito no disable-movement e disable-attack)
    // Isso impede que o mob fique tentando andar na direção do jogador
    @EventHandler
    public void onMobTarget(EntityTargetEvent e) {
        if (e.getEntity().hasMetadata("mob_spawner")) {
            if (plugin.getConfig().getBoolean("mobs.disable-attack") || plugin.getConfig().getBoolean("mobs.disable-movement")) {
                e.setCancelled(true);
            }
        }
    }

    // 3. Desativar Explosão do Creeper
    // Usamos ExplosionPrimeEvent que é chamado um segundo antes dele explodir
    @EventHandler
    public void onCreeperExplode(ExplosionPrimeEvent e) {
        if (e.getEntity() instanceof Creeper && e.getEntity().hasMetadata("mob_spawner")) {
            if (plugin.getConfig().getBoolean("mobs.disable-explosion")) {
                e.setCancelled(true); // Impede ele de explodir
            }
        }
    }

    // 4. Desativar Teleporte do Enderman
    @EventHandler
    public void onEndermanTeleport(EntityTeleportEvent e) {
        if (e.getEntity() instanceof Enderman && e.getEntity().hasMetadata("mob_spawner")) {
            if (plugin.getConfig().getBoolean("mobs.disable-teleport")) {
                e.setCancelled(true); // Impede ele de sumir/teleportar
            }
        }
    }
}