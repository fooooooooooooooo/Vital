package io.github.caprapaul.vital.commands;

import io.github.caprapaul.bettercommandexecutor.BetterCommand;
import io.github.caprapaul.bettercommandexecutor.BetterCommandExecutor;
import io.github.caprapaul.bettercommandexecutor.BetterExecutor;
import io.github.caprapaul.bettercommandexecutor.CommandTarget;
import io.github.caprapaul.vital.systems.TeleportSystem;
import io.github.caprapaul.vitalcore.VitalCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.PluginManager;

import java.util.HashMap;
import java.util.Objects;

@BetterExecutor
public class BackCommand extends BetterCommandExecutor implements Listener
{
    public BackCommand(VitalCore plugin)
    {
        super(plugin);
        loadCommands(this, this.plugin);
    }

    private HashMap<String, Location> previousLocations;
    private HashMap<String, Long> backCooldowns = new HashMap<String, Long>();

    private void loadConfig()
    {
        plugin.getConfig().addDefault("back-cooldown", 60);
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    @Override
    public void onEnable()
    {
        previousLocations = new HashMap<String, Location>();
        backCooldowns = new HashMap<String, Long>();
        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.registerEvents(this, plugin);
        loadConfig();
    }

    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args)
    {
        parseCommand(this, commandSender, command, commandLabel, args);
        return false;
    }

    private boolean areLocationsCoordinatesEqual(Location location1, Location location2)
    {
        return location1.getWorld().getName().equals(location2.getWorld().getName()) &&
                Double.compare(location1.getX(), location2.getX()) == 0 &&
                Double.compare(location1.getY(), location2.getY()) == 0 &&
                Double.compare(location1.getZ(), location2.getZ()) == 0;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event)
    {
        if (areLocationsCoordinatesEqual(event.getFrom(), Objects.requireNonNull(event.getTo())))
        {
            return;
        }

        if (!(event.getCause().equals(PlayerTeleportEvent.TeleportCause.COMMAND) ||
                event.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN)))
        {
            return;
        }

        Player player = event.getPlayer();
        previousLocations.put(player.getUniqueId().toString(), event.getFrom());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        Player player = event.getEntity();
        previousLocations.put(player.getUniqueId().toString(), player.getLocation());
    }

    @BetterCommand(name = "back", target = CommandTarget.PLAYER)
    public void back(CommandSender commandSender, String[] args, String commandLabel)
    {
        Player player = (Player) commandSender;

        if (!player.hasPermission("vital.back.overridecooldown"))
        {
            int cooldown = plugin.getConfig().getInt("back-cooldown");
            if (backCooldowns.containsKey(player.getUniqueId().toString()))
            {
                long diff = (System.currentTimeMillis() - backCooldowns.get(player.getUniqueId().toString())) / 1000;
                if (diff < cooldown)
                {
                    player.sendMessage(plugin.prefix + ChatColor.RED + "Error: You must wait " + cooldown + " seconds before you can do that again!");
                    return;
                }
            }
        }

        if(!(previousLocations.containsKey(player.getUniqueId().toString())))
        {
            player.sendMessage(plugin.prefix + ChatColor.RED + "Error: You have nowhere to go!");
            return;
        }

        player.sendMessage(plugin.prefix + ChatColor.GRAY + "Teleporting...");

        TeleportSystem.teleport(plugin, player, previousLocations.get(player.getUniqueId().toString()));
        backCooldowns.put(player.getUniqueId().toString(), System.currentTimeMillis());
    }
}
