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

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigs().reloadConfig();
            plugin.getSpawnerConfig().reloadConfig();
            plugin.getMessagesConfig().reloadConfig();
            plugin.getSpawnerFactory().load();
            sender.sendMessage("§aConfigurações recarregadas com sucesso.");
            return true;
        }

        Messages.HELP.getMessageList().forEach(sender::sendMessage);
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
