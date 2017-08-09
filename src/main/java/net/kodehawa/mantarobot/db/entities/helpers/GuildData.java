package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;
import net.kodehawa.mantarobot.modules.commands.base.Category;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GuildData {
    private boolean antiSpam = false;
    private HashMap<String, String> autoroles = new HashMap<>();
    private String birthdayChannel = null;
    private String birthdayRole = null;
    private Long cases = 0L;
    private HashMap<String, List<Category>> channelSpecificDisabledCategories = new HashMap<>();
    private HashMap<String, List<String>> channelSpecificDisabledCommands = new HashMap<>();
    private boolean customAdminLock = false;
    private Set<Category> disabledCategories = new HashSet<>();
    private Set<String> disabledChannels = new HashSet<>();
    private Set<String> disabledCommands = new HashSet<>();
    private Set<String> disabledRoles = new HashSet<>();
    private Set<String> disabledUsers = new HashSet<>();
    private String guildAutoRole = null;
    private String guildCustomPrefix = null;
    private String guildLogChannel = null;
    private Set<String> guildUnsafeChannels = new HashSet<>();
    private String joinMessage = null;
    private String leaveMessage = null;
    private boolean linkProtection = false;
    private Set<String> linkProtectionAllowedChannels = new HashSet<>();
    private Set<String> logExcludedChannels = new HashSet<>();
    private String logJoinLeaveChannel = null;
    private int maxFairQueue = 4;
    private int maxResultsSearch = 5;
    private Set<String> modlogBlacklistedPeople = new HashSet<>();
    private boolean musicAnnounce = true;
    private String musicChannel = null;
    private Long musicQueueSizeLimit = null;
    private Long musicSongDurationLimit = null;
    private String mutedRole = null;
    private ConcurrentHashMap<Long, Long> mutedTimelyUsers = new ConcurrentHashMap<>();
    private boolean noMentionsAction = false;
    private Long quoteLastId = 0L;
    private long ranPolls = 0L;
    private boolean reactionMenus = true;
    private Set<String> rolesBlockedFromCommands = new HashSet<>();
    private boolean rpgDevaluation = true;
    private boolean rpgLocalMode = false;
    private long setModTimeout = 0L;
    private boolean slowMode = false;
    private Set<String> slowModeChannels = new HashSet<>();
    private Set<String> spamModeChannels = new HashSet<>();
    private int timeDisplay = 0; //0 = 24h, 1 = 12h
    private String premiumKey;

    public void write(Output out) {
        out.writeBoolean(antiSpam);
        out.writeMap(autoroles, Output::writeUTF, Output::writeUTF);
        out.writeUTF(birthdayChannel, true);
        out.writeUTF(birthdayRole, true);
        out.writeLong(cases);
        out.writeMap(channelSpecificDisabledCategories, Output::writeUTF, (out2, list) ->
                out2.writeCollection(list, (out3, c) -> out3.writeUTF(c.name()))
        );
        out.writeMap(channelSpecificDisabledCommands, Output::writeUTF, (out2, list) ->
                out2.writeCollection(list, Output::writeUTF)
        );
        out.writeBoolean(customAdminLock);
        out.writeCollection(disabledCategories, (out2, category) -> out2.writeUTF(category.name()));
        out.writeCollection(disabledChannels, Output::writeUTF);
        out.writeCollection(disabledCommands, Output::writeUTF);
        out.writeCollection(disabledRoles, Output::writeUTF);
        out.writeCollection(disabledUsers, Output::writeUTF);
        out.writeUTF(guildAutoRole, true);
        out.writeUTF(guildCustomPrefix, true);
        out.writeUTF(guildLogChannel, true);
        out.writeCollection(guildUnsafeChannels, Output::writeUTF);
        out.writeUTF(joinMessage, true);
        out.writeUTF(leaveMessage, true);
        out.writeBoolean(linkProtection);
        out.writeCollection(linkProtectionAllowedChannels, Output::writeUTF);
        out.writeCollection(logExcludedChannels, Output::writeUTF);
        out.writeUTF(logJoinLeaveChannel, true);
        out.writeInt(maxFairQueue);
        out.writeInt(maxResultsSearch);
        out.writeCollection(modlogBlacklistedPeople, Output::writeUTF);
        out.writeBoolean(musicAnnounce);
        out.writeUTF(musicChannel, true);
        out.writeLong(musicQueueSizeLimit, true);
        out.writeLong(musicSongDurationLimit, true);
        out.writeUTF(mutedRole, true);
        out.writeMap(mutedTimelyUsers, Output::writeLong, Output::writeLong);
        out.writeBoolean(noMentionsAction);
        out.writeLong(quoteLastId);
        out.writeLong(ranPolls);
        out.writeBoolean(reactionMenus);
        out.writeCollection(rolesBlockedFromCommands, Output::writeUTF);
        out.writeBoolean(rpgDevaluation);
        out.writeBoolean(rpgLocalMode);
        out.writeLong(setModTimeout);
        out.writeBoolean(slowMode);
        out.writeCollection(slowModeChannels, Output::writeUTF);
        out.writeCollection(spamModeChannels, Output::writeUTF);
        out.writeInt(timeDisplay);
        out.writeUTF(premiumKey, true);
    }

    public void read(Input in) {
        antiSpam = in.readBoolean();
        autoroles = in.readMap(Input::readUTF, Input::readUTF);
        birthdayChannel = in.readUTF(true);
        birthdayRole = in.readUTF(true);
        cases = in.readLong();
        channelSpecificDisabledCategories = in.readMap(Input::readUTF, in2 ->
                in2.readList(in3 -> Category.valueOf(in3.readUTF()))
        );
        channelSpecificDisabledCommands = in.readMap(Input::readUTF, (in2) ->
                in2.readList(Input::readUTF)
        );
        customAdminLock = in.readBoolean();
        disabledCategories = in.readSet(in2 -> Category.valueOf(in2.readUTF()));
        disabledChannels = in.readSet(Input::readUTF);
        disabledCommands = in.readSet(Input::readUTF);
        disabledRoles = in.readSet(Input::readUTF);
        disabledUsers = in.readSet(Input::readUTF);
        guildAutoRole = in.readUTF(true);
        guildCustomPrefix = in.readUTF(true);
        guildLogChannel = in.readUTF(true);
        guildUnsafeChannels = in.readSet(Input::readUTF);
        joinMessage = in.readUTF(true);
        leaveMessage = in.readUTF(true);
        linkProtection = in.readBoolean();
        linkProtectionAllowedChannels = in.readSet(Input::readUTF);
        logExcludedChannels = in.readSet(Input::readUTF);
        logJoinLeaveChannel = in.readUTF(true);
        maxFairQueue = in.readInt();
        maxResultsSearch = in.readInt();
        modlogBlacklistedPeople = in.readSet(Input::readUTF);
        musicAnnounce = in.readBoolean();
        musicChannel = in.readUTF(true);
        musicQueueSizeLimit = in.readLong(true);
        musicSongDurationLimit = in.readLong(true);
        mutedRole = in.readUTF(true);
        mutedTimelyUsers = in.readMap(ConcurrentHashMap::new, Input::readLong, Input::readLong);
        noMentionsAction = in.readBoolean();
        quoteLastId = in.readLong();
        ranPolls = in.readLong();
        reactionMenus = in.readBoolean();
        rolesBlockedFromCommands = in.readSet(Input::readUTF);
        rpgDevaluation = in.readBoolean();
        rpgLocalMode = in.readBoolean();
        setModTimeout = in.readLong();
        slowMode = in.readBoolean();
        slowModeChannels = in.readSet(Input::readUTF);
        spamModeChannels = in.readSet(Input::readUTF);
        timeDisplay = in.readInt();
        premiumKey = in.readUTF(true);
    }
}
