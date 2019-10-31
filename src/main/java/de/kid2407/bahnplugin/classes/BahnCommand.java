package de.kid2407.bahnplugin.classes;

import de.kid2407.bahnplugin.BahnPlugin;
import de.kid2407.bahnplugin.util.DBHelper;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Tobias Franz on 31.01.2019.
 */
public class BahnCommand implements CommandExecutor, TabCompleter {

    private static HashMap<String, String> entries;
    private static boolean hasChanged = false;
    private CommandSender commandSender;
    private static ArrayList<String> commandList = new ArrayList<>(Arrays.asList("delete", "get", "help", "set"));
    private int retryCount = 0;

    public BahnCommand() {
        generateEntries();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        int argsCount = args.length;
        ArrayList<String> result = new ArrayList<>();
        if (argsCount == 0) {
            // No args provided
            result = commandList;
        } else if (argsCount == 1 && !commandList.contains(args[0])) {
            // One arg provided, but not a valid one
            for (String singleCommand : commandList) {
                if (singleCommand.startsWith(args[0])) {
                    result.add(singleCommand);
                }
            }
        } else {
            switch (args[0]) {
                case "delete":
                case "get":
                case "set":
                    if (argsCount == 1) {
                        // Only the arg provided
                        result.addAll(entries.keySet());
                    } else if (argsCount == 2) {
                        // The arg and at least one more provided
                        result.addAll(getPlayernamesFromSearch(args[1], entries.keySet().toArray(new String[0])));
                    }
                    break;
                default:
                    break;
            }
        }

        return result;
    }

    private ArrayList<String> getPlayernamesFromSearch(String search, String[] iterable) {
        int MAX_RESULTS_COUNT = 10;
        ArrayList<String> result = new ArrayList<>();
        for (String playerName : iterable) {
            if (playerName.toLowerCase().startsWith(search.toLowerCase())) {
                result.add(playerName);
            }
            if (result.size() == MAX_RESULTS_COUNT) {
                break;
            }
        }

        return result;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        this.commandSender = commandSender;

        if (args.length > 0) {
            switch (args[0]) {
                case "get":
                    if (args.length >= 2) {
                        return getStationsByPlayerName(args[1]);
                    } else {
                        commandSender.sendMessage(BahnPlugin.prefix + "Benutzung: /bahn get §4<Spielername>");
                        return true;
                    }
                case "set":
                    if (args.length >= 3) {
                        return setOrAddStation(args[1], args[2]);
                    } else if (args.length == 2) {
                        // second argument missing
                        commandSender.sendMessage(BahnPlugin.prefix + "Benutzung: /bahn set <Spielername> §4<Bahnhof>");
                        return true;
                    } else {
                        // both arguments missing
                        commandSender.sendMessage(BahnPlugin.prefix + "Benutzung: /bahn set §4<Spielername> <Bahnhof>");
                        return true;
                    }
                case "delete":
                    if (args.length >= 2) {
                        return removePlayerStation(args[1]);
                    } else {
                        commandSender.sendMessage(BahnPlugin.prefix + "Benutzung: /bahn delete §4<Spielername>");
                        return true;
                    }
                case "help":
                    commandSender.sendMessage(getCommandHelp());
                    return true;
                default:
                    commandSender.sendMessage(BahnPlugin.prefix + String.format("Unbekannter Befehl \"%s\"", args[0]));
                    break;
            }
        }

        return false;
    }

    private boolean getStationsByPlayerName(@NotNull String playername) {
        if (playername.matches("^[a-zA-Z\\d_]{1,32}$")) {
            StringBuilder returnMessage = new StringBuilder();
            if (hasChanged) {
                generateEntries();
                hasChanged = false;
            }

            HashMap<String, String> results = new HashMap<>();

            for (Map.Entry<String, String> entry : entries.entrySet()) {
                if (entry.getKey().toLowerCase().startsWith(playername.toLowerCase())) {
                    results.put(entry.getKey().toLowerCase(), entry.getValue());
                }
            }

            if (results.size() == 0) {
                returnMessage.append(BahnPlugin.prefix).append(String.format("Keine Eintr\u00e4ge f\u00fcr %s gefunden", playername));
            } else {
                if (results.size() == 1) {
                    returnMessage.append(BahnPlugin.prefix).append("Es wurde 1 Ergebnis gefunden:\n\n");
                } else {
                    returnMessage.append(BahnPlugin.prefix).append(String.format("Es wurden %d Ergebnisse gefunden:\n\n", results.size()));
                }

                for (Map.Entry<String, String> singleResult : results.entrySet()) {
                    returnMessage.append(BahnPlugin.prefix).append(String.format("Spieler: %s | Bahnhof: %s\n", singleResult.getKey(), singleResult.getValue()));
                }
            }

            commandSender.sendMessage(returnMessage.toString());
        } else {
            commandSender.sendMessage(BahnPlugin.prefix + "Ung\u00fcltiger Spielername.");
        }

        return true;
    }

    private boolean setOrAddStation(String playername, String station) {
        // Valid Station Identifier
        if (station.matches("^([NOSW]\\d{1,2}){1,2}$") && station.length() >= 2 && station.length() <= 6) {
            if (playername.length() <= 32) {
                if (playername.matches("^[a-zA-Z\\d_]{1,32}$") || commandSender.isOp() || commandSender instanceof ConsoleCommandSender) { // Allow anything, not just playernames, when send by console or OP
                    if ((commandSender instanceof Player && (((Player) commandSender).getDisplayName().equals(playername)) || commandSender.isOp()) || commandSender instanceof ConsoleCommandSender) {
                        try {
                            PreparedStatement statement = DBHelper.prepare("INSERT INTO bahnsystem (position, playername) VALUES (?, ?) ON DUPLICATE KEY UPDATE position=?");
                            if (statement != null) {
                                statement.setString(1, station);
                                statement.setString(2, playername.toLowerCase());
                                statement.setString(3, station);
                                statement.execute();
                            }

                            commandSender.sendMessage(BahnPlugin.prefix + "Eintrag erfolgreich ver\u00e4ndert oder hinzugef\u00fcgt.");
                            hasChanged = true;

                            return true;
                        } catch (SQLException e) {
                            if (retryCount < 3) {
                                DBHelper.resetConnection();
                                retryCount++;
                                return setOrAddStation(playername, station);
                            } else {
                                BahnPlugin.logger.severe("Fehler beim Anlegen oder Updaten eines Eintrages.");
                                BahnPlugin.logger.severe(e.getMessage());
                                commandSender.sendMessage(BahnPlugin.prefix + "Es gab einen internen Fehler, bitte erneut versuchen.");

                                return false;
                            }
                        }
                    } else {
                        commandSender.sendMessage(BahnPlugin.prefix + String.format("Du hast keine Berechtigung, Eintr\u00e4ge f\u00fcr %s zu ver\u00e4ndern.", playername));
                        return true;
                    }
                } else {
                    commandSender.sendMessage("Ung\u00fcltiger Spielername.");
                    return true;
                }
            }

            commandSender.sendMessage(BahnPlugin.prefix + "Der angegebene Nutzername ist zu lang, es sind maximal 32 Zeichen erlaubt.");
            return true;
        } else {
            commandSender.sendMessage(BahnPlugin.prefix + String.format("\"%s\" ist kein g\u00fcltiger Bahnhof.", station));
            return true;
        }
    }

    private boolean removePlayerStation(String playername) {
        if (playername.length() <= 32) {
            if ((commandSender instanceof Player && (((Player) commandSender).getDisplayName().equals(playername)) || commandSender.isOp()) || commandSender instanceof ConsoleCommandSender) {
                try {
                    PreparedStatement statement = DBHelper.prepare("DELETE FROM bahnsystem WHERE playername = ?");
                    if (statement != null) {
                        statement.setString(1, playername.toLowerCase());
                        statement.execute();

                        commandSender.sendMessage(BahnPlugin.prefix + "Eintrag erfolgreich gel\u00f6scht.");

                        return true;
                    } else {
                        commandSender.sendMessage(BahnPlugin.prefix + "Es gab einen internen Fehler, bitte erneut versuchen.");
                        return false;
                    }
                } catch (SQLException e) {
                    if (retryCount < 3) {
                        DBHelper.resetConnection();
                        retryCount++;
                        return removePlayerStation(playername);
                    } else {
                        BahnPlugin.logger.severe("Fehler beim L\u00f6schen eines Eintrags.");
                        BahnPlugin.logger.severe(e.getMessage());
                        commandSender.sendMessage(BahnPlugin.prefix + "Es gab einen internen Fehler, bitte erneut versuchen.");
                        return false;
                    }
                }
            } else {
                commandSender.sendMessage(BahnPlugin.prefix + String.format("Du hast keine Berechtigung, Eintr\u00e4ge f\u00fcr %s zu l\u00f6schen.", playername));
                return true;
            }
        } else {
            commandSender.sendMessage(BahnPlugin.prefix + "Ung\u00fcltiger Spielername.");
            return true;
        }
    }

    private String[] getCommandHelp() {
        ArrayList<String> help = new ArrayList<>();
        help.add(BahnPlugin.prefix + "Mit diesem Plugin k\u00f6nnen die Bahnhofsanbindungen eingetragener Spieler angefragt, ver\u00e4ndert und entfernt werden.");
        help.add(BahnPlugin.prefix + "Jeder kann nur seinen eigenen Eintrag bearbeiten, nicht die der Anderen.");
        help.add(BahnPlugin.prefix + "Das Format f\u00fcr die Kennzeichnung eines Bahnhofs lautet wie folgt:");
        help.add(BahnPlugin.prefix + "( Kennzeichnung f\u00fcr eine Himmelsrichtung[N,O,S,W] + eine Zahl von 1 bis 99), ggf. Mal 2, wenn man nicht an einer Hauptlinie wohnt.");
        help.add(BahnPlugin.prefix + "Beispiele f\u00fcr Bahnh\u00f6fe, die an einer Hauptlinie liegen: S2, O13, W6");
        help.add(BahnPlugin.prefix + "Beispiele f\u00fcr Bahnh\u00f6fe, die \u00a7lnicht\u00a7r an einer Hauptlinie liegen: O8S7, W7S78, N47O17");
        help.add(BahnPlugin.prefix + "");
        help.add(BahnPlugin.prefix + "Es stehen insgesamt vier Kommandos zur Verf\u00fcgung:");
        help.add(BahnPlugin.prefix + "/bahn help - Gibt diese Hilfe aus");
        help.add(BahnPlugin.prefix + "/bahn get <Spielername> - Gibt den Bahnhof f\u00fcr diesen Spieler aus. Akzeptiert auch angefangene Namen. z.B \"kid\" findet auch \"kid2407\"");
        help.add(BahnPlugin.prefix + "/bahn delete <Spielername> - L\u00f6scht den Eintrag f\u00fcr diesen Spieler");
        help.add(BahnPlugin.prefix + "/bahn set <Spielername> <Bahnhof> - F\u00fcgt den Eintrag f\u00fcr diesen Spieler hinzu oder aktualisiert ihn.");

        return help.toArray(new String[0]);
    }

    private void generateEntries() {
        PreparedStatement statement = DBHelper.prepare("SELECT * FROM bahnsystem");
        try {
            if (statement != null) {
                statement.execute();
                ResultSet resultset = statement.getResultSet();

                entries = new HashMap<>();

                while (resultset.next()) {
                    entries.put(resultset.getString("playername").toLowerCase(), resultset.getString("position"));
                }
            }
        } catch (SQLException e) {
            if (retryCount < 3) {
                DBHelper.resetConnection();
                retryCount++;
                generateEntries();
            } else {
                e.printStackTrace();
            }
        }
    }
}
