package de.kid2407.bahnplugin.util;

import de.kid2407.bahnplugin.BahnPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Tobias Franz on 31.01.2019.
 */
public class DBHelper {

    private static boolean dbExists = false;
    private static Connection connection;

    public static void initConnection() {
        if (connection == null) {
            try {
                FileConfiguration config = BahnPlugin.instance.getConfig();

                String url = String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true", config.getString("mysql.host"), config.getInt("mysql.port"), config.getString("mysql.dbname"));
                String username = config.getString("mysql.user");
                String password = config.getString("mysql.pass");


                connection = DriverManager.getConnection(url, username, password);
            } catch (SQLException sqlException) {
                BahnPlugin.logger.severe("Fehler beim Herstellen der Datenbankverbindung!");
                BahnPlugin.logger.severe(sqlException.getMessage());
            }
        }

        if (!dbExists) {
            // Setup Tables
            try {
                PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS bahnsystem (position VARCHAR(6) NOT NULL, playername VARCHAR(32) NOT NULL, UNIQUE (playername))");
                if (statement != null) {
                    statement.execute();
                    dbExists = true;
                    fixOldData();
                } else {
                    BahnPlugin.logger.severe("Konnte die Tabelle \"bahnsystem\" nicht erzeugen.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void resetConnection() {
        if (connection != null) {
            try {
                if (!connection.isValid(5)) {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                    connection = null;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException sqlException) {
            BahnPlugin.logger.severe("Fehler beim Schliessen der Datenbankverbindung");
            BahnPlugin.logger.severe(sqlException.getMessage());
        }
    }

    public static PreparedStatement prepare(String query) {
        initConnection();
        try {
            return connection.prepareStatement(query);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void fixOldData() {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE bahnsystem SET position = 'HUB' WHERE position REGEXP '[NSOW]0'");
            statement.execute();
        } catch (SQLException e) {
            BahnPlugin.logger.severe(e.getMessage());
        }
    }

}
