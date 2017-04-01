package net.kodehawa.mantarobot.connectionwatcher;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.io.IOException;
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
            if(!owners.contains(e.getAuthor().getId())) {
                e.getChannel().sendMessage("You don't have permission to do this").queue();
                return;
            }
            String message = e.getMessage().getContent();
            if(message.startsWith(prefix)) {
                String command = message.substring(prefix.length());
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
                }
            }
        }
    }
}
