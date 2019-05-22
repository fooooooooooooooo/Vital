package io.github.caprapaul.vital.commands;

import io.github.caprapaul.bettercommandexecutor.BetterCommand;
import io.github.caprapaul.bettercommandexecutor.BetterCommandExecutor;
import io.github.caprapaul.vital.*;
import io.github.caprapaul.vital.data.Warp;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.HashMap;

public class HomeCommands extends BetterCommandExecutor implements Listener
{
    private final Vital plugin;

    // { HashMap | Key: UUID, Value: { HashMap | Key: Home name, Value: Warp class } }
    private HashMap<String, HashMap<String, Warp>> playerHomes;

    public HomeCommands(Vital plugin)
    {
        this.plugin = plugin;
        this.playerHomes = new HashMap<String, HashMap<String, Warp>>();
        loadCommands(this, this.plugin);

        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        pluginManager.registerEvents(this, this.plugin);

        /*
         When reloading plugins we want to make sure that all the
         player homes for players currently on the server are loaded
        */
        @SuppressWarnings("unchecked")
        Iterable<Player> onlinePlayers = (Iterable<Player>) Bukkit.getServer().getOnlinePlayers();
        this.loadMultiplePlayersHomes(onlinePlayers);
    }

    private void loadMultiplePlayersHomes(Iterable<Player> players)
    {
        for (Player player : players)
        {
            loadPlayerHomes(player);
        }
    }

    private void loadPlayerHomes(Player player)
    {
        // Get player UUID
        String playerUUID = player.getUniqueId().toString();

        /*
         Get a list of all the homes that belong to the player
         where the player UUID is the key in the YAML
        */
        @SuppressWarnings("unchecked")
        HashMap<String, Warp> homesArrayList = (HashMap<String, Warp>)plugin
                .getPlayerHomes()
                .get("homes." + playerUUID, new HashMap<String, Warp>());

        HashMap<String, Warp> homes = new HashMap<String, Warp>();
        /*
         For each warp class we got from the YAML for that player
         Add them to the hash map with their names
         This allows for name key access instead of iterating the warps each time looking for a name.
        */
        if (homesArrayList != null) {
            for (Warp home: homesArrayList)
            {
                homes.put(home.getName(), home);
            }
        }

        // Add the loaded; or newly created, HashMap to the player home HashMap
        this.playerHomes.put(playerUUID, homes);
    }

    // Update the loaded YML file for homes and remove the player from the list of loaded homes
    private void unloadPlayerHomes(Player player)
    {
        String playerUUID = player.getUniqueId().toString();
        this.plugin.getPlayerHomes().set("homes."+playerUUID, this.playerHomes.get(playerUUID));
        this.playerHomes.remove(playerUUID);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        this.loadPlayerHomes(event.getPlayer());
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerLeave(PlayerQuitEvent event)
    {
        this.unloadPlayerHomes(event.getPlayer());
    }

    /*
     Add a home to the player-home HashMap
     Will also overwrite old homes when named
    */
    private void addHome(Player player, Warp home)
    {
        HashMap<String, Warp> homes = this.playerHomes.get(player.getUniqueId().toString());
        homes.put(home.getName(), home);
    }

    private void removeHome(Player player, String homeName)
    {
        HashMap<String, Warp> homes = this.playerHomes.get(player.getUniqueId().toString());
        homes.remove(homeName);
    }

    /*
     Takes the player to their default home. Usage:
        /home
     Takes the player to a named home. Usage:
        /home <home name>
    */
    @BetterCommand(name = "home")
    @SuppressWarnings("unused")
    public void home(CommandSender commandSender, String[] args, String commandLabel)
    {
        // Get the player from commandSender
        Player player = (Player) commandSender;

        // Get the UUID of the player who sent the command
        String playerUUID = player.getUniqueId().toString();
        HashMap<String, Warp> playerHomes = this.playerHomes.get(playerUUID);

        Warp target;

        switch (args.length)
        {
            // Default home
            case 0:
                // Set the target location as the "default" home
                target = this.playerHomes.get(playerUUID).get("default");
                break;

            // Named home
            case 1:
                target = this.playerHomes.get(playerUUID).get(args[0]);
                break;

            // Error: too many arguments
            default:
                player.sendMessage(this.plugin.prefix + ChatColor.RED + "Too many arguments! Usage /home <home name>");
                return;
        }

        // If the home doesn't exist, send an error message to the user.
        // And return from this method
        if (target == null)
        {
            player.sendMessage(this.plugin.prefix + ChatColor.RED + "Home does not exist! Use /homes to see your set homes");
            return;
        }

        // Send a message and then teleport the player to the target.
        player.sendMessage(this.plugin.prefix + ChatColor.GRAY + "Teleporting to home: " + ChatColor.GOLD + target.getName() + ChatColor.GRAY + "...");
        player.teleport(target.toLocation(this.plugin));
    }

    /*
     List the player's homes. Usage:
        /homes
     Show a page from the list of homes. Usage:
        /homes <page number>
    */
    @BetterCommand(name = "homes")
    @SuppressWarnings("unused")
    public void homes(CommandSender commandSender, String[] args, String commandLabel)
    {
        Player player = (Player) commandSender;

        switch (args.length)
        {
            // Show the first page of the player's homes
            case 0:
                break;

            // Show the selected page of the player's homes
            case 1:
                break;

            // Error: Too many arguments
            default:
                player.sendMessage(this.plugin.prefix + ChatColor.RED + "Too many arguments! Usage /homes <page number>");
                break;
        }
    }

    /*
     Sets the player's default home at the player's current location. Usage:
        /sethome
     Sets a named home at the player's current location. Usage:
        /sethome <home name>
    */
    @BetterCommand(name = "sethome")
    @SuppressWarnings("unused")
    public void setHome(CommandSender commandSender, String[] args, String commandLabel)
    {
        Player player = (Player) commandSender;

        switch (args.length)
        {
            // Set the player's default home
            case 0:
                this.addHome(player, new Warp("default", player.getLocation()));
                player.sendMessage(this.plugin.prefix + "Set default home.");
                break;

            // Set a named home
            case 1:
                this.addHome(player, new Warp(args[0], player.getLocation()));
                player.sendMessage(this.plugin.prefix + ChatColor.GRAY + "Set home" + ChatColor.GOLD + args[0] + ChatColor.GRAY + ".");
                break;

            // Error: Too many arguments
            default:
                player.sendMessage(this.plugin.prefix + ChatColor.RED + "Too many arguments! Usage /sethome <home name>");
                break;
        }
    }

    /*
     Deletes the default home of the player. Usage:
        /delhome
     Deletes a named home of the player. Usage:
        /delhome <home name>
    */
    @BetterCommand(name = "delhome")
    @SuppressWarnings("unused")
    public void delHome(CommandSender commandSender, String[] args, String commandLabel)
    {
        // Get the player
        Player player = (Player) commandSender;

        switch (args.length)
        {
            // Delete the default home
            case 0:
                this.removeHome(player, "default");
                player.sendMessage(this.plugin.prefix + "Deleted home default.");
                break;

            // Delete a named home
            case 1:
                this.removeHome(player, args[0]);
                player.sendMessage(this.plugin.prefix + "Deleted home " + args[0] + ".");
                break;

            // Error: Too many arguments
            default:
                player.sendMessage(this.plugin.prefix + ChatColor.RED + "Too many arguments! Usage /homes <page number>");
                return;
        }
        this.removeHome(player, args[0]);
    }

    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args)
    {
        parseCommand(this, commandSender, command, commandLabel, args);
        return false;
    }
}