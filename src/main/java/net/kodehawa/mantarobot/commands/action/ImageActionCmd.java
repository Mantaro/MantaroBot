package net.kodehawa.mantarobot.commands.action;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;

@Slf4j
public class ImageActionCmd extends NoArgsCommand {
	public static final URLCache CACHE = new URLCache(20);

	private final Color color;
	private final String desc;
	private final String format;
	private final String imageName;
	private final List<String> images;
	private final String lonelyLine;
	private final String name;
	private boolean swapNames = false;

	public ImageActionCmd(String name, String desc, Color color, String imageName, String format, List<String> images, String lonelyLine) {
		super(Category.ACTION);
		this.name = name;
		this.desc = desc;
		this.color = color;
		this.imageName = imageName;
		this.format = format;
		this.images = images;
		this.lonelyLine = lonelyLine;
	}

	public ImageActionCmd(String name, String desc, Color color, String imageName, String format, List<String> images, String lonelyLine, boolean swap) {
		super(Category.ACTION);
		this.name = name;
		this.desc = desc;
		this.color = color;
		this.imageName = imageName;
		this.format = format;
		this.images = images;
		this.lonelyLine = lonelyLine;
		this.swapNames = swap;
	}

	@Override
	protected void call(GuildMessageReceivedEvent event, String content) {
		String random = random(images);
		try {
			if (mentions(event).isEmpty()) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention a user").queue();
				return;
			}
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			MessageBuilder toSend = new MessageBuilder()
					.append(String.format(format, mentions(event), event.getAuthor().getAsMention()));

			if(!guildData.isNoMentionsAction() && swapNames) {
				toSend = new MessageBuilder()
						.append(String.format(format, event.getAuthor().getAsMention(), mentions(event)));
			}

			if(guildData.isNoMentionsAction()) {
				toSend = new MessageBuilder()
						.append(String.format(format, "**" + noMentions(event) + "**", "**" + event.getMember().getEffectiveName() + "**"));
			}

			if(swapNames && guildData.isNoMentionsAction()) {
				toSend = new MessageBuilder()
						.append(String.format(format, "**" +  event.getMember().getEffectiveName() + "**", "**" + noMentions(event) + "**"));
			}

			if(isLonely(event)) {
				toSend = new MessageBuilder().append("**").append(lonelyLine).append("**");
			}

			event.getChannel().sendFile(
				CACHE.getInput(random),
				imageName,
				toSend.build()
			).queue();
		} catch (Exception e) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "I'd like to know what happened, but I couldn't send the image. Probably I don't have permissions to.").queue();
		}
	}

	@Override
	public MessageEmbed help(GuildMessageReceivedEvent event) {
		return helpEmbed(event, name)
			.setDescription(desc)
			.setColor(color)
			.build();
	}

	private boolean isLonely(GuildMessageReceivedEvent event) {
		return event.getMessage().getMentionedUsers().stream().anyMatch(
			user -> user.getId().equals(event.getAuthor().getId()));
	}

	private String mentions(GuildMessageReceivedEvent event) {
		return event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(", ")).trim();
	}

	private String noMentions(GuildMessageReceivedEvent event) {
		return event.getMessage().getMentionedUsers().stream().map(user -> event.getGuild().getMember(user).getEffectiveName()).collect(Collectors.joining(", ")).trim();
	}
}
