package com.github.jpmcodes.spawner.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Configs {
    private JavaPlugin plugin;
    private String name;

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    private File file;
    private FileConfiguration config;

    @Generated
    public void setFile(File file) {
        this.file = file;
    }

    @Generated
    public void setConfig(FileConfiguration config) {
        this.config = config;
    }

    @Generated
    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public File getFile() {
        return this.file;
    }

    @Generated
    public FileConfiguration getConfig() {
        return this.config;
    }

    public Configs setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public Configs(String name, JavaPlugin plugin) {
        this.plugin = plugin;
        this.name = name;
        reloadConfig();
    }

    public Configs(String name) {
        this(name, null);
    }

    public void reloadConfig() {
        this.file = new File(this.plugin.getDataFolder(), this.name);
        this.config = (FileConfiguration) YamlConfiguration.loadConfiguration(this.file);
        InputStream defaults = this.plugin.getResource(this.file.getName());
        if (defaults != null) {

            YamlConfiguration loadConfig = YamlConfiguration.loadConfiguration(defaults);
            this.config.setDefaults((Configuration) loadConfig);
        }
    }

    public void saveConfig() {
        try {
            this.config.save(this.file);
        } catch (IOException iOException) {
        }
    }

    public String message(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString(path));
    }

    public Configs saveDefaultConfig() {
        if (this.plugin.getResource(this.name) != null)
            this.plugin.saveResource(this.name, false);
        return this;
    }

    public void remove(String path) {
        this.config.set(path, null);
    }

    public Configs saveDefault() {
        this.config.options().copyDefaults(true);
        saveConfig();
        return this;
    }

    public void setItem(String path, ItemStack item) {
        setItem(create(path), item);
    }

    public ItemStack getItem(String path) {
        return getItem(getSection(path));
    }

    public void setLocation(String path, Location location) {
        setLocation(create(path), location);
    }

    public Location getLocation(String path) {
        return getLocation(getSection(path));
    }

    public static void setItem(ConfigurationSection section, ItemStack item) {
        section.set("id", Integer.valueOf(item.getTypeId()));
        section.set("data", Short.valueOf(item.getDurability()));
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                section.set("name", meta.getDisplayName());
            }
            if (meta.hasLore()) {
                List<String> lines = new ArrayList<>(meta.getLore());
                section.set("lore", lines);
            }
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Enchantment, Integer> enchant : (Iterable<Map.Entry<Enchantment, Integer>>) item
                .getEnchantments()
                .entrySet()) {
            text.append(((Enchantment) enchant.getKey()).getId()).append("-").append(enchant.getValue()).append(",");
        }
        section.set("enchant", text.toString());
    }

    public static void setLocation(ConfigurationSection section, Location location) {
        section.set("world", location.getWorld().getName());
        section.set("x", Double.valueOf(location.getX()));
        section.set("y", Double.valueOf(location.getY()));
        section.set("z", Double.valueOf(location.getZ()));
        section.set("yaw", Float.valueOf(location.getYaw()));
        section.set("pitch", Float.valueOf(location.getPitch()));
    }

    public static Location getLocation(ConfigurationSection section) {
        World world = Bukkit.getWorld(section.getString("world"));
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static Location toLocation(String text) {
        String[] split = text.split(",");
        World world = Bukkit.getWorld(split[0]);
        double x = Double.parseDouble(split[1]);
        double y = Double.parseDouble(split[2]);
        double z = Double.parseDouble(split[3]);
        float yaw = Float.parseFloat(split[4]);
        float pitch = Float.parseFloat(split[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static String toChatMessage(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String saveLocation(Location location) {
        return location.getWorld().getName() + "," + location
                .getX() + ","
                + location
                        .getY()
                + "," + location
                        .getZ()
                + "," + location
                        .getYaw()
                + "," + location
                        .getPitch();
    }

    public static String toConfigMessage(String text) {
        return text.replace("§", "&");
    }

    public static ItemStack getItem(ConfigurationSection section) {
        ItemStack item = new ItemStack(section.getInt("id"), section.getInt("data"));
        ItemMeta meta = item.getItemMeta();
        if (section.contains("name")) {
            meta.setDisplayName(toChatMessage(section.getString("name")));
        }
        if (section.contains("lore")) {
            List<String> lines = new ArrayList<>();
            for (String line : meta.getLore()) {
                lines.add(toChatMessage(line));
            }
            meta.setLore(lines);
        }
        if (section.contains("enchant"))
            for (String value : section.getString("enchant").split(",")) {
                if (!value.isEmpty()) {
                    if (value.contains("-")) {
                        String[] split = value.split("-");
                        item.addUnsafeEnchantment(
                                Enchantment.getById(Integer.parseInt(split[0])),
                                Integer.parseInt(split[1]));
                    }
                }
            }
        return item;
    }

    public boolean delete() {
        return this.file.delete();
    }

    public boolean exists() {
        return this.file.exists();
    }

    public void add(String path, Object value) {
        this.config.addDefault(path, value);
    }

    public boolean contains(String path) {
        return this.config.contains(path);
    }

    public ConfigurationSection create(String path) {
        return this.config.createSection(path);
    }

    public Object get(String path) {
        return this.config.get(path);
    }

    public boolean getBoolean(String path) {
        return this.config.getBoolean(path);
    }

    public ConfigurationSection getSection(String path) {
        return this.config.getConfigurationSection(path);
    }

    public double getDouble(String path) {
        return this.config.getDouble(path);
    }

    public int getInt(String path) {
        return this.config.getInt(path);
    }

    public List<Integer> getIntegerList(String path) {
        return this.config.getIntegerList(path);
    }

    public ItemStack getItemStack(String path) {
        return this.config.getItemStack(path);
    }

    public Set<String> getKeys(boolean deep) {
        return this.config.getKeys(deep);
    }

    public List<?> getList(String path) {
        return this.config.getList(path);
    }

    public long getLong(String path) {
        return this.config.getLong(path);
    }

    public List<Long> getLongList(String path) {
        return this.config.getLongList(path);
    }

    public List<Map<?, ?>> getMapList(String path) {
        return this.config.getMapList(path);
    }

    public String getString(String path) {
        return this.config.getString(path);
    }

    public List<String> getStringList(String path) {
        return this.config.getStringList(path);
    }

    public Map<String, Object> getValues(boolean deep) {
        return this.config.getValues(deep);
    }

    public void set(String path, Object value) {
        this.config.set(path, value);
    }
}