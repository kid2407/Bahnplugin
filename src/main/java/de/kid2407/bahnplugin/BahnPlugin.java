package de.kid2407.bahnplugin;

import de.kid2407.bahnplugin.classes.BahnCommand;
import de.kid2407.bahnplugin.util.DBHelper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

/**
 * Created by Tobias Franz on 31.01.2019.
 */
public class BahnPlugin extends JavaPlugin {

    public static BahnPlugin instance;
    public static Logger logger;
    public static boolean hasChanged = true;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        File config = new File(getDataFolder(), "config.yml");
        if (!config.exists()) {
            config.getParentFile().mkdirs();
            createConfig();
            logger.info("Die Konfigurationsdatei wurde erzeugt. Um das Plugin nutzen zu können, die Werte entsprechend anpassen");
        } else {
            DBHelper.initConnection();
            getCommand("bahn").setExecutor(new BahnCommand());
        }

        getLogger().info("BahnPlugin enabled.");
    }

    @Override
    public void onDisable() {
        DBHelper.onDisable();

        getLogger().info("BahnPlugin disabled.");
    }

    @Override
    public FileConfiguration getConfig() {
        return super.getConfig();
    }

    private void createConfig() {
        FileConfiguration config = getConfig();

        config.addDefault("mysql.host", "localhost");
        config.addDefault("mysql.port", 3306);
        config.addDefault("mysql.dbname", "minecraft");
        config.addDefault("mysql.user", "root");
        config.addDefault("mysql.pass", "");
        config.options().copyDefaults(true);
        saveConfig();
    }
}
