package com.github.jpmcodes.spawner.config;

import com.github.jpmcodes.spawner.utils.Configs;

public final class PluginConfigCache {
    private volatile PluginConfigSnapshot plugin;
    private volatile MessagesSnapshot messages;

    public PluginConfigCache() {
        this.plugin = PluginConfigSnapshot.empty();
        this.messages = MessagesSnapshot.empty();
    }

    public synchronized void reloadAll(Configs config, Configs messagesConfig) {
        PluginConfigSnapshot nextPlugin = PluginConfigSnapshot.from(config);
        MessagesSnapshot nextMessages = MessagesSnapshot.from(messagesConfig);
        this.plugin = nextPlugin;
        this.messages = nextMessages;
    }

    public PluginConfigSnapshot getPlugin() {
        return this.plugin;
    }

    public MessagesSnapshot getMessages() {
        return this.messages;
    }
}
