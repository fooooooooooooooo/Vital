package io.github.caprapaul.vital.commands;

import io.github.caprapaul.bettercommandexecutor.BetterCommand;
import io.github.caprapaul.bettercommandexecutor.BetterCommandExecutor;
import io.github.caprapaul.vital.Vital;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class TeleportCommands extends BetterCommandExecutor
{
    private enum RequestType
    {
        HERE,
        THERE
    }

    private class Request
    {
        public Request(String sender, RequestType type)
        {
            this.sender = sender;
            this.type = type;
        }

        private String sender;
        private RequestType type;

        public String getSender()
        {
            return sender;
        }

        public RequestType getType()
        {
            return type;
        }
    }

    private final Vital plugin;
    private Map<String, Long> tpaCooldowns = new HashMap<String, Long>();
    private Map<String, Request> currentRequests = new HashMap<String, Request>();

    public TeleportCommands(Vital plugin)
    {
        this.plugin = plugin;
        loadCommands(this, this.plugin);
        loadConfig();
    }

    private void loadConfig()
    {
        plugin.getConfig().addDefault("tpa-cooldown", 5);
        plugin.getConfig().addDefault("keep-alive", 30);
        plugin.getConfig().addDefault("override-old-requests", false);
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args)
    {
        parseCommand(this, commandSender, command, commandLabel, args);
        return false;
    }

    private void sendRequest(Player sender, Player recipient, RequestType type)
    {
        sender.sendMessage(ChatColor.GRAY + "Sending a teleport request to " + recipient.getName() + ".");

        String sendTpAccept = "";
        String sendTpDeny = "";

        if (recipient.hasPermission("vital.tpaccept"))
        {
            sendTpAccept = " To accept the teleport request, type " +  ChatColor.GREEN + "/tpaccept" + ChatColor.RESET + ".";
        }
        else
        {
            sendTpAccept = "";
        }

        if (recipient.hasPermission("vital.tpdeny"))
        {
            sendTpDeny = " To deny the teleport request, type " + ChatColor.RED + "/tpdeny" + ChatColor.RESET + ".";
        }
        else
        {
            sendTpDeny = "";
        }

        switch (type)
        {
            case HERE:
                recipient.sendMessage(ChatColor.GOLD + sender.getName() + ChatColor.RESET + ChatColor.GRAY + " has sent you a request to teleport to them." + ChatColor.RESET + sendTpAccept + sendTpDeny);
                break;
            case THERE:
                recipient.sendMessage(ChatColor.GOLD + sender.getName() + ChatColor.RESET + ChatColor.GRAY + " has sent a request to teleport to you." + ChatColor.RESET + sendTpAccept + sendTpDeny);
                break;
        }
        currentRequests.put(recipient.getName(), new Request(sender.getName(), type));
    }

    private boolean killRequest(String key)
    {
        if (currentRequests.containsKey(key))
        {
            Player sender = plugin.getServer().getPlayer(currentRequests.get(key).getSender());
            if (!(sender == null))
            {
                sender.sendMessage(ChatColor.RED + "Your teleport request timed out.");
            }

            currentRequests.remove(key);

            return true;
        }
        else
        {
            return false;
        }
    }

    @BetterCommand(name = "tpa")
    public void tpa(CommandSender commandSender, String[] args, String commandLabel)
    {
        if (!(commandSender instanceof Player))
        {
            commandSender.sendMessage(ChatColor.RED + "Error: The console can't teleport!");
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("vital.overridecooldown"))
        {
            int cooldown = plugin.getConfig().getInt("tpa-cooldown");
            if (tpaCooldowns.containsKey(player.getName()))
            {
                long diff = (System.currentTimeMillis() - tpaCooldowns.get(commandSender.getName())) / 1000;
                if (diff < cooldown)
                {
                    player.sendMessage(ChatColor.RED + "Error: You must wait a " + cooldown + " second cooldown in between teleport requests!");
                    return;
                }
            }
        }

        if (args.length == 0)
        {
            player.sendMessage(ChatColor.GRAY + "Send a teleport request to a player.");
            player.sendMessage(ChatColor.GRAY + "/tpa <player>");
            return;
        }

        final Player target = plugin.getServer().getPlayer(args[0]);
        long keepAlive = plugin.getConfig().getLong("keep-alive");

        if (target == null)
        {
            player.sendMessage(ChatColor.RED + "Error: You can only send a teleport request to online players!");
            return;
        }

        if (target == player)
        {
            player.sendMessage(ChatColor.RED + "Error: You can't teleport to yourself!");
            return;
        }

        boolean overrideOldRequest = plugin.getConfig().getBoolean("override-old-request");

        if (!(overrideOldRequest) && currentRequests.containsKey(target.getName()))
        {
            if (currentRequests.get(target.getName()).getSender().equals(player.getName()))
            {
                player.sendMessage(ChatColor.RED + "Error: You can't send multiple requests to the same player!");
                return;
            }
        }

        sendRequest(player, target, RequestType.THERE);

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
        {
            public void run()
            {
                killRequest(target.getName());
            }
        }, keepAlive);

        tpaCooldowns.put(player.getName(), System.currentTimeMillis());
    }

    @BetterCommand(name = "tpahere")
    public void tpahere(CommandSender commandSender, String[] args, String commandLabel)
    {
        if (!(commandSender instanceof Player))
        {
            commandSender.sendMessage(ChatColor.RED + "Error: Can't teleport to console!");
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("vital.overridecooldown"))
        {
            int cooldown = plugin.getConfig().getInt("tpa-cooldown");
            if (tpaCooldowns.containsKey(player.getName()))
            {
                long diff = (System.currentTimeMillis() - tpaCooldowns.get(commandSender.getName())) / 1000;
                if (diff < cooldown)
                {
                    player.sendMessage(ChatColor.RED + "Error: You must wait a " + cooldown + " second cooldown in between teleport requests!");
                    return;
                }
            }
        }

        if (args.length == 0)
        {
            player.sendMessage("Send a teleport request to a player.");
            player.sendMessage("/tpahere <player>");
            return;
        }

        final Player target = plugin.getServer().getPlayer(args[0]);
        long keepAlive = plugin.getConfig().getLong("keep-alive") * 20;

        if (target == null)
        {
            player.sendMessage(ChatColor.RED + "Error: You can only send a teleport request to online players!");
            return;
        }

        if (target == player)
        {
            player.sendMessage(ChatColor.RED + "Error: You can't teleport to yourself!");
            return;
        }

        boolean overrideOldRequest = plugin.getConfig().getBoolean("override-old-request");

        if (!(overrideOldRequest) && currentRequests.containsKey(target.getName()))
        {
            if (currentRequests.get(target.getName()).getSender().equals(player.getName()))
            {
                player.sendMessage(ChatColor.RED + "Error: You can't send multiple requests to the same player!");
                return;
            }
        }

        sendRequest(player, target, RequestType.HERE);

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
        {
            public void run()
            {
                killRequest(target.getName());
            }
        }, keepAlive);

        tpaCooldowns.put(player.getName(), System.currentTimeMillis());
    }

    @BetterCommand(name = "tpaccept")
    public void tpaccept(CommandSender commandSender, String[] args, String commandLabel)
    {
        if (!(commandSender instanceof Player))
        {
            commandSender.sendMessage(ChatColor.RED + "Error: The console can't accept teleport requests!");
            return;
        }

        Player player = (Player) commandSender;

        if (!(currentRequests.containsKey(player.getName())))
        {
            player.sendMessage(ChatColor.RED + "Error: It appears you don't have any tp requests currently. Maybe it timed out?");
            return;
        }
        Request request = currentRequests.get(player.getName());

        Player teleportingPlayer = null;
        Player targetPlayer = null;
        switch (request.type)
        {
            case HERE:
                teleportingPlayer = player;
                targetPlayer = plugin.getServer().getPlayer(request.getSender());
                break;
            case THERE:
                teleportingPlayer = plugin.getServer().getPlayer(request.getSender());
                targetPlayer = player;
                break;
        }

        currentRequests.remove(player.getName());

        if (teleportingPlayer == null)
        {
            targetPlayer.sendMessage(ChatColor.RED + "Error: It appears that the person trying to teleport to you doesn't exist anymore.");
            return;
        }

        if (targetPlayer == null)
        {
            teleportingPlayer.sendMessage(ChatColor.RED + "Error: It appears that the person you are trying to teleport to doesn't exist anymore.");
            return;
        }

        teleportingPlayer.teleport(targetPlayer);

        targetPlayer.sendMessage(ChatColor.GRAY + "Teleporting...");
        teleportingPlayer.sendMessage(ChatColor.GRAY + "Teleporting...");
    }

    @BetterCommand(name = "tpdeny")
    public void tpdeny(CommandSender commandSender, String[] args, String commandLabel)
    {
        if (!(commandSender instanceof Player))
        {
            commandSender.sendMessage(ChatColor.RED + "Error: The console can't deny teleport requests!");
            return;
        }

        Player player = (Player) commandSender;

        if (!(currentRequests.containsKey(player.getName())))
        {
            player.sendMessage(ChatColor.RED + "Error: It appears you don't have any tp requests currently. Maybe it timed out?");
            return;
        }
        Request request = currentRequests.get(player.getName());

        Player rejectedPlayer = plugin.getServer().getPlayer(currentRequests.get(player.getName()).getSender());
        currentRequests.remove(player.getName());

        if (rejectedPlayer == null)
        {
            return;
        }

        rejectedPlayer.sendMessage(ChatColor.RED + player.getName() + " rejected your teleport request! :(");
        player.sendMessage(ChatColor.GRAY + rejectedPlayer.getName() + " was rejected!");
    }

    @BetterCommand(name = "test")
    public void test(CommandSender commandSender, String[] args, String commandLabel)
    {
        plugin.getLogger().info("TEST!");
    }
}