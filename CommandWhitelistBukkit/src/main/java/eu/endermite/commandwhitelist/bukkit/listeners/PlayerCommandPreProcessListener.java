package eu.endermite.commandwhitelist.bukkit.listeners;

import eu.endermite.commandwhitelist.bukkit.CommandWhitelistBukkit;
import eu.endermite.commandwhitelist.common.CWPermission;
import eu.endermite.commandwhitelist.common.CommandUtil;
import eu.endermite.commandwhitelist.common.ConfigCache;
import eu.endermite.commandwhitelist.common.commands.CWCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashSet;
import java.util.HashMap;

public class PlayerCommandPreProcessListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void PlayerExecuteCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(CWPermission.BYPASS.permission())) return;
        
        ConfigCache config = CommandWhitelistBukkit.getConfigCache();
        
        // Get the original command as typed by the player
        String caseSensitiveLabel = CommandUtil.getCommandLabel(event.getMessage());
        String label = caseSensitiveLabel.toLowerCase();
        String messageWithoutSlash = event.getMessage().startsWith("/") ? event.getMessage().substring(1) : event.getMessage();

        config.debug("Player command: '" + event.getMessage() + "' | Extracted label: '" + label + "'");

        // Check if this command is an alias that maps to another command
        // If the command IS an alias, we approve it and mark the target command as approved
        if (config.getCommandAliases().containsKey(label)) {
            String targetCommand = config.getCommandAliases().get(label);
            config.debug("Command '" + label + "' is an alias for '" + targetCommand + "', checking permission for alias");
            
            BukkitAudiences audiences = CommandWhitelistBukkit.getAudiences();
            HashSet<String> commands = CommandWhitelistBukkit.getCommands(player);
            config.debug("Player allowed commands: " + commands);
            config.debug("Checking if '" + label + "' is in allowed commands");
            
            // Check if the ALIAS is in the allowed commands
            if (!commands.contains(label)) {
                event.setCancelled(true);
                Component message = CWCommand.getParsedErrorMessage(
                        messageWithoutSlash,
                        config.prefix + CommandWhitelistBukkit.getCommandDeniedMessage(label)
                );
                switch (config.messageType) {
                    case CHAT:
                        audiences.player(player).sendMessage(message);
                        break;
                    case ACTIONBAR:
                        audiences.player(player).sendActionBar(message);
                        break;
                }
                return;
            }
            
            // Alias is allowed! Mark the target command as pre-approved for this player
            player.setMetadata("cw_approved_command", new FixedMetadataValue(CommandWhitelistBukkit.getPlugin(), targetCommand));
            config.debug("Alias '" + label + "' approved, marking target '" + targetCommand + "' as pre-approved");
            return; // Let the command through - it will be transformed by other plugins
        }

        // Check if this command was pre-approved by an alias
        if (player.hasMetadata("cw_approved_command")) {
            String approvedCommand = player.getMetadata("cw_approved_command").get(0).asString();
            player.removeMetadata("cw_approved_command", CommandWhitelistBukkit.getPlugin());
            
            if (label.equals(approvedCommand)) {
                config.debug("Command '" + label + "' was pre-approved by an alias, allowing through");
                return; // This is the transformed command from an approved alias
            }
        }

        // Normal command processing (not an alias)
        BukkitAudiences audiences = CommandWhitelistBukkit.getAudiences();
        HashSet<String> commands = CommandWhitelistBukkit.getCommands(player);
        config.debug("Player allowed commands: " + commands);
        config.debug("Checking if '" + label + "' is in allowed commands");
        
        if (!commands.contains(label)) {
            event.setCancelled(true);
            Component message = CWCommand.getParsedErrorMessage(
                    messageWithoutSlash,
                    config.prefix + CommandWhitelistBukkit.getCommandDeniedMessage(label)
            );
            switch (config.messageType) {
                case CHAT:
                    audiences.player(player).sendMessage(message);
                    break;
                case ACTIONBAR:
                    audiences.player(player).sendActionBar(message);
                    break;
            }
            return;
        }

        HashSet<String> bannedSubCommands = CommandWhitelistBukkit.getSuggestions(player);

        for (String bannedSubCommand : bannedSubCommands) {
            if (messageWithoutSlash.startsWith(bannedSubCommand)) {
                event.setCancelled(true);
                audiences.player(player).sendMessage(CWCommand.miniMessage.deserialize(config.prefix + config.subcommand_denied));
                return;
            }
        }

    }
}
