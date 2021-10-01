package net.nuggetmc.tplus.bot;

import net.minecraft.server.v1_16_R3.PlayerConnection;
import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.bot.agent.Agent;
import net.nuggetmc.tplus.bot.agent.legacyagent.LegacyAgent;
import net.nuggetmc.tplus.bot.agent.legacyagent.ai.NeuralNetwork;
import net.nuggetmc.tplus.bot.event.BotDeathEvent;
import net.nuggetmc.tplus.utils.MojangAPI;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BotManager implements Listener {

    private final Agent agent;
    private final Set<Bot> bots;
    private final NumberFormat numberFormat;
    private final TerminatorPlus plugin;

    public boolean joinMessages = false;

    public BotManager() {
        this.agent = new LegacyAgent(this);
        this.bots = ConcurrentHashMap.newKeySet(); //should fix concurrentmodificationexception
        this.numberFormat = NumberFormat.getInstance(Locale.US);
        this.plugin = TerminatorPlus.getInstance();
    }

    public Set<Bot> fetch() {
        return bots;
    }

    public void add(Bot bot) {
        if (joinMessages) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + bot.getName() + " joined the game");
        }

        bots.add(bot);
    }

    public Bot getFirst(String name) {
        for (Bot bot : bots) {
            if (name.equals(bot.getName())) {
                return bot;
            }
        }

        return null;
    }

    public List<String> fetchNames() {
        return bots.stream().map(Bot::getName).collect(Collectors.toList());
    }

    public Agent getAgent() {
        return agent;
    }

    public void createBots(Player sender, String name, String skinName, int n) {
        createBots(sender, name, skinName, n, null);
    }
    
    public void createBotsAt(Player sender, String name, double x, double y, double z) {
    
        Location location = new Location(sender.getWorld(), x, y, z);
    
        sender.sendMessage("Creating a new bot with name " + ChatColor.GREEN
            + name + ChatColor.RESET + " at "
            + ChatColor.YELLOW + location.getX()
            + ", " + location.getY() + ", " + location.getZ());
    
        createBots(location, name, MojangAPI.getSkin(name), 1, null);
    }

    public void createBots(Player sender, String name, String skinName, int n, NeuralNetwork network) {
        long timestamp = System.currentTimeMillis();

        if (n < 1) n = 1;

        sender.sendMessage("Creating " + (n == 1 ? "new bot" : ChatColor.RED + numberFormat.format(n) + ChatColor.RESET + " new bots")
                + " with name " + ChatColor.GREEN + name.replace("%", ChatColor.LIGHT_PURPLE + "%" + ChatColor.RESET)
                + (skinName == null ? "" : ChatColor.RESET + " and skin " + ChatColor.GREEN + skinName)
                + ChatColor.RESET + "...");

        skinName = skinName == null ? name : skinName;

        createBots(sender.getTargetBlock(null, 32).getLocation(), name, MojangAPI.getSkin(skinName), n, network);

        sender.sendMessage("Process completed (" + ChatColor.RED + ((System.currentTimeMillis() - timestamp) / 1000D) + "s" + ChatColor.RESET + ").");
    }

    public Set<Bot> createBots(Location loc, String name, String[] skin, int n, NeuralNetwork network) {
        List<NeuralNetwork> networks = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            networks.add(network);
        }

        return createBots(loc, name, skin, networks);
    }

    public Set<Bot> createBots(Location loc, String name, String[] skin, List<NeuralNetwork> networks) {
        Set<Bot> bots = new HashSet<>();
        World world = loc.getWorld();

        int n = networks.size();
        int i = 1;

        double f = n < 100 ? .004 * n : .4;

        for (NeuralNetwork network : networks) {
            Bot bot = Bot.createBot(loc, name.replace("%", String.valueOf(i)), skin);

            if (network != null) {
                bot.setNeuralNetwork(network == NeuralNetwork.RANDOM ? NeuralNetwork.generateRandomNetwork() : network);
                bot.setShield(true);
                bot.setDefaultItem(new ItemStack(Material.WOODEN_AXE));
                //bot.setRemoveOnDeath(false);
            }

            if (network != null) {
                bot.setVelocity(randomVelocity());
            } else if (i > 1) {
                bot.setVelocity(randomVelocity().multiply(f));
            }

            bots.add(bot);
            i++;
        }

        if (world != null) {
            world.spawnParticle(Particle.CLOUD, loc, 100, 1, 1, 1, 0.5);
        }

        return bots;
    }

    private Vector randomVelocity() {
        return new Vector(Math.random() - 0.5, 0.5, Math.random() - 0.5).normalize();
    }

    public void remove(Bot bot) {
        bots.remove(bot);
    }

    public void reset() {
        if (!bots.isEmpty()) {
            bots.forEach(Bot::removeVisually);
            bots.clear(); // Not always necessary, but a good security measure
        }

        agent.stopAllTasks();
    }

    public Bot getBot(Player player) { // potentially memory intensive
        Bot bot = null;

        int id = player.getEntityId();

        for (Bot b : bots) {
            if (id == b.getId()) {
                bot = b;
                break;
            }
        }

        return bot;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerConnection connection = ((CraftPlayer) event.getPlayer()).getHandle().playerConnection;
        bots.forEach(bot -> bot.render(connection, true));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Bot bot = getBot(player);

        if (bot != null) {
            agent.onBotDeath(new BotDeathEvent(event, bot));
        }
    }

    @EventHandler
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Location playerPos = player.getLocation();

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                bots.forEach(bot -> {
                    String name = bot.getName();
                    reset();
                    bot.createBot(playerPos, name, MojangAPI.getSkin(name));
                });

                player.sendMessage("The bots have followed you into the portal.");
            }
        }, 200L);
    }
}
