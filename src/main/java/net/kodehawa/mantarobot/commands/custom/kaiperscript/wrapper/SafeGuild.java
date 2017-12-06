package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.Region;
import net.dv8tion.jda.core.entities.Guild;

import java.util.List;
import java.util.stream.Collectors;

class SafeGuild extends SafeISnowflake<Guild> {
    private final SafeChannel channel;

    SafeGuild(Guild guild, SafeChannel channel) {
        super(guild);
        this.channel = channel;
    }

    public String getName() {
        return snowflake.getName();
    }

    public Guild.ExplicitContentLevel getExplicitContentLevel() {
        return snowflake.getExplicitContentLevel();
    }

    public Region getRegion() {
        return snowflake.getRegion();
    }

    public List<SafeChannel> getTextChannels() {
        return snowflake.getTextChannels().stream().map(c->c.getIdLong() == channel.getIdLong() ? channel : new SafeChannel(c, 0)).collect(Collectors.toList());
    }

    public List<SafeRole> getRoles() {
        return snowflake.getRoles().stream().map(SafeRole::new).collect(Collectors.toList());
    }

    public List<SafeMember> getMembers() {
        return snowflake.getMembers().stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public SafeMember getOwner() {
        return new SafeMember(snowflake.getOwner());
    }

    public List<SafeMember> getMembersByName(String name, boolean ignoreCase) {
        return snowflake.getMembersByName(name, ignoreCase).stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public List<SafeMember> getMembersByNickname(String name, boolean ignoreCase) {
        return snowflake.getMembersByNickname(name, ignoreCase).stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public List<SafeMember> getMembersByEffectiveName(String name, boolean ignoreCase) {
        return snowflake.getMembersByEffectiveName(name, ignoreCase).stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public String getIconUrl() {
        return snowflake.getIconUrl();
    }
}
