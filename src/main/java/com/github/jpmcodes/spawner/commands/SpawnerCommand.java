package com.github.jpmcodes.spawner.commands;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.utils.Messages;
import java.util.Collections;
import java.util.List;
import lombok.Generated;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public class SpawnerCommand implements CommandExecutor, TabExecutor {
    private final JSpawnerPlugin plugin;

    @Generated
    public SpawnerCommand(JSpawnerPlugin plugin) {
        this.plugin = plugin;
    }

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
                this.plugin.getSpawnerCache().getCachedElements()
                        .forEach(spawner -> spawnerList.append(spawner.getId()).append(", "));
                sender.sendMessage(msg.replace("{mobs}", spawnerList.toString()));
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.getConfigs().reloadConfig();
            this.plugin.getSpawnerConfig().reloadConfig();
            this.plugin.getMessagesConfig().reloadConfig();
            this.plugin.getConfigCache().reloadAll(this.plugin.getConfigs(), this.plugin.getMessagesConfig());
            this.plugin.getSpawnerFactory().load();
            sender.sendMessage("§aConfigurações recarregadas com sucesso.");
            return true;
        }

        Messages.HELP.getMessageList().forEach(sender::sendMessage);
        return false;
    }

    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
