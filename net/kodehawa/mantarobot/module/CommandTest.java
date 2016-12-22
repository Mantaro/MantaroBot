package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Random;

public class CommandTest extends Module {

    public CommandTest(){
        registerCommands();
    }

    @Override
    public void registerCommands(){
        super.registerCommands();
        super.register("lewd", "even lewder", new Callback() {
            @Override
            public void onCommand(String[]args, String content, MessageReceivedEvent event) {
                event.getChannel().sendMessage("lewd").queue();
            }

            @Override
            public String help() {
                return "lewd";
            }

            @Override
            public CommandType commandType() {
                return CommandType.USER;
            }
        });

        super.register("random", "aaaa", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                Random rand = new Random();
                event.getChannel().sendMessage(rand.nextInt() + " lewds").queue();
            }

            @Override
            public String help() {
                return "randomized af";
            }

            @Override
            public CommandType commandType() {
                return CommandType.USER;
            }
        });
    }
}
