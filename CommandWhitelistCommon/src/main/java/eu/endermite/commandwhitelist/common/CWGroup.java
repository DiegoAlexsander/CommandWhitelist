package eu.endermite.commandwhitelist.common;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CWGroup {

    private final String id, permission, commandDeniedMessage;
    private final HashSet<String> commands = new HashSet<>();
    private final HashSet<String> subCommands = new HashSet<>();
    private final HashSet<String> hiddenCommands = new HashSet<>();

    public CWGroup(String id, Collection<String> commands, Collection<String> subCommands, Collection<String> hiddenCommands, String custom_command_denied_message) {
        this.id = id;
        this.permission = "commandwhitelist.group." + id;
        this.commands.addAll(commands);
        this.commandDeniedMessage = custom_command_denied_message;
        this.subCommands.addAll(subCommands);
        this.hiddenCommands.addAll(hiddenCommands);
    }

    public String getId() {
        return id;
    }

    public String getPermission() {
        return permission;
    }

    public HashSet<String> getCommands() {
        return commands;
    }

    public @Nullable String getCommandDeniedMessage() {
        return commandDeniedMessage;
    }

    public void addCommand(String command) {
        commands.add(command);
    }

    public void removeCommand(String command) {
        commands.remove(command);
    }

    public HashSet<String> getSubCommands() {
        return subCommands;
    }

    public void addSubCommand(String subCommand) {
        subCommands.add(subCommand);
    }

    public void removeSubCommand(String subCommand) {
        subCommands.remove(subCommand);
    }

    public HashSet<String> getHiddenCommands() {
        return hiddenCommands;
    }

    public void addHiddenCommand(String command) {
        hiddenCommands.add(command);
    }

    public void removeHiddenCommand(String command) {
        hiddenCommands.remove(command);
    }

    public HashMap<String, Object> serialize() {
        HashMap<String, Object> serializedGroup = new LinkedHashMap<>();
        List<String> commands = new ArrayList<>(this.commands);
        List<String> subCommands = new ArrayList<>(this.subCommands);
        List<String> hiddenCommands = new ArrayList<>(this.hiddenCommands);
        serializedGroup.put("commands", commands);
        serializedGroup.put("subcommands", subCommands);
        serializedGroup.put("hidden_commands", hiddenCommands);
        return serializedGroup;
    }
}
