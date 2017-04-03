package net.kodehawa.mantarobot.connectionwatcher;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.load.LoaderException;
import net.sandius.rembulan.runtime.LuaFunction;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static net.kodehawa.mantarobot.connectionwatcher.ConnectionWatcher.LOGGER;

public class CommandListener implements EventListener {
    private final String prefix;
    private final List<String> owners;

    public CommandListener(String prefix, List<String> owners) {
        this.prefix = prefix;
        this.owners = owners;
    }

    @Override
    public void onEvent(Event event) {
        if(event instanceof ReadyEvent) {
            DiscordLogBack.enable();
            LOGGER.info("Launching Mantaro!");
            try {
                ConnectionWatcher.getInstance().launchMantaro(false);
            } catch(IOException e) {
                LOGGER.error("Error starting mantaro", e);
            }
            return;
        }
        if(event instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent e = (GuildMessageReceivedEvent)event;
            if(e.getAuthor().isBot() || e.getAuthor().isFake()) return;
            String message = e.getMessage().getRawContent();
            if(message.startsWith(prefix)) {
                if(!owners.contains(e.getAuthor().getId())) {
                    e.getChannel().sendMessage("You don't have permission to do this").queue();
                    return;
                }
                String[] args = message.substring(prefix.length()).split("\\s+");
                String command = args[0];
                if(args.length > 1) args = Arrays.copyOfRange(args, 1, args.length);
                else args = new String[0];
                switch(command) {
                    case "shutdown":
                        ConnectionWatcher.getInstance().stopMantaro(command.contains("hardkill"));
                        e.getChannel().sendMessage("Successfully stopped mantaro").queue();
                        break;
                    case "reboot":
                        try {
                            ConnectionWatcher.getInstance().launchMantaro(command.contains("hardkill"));
                            e.getChannel().sendMessage("Successfully rebooted mantaro").queue();
                        } catch(IOException ex) {
                            e.getChannel().sendMessage("There was an unexpected error while rebooting mantaro: " + ex.getMessage()).queue();
                            LOGGER.error("Error rebooting mantaro", ex);
                        }
                        break;
                    case "start":
                        try {
                            ConnectionWatcher.getInstance().launchMantaro(message.contains("hardkill"));
                            e.getChannel().sendMessage("Successfully started mantaro").queue();
                        } catch(IOException ex) {
                            e.getChannel().sendMessage("There was an unexpected error while starting mantaro: " + ex.getMessage()).queue();
                            LOGGER.error("Error starting mantaro", ex);
                        }
                        break;
                    case "eval":
                        try {
                            Object[] o = LuaEvaluator.eval(String.join(" ", args));
                            if(o == null) {
                                e.getChannel().sendMessage(new EmbedBuilder()
                                        .setColor(Color.RED)
                                        .setAuthor("Evaluated and errored", null, e.getAuthor().getAvatarUrl())
                                        .setDescription("Array returned by eval was null")
                                        .setFooter("Asked by: " + e.getAuthor().getName(), null)
                                        .build()).queue();
                                return;
                            }
                            List<Object> returns = new ArrayList<>();
                            for(Object obj : o) {
                                if(obj instanceof String || obj instanceof Number || obj == null) {
                                    returns.add(obj);
                                } else if(obj instanceof Table) {
                                    Map<Object, Object> map = new HashMap<>();
                                    Table t = (Table)obj;
                                    for(Object key = t.initialKey(); key != null; key = t.successorKeyOf(key)) {
                                        Object v = t.rawget(key);
                                        Object k = key.getClass().getName().equals("net.sandius.rembulan.StringByteString") ? key.toString() : key;
                                        if(v instanceof LuaFunction) {
                                            map.put(k, "function: 0x" + Integer.toHexString(System.identityHashCode(v)));
                                        } if(v instanceof Table) {
                                            map.put(k, "table: 0x" + Integer.toHexString(System.identityHashCode(v)));
                                        } else {
                                            map.put(k, v);
                                        }
                                    }
                                    returns.add(map);
                                } else if(obj instanceof LuaFunction) {
                                    returns.add("function: 0x" + Integer.toHexString(System.identityHashCode(obj)));
                                } else {
                                    returns.add(obj);
                                }
                            }
                            String desc;
                            switch(returns.size()) {
                                case 0:
                                    desc = "nothing";
                                    break;
                                case 1:
                                    Object returned = returns.get(0);
                                    desc = returned == null ? null : String.valueOf(returned);
                                    break;
                                default:
                                    desc = returns.toString();
                                    break;
                            }
                            if(desc != null && desc.length() > MessageEmbed.TEXT_MAX_LENGTH) {
                                try {
                                    String pasteToken = Unirest.post("https://hastebin.com/documents")
                                            .header("User-Agent", "Mantaro")
                                            .header("Content-Type", "text/plain")
                                            .body(desc)
                                            .asJson()
                                            .getBody()
                                            .getObject()
                                            .getString("key");
                                    desc = "https://hastebin.com/" + pasteToken;
                                } catch(UnirestException ex) {
                                    LOGGER.error("Error posting to hastebin", ex);
                                    desc = "Error posting to hastebin";
                                }
                            }
                            e.getChannel().sendMessage(new EmbedBuilder()
                                    .setColor(Color.GREEN)
                                    .setAuthor("Evaluated with success", null, e.getAuthor().getAvatarUrl())
                                    .setDescription(desc == null ? "Executed successfully with no objects returned" : "Executed successfully and returned: " + desc)
                                    .setFooter("Asked by: " + e.getAuthor().getName(), null)
                                    .build()).queue();
                        } catch(LoaderException ex) {
                            String err = ex.getLuaStyleErrorMessage();
                            e.getChannel().sendMessage(new EmbedBuilder()
                                    .setColor(Color.RED)
                                    .setAuthor("Evaluated and errored", null, e.getAuthor().getAvatarUrl())
                                    .setDescription(err)
                                    .setFooter("Asked by: " + e.getAuthor().getName(), null)
                            .build()).queue();
                        } catch(LuaEvaluator.RunningException ex) {
                            String err = ex.getTraceback();
                            e.getChannel().sendMessage(new EmbedBuilder()
                                    .setColor(Color.RED)
                                    .setAuthor("Evaluated and errored", null, e.getAuthor().getAvatarUrl())
                                    .setDescription(err)
                                    .setFooter("Asked by: " + e.getAuthor().getName(), null)
                                    .build()).queue();
                        }
                }
            }
        }
    }
}
