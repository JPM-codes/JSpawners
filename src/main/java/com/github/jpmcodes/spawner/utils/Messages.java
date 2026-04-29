package com.github.jpmcodes.spawner.utils;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public enum Messages {
    HELP("help"),
    NO_PERMISSION("no-permission"),
    MOB_NOT_FOUND("mob-not-found"),
    PLAYER_NOT_FOUND("player-not-found"),
    SPAWNER_NOT_FOUND("spawner-not-found"),
    SPAWNER_GIVEN("spawner-given"),
    SPAWNER_GIVEN_SELF("spawner-given-self"),
    SPAWNER_FULL_INVENTORY("spawner-full-inventory"),
    SPAWNER_LIST("spawner-list"),
    INVALID_AMOUNT("invalid-amount"),
    SPAWNER_NOT_OWNER("spawner-not-owner"),
    SPAWNER_BREAK_SUCCESS("spawner-break-success"),
    SPAWNER_BREAK_NOT_OWNER("spawner-break-not-owner"),
    SPAWNER_PLACE_SUCCESS("spawner-place-success"),
    SPAWNER_LIMIT_REACHED("spawner-limit-reached"),
    SPAWNER_SILK_TOUCH_REQUIRED("spawner-silk-touch-required");

    private final String path;

    public String getMessage() {
        return JSpawnerPlugin.getInstance().getMessagesConfig().getConfig().getString(path, "Message not found").replace("&", "§");
    }

    public List<String> getMessageList() {
        return JSpawnerPlugin.getInstance().getMessagesConfig().getConfig().getStringList(path).stream().map(s -> s.replace("&", "§")).collect(Collectors.toList());
    }
}
