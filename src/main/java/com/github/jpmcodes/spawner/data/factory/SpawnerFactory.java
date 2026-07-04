package com.github.jpmcodes.spawner.data.factory;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.data.models.drop.DropModel;
import com.github.jpmcodes.spawner.utils.Configs;
import com.github.jpmcodes.spawner.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class SpawnerFactory {
    private final Configs spawnerConfig;
    private final JSpawnerPlugin plugin;

    public SpawnerFactory(JSpawnerPlugin plugin) {
        this.plugin = plugin;
        this.spawnerConfig = plugin.getSpawnerConfig();
    }

    public void load() {
        if (!this.plugin.getSpawnerCache().isEmpty()) {
            this.plugin.getSpawnerCache().getCachedElements().clear();
        }

        loadSpawners();
    }

    private void loadSpawners() {
        for (String path : this.spawnerConfig.getConfig().getConfigurationSection("spawners").getKeys(false)) {
            ConfigurationSection section = this.spawnerConfig.getConfig().getConfigurationSection("spawners." + path);
            EntityType type = EntityType.valueOf(section.getString("type").toUpperCase());

            List<DropModel> drops = new LinkedList<>();

            List<Map<?, ?>> mapList = section.getMapList("drops.list");

            for (Map<?, ?> rawMap : mapList) {
                ItemStack dropItem;

                if (rawMap.containsKey("iteminfo")) {
                    String itemInfo = String.valueOf(rawMap.get("iteminfo"));
                    dropItem = parseItemInfo(itemInfo);
                } else {
                    Object materialObject = rawMap.get("material");

                    String name = rawMap.containsKey("name") ? (String) rawMap.get("name") : null;

                    short durabilidade = rawMap.containsKey("durability") ? ((Number) rawMap.get("durability")).shortValue()
                            : 0;

                    List<String> lore = new ArrayList<>();
                    if (rawMap.containsKey("lore")) {
                        lore = (List<String>) rawMap.get("lore");
                    }

                    List<String> enchants = new ArrayList<>();
                    if (rawMap.containsKey("enchants")) {
                        enchants = (List<String>) rawMap.get("enchants");
                    }

                    Material material = null;
                    if (materialObject != null) {
                        String materialString = String.valueOf(materialObject);

                        material = Material.getMaterial(materialString.toUpperCase());

                        if (material == null) {
                            try {
                                int id = Integer.parseInt(materialString);
                                material = Material.getMaterial(id);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }

                    if (material == null) {
                        throw new IllegalArgumentException("Material invalido no YAML: " + materialObject);
                    }

                    ItemBuilder builder = new ItemBuilder(material);

                    if (name != null) {
                        builder.name(name);
                    }

                    if (lore != null && !lore.isEmpty()) {
                        builder.lore(lore);
                    }

                    if (enchants != null && !enchants.isEmpty()) {
                        builder.addEnchants(enchants);
                    }

                    if (durabilidade != 0) {
                        builder.durability(durabilidade);
                    }

                    dropItem = builder.build();
                }

                int minAmount = rawMap.containsKey("min-amount") ? ((Number) rawMap.get("min-amount")).intValue() : 1;
                int maxAmount = rawMap.containsKey("max-amount") ? ((Number) rawMap.get("max-amount")).intValue() : 1;

                double chance = rawMap.containsKey("chance") ? ((Number) rawMap.get("chance")).doubleValue() : 1.0D;

                DropModel drop = new DropModel(dropItem, chance, minAmount, maxAmount);
                drops.add(drop);
            }

            double mcmmoXp = section.getDouble("mcmmo-xp", 0.0D);

            this.plugin.getSpawnerCache().addCachedElements(new SpawnerModel(path, type, drops, mcmmoXp, null, null));
        }
    }

    private ItemStack parseItemInfo(String itemInfo) {
        String[] parts = itemInfo.split(":");
        String materialName = parts[0].trim();
        Material material = Material.getMaterial(materialName.toUpperCase());

        if (material == null) {
            try {
                material = Material.getMaterial(Integer.parseInt(materialName));
            } catch (NumberFormatException ignored) {
            }
        }

        if (material == null) {
            throw new IllegalArgumentException("Material invalido no YAML: " + itemInfo);
        }

        short durability = 0;
        if (parts.length > 1) {
            durability = Short.parseShort(parts[1].trim());
        }
        return new ItemStack(material, 1, durability);
    }
}