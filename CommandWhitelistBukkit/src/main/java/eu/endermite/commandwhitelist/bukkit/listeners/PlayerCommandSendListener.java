package eu.endermite.commandwhitelist.bukkit.listeners;

import eu.endermite.commandwhitelist.bukkit.CommandWhitelistBukkit;
import eu.endermite.commandwhitelist.common.CWPermission;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;

public class PlayerCommandSendListener implements Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    public void PlayerCommandSendEvent(org.bukkit.event.player.PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(CWPermission.BYPASS.permission())) return;
        // Only show visible commands in tab completion (hidden commands are excluded)
        HashSet<String> commandList = CommandWhitelistBukkit.getVisibleCommands(player);
        event.getCommands().removeIf((cmd) -> !commandList.contains(cmd));
    }
}
