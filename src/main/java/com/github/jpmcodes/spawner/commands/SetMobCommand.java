package com.github.jpmcodes.spawner.commands;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.PlayerSpawnerModel;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import lombok.Generated;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class SetMobCommand implements CommandExecutor {
    @Generated
    public SetMobCommand(JSpawnerPlugin plugin) {
        this.plugin = plugin;
    }

    private final JSpawnerPlugin plugin;

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        Player p = (Player) sender;

        if (args.length == 0) {
            p.sendMessage("§cUse: /setmob <mob>");
            return true;
        }

        String mobName = args[0].toUpperCase();
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(mobName);
        } catch (IllegalArgumentException e) {
            p.sendMessage("§cO mob §e" + mobName + " §cnão existe!");
            return true;
        }

        Location loc = p.getLocation();
        double stackRadius = this.plugin.getConfigCache().getPlugin().getStackMobsRadius();

        // Verificar se está no ar e se o mob vai cair para fora do raio
        if (p.isFlying() || loc.getBlock().getType() == org.bukkit.Material.AIR) {
            boolean foundGround = false;
            // Verificamos se há algum bloco sólido abaixo dentro do raio de stack
            for (int i = 1; i <= (int) stackRadius; i++) {
                if (loc.clone().subtract(0, i, 0).getBlock().getType().isSolid()) {
                    foundGround = true;
                    break;
                }
            }

            if (!foundGround) {
                p.sendMessage("§cVocê não pode definir o local de nascimento no ar a mais de §e" + (int) stackRadius + " §cblocos do chão!");
                return true;
            }
        }

        String path = "locais_nascimento." + p.getUniqueId() + "." + mobName;
        this.plugin.getSavesConfig().setLocation(path, loc);

        this.plugin.getSavesConfig().saveConfig();
        this.plugin.getSpawnLocationCache().put(path, loc);

        PlayerSpawnerModel playerSpawner = this.plugin.getPlayerSpawnerCache().getByPlayerUUID(p.getUniqueId());
        if (playerSpawner != null) {
            double activationRange = this.plugin.getConfigCache().getPlugin().getActivationRange();
            double activationRangeSq = activationRange * activationRange;

            for (SpawnerModel spawner : playerSpawner.getSpawners()) {
                if (spawner.getType() == entityType) {
                    Location spawnerLoc = spawner.getLocation();
                    if (spawnerLoc == null || spawnerLoc.getWorld() == null) continue;

                    for (Entity entity : spawnerLoc.getWorld().getEntities()) {
                        if (entity.hasMetadata("spawner-id")) {
                            for (MetadataValue meta : entity.getMetadata("spawner-id")) {
                                if (meta.asString().equals(spawner.getId())) {
                                    // Só teletransporta se estiver perto do spawner original
                                    if (entity.getLocation().distanceSquared(spawnerLoc) <= activationRangeSq) {
                                        entity.teleport(loc);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        p.sendMessage("§aO local de nascimento para os spawners de §e" + mobName + " §afoi definido onde você está!");
        return true;
    }
}