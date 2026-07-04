package com.github.jpmcodes.spawner.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ItemBuilder
        implements Cloneable {
    private ItemStack itemStack;
    private ItemMeta meta;

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.meta = this.itemStack.getItemMeta();
    }

    public ItemBuilder(Material material, int amount, int data) {
        this.itemStack = new ItemStack(material, amount, (short) data);
        this.meta = this.itemStack.getItemMeta();
    }

    public ItemBuilder material(Material material) {
        this.itemStack.setType(material);
        return this;
    }

    public ItemBuilder data(int data) {
        this.itemStack.setDurability((short) data);
        return this;
    }

    public ItemBuilder durability(short durability) {
        this.itemStack.setDurability(durability);
        return this;
    }

    public ItemBuilder addDurability(short durability) {
        short currentDurability = this.itemStack.getDurability();
        if (currentDurability == 0) {
            return this;
        }
        short newDurability = (short) (currentDurability + durability);
        this.itemStack.setDurability(newDurability);
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder name(String name) {
        this.meta.setDisplayName(colorize(name));
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(List<String> lore) {
        this.meta.setLore(colorize(lore));
        return this;
    }

    public ItemBuilder addLore(String... lore) {
        return addLore(Arrays.asList(lore));
    }

    public ItemBuilder addLore(List<String> lore) {
        List<String> newLore = (this.meta.getLore() == null) ? new ArrayList<>() : this.meta.getLore();

        newLore.addAll(lore);
        this.meta.setLore(colorize(newLore));
        return this;
    }

    public ItemBuilder addLoreIf(boolean condition, List<String> lore) {
        if (!condition)
            return this;
        return addLore(lore);
    }

    public ItemBuilder addLoreIf(boolean condition, String... lore) {
        if (!condition)
            return this;
        return addLore(Arrays.asList(lore));
    }

    public ItemBuilder removeLore(String... lore) {
        return removeLore(Arrays.asList(lore));
    }

    public ItemBuilder removeLore(List<String> lore) {
        if (lore.isEmpty()) {
            return this;
        }
        List<String> currentLore = this.meta.getLore();
        List<String> newLore = (currentLore == null) ? new ArrayList<>() : currentLore;

        newLore.removeAll(lore);
        this.meta.setLore(colorize(newLore));
        return this;
    }

    public ItemBuilder removeLoreLine(int line) {
        List<String> currentLore = this.meta.getLore();
        if (currentLore == null || line > currentLore.size()) {
            return this;
        }
        currentLore.remove(line);
        this.meta.setLore(colorize(currentLore));
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        this.meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder addEnchantments(Map<Enchantment, Integer> enchantments) {
        if (enchantments.isEmpty()) {
            return this;
        }
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            this.meta.addEnchant(entry.getKey(), ((Integer) entry.getValue()).intValue(), true);
        }
        return this;
    }

    public ItemBuilder addEnchants(List<String> enchantments) {
        if (enchantments.isEmpty()) {
            return this;
        }
        for (String enchantmentString : enchantments) {
            String[] split = enchantmentString.split(":");
            if (split.length < 2) {
                continue;
            }
            String enchantmentName = split[0];
            int level = Integer.parseInt(split[1]);

            Enchantment enchantment = Enchantment.getByName(enchantmentName);
            if (enchantment == null) {
                continue;
            }
            this.meta.addEnchant(enchantment, level, true);
        }

        return this;
    }

    public ItemBuilder removeEnchantment(Enchantment... enchantments) {
        return removeEnchantment(Arrays.asList(enchantments));
    }

    public ItemBuilder removeEnchantment(List<Enchantment> enchantments) {
        if (enchantments.isEmpty()) {
            return this;
        }
        for (Enchantment enchantment : enchantments) {
            this.itemStack.removeEnchantment(enchantment);
        }
        return this;
    }

    public ItemBuilder skull(String owner) {
        if (this.itemStack == null || this.itemStack
                .getType() != Material.SKULL_ITEM || this.itemStack
                        .getDurability() != 3) {
            this.itemStack = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        }
        SkullMeta skullMeta = (SkullMeta) this.meta;
        skullMeta.setOwner(owner);
        return this;
    }

    public ItemBuilder armor(Color color) {
        LeatherArmorMeta armorMeta = (LeatherArmorMeta) this.meta;
        armorMeta.setColor(color);
        return this;
    }

    public ItemBuilder addPotion(List<String> potions) {
        if (this.itemStack.getType() != Material.POTION) {
            return this;
        }
        for (String potionString : potions) {
            String[] split = potionString.split(":");

            if (split.length < 3) {
                continue;
            }
            String potionName = split[0];
            int duration = Integer.parseInt(split[1]);
            int amplifier = Integer.parseInt(split[2]);

            PotionMeta potionMeta = (PotionMeta) this.meta;
            PotionEffectType type = PotionEffectType.getByName(potionName);

            if (type == null) {
                continue;
            }
            PotionEffect effect = type.createEffect(duration * 20, amplifier);
            potionMeta.addCustomEffect(effect, true);

            Potion potion = Potion.fromItemStack(this.itemStack);
            potion.setSplash(potion.isSplash());
            potion.apply(this.itemStack);
        }

        return this;
    }

    public ItemBuilder addPotion(String potionName, int duration, int amplifier) {
        if (this.itemStack.getType() != Material.POTION) {
            return this;
        }
        PotionMeta potionMeta = (PotionMeta) this.meta;
        PotionEffectType type = PotionEffectType.getByName(potionName);

        if (type == null) {
            return this;
        }
        PotionEffect effect = type.createEffect(duration * 20, amplifier);
        potionMeta.addCustomEffect(effect, true);

        Potion potion = Potion.fromItemStack(this.itemStack);
        potion.setSplash(potion.isSplash());
        potion.apply(this.itemStack);
        return this;
    }

    public ItemBuilder removePotion(String potionName) {
        if (this.itemStack.getType() != Material.POTION) {
            return this;
        }
        PotionMeta potionMeta = (PotionMeta) this.meta;
        PotionEffectType type = PotionEffectType.getByName(potionName);

        if (type == null) {
            return this;
        }
        potionMeta.removeCustomEffect(type);

        Potion potion = Potion.fromItemStack(this.itemStack);
        potion.setSplash(potion.isSplash());
        potion.apply(this.itemStack);
        return this;
    }

    public ItemBuilder clearPotion() {
        if (this.itemStack.getType() != Material.POTION) {
            return this;
        }
        PotionMeta potionMeta = (PotionMeta) this.meta;
        potionMeta.clearCustomEffects();

        Potion potion = Potion.fromItemStack(this.itemStack);
        potion.setSplash(potion.isSplash());
        potion.apply(this.itemStack);
        return this;
    }

    public ItemStack build() {
        this.itemStack.setItemMeta(this.meta);
        return this.itemStack;
    }

    public ItemBuilder clone() {
        try {
            return (ItemBuilder) super.clone();
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private String[] colorize(String... lore) {
        Arrays.setAll(lore, i -> colorize(lore[i]));
        return lore;
    }

    private List<String> colorize(List<String> lore) {
        return (List<String>) lore.stream().map(this::colorize).collect(Collectors.toList());
    }
}