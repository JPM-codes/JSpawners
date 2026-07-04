package com.github.jpmcodes.spawner.config;

import com.github.jpmcodes.spawner.utils.Configs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MessagesSnapshot {
    private final Map<String, String> messages;
    private final Map<String, List<String>> messageLists;

    MessagesSnapshot(Map<String, String> messages, Map<String, List<String>> messageLists) {
        this.messages = Collections.unmodifiableMap(messages);
        this.messageLists = Collections.unmodifiableMap(messageLists);
    }

    public static MessagesSnapshot from(Configs messagesConfig) {
        Map<String, String> messages = new HashMap<>();
        Map<String, List<String>> messageLists = new HashMap<>();
        for (String key : messagesConfig.getKeys(false)) {
            Object value = messagesConfig.get(key);
            if (value instanceof String) {
                messages.put(key, translate((String) value));
            } else if (value instanceof List) {
                List<String> translated = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    if (item != null) {
                        translated.add(translate(String.valueOf(item)));
                    }
                }
                messageLists.put(key, Collections.unmodifiableList(translated));
            }
        }
        return new MessagesSnapshot(messages, messageLists);
    }

    public static MessagesSnapshot empty() {
        return new MessagesSnapshot(Collections.emptyMap(), Collections.emptyMap());
    }

    private static String translate(String value) {
        return value.replace("&", "§");
    }

    public String getMessage(String key) {
        return this.messages.getOrDefault(key, "Message not found");
    }

    public List<String> getMessageList(String key) {
        return this.messageLists.getOrDefault(key, Collections.emptyList());
    }
}
