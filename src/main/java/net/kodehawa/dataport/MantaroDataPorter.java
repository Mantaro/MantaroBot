package net.kodehawa.dataport;

import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.db.ManagedDatabase;
import net.kodehawa.mantarobot.data.entities.CustomCommand;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.DBUser;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

public class MantaroDataPorter {
	public static void main(String[] args) throws Exception {
		System.out.println("MantaroDataPorter by AdrianTodt");

		ManagedDatabase db = MantaroData.db();

		System.out.println("\nLoading files...");
		Data data = new GsonDataManager<>(Data.class, "data.json", Data::new).get();

		System.out.println("\nPorting Data.JSON...");
		data.guilds.forEach((id, guildData) -> {
			DBGuild dbGuild = DBGuild.of(id);
			GuildData dbGuildData = dbGuild.getData();
			dbGuildData.setBirthdayChannel(guildData.birthdayChannel);
			dbGuildData.setBirthdayRole(guildData.birthdayRole);
			dbGuildData.setCustomAdminLock(guildData.customCommandsAdminOnly);
			dbGuildData.setGuildAutoRole(guildData.autoRole);
			dbGuildData.setGuildCustomPrefix(guildData.prefix);
			dbGuildData.setRpgDevaluation(guildData.devaluation);
			dbGuildData.setRpgLocalMode(guildData.localMode);
			dbGuildData.setGuildUnsafeChannels(guildData.unsafeChannels);
			dbGuildData.setMusicChannel(guildData.musicChannel);
			dbGuildData.setGuildLogChannel(guildData.logChannel);
			dbGuildData.setMusicQueueSizeLimit(guildData.queueSizeLimit == null ? null : guildData.queueSizeLimit.longValue());
			dbGuildData.setMusicSongDurationLimit(guildData.songDurationLimit == null ? null : guildData.songDurationLimit.longValue());

			guildData.customCommands.forEach((name, answers) -> {
				CustomCommand.of(id, name, answers).saveAsync();
			});

			dbGuild.saveAsync();
		});

		data.users.forEach((id, playerData) -> {
			DBUser user = DBUser.of(id);
			user.getData().setBirthday(playerData.birthdayDate);

			user.saveAsync();
		});
	}
}
