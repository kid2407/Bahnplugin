package de.kid2407.bahnplugin.classes;

import de.kid2407.bahnplugin.BahnPlugin;
import de.kid2407.bahnplugin.util.DBHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

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
    private static boolean hasChanged = true;
    private CommandSender commandSender;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        this.commandSender = commandSender;

        if (args.length > 0) {
            if (!args[0].equals("set") && !args[0].equals("delete")) {
                return getStationsByPlayerName(args[0]);
            } else if (args[0].equals("set")) {
                if (args.length >= 3) {
                    return setOrAddStation(args[1], args[2]);
                } else if (args.length == 2) {
                    commandSender.sendMessage("Bitte einen Bahnhof angeben.");
                } else {
                    commandSender.sendMessage("Bitte einen Spielernamen und einen Bahnhof angeben.");
                }
            } else {
                if (args.length >= 2) {
                    return removePlayerStation(args[1]);
                } else {
                    commandSender.sendMessage("Bitte einen Spielernamen angeben.");
                }
            }
        }

        return false;
    }

    private boolean getStationsByPlayerName(String playername) {
        StringBuilder returnMessage = new StringBuilder();
        if (entries == null || hasChanged) {
            generateEntries();
            hasChanged = false;
        }

        String regex = String.format("^%s.*", playername);
        HashMap<String, String> results = new HashMap<>();

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (entry.getKey().matches(regex)) {
                results.put(entry.getKey(), entry.getValue());
            }
        }

        if (results.size() == 0) {
            returnMessage.append(String.format("Keine Eintr\u00e4ge f\u00fcr %s gefunden", playername));
        } else {
            if (results.size() == 1) {
                returnMessage.append("Es wurde 1 Ergebnis gefunden:\n\n");
            } else {
                returnMessage.append(String.format("Es wurden %d Ergebnisse gefunden:\n\n", results.size()));
            }

            for (Map.Entry<String, String> singleResult : results.entrySet()) {
                returnMessage.append(String.format("Spieler: %s | Bahnhof: %s\n", singleResult.getKey(), singleResult.getValue()));
            }
        }

        commandSender.sendMessage(returnMessage.toString());

        return true;
    }

    private boolean setOrAddStation(String playername, String station) {
        // Valid Station Identifier
        if (station.matches("^[NOSW]\\d{1,2}[NOSW]\\d{1,2}$") && station.length() >= 4 && station.length() <= 6) {
            if (playername.length() <= 32) {
                if ((commandSender instanceof Player && (((Player) commandSender).getDisplayName().equals(playername)) || commandSender.isOp()) || commandSender instanceof ConsoleCommandSender) {
                    try {
                        PreparedStatement statement = DBHelper.prepare("INSERT INTO bahnsystem (position, playername) VALUES (?, ?) ON DUPLICATE KEY UPDATE position=?");
                        if (statement != null) {
                            statement.setString(1, station);
                            statement.setString(2, playername);
                            statement.setString(3, station);
                            statement.execute();
                        }

                        commandSender.sendMessage("Eintrag erfolgreich ver\u00e4ndert oder hinzugef\u00fcgt.");
                        hasChanged = true;

                        return true;
                    } catch (SQLException e) {
                        BahnPlugin.logger.severe("Fehler beim Anlegen oder Updaten eines Eintrages.");
                        BahnPlugin.logger.severe(e.getMessage());
                        commandSender.sendMessage("Es gab einen internen Fehler, bitte erneut versuchen.");

                        return false;
                    }
                } else {
                    commandSender.sendMessage(String.format("Du hast keine Berechtigung, Eintr\u00e4ge f\u00fcr %s zu ver\u00e4ndern.", playername));
                    return false;
                }
            }

            commandSender.sendMessage("Der angegebene Nutzername ist zu lang, es sind maximal 32 Zeichen erlaubt.");
            return false;
        } else {
            commandSender.sendMessage(String.format("\"%s\" ist kein g\u00fcltiger Bahnhof.", station));
            return false;
        }
    }

    private boolean removePlayerStation(String playername) {
        if (playername.length() <= 32) {
            if ((commandSender instanceof Player && (((Player) commandSender).getDisplayName().equals(playername)) || commandSender.isOp()) || commandSender instanceof ConsoleCommandSender) {
                try {
                    PreparedStatement statement = DBHelper.prepare("DELETE FROM bahnsystem WHERE playername = ?");
                    if (statement != null) {
                        statement.setString(1, playername);
                        statement.execute();

                        commandSender.sendMessage("Eintrag erfolgreich gel\u00f6scht.");

                        return true;
                    }
                } catch (SQLException e) {
                    BahnPlugin.logger.severe("Fehler beim L\u00f6schen eines Eintrags.");
                    BahnPlugin.logger.severe(e.getMessage());
                    commandSender.sendMessage("Es gab einen internen Fehler, bitte erneut versuchen.");

                }
            } else {
                commandSender.sendMessage(String.format("Du hast keine Berechtigung, Eintr\u00e4ge f\u00fcr %s zu l\u00f6schen.", playername));
                return false;
            }
        } else {
            commandSender.sendMessage("Ung\u00fcltiger Spielername.");
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
