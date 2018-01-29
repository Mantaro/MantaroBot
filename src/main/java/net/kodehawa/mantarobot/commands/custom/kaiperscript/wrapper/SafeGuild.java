package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.Region;
import net.dv8tion.jda.core.entities.Guild;

import java.util.List;
import java.util.stream.Collectors;

class SafeGuild extends SafeJDAObject<Guild> {
    private final SafeChannel channel;

    SafeGuild(Guild guild, SafeChannel channel) {
        super(guild);
        this.channel = channel;
    }

    public String getName() {
        return object.getName();
    }

    public Guild.ExplicitContentLevel getExplicitContentLevel() {
        return object.getExplicitContentLevel();
    }

    public Region getRegion() {
        return object.getRegion();
    }

    public List<SafeChannel> getTextChannels() {
        return object.getTextChannels().stream().map(c->c.getIdLong() == channel.getIdLong() ? channel : new SafeChannel(c)).collect(Collectors.toList());
    }

    public List<SafeRole> getRoles() {
        return object.getRoles().stream().map(SafeRole::new).collect(Collectors.toList());
    }

    public List<SafeMember> getMembers() {
        return object.getMembers().stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public SafeMember getOwner() {
        return new SafeMember(object.getOwner());
    }

    public List<SafeMember> getMembersByName(String name, boolean ignoreCase) {
        return object.getMembersByName(name, ignoreCase).stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public List<SafeMember> getMembersByNickname(String name, boolean ignoreCase) {
        return object.getMembersByNickname(name, ignoreCase).stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public List<SafeMember> getMembersByEffectiveName(String name, boolean ignoreCase) {
        return object.getMembersByEffectiveName(name, ignoreCase).stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public String getIconUrl() {
        return object.getIconUrl();
    }
}
