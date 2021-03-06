/**
 * BetonQuest - advanced quests for Bukkit
 * Copyright (C) 2015  Jakub "Co0sh" Sapalski
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pl.betoncraft.betonquest.utils;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;

import pl.betoncraft.betonquest.BetonQuest;
import pl.betoncraft.betonquest.config.ConfigAccessor;
import pl.betoncraft.betonquest.config.ConfigHandler;
import pl.betoncraft.betonquest.config.Zipper;
import pl.betoncraft.betonquest.database.Database;
import pl.betoncraft.betonquest.database.Database.QueryType;
import pl.betoncraft.betonquest.database.Database.UpdateType;

/**
 * Various utilities.
 * 
 * @author Coosh
 */
public class Utils {

    /**
     * Does a full configuration backup.
     */
    public static void backup() {
        Debug.broadcast("Backing up!");
        long time = new Date().getTime();
        BetonQuest instance = BetonQuest.getInstance();
        if (!backupDatabase(new File(instance.getDataFolder(), "database-backup.yml"))) {
            Debug.error("There was an error during backing up the database! This does not affect"
                + " the configuration backup, nor damage your database. You should backup"
                + " the database maually if you want to be extra safe, but it's not necessary if"
                + " you don't want to downgrade later.");
        }
        // create backups folder if it does not exist
        File backupFolder = new File(instance.getDataFolder(), "backups");
        if (!backupFolder.isDirectory()) {
            backupFolder.mkdir();
        }
        // zip all the files
        String outputPath = backupFolder.getAbsolutePath() + File.separator + "backup-"
            + instance.getConfig().getString("version", null);
        new Zipper(instance.getDataFolder().getAbsolutePath(), outputPath);
        // delete database backup so it doesn't make a mess later on
        new File(instance.getDataFolder(), "database-backup.yml").delete();
        // done
        Debug.info("Done in " + (new Date().getTime() - time) + "ms");
        Debug.broadcast("Done, you can find the backup in \"backups\" directory.");
    }
    
    /**
     * Backs the database up to a specified .yml file (it should not exist)
     * 
     * @param databaseBackupFile
     *            non-existent file where the database should be dumped
     * @return true if the backup was successful, false if there was an error
     */
    public static boolean backupDatabase(File databaseBackupFile) {
        BetonQuest instance = BetonQuest.getInstance();
        try {
            // prepare the config file
            databaseBackupFile.createNewFile();
            ConfigAccessor accessor = new ConfigAccessor(instance,
                    databaseBackupFile, databaseBackupFile.getName());
            FileConfiguration config = accessor.getConfig(); 
            // prepare the database and map
            Database database = instance.getDB();
            HashMap<String, ResultSet> map = new HashMap<>();
            String[] tables = new String[]{"objectives", "tags", "points", "journals"};
            // open database connection
            database.openConnection();
            // load resultsets into the map
            for (String table : tables) {
                Debug.info("Loading " + table);
                String enumName = ("LOAD_ALL_" + table).toUpperCase();
                map.put(table, database.querySQL(QueryType.valueOf(enumName), new String[]{}));
            }
            // extract data from resultsets into the config file
            for (String key : map.keySet()) {
                Debug.info("Saving " + key + " to the backup file");
                // prepare resultset and meta
                ResultSet res = map.get(key);
                ResultSetMetaData rsmd = res.getMetaData();
                // get the list of column names
                List<String> columns = new ArrayList<>();
                int columnCount = rsmd.getColumnCount();
                Debug.info("  There are " + columnCount + " columns in this ResultSet");
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    String columnName = rsmd.getColumnName(i);
                    Debug.info("    Adding column " + columnName);
                    columns.add(columnName);
                }
                // counter for counting rows
                int counter = 0;
                while (res.next()) {
                    // for each column add a value to a config
                    for (String columnName : columns) {
                        String value = res.getString(columnName);
                        config.set(key + "." + counter + "." + columnName, value);
                    }
                    counter++;
                }
                Debug.info("  Saved " + (counter + 1) + " rows");
            }
            // close connection
            database.closeConnection();
            // save the config at the end
            accessor.saveConfig();
            return true;
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            File brokenFile = new File(instance.getDataFolder(), "database-backup.yml");
            if (brokenFile.exists()) {
                brokenFile.delete();
            }
            return false;
        }
    }

    /**
     * Converts string to list of pages for a book. SingleString defines if you
     * passed a string separated by "|" for every page. False means that it is
     * separated, true that it isn't.
     * 
     * @param string
     *            text to convert
     * @param singleString
     *            if it's a single string or it has characters splitting it to
     *            pages
     * @return the list of pages for a book
     */
    public static List<String> pagesFromString(String string, boolean singleString) {
        List<String> pages = new ArrayList<>();
        if (singleString) {
            StringBuilder page = new StringBuilder();
            for (String word : string.split(" ")) {
                if (page.length() + word.length() + 1 > 245) {
                    pages.add(page.toString().trim());
                    page = new StringBuilder();
                }
                page.append(word + " ");
            }
            pages.add(page.toString().trim());
        } else {
            pages = Arrays.asList(string.replaceAll("\\\\n", "\n").split("\\|"));
        }
        return pages;
    }

    /**
     * If the database backup file exists, loads it into the database.
     */
    public static void loadDatabaseFromBackup() {
        boolean isOldDatabaseBackedUP = false;
        String filename = null;
        try {
            BetonQuest instance = BetonQuest.getInstance();
            File file = new File(instance.getDataFolder(), "database-backup.yml");
            // if the backup doesn't exist then there is nothing to load, return
            if (!file.exists()) {
                return;
            }
            Debug.broadcast("Loading database backup!");
            // backup the database
            File backupFolder = new File(instance.getDataFolder(), "backups");
            if (!backupFolder.isDirectory()) {
                backupFolder.mkdirs();
            }
            int i = 0;
            while (new File(backupFolder, "old-database-" + i + ".yml").exists()) {
                i++;
            }
            filename = "old-database-" + i + ".yml";
            Debug.broadcast("Backing up old database!");
            if (!(isOldDatabaseBackedUP = backupDatabase(new File(backupFolder, filename)))) {
                Debug.error("There was an error during old database backup process. This means that"
                    + " if the plugin loaded new database (from backup), the old one would be lost "
                    + "forever. Because of that the loading of backup was aborted!");
                return;
            }
            ConfigAccessor accessor = new ConfigAccessor(instance, file, "database-backup.yml");
            FileConfiguration config = accessor.getConfig();
            Database database = instance.getDB();
            // create tables if they don't exist, so we can be 100% sure
            // that we can drop them without an error (should've been done
            // in a different way...)
            database.createTables(instance.isMySQLUsed());
            // drop all tables
            database.openConnection();
            database.updateSQL(UpdateType.DROP_OBJECTIVES, new String[]{});
            database.updateSQL(UpdateType.DROP_TAGS, new String[]{});
            database.updateSQL(UpdateType.DROP_POINTS, new String[]{});
            database.updateSQL(UpdateType.DROP_JOURNALS, new String[]{});
            database.closeConnection();
            // create new tables
            database.createTables(instance.isMySQLUsed());
            // load objectives
            database.openConnection();
            ConfigurationSection objectives = config.getConfigurationSection("objectives");
            if (objectives != null) for (String key : objectives.getKeys(false)) {
                database.updateSQL(UpdateType.INSERT_OBJECTIVE, new String[]{
                    objectives.getString(key + ".id"),
                    objectives.getString(key + ".playerID"),
                    objectives.getString(key + ".instructions"),
                });
            }
            // load tags
            ConfigurationSection tags = config.getConfigurationSection("tags");
            if (tags != null) for (String key : tags.getKeys(false)) {
                database.updateSQL(UpdateType.INSERT_TAG, new String[]{
                    tags.getString(key + ".id"),
                    tags.getString(key + ".playerID"),
                    tags.getString(key + ".tag"),
                });
            }
            // load points
            ConfigurationSection points = config.getConfigurationSection("points");
            if (points != null) for (String key : points.getKeys(false)) {
                database.updateSQL(UpdateType.INSERT_POINT, new String[]{
                    points.getString(key + ".id"),
                    points.getString(key + ".playerID"),
                    points.getString(key + ".category"),
                    points.getString(key + ".count"),
                });
            }
            // load journals
            ConfigurationSection journals = config.getConfigurationSection("journals");
            if (journals != null) for (String key : journals.getKeys(false)) {
                database.updateSQL(UpdateType.INSERT_JOURNAL, new String[]{
                    journals.getString(key + ".id"),
                    journals.getString(key + ".playerID"),
                    journals.getString(key + ".pointer"),
                    journals.getString(key + ".date"),
                });
            }
            ConfigurationSection backpack = config.getConfigurationSection("backpack");
            if (backpack != null) for (String key : backpack.getKeys(false)) {
                database.updateSQL(UpdateType.INSERT_BACKPACK, new String[]{
                    backpack.getString(key + ".id"),
                    backpack.getString(key + ".playerID"),
                    backpack.getString(key + ".instruction"),
                    backpack.getString(key + ".amount"),
                });
            }
            database.closeConnection();
            // delete backup file so it doesn't get loaded again
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
            if (isOldDatabaseBackedUP) {
                Debug.error("Your database probably got corrupted, sorry for that :( The good news"
                    + " is that you have a backup of your old database, you can find it in backups"
                    + " folder, named as " + filename + ". You can try to use it to load the "
                    + "backup, but it will probably have the same effect. Please contact the "
                    + "developer at <coosheck@gmail.com> in order to fix this manually.");
            } else {
                Debug.error("There was an error during database loading, but fortunatelly the "
                    + "original database wasn't even touched yet. You can try to load the backup "
                    + "again, and if the problem persists you should contact the developer to find"
                    + " a solution.");
            }
        }
    }
    
    /**
     * Converts ItemStack to string, which can be later parsed by QuestItem
     * 
     * @param item
     *          ItemStack to convert
     * @return converted string
     */
    @SuppressWarnings("deprecation")
    public static String itemToString(ItemStack item) {
        String name = "";
        String lore = "";
        String enchants = "";
        String title = "";
        String text = "";
        String author = "";
        String effects = "";
        String color = "";
        String owner = "";
        ItemMeta meta = item.getItemMeta();
        // get display name
        if (meta.hasDisplayName()) {
            name = " name:" + meta.getDisplayName().replace(" ", "_");
        }
        // get lore
        if (meta.hasLore()) {
            StringBuilder string = new StringBuilder();
            for (String line : meta.getLore()) {
                string.append(line + ";");
            }
            lore = " lore:" + string.substring(0, string.length() - 1).replace(" ", "_");
        }
        // get enchants
        if (meta.hasEnchants()) {
            StringBuilder string = new StringBuilder();
            for (Enchantment enchant : meta.getEnchants().keySet()) {
                string.append(enchant.getName() + ":" + meta.getEnchants().get(enchant) + ",");
            }
            enchants = " enchants:" + string.substring(0, string.length() - 1);
        }
        // check if it's a book and add title, author and text if so
        if (meta instanceof BookMeta) {
            BookMeta bookMeta = (BookMeta) meta;
            if (bookMeta.hasAuthor()) {
                author = " author:" + bookMeta.getAuthor().replace(" ", "_");
            }
            if (bookMeta.hasTitle()) {
                title = " title:" + bookMeta.getTitle().replace(" ", "_");
            }
            if (bookMeta.hasPages()) {
                StringBuilder strBldr = new StringBuilder();
                for (String page : bookMeta.getPages()) {
                    if (page.startsWith("\"") && page.endsWith("\"")) {
                        page = page.substring(1, page.length() - 1);
                    }
                    strBldr.append(page.replaceAll(" ", "_").replaceAll("\\n", "\\\\n") + "|");
                }
                text = " text:" + strBldr.substring(0, strBldr.length() - 1);
            }
        }
        // check if it's a potion and add effect type, duration and power if so
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) meta;
            if (potionMeta.hasCustomEffects()) {
                StringBuilder string = new StringBuilder();
                for (PotionEffect effect : potionMeta.getCustomEffects()) {
                    int power = effect.getAmplifier() + 1;
                    int duration = (effect.getDuration() - (effect.getDuration() % 20)) / 20;
                    string.append(effect.getType().getName() + ":" + power + ":" + duration + ",");
                }
                effects = " effects:" + string.substring(0, string.length() - 1);
            }
        }
        // check for leather armor color
        if (meta instanceof LeatherArmorMeta) {
            LeatherArmorMeta armorMeta = (LeatherArmorMeta) meta;
            if (!armorMeta.getColor().equals(Bukkit.getServer().getItemFactory().getDefaultLeatherColor())) {
                color = " color:" + armorMeta.getColor().asRGB();
            }
        }
        // check for enchanted book
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
            if (storageMeta.hasStoredEnchants()) {
                StringBuilder string = new StringBuilder();
                for (Enchantment enchant : storageMeta.getStoredEnchants().keySet()) {
                    string.append(enchant.getName() + ":"
                        + storageMeta.getStoredEnchants().get(enchant) + ",");
                }
                enchants = " enchants:" + string.substring(0, string.length() - 1);
            }
        }
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            if (skullMeta.hasOwner()) {
                owner = " owner:" + skullMeta.getOwner();
            }
        }
        // put it all together in a single string
        return item.getType() + " data:" + item.getData().getData() + name + lore
            + enchants + title + author + text + effects + color + owner;
    }

    public static boolean isQuestItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore()
                .contains(ConfigHandler.getString("messages." + ConfigHandler.getString
                ("config.language") + ".quest_item").replaceAll("&", "§"))) {
            return true;
        }
        return false;
    }
}
