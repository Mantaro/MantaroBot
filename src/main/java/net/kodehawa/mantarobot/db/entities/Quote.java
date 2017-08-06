package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;

import java.beans.ConstructorProperties;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.cache;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
public class Quote implements ManagedObject {
    public static final String DB_TABLE = "quotes";
    private String channelId;
    private String channelName;
    private String content;
    private String guildName;
    private String id;
    private String userAvatar;
    private String userId;
    private String userName;

    @ConstructorProperties({"id", "userId", "channelId", "content", "guildName", "userName", "userAvatar", "channelName"})
    public Quote(String id, String userId, String channelId, String content, String guildName, String userName, String userAvatar, String channelName) {
        this.id = id;
        this.userId = userId;
        this.channelId = channelId;
        this.content = content;
        this.guildName = guildName;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.channelName = channelName;
    }

    @JsonIgnore
    public Quote() {

    }

    public static Quote of(Member member, TextChannel channel, Message message) {
        return new Quote(
                member.getGuild().getId() + ":",
                member.getUser().getId(),
                channel.getId(),
                message.getRawContent(),
                member.getGuild().getName(),
                member.getEffectiveName(),
                member.getUser().getEffectiveAvatarUrl(),
                channel.getName()
        );
    }

    public static Quote of(GuildMessageReceivedEvent event) {
        return of(event.getMember(), event.getChannel(), event.getMessage());
    }

    @Override
    public void delete() {
        r.table(DB_TABLE).get(id).delete().runNoReply(conn());
        cache().invalidate(id);
    }

    @Override
    public void save() {
        cache().set(id, this);
        r.table(DB_TABLE).insert(this)
                .optArg("conflict", "replace")
                .runNoReply(conn());
    }

    @Override
    public void write(Output out) {
        out.writeLong(Long.parseLong(id.substring(0, id.length() - 1)));
        out.writeLong(Long.parseLong(channelId));
        out.writeLong(Long.parseLong(userId));
        out.writeUTF(userName);
        out.writeUTF(userAvatar);
        out.writeUTF(guildName);
        out.writeUTF(channelName);
        out.writeUTF(content);
    }

    @Override
    public void read(Input in) {
        id = in.readLong() + ":";
        channelId = String.valueOf(in.readLong());
        userId = String.valueOf(in.readLong());
        userName = in.readUTF();
        userAvatar = in.readUTF();
        guildName = in.readUTF();
        channelId = in.readUTF();
        content = in.readUTF();
    }

    @JsonIgnore
    public String getGuildId() {
        return getId().split(":")[0];
    }

    @JsonIgnore
    public String getQuoteId() {
        return getId().split(":")[1];
    }

}
