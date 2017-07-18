package net.kodehawa.mantarobot.core.listeners.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.dataporter.oldentities.OldPlayer;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.CommandProcessorAndRegistry;
import net.kodehawa.mantarobot.core.ShardMonitorEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.shard.MantaroShard;

import java.util.Random;

@Slf4j
public class CommandListener implements EventListener {
	public static final CommandProcessorAndRegistry PROCESSOR = new CommandProcessorAndRegistry();
	//Message cache of 5000 messages. If it reaches 5000 it will delete the first one stored, and continue being 5000
	@Getter
	private static final Cache<String, Message> messageCache = CacheBuilder.newBuilder().concurrencyLevel(10).maximumSize(5000).build();

	private final Random random = new Random();
	private final int shardId;
	private final MantaroShard shard;

	public CommandListener(int shardId, MantaroShard shard) {
		this.shardId = shardId;
		this.shard = shard;
	}

	@Override
	public void onEvent(Event e) {
		if (e instanceof ShardMonitorEvent) {
			if(MantaroBot.getInstance().getShardedMantaro().getShards()[shardId].getEventManager().getLastJDAEventTimeDiff() > 120000) return;
			((ShardMonitorEvent) e).alive(shardId, ShardMonitorEvent.COMMAND_LISTENER);
			return;
		}

		if (e instanceof GuildMessageReceivedEvent) {
			GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) e;
			messageCache.put(event.getMessage().getId(), event.getMessage());

			//I KNOW THIS TYPE OF SYNTAX IS A BITCH. But hey, it's readable.
			// @formatter:off
			if (
				event.getAuthor().isBot()
				||
				!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)
				&&
				!event.getGuild().getSelfMember().hasPermission(Permission.ADMINISTRATOR)
			) return;
			// @formatter:on

			shard.getCommandPool().execute(() -> PROCESSOR.run(event));

			if (random.nextInt(15) > 10) {
				if (event.getMember() == null) return;
				OldPlayer player = MantaroData.db().getPlayer(event.getMember());
				if(event.getMember().getUser().isBot()) return;
				if (player != null) {
					if (player.getLevel() == 0) player.setLevel(1);
					player.getData().setExperience(player.getData().getExperience() + Math.round(random.nextInt(6)));

					if (player.getData().getExperience() > (player.getLevel() * Math.log10(player.getLevel()) * 1000)) {
						player.setLevel(player.getLevel() + 1);
					}

					player.saveAsync();
				}
			}
		}
	}
}
