package net.kodehawa.mantarobot.listeners.generic;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.audio.MusicManager;

import java.util.List;
import java.util.function.Function;

/**
 * <pre>You found me!</pre>
 * Creates a generic listener.
 * It listens to any defined Function by the user, and it can be used for all kinds of applications.
 */
public final class OldGenericListener extends ListenerAdapter {
	private enum Type {
		MUSIC, GENERAL
	}

	public String[] _args;
	public AudioPlaylist _audioPlaylist;
	public MessageReceivedEvent _evt;
	public GuildMessageReceivedEvent _gEventTmp;
	public Guild _guild;
	public List _list;
	public MusicManager _musicManager;
	public String _userId;
	private Function<OldGenericListener, String> _genericFunction;
	private Function<OldGenericListener, String> _musicFunction;
	private Type requestType;

	public OldGenericListener(Function<OldGenericListener, String> action, AudioPlaylist audioPlaylist, Guild guild, MusicManager musicManager, List<String> list,
							  String id, String[] args, MessageReceivedEvent evt, Function<OldGenericListener, String> function) {
		requestType = Type.MUSIC;
		_musicFunction = function;
		_guild = guild;
		_musicManager = musicManager;
		_audioPlaylist = audioPlaylist;
		_userId = id;
		_args = args;
		_evt = evt;
		_list = list;

		action.apply(this);
	}

	public OldGenericListener(Function<OldGenericListener, String> action, Guild guild, List<String> list,
							  String id, String[] args, MessageReceivedEvent evt, Function<OldGenericListener, String> function) {
		requestType = Type.GENERAL;
		_genericFunction = function;
		_guild = guild;
		_userId = id;
		_args = args;
		_evt = evt;
		_list = list;

		action.apply(this);
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		_gEventTmp = event;
		if (requestType.equals(Type.MUSIC)) {
			_musicFunction.apply(this);
		} else {
			_genericFunction.apply(this);
		}
	}
}
