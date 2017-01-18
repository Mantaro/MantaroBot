package net.kodehawa.mantarobot.listeners.generic;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import net.dv8tion.jda.core.EmbedBuilder;
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
public class GenericListener extends ListenerAdapter {
    private Function<MusicManager, String> _musicFunction;
    private Function<EmbedBuilder, String> _genericFunction;
    private Type requestType;

    public GenericListener(Function<StringBuilder, String> action, AudioPlaylist audioPlaylist, Guild guild, MusicManager musicManager, List<String> list,
                     String id, String[] args, MessageReceivedEvent evt, Function<MusicManager, String> function){
        requestType = Type.MUSIC;
        _musicFunction = function;
        Variables.instance._guild = guild;
        Variables.instance._musicManager = musicManager;
        Variables.instance._audioPlaylist = audioPlaylist;
        Variables.instance._userId = id;
        Variables.instance._args = args;
        Variables.instance._evt = evt;
        Variables.instance._list = list;

        StringBuilder sb = new StringBuilder();
        action.apply(sb);
    }

    public GenericListener(Function<StringBuilder, String> action, Guild guild, List<String> list,
                           String id, String[] args, MessageReceivedEvent evt, Function<EmbedBuilder, String> function){
        requestType = Type.GENERAL;
        _genericFunction = function;
        Variables.instance._guild = guild;
        Variables.instance._userId = id;
        Variables.instance._args = args;
        Variables.instance._evt = evt;
        Variables.instance._list = list;

        StringBuilder sb = new StringBuilder();
        action.apply(sb);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        Variables.instance._gEventTmp = event;
        if(requestType.equals(Type.MUSIC)){
            _musicFunction.apply(Variables.instance._musicManager);
        }
        else {
            EmbedBuilder builder = new EmbedBuilder();
            _genericFunction.apply(builder);
        }
    }

    private enum Type{
        MUSIC, GENERAL
    }
}
