package net.kodehawa.mantarobot.db.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.dv8tion.jda.core.entities.Message;
import net.kodehawa.mantarobot.db.ManagedObject;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
@ToString
@RequiredArgsConstructor
public class QuotedMessage implements ManagedObject {
    public static final String DB_TABLE = "quotes";
    private final String channelId;
    private final String channelName;
    private final String content;
    private final String guildId;
    private final String guildName;
    private final String id;
    private final String userAvatar;
    private final String userId;
    private final String userName;

    public QuotedMessage(Message message) {
        this.id = message.getId();

        this.content = message.getRawContent();

        this.userId = message.getAuthor().getId();
        this.userAvatar = message.getAuthor().getEffectiveAvatarUrl();
        this.userName = message.getGuild().getMember(message.getAuthor()).getEffectiveName();

        this.guildId = message.getGuild().getId();
        this.guildName = message.getGuild().getName();

        this.channelId = message.getTextChannel().getId();
        this.channelName = message.getTextChannel().getName();
    }

    @Override
    public void delete() {
        r.table(DB_TABLE).get(getId()).delete().runNoReply(conn());
    }

    @Override
    public void save() {
        r.table(DB_TABLE).insert(this)
                .optArg("conflict", "replace")
                .runNoReply(conn());
    }
}
