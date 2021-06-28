package net.nuggetmc.ai.commands.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import net.nuggetmc.ai.PlayerAI;
import net.nuggetmc.ai.bot.Bot;
import net.nuggetmc.ai.bot.BotManager;
import net.nuggetmc.ai.commands.CommandHandler;
import net.nuggetmc.ai.commands.CommandInstance;
import net.nuggetmc.ai.utils.ChatUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.text.NumberFormat;
import java.util.Locale;

public class PlayerAICommand extends CommandInstance {

    private BotManager manager;

    public PlayerAICommand(CommandHandler commandHandler) {
        super(commandHandler);

        this.manager = PlayerAI.getInstance().getManager();
    }

    @Command(name = "", desc = "The PlayerAI main command.")
    @Require("playerai.manage")
    public void rootCommand(@Sender Player sender) {
        sender.sendMessage(ChatUtils.LINE);
        sender.sendMessage(ChatColor.GOLD + "PlayerAI" + ChatColor.GRAY + " [" + ChatColor.RED + "v" + PlayerAI.VERSION + ChatColor.GRAY + "]");

        for (String line : getCommandHandler().getHelp(getClass())) {
            sender.sendMessage(line);
        }

        sender.sendMessage(ChatUtils.LINE);
    }

    @Command(name = "create", desc = "Create bots.", usage = "<name> [skin]")
    @Require("playerai.manage")
    public void createBotCommand(@Sender Player sender, String name, @OptArg String skin) {
        manager.createBots(sender, name, skin, 1);
    }

    @Command(name = "multi", desc = "Create multiple bots at once.", usage = "<amount> <name> [skin]")
    @Require("playerai.manage")
    public void multiBotCommand(@Sender Player sender, int n, String name, @OptArg String skin) {
        manager.createBots(sender, name, skin, n);
    }

    @Command(name = "debug", desc = "Debug bot stats.")
    @Require("playerai.manage")
    public void debugCommand(@Sender Player sender) {
        // This will be used for miscellaneous code for testing as the plugin is worked on

        Location loc = sender.getLocation();

        for (Bot bot : PlayerAI.getInstance().getManager().fetch()) {
            bot.faceLocation(loc);
        }
    }

    @Command(name = "info", desc = "Information about loaded bots.")
    @Require("playerai.manage")
    public void infoCommand(@Sender Player player) {
        // This will be the future GUI where players can view information about every loaded bot
    }

    @Command(name = "reset", desc = "Remove all loaded bots.")
    @Require("playerai.manage")
    public void resetCommand(@Sender CommandSender sender) {
        sender.sendMessage("Removing every bot...");

        BotManager manager = PlayerAI.getInstance().getManager();
        int size = manager.fetch().size();
        manager.reset();

        String formatted = NumberFormat.getNumberInstance(Locale.US).format(size);
        sender.sendMessage("Removed " + ChatColor.RED + formatted + ChatColor.RESET + " entit" + (size == 1 ? "y" : "ies") + ".");
    }
}