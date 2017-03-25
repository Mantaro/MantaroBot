package net.kodehawa.dataport;

import net.kodehawa.mantarobot.commands.utils.data.BugData;
import net.kodehawa.mantarobot.commands.utils.data.QuotesData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.db.ManagedDatabase;
import net.kodehawa.mantarobot.data.entities.CustomCommand;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.DBUser;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.data.entities.helpers.Inventory.Resolver;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

//TODO COMPLETE DATA PORTER
public class MantaroDataPorter {
	public static void main(String[] args) throws Exception {
		System.out.println("MantaroDataPorter by AdrianTodt");

		ManagedDatabase db = MantaroData.db();

		System.out.println("\nLoading files...");
		Data data = new GsonDataManager<>(Data.class, "data.json", Data::new).get();
		QuotesData quotesData = new GsonDataManager<>(QuotesData.class, "quotes.json", QuotesData::new).get();
		BugData bugData = new GsonDataManager<>(BugData.class, "bugs.json", BugData::new).get();

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
			dbGuildData.rawgetGuildUnsafeChannels().addAll(guildData.unsafeChannels);
			dbGuildData.setMusicChannel(guildData.musicChannel);
			dbGuildData.setGuildLogChannel(guildData.logChannel);
			dbGuildData.setMusicQueueSizeLimit(guildData.queueSizeLimit == null ? null : guildData.queueSizeLimit.longValue());
			dbGuildData.setMusicSongDurationLimit(guildData.songDurationLimit == null ? null : guildData.songDurationLimit.longValue());

			guildData.users.forEach((pid, localPlayerData) -> {
				Player p = new Player(pid + ":" + id, localPlayerData.health, localPlayerData.money, localPlayerData.reputation, localPlayerData.stamina, "");
				p.inventory().replaceWith(Resolver.unserialize(localPlayerData.inventory));
			});

			guildData.customCommands.forEach((name, answers) -> {
				CustomCommand.of(id, name, answers).saveAsync();
			});

			dbGuild.saveAsync();
		});

		data.users.forEach((id, playerData) -> {
			DBUser user = DBUser.of(id);
			user.getData().setBirthday(playerData.birthdayDate);

			user.saveAsync();

			Player p = new Player(id + ":g", playerData.health, playerData.money, playerData.reputation, playerData.stamina, "");
			p.inventory().replaceWith(Resolver.unserialize(playerData.inventory));

			p.saveAsync();
		});
	}
}
