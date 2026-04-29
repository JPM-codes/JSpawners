package com.github.jpmcodes.spawner.commands;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.utils.Messages;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class SpawnerCommand implements CommandExecutor, TabExecutor {

    private final JSpawnerPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            Messages.HELP.getMessageList().forEach(sender::sendMessage);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            Messages.HELP.getMessageList().forEach(sender::sendMessage);
            return true;
        }

        if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("lista")) {
            Messages.SPAWNER_LIST.getMessageList().forEach(msg -> {
                StringBuilder spawnerList = new StringBuilder();
                plugin.getSpawnerCache().getCachedElements().forEach(spawner -> {
                    spawnerList.append(spawner.getId().toUpperCase()).append(", ");
                });
                sender.sendMessage(msg.replace("{mobs}", spawnerList.toString()));
            });
            return true;
        }

        String mobID = args[0];
        SpawnerModel spawner = plugin.getSpawnerCache().getByID(mobID);

        if (spawner == null) {
            sender.sendMessage(Messages.SPAWNER_NOT_FOUND.getMessage()
                    .replace("{mob}", mobID));
            return true;
        }

        Player target;
        int amount;

        // Lógica para: /spawner <mob> <quantidade>
        if (args.length == 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cO console precisa especificar um jogador: /spawner <mob> <jogador> <quantidade>");
                return true;
            }
            target = (Player) sender;

            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cA quantidade precisa ser um número válido.");
                return true;
            }

            target.getInventory().addItem(spawner.getItem());
            target.sendMessage(Messages.SPAWNER_GIVEN_SELF.getMessage().replace("{mob}", spawner.getType().name()).replace("{amount}", String.valueOf(amount)));
        }

        // Lógica para: /spawner <mob> <player> <quantidade>
        else if (args.length == 3) {
            target = plugin.getServer().getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage(Messages.PLAYER_NOT_FOUND.getMessage()
                        .replace("{player}", args[1]));
                return true;
            }

            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cA quantidade precisa ser um número válido.");
                return true;
            }

            target.getInventory().addItem(spawner.getItem());
            sender.sendMessage(Messages.SPAWNER_GIVEN.getMessage().replace("{mob}", spawner.getType().name()).replace("{player}", target.getName()).replace("{amount}", String.valueOf(amount)));
        }

        // Caso o usuário digite argumentos demais
        else {
            Messages.HELP.getMessageList().forEach(sender::sendMessage);
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cA quantidade deve ser maior que zero.");
            return true;
        }


        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
