package net.kodehawa.mantarobot.listeners.generic;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.audio.MusicManager;

import java.util.List;

public class Variables {
    public static volatile Variables instance = new Variables();

    public String[] _args;
    public String _userId;
    public MessageReceivedEvent _evt;
    public GuildMessageReceivedEvent _gEventTmp;
    public List _list;
    public Guild _guild;
    public MusicManager _musicManager;
    public AudioPlaylist _audioPlaylist;


}
