package de.kid2407.bahnplugin.classes;

import de.kid2407.bahnplugin.util.DBHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tobias Franz on 31.01.2019.
 */
public class BahnCommand implements CommandExecutor {

    private static HashMap<String, String> entries;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length == 1) {
            if (entries == null) {
                generateEntries();
            }

            String playername = args[0];
            String regex = String.format("^%s.*", playername);
            HashMap<String, String> results = new HashMap<>();

            for (Map.Entry<String, String> entry : entries.entrySet()) {
                if (entry.getKey().matches(regex)) {
                    results.put(entry.getKey(), entry.getValue());
                }
            }

            if (results.size() == 0) {
                commandSender.sendMessage(String.format("Keine Eintr\u00e4ge f\u00fcr %s gefunden", playername));
            } else {
                commandSender.sendMessage(String.format("Es wurden %d Ergebnisse gefunden:\n\n", results.size()));

                for (Map.Entry<String, String> singleResult : results.entrySet()) {
                    commandSender.sendMessage(String.format("Spieler: %s | Bahhof: %s", singleResult.getKey(), singleResult.getValue()));
                }
            }

            return true;
        }
        return false;
    }

    private void generateEntries() {
        PreparedStatement statement = DBHelper.prepare("SELECT * FROM bahnsystem");
        try {
            if (statement != null) {
                statement.execute();
                ResultSet resultset = statement.getResultSet();

                entries = new HashMap<>();

                while (resultset.next()) {
                    entries.put(resultset.getString("playername"), resultset.getString("position"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
