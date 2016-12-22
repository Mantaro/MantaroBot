package net.kodehawa.mantarobot.cmd;

import bsh.Interpreter;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.listeners.Listener;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.util.StringArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Owner extends Module {

    public Owner(){
        this.registerCommands();
    }

    @Override
    public void registerCommands(){
        super.register("add", "Adds a item to a list.", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                if(event.getAuthor().getId().equals(Mantaro.OWNER_ID)){
                    String noArgs = content.split(" ")[0];
                    switch(noArgs){
                        case "greeting":
                            String greet = content.replace("greeting" + " ", "");
                            Action.greeting.add(greet);
                            new StringArrayUtils("greeting", Action.greeting, true, true);
                            channel.sendMessage(":speech_balloon:" + "Added to greeting list: " + greet);
                            break;
                        case "tsun":
                            String tsun = content.replace("tsun" + " ", "");
                            Action.tsunLines.add(tsun);
                            new StringArrayUtils("tsunderelines", Action.tsunLines, true, true);
                            channel.sendMessage(":speech_balloon:" + "Added to tsundere list: " + tsun);
                            break;
                        default:
                            channel.sendMessage(":speech_balloon:" + "Silly master, use ~>add greeting or ~>add tsun");
                            break;
                    }
                }
                else{
                    channel.sendMessage(":heavy_multiplication_x:" + "How did you even know?");
                }
            }

            @Override
            public String help() {
                return "";
            }

            @Override
            public CommandType commandType() {
                return CommandType.OWNER;
            }
        });

        super.register("eval", "Evaluates arbitrary code.", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                if(event.getAuthor().getId().equals(Mantaro.OWNER_ID)){
                    try {
                        Interpreter interpreter = new Interpreter();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(baos);
                        PrintStream old = System.out;
                        System.setOut(ps);
                        interpreter.eval("import *; " + content);
                        System.out.flush();
                        System.setOut(old);
                        event.getChannel().sendMessage(baos.toString()).queue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public String help() {
                return "";
            }

            @Override
            public CommandType commandType() {
                return CommandType.OWNER;
            }
        });

        super.register("shutdown", "Shuts down the bot.", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                channel = event.getChannel();
                if(event.getAuthor().getId().equals(Mantaro.OWNER_ID)){
                    channel.sendMessage("Gathering information...").queue();
                    try {
                        new StringArrayUtils("quotes", Quote.quotes, true);
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {	}

                    channel.sendMessage("Gathered.").queue();

                    channel.sendMessage("Starting bot shutdown...").queue();
                    try {
                        Action.tsunLines.clear();
                        System.gc();
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {	}

                    channel.sendMessage("*goes to sleep*").queue();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {	}

                    try{
                        Mantaro.instance().getSelf().removeEventListener(new Listener());
                        System.exit(1);
                    }
                    catch (Exception e){
                        Mantaro.instance().getSelf().addEventListener(new Listener());
                        channel.sendMessage(":heavy_multiplication_x: " + "Couldn't shut down." + e.toString()).queue();
                    }
                }
                else{
                    channel.sendMessage(":heavy_multiplication_x:" + "You cannot do that, silly.").queue();
                }
            }

            @Override
            public String help() {
                return "";
            }

            @Override
            public CommandType commandType() {
                return CommandType.OWNER;
            }
        });
    }
}
