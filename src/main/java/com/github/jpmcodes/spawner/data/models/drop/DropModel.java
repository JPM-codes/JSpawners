package com.github.jpmcodes.spawner.data.models.drop;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
@Getter
@Setter
public class DropModel {
    private ItemStack item;
    private double chance;
    private int minAmount;
    private int maxAmount;
}
