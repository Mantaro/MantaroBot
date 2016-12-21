package net.kodehawa.mantarobot.cmd;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.management.Command;
import net.kodehawa.mantarobot.thread.ThreadPoolHelper;

public class Test extends Command {

    public Test(){
        this.setName("test");
        this.setDescription("Test");
    }

    private String channelId;
    private String userId;
    @Override
    public void onCommand(String[] message, String content, MessageReceivedEvent event)
    {
        Runnable r = () -> {
            if(content.equals("hi")){
                channelId = event.getChannel().getId();
                userId = event.getAuthor().getId();
                Mantaro.instance().getSelf().addEventListener(new ListenerA());
                event.getChannel().sendMessage("say hi").queue();
            }
        };
        ThreadPoolHelper.instance().startThread("test", r);
    }

    private class ListenerA extends ListenerAdapter{
        @Override
        public void onMessageReceived(MessageReceivedEvent event){
            if(event.getMessage().getContent().equals("hi hi")
                    && event.getChannel().getId().equals(channelId) && event.getAuthor().getId().equals(userId)){
                Runnable r = () -> {
                    event.getChannel().sendMessage("test1").queue();
                    Mantaro.instance().getSelf().removeEventListener(this);
                };
                ThreadPoolHelper.instance().startThread("test listener", r);
            }
        }
    }
}
