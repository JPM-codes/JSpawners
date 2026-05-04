package com.github.jpmcodes.spawner.data.factory;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import com.github.jpmcodes.spawner.data.models.SpawnerModel;
import com.github.jpmcodes.spawner.data.models.drop.DropModel;
import com.github.jpmcodes.spawner.utils.Configs;
import com.github.jpmcodes.spawner.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SpawnerFactory {

    private final Configs spawnerConfig;
    private final JSpawnerPlugin plugin;

    public SpawnerFactory(JSpawnerPlugin plugin) {
        this.plugin = plugin;
        this.spawnerConfig = plugin.getSpawnerConfig();
    }

    public void load() {
        if (!plugin.getSpawnerCache().isEmpty()) {
            plugin.getSpawnerCache().getCachedElements().clear();
        }

        loadSpawners();
    }

    private void loadSpawners() {
        for (String path : spawnerConfig.getConfig().getConfigurationSection("spawners").getKeys(false)) {
            ConfigurationSection section = spawnerConfig.getConfig().getConfigurationSection("spawners." + path);
            EntityType type = EntityType.valueOf(section.getString("type"));
            int minDelay = section.getInt("min-delay");
            int maxDelay = section.getInt("max-delay");
            int spawnCount = section.getInt("spawn-count");
            int spawnRange = section.getInt("spawn-range");
            // Load drops
            List<DropModel> drops = new LinkedList<>();

            boolean hasItem = section.contains("item");
            ItemStack item = null;
            if (hasItem) {
                String materialString = section.getString("item.material");
                int data = section.getInt("item.data", 0);
                String name = section.getString("item.name", null);
                List<String> lore = section.getStringList("item.lore");
                List<String> enchants = section.getStringList("item.enchants");
                Material material = Material.getMaterial(materialString.toUpperCase());
                if (material == null) {
                    throw new IllegalArgumentException("Material invalido no YAML para spawner " + path + ": " + materialString);
                }
                ItemBuilder itemBuilder = new ItemBuilder(material, 1, data);

                if (name != null) {
                    itemBuilder.name(name);
                }

                if (lore != null && !lore.isEmpty()) {
                    itemBuilder.lore(lore);
                }

                if (enchants != null && !enchants.isEmpty()) {
                    itemBuilder.addEnchants(enchants);
                }

                item = itemBuilder.build();
            }

            // Pega a lista de mapas do YAML
            List<Map<?, ?>> mapList = section.getMapList("drops.list");

            for (Map<?, ?> rawMap : mapList) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) rawMap;
                // Lendo os valores obrigatórios
                Object materialObject = map.get("material");
                int minAmount = (int) map.getOrDefault("min-amount", 1);
                int maxAmount = (int) map.getOrDefault("max-amount", 1);

                // Cuidado com números quebrados no YAML, o Bukkit pode ler como Double ou Integer.
                // Usar (Number) e depois .doubleValue() evita erros de Cast.
                double chance = map.containsKey("chance") ? ((Number) map.get("chance")).doubleValue() : 1.0;

                // Lendo os valores opcionais (podem ser nulos)
                String name = map.containsKey("name") ? (String) map.get("name") : null;

                List<String> lore = new ArrayList<>();
                if (map.containsKey("lore")) {
                    lore = (List<String>) map.get("lore");
                }

                List<String> enchants = new ArrayList<>();
                if (map.containsKey("enchants")) {
                    enchants = (List<String>) map.get("enchants");
                }

                // --- INÍCIO DA CORREÇÃO DO MATERIAL ---
                Material material = null;
                if (materialObject != null) {
                    String materialString = String.valueOf(materialObject);

                    // Tenta pelo nome da String
                    material = Material.getMaterial(materialString.toUpperCase());

                    // Se for nulo, tenta converter para ID numérico (compatível com 1.5.2)
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
                // --- FIM DA CORREÇÃO DO MATERIAL ---
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

                ItemStack dropItem = builder.build();

                // Cria o seu modelo de drop (ajuste de acordo com o construtor da sua classe DropModel)
                DropModel drop = new DropModel(dropItem, chance, minAmount, maxAmount);
                drops.add(drop);
            }

            plugin.getSpawnerCache().addCachedElements(
                    new SpawnerModel(
                            path,
                            type,
                            item,
                            minDelay,
                            maxDelay,
                            spawnCount,
                            spawnRange,
                            drops,
                            null
                    )
            );
        }
    }
}