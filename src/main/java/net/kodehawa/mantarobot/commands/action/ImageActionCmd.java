package net.kodehawa.mantarobot.commands.action;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.NoArgsCommand;
import net.kodehawa.mantarobot.utils.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import static br.com.brjdevs.java.utils.extensions.CollectionUtils.random;

@Slf4j
public class ImageActionCmd extends NoArgsCommand {
	private final Color color;
	private final String desc;
	private final String format;
	private final String imageName;
	private final List<String> images;
	private final String name;

	public ImageActionCmd(String name, String desc, Color color, String imageName, String format, List<String> images) {
		this.name = name;
		this.desc = desc;
		this.color = color;
		this.imageName = imageName;
		this.format = format;
		this.images = images;
	}

	@Override
	protected void call(GuildMessageReceivedEvent event) {
		String random = random(images);
		try {
			event.getChannel().sendFile(
				new FileInputStream(URLCache.getFile(random)),
				imageName,
				new MessageBuilder()
					.append(String.format(format, mentions(event), event.getAuthor().getAsMention()))
					.build()
			).queue();
		} catch (IOException e) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "I'd like to know what happened, but I couldn't send the image.").queue();
			log.error("Error while performing Action Command ``" + name + "``. The image ``" + random + "`` throwed an Exception.", e);
		}
	}

	@Override
	public CommandPermission permissionRequired() {
		return CommandPermission.USER;
	}

	@Override
	public MessageEmbed help(GuildMessageReceivedEvent event) {
		return helpEmbed(event, name)
			.setDescription(desc)
			.setColor(color)
			.build();
	}

	private String mentions(GuildMessageReceivedEvent event) {
		return event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" ")).trim();
	}

}
