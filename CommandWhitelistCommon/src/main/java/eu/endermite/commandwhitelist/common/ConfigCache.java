package eu.endermite.commandwhitelist.common;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigCache {

    private final File configFile;
    private ConfigFile config;
    private final Object logger;
    private final boolean canDoProtocolLib;
    private final HashMap<String, CWGroup> groupList = new LinkedHashMap<>();
    private final HashMap<String, String> commandAliases = new HashMap<>();
    public String prefix, command_denied, no_permission, no_such_subcommand, config_reloaded, added_to_whitelist,
            removed_from_whitelist, group_doesnt_exist, subcommand_denied;
    public boolean useProtocolLib = false;
    public MessageType messageType = MessageType.CHAT;
    public boolean debug = false;

    public ConfigCache(File configFile, boolean canDoProtocolLib, Object logger) {
        this.configFile = configFile;
        this.canDoProtocolLib = canDoProtocolLib;
        this.logger = logger;

        try {
            reloadConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean reloadConfig() {

        createFiles();
        try {
            config = ConfigFile.loadConfig(configFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        config.addDefault("messages.prefix", "CommandWhitelist > ");
        config.addDefault("messages.command_denied", "No such command.");
        config.addDefault("messages.subcommand_denied", "You cannot use this subcommand");
        config.addDefault("messages.no_permission", "<red>You don't have permission to do this.");
        config.addDefault("messages.no_such_subcommand", "<red>No subcommand by that name.");
        config.addDefault("messages.config_reloaded", "<yellow>Configuration reloaded.");
        config.addDefault("messages.added_to_whitelist", "<yellow>Whitelisted command <gold>%s <yellow>for permission <gold>%s");
        config.addDefault("messages.removed_from_whitelist", "<yellow>Removed command <gold>%s <yellow>from permission <gold>%s");
        config.addDefault("messages.group_doesnt_exist", "<red>Group doesn't exist or error occured");

        config.addComment("messages", "Messages use MiniMessage formatting (https://docs.adventure.kyori.net/minimessage/format)");

        if (canDoProtocolLib)
            config.addDefault("use_protocollib", false, "Do not enable if you don't have issues with aliased commands.\nThis requires server restart to take effect.");

        config.addDefault("message_type", MessageType.CHAT.toString(), "Valid message types are CHAT and ACTIONBAR. Does nothing on velocity.");

        if (config.isNew()) {
            List<String> exampleCommands = new ArrayList<>();
            exampleCommands.add("example");
            List<String> exampleSubCommands = new ArrayList<>();
            exampleSubCommands.add("example of");
            String exampleCustomCommandDeniedMessage = "You don't have commandwhitelist.group.example permission.";

            config.addExample("groups.example.commands", exampleCommands, "This is the WHITELIST of commands that players will be able to see/use in the group \"example\"");
            config.addExample("groups.example.subcommands", exampleSubCommands, "This is the BLACKLIST of subcommands that players will NOT be able to see/use in the group \"example\"");
            config.addExample("groups.example.custom_command_denied_message", exampleCustomCommandDeniedMessage, "This is a custom message that players will see if they do not have commandwhitelist.group.<group_name> permission.\ncommandwhitelist.group.example in this case\nIf you don't want to use a custom message, set custom_command_denid_message: \"\"");
            config.addComment("groups.example", "All groups except from default require commandwhitelist.group.<group_name> permission\ncommandwhitelist.group.example in this case\n If you wish to leave the list empty, put \"commands: []\" or \"subcommands: []\"");
        }

        config.makeSectionLenient("groups");
        List<String> defaultCommands = new ArrayList<>();
        defaultCommands.add("help");
        defaultCommands.add("spawn");
        defaultCommands.add("bal");
        defaultCommands.add("balance");
        defaultCommands.add("baltop");
        defaultCommands.add("pay");
        defaultCommands.add("r");
        defaultCommands.add("msg");
        defaultCommands.add("tpa");
        defaultCommands.add("tpahere");
        defaultCommands.add("tpaccept");
        defaultCommands.add("tpdeny");
        defaultCommands.add("warp");
        List<String> defaultSubcommands = new ArrayList<>();
        defaultSubcommands.add("help about");
        List<String> defaultHiddenCommands = new ArrayList<>();

        String defaultCustomCommandDeniedMessage = "";

        config.addDefault("groups.default", new CWGroup("default", defaultCommands, defaultSubcommands, defaultHiddenCommands, defaultCustomCommandDeniedMessage).serialize());

        prefix = config.getString("messages.prefix");
        command_denied = config.getString("messages.command_denied");
        subcommand_denied = config.getString("messages.subcommand_denied");
        no_permission = config.getString("messages.no_permission");
        no_such_subcommand = config.getString("messages.no_such_subcommand");
        config_reloaded = config.getString("messages.config_reloaded");
        added_to_whitelist = config.getString("messages.added_to_whitelist");
        removed_from_whitelist = config.getString("messages.removed_from_whitelist");
        group_doesnt_exist = config.getString("messages.group_doesnt_exist");
        useProtocolLib = config.getBoolean("use_protocollib");
        debug = config.getBoolean("debug", false);
        try {
            String chatTypeId = config.getString("message_type");
            if (chatTypeId == null) {
                warn("Invalid message type. Using CHAT.");
            } else {
                messageType = MessageType.valueOf(chatTypeId.toUpperCase(Locale.ENGLISH));
            }
        } catch (IllegalArgumentException e) {
            warn("Invalid message type. Using CHAT.");
        }

        ConfigSection groupSection = config.getConfigSection("groups");
        for (String key : groupSection.getKeys(false)) {
            groupList.put(key, loadCWGroup(key, groupSection));
        }

        // Load command aliases
        commandAliases.clear();
        config.makeSectionLenient("command_aliases");
        config.addComment("command_aliases", "Command aliases allow you to create custom command names that map to actual commands.\n" +
                "This is useful when other plugins transform commands (like ChatControl rules).\n" +
                "Example: If you create a rule that transforms '/s <message>' into '/channel send staff <message>',\n" +
                "you can add 's: channel' here so players only need permission for 's' instead of 'channel'.\n" +
                "Format: 'alias: actual_command' (without the /)");
        ConfigSection aliasSection = config.getConfigSection("command_aliases");
        if (aliasSection != null) {
            for (String alias : aliasSection.getKeys(false)) {
                String actualCommand = aliasSection.getString(alias);
                if (actualCommand != null && !actualCommand.isEmpty()) {
                    commandAliases.put(alias.toLowerCase(), actualCommand.toLowerCase());
                    debug("Loaded command alias: " + alias + " -> " + actualCommand);
                }
            }
        }

        return saveConfig();
    }

    private boolean saveConfig() {
        try {
            config.save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void createFiles() {
        try {
            File parent = new File(configFile.getParent());
            if (!parent.exists())
                parent.mkdir();
            if (!configFile.exists())
                configFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CWGroup loadCWGroup(String id, ConfigSection section) {
        HashSet<String> commands = new HashSet<>();
        for (String cmd : section.getStringList(id + ".commands")) {
            if (cmd.contains(" ")) {
                String[] cmdSplit = cmd.split(" ");
                warn("CommandWhitelist - \"" + cmd + "\" is not a command. Loading it as \"" + cmdSplit[0] + "\".");
                cmd = cmdSplit[0];
            }
            if (commands.contains(cmd)) continue;
            commands.add(cmd);
        }
        List<String> subCommands = new ArrayList<>();
        for (String subCmd : section.getStringList(id + ".subcommands")) {
            if (!subCmd.contains(" ")) {
                warn("CommandWhitelist - \"" + subCmd + "\" is not a subcommand. Skipping it.");
                continue;
            }
            subCommands.add(subCmd);
        }
        
        // Load hidden commands (commands that work but don't show in tab completion)
        HashSet<String> hiddenCommands = new HashSet<>();
        for (String cmd : section.getStringList(id + ".hidden_commands")) {
            if (cmd.contains(" ")) {
                String[] cmdSplit = cmd.split(" ");
                warn("CommandWhitelist - \"" + cmd + "\" is not a command. Loading it as \"" + cmdSplit[0] + "\".");
                cmd = cmdSplit[0];
            }
            if (hiddenCommands.contains(cmd)) continue;
            hiddenCommands.add(cmd);
            debug("Loaded hidden command: " + cmd + " for group: " + id);
        }
        
        String customCommandDeniedMessage = section.getString(id + ".custom_command_denied_message");
        return new CWGroup(id, commands, subCommands, hiddenCommands, customCommandDeniedMessage);
    }

    public void saveCWGroup(String id, CWGroup group) {
        config.set("groups." + id, group.serialize());
        saveConfig();
    }

    public HashMap<String, CWGroup> getGroupList() {
        return groupList;
    }

    public HashMap<String, String> getCommandAliases() {
        return commandAliases;
    }

    private void warn(String log) {
        if (logger == null) {
            System.out.println("WARNING: "+log);
            return;
        }
        if (logger instanceof java.util.logging.Logger) {
            ((java.util.logging.Logger) logger).warning(log);
            return;
        }
        System.out.println("WARNING: "+log);
    }

    public void debug(String log) {
        if (!debug) return;
        if (logger == null) {
            System.out.println("DEBUG: "+log);
            return;
        }
        if (logger instanceof java.util.logging.Logger) {
            ((java.util.logging.Logger) logger).info(log);
            return;
        }
        System.out.println("DEBUG: "+log);
    }

}
