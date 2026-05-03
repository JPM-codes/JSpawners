package com.github.jpmcodes.spawner.commands;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class SetMobCommand implements CommandExecutor {

    private final JSpawnerPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Verifica se quem digitou é um jogador (o console não tem localização)
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        Player p = (Player) sender;

        // Verifica se ele digitou o nome do mob
        if (args.length == 0) {
            p.sendMessage("§cUse: /setmob <mob>");
            return true;
        }

        String mobName = args[0].toUpperCase();

        // Validação (Opcional): Verifica se o mob realmente existe no jogo
        try {
            EntityType.valueOf(mobName);
        } catch (IllegalArgumentException e) {
            p.sendMessage("§cO mob §e" + mobName + " §cnão existe!");
            return true;
        }

        // Pega a localização exata do jogador
        Location loc = p.getLocation();

        // Salva na saves.yml
        String path = "locais_nascimento." + p.getUniqueId() + "." + mobName;
        plugin.getSavesConfig().setLocation(path, loc);
        
        plugin.getSavesConfig().saveConfig();

        p.sendMessage("§aO local de nascimento para os spawners de §e" + mobName + " §afoi definido onde você está!");
        return true;
    }
}