package net.kodehawa.mantarobot.commands.action;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.stream.Collectors;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;

public class ImageCmd extends NoArgsCommand {
    public static final URLCache CACHE = new URLCache(20);

    private final String desc;
    private final String imageName;
    private final List<String> images;
    private final String name;
    private final String toSend;
    private boolean noMentions = false;

    public ImageCmd(String name, String desc, String imageName, List<String> images, String toSend){
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.imageName = imageName;
        this.images = images;
        this.toSend = toSend;
    }

    public ImageCmd(String name, String desc, String imageName, List<String> images, String toSend, boolean noMentions){
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.imageName = imageName;
        this.images = images;
        this.toSend = toSend;
        this.noMentions = noMentions;
    }

    @Override
    protected void call(GuildMessageReceivedEvent event, String content) {
        String random = random(images);
        String extension = random.substring(random.lastIndexOf("."));
        MessageBuilder builder = new MessageBuilder();
        builder.append(EmoteReference.TALKING);

        if(!noMentions){
            List<User> users = event.getMessage().getMentionedUsers();
            String names = "";
            if(users != null) names = users.stream().map(user -> event.getGuild().getMember(user).getEffectiveName()).collect(Collectors.joining(", "));
            if(!names.isEmpty()) builder.append("**").append(names).append("**, ");
        }

        builder.append(toSend);
        event.getChannel().sendFile(
                CACHE.getInput(random),
                imageName + "." + extension,
                builder.build()
        ).queue();
    }

    @Override
    public MessageEmbed help(GuildMessageReceivedEvent event) {
        return helpEmbed(event, name)
                .setDescription(desc)
                .setColor(event.getMember().getColor())
                .build();
    }
}
