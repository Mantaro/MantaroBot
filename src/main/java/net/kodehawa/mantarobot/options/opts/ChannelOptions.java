package net.kodehawa.mantarobot.options.opts;

import com.google.common.eventbus.Subscribe;
import net.kodehawa.dataporter.oldentities.OldGuild;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.helpers.ExtraGuildData;
import net.kodehawa.mantarobot.options.OptionType;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

@Option
public class ChannelOptions extends OptionHandler {

    public ChannelOptions() {
        setType(OptionType.CHANNEL);
    }

    @Subscribe
    public void onRegister(OptionRegistryEvent e) {
        registerOption("nsfw:toggle", "NSFW toggle", "Toggles NSFW mode in the channel the command was ran at.", (event) -> {
            OldGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            ExtraGuildData guildData = dbGuild.getData();
            if(guildData.getGuildUnsafeChannels().contains(event.getChannel().getId())) {
                guildData.getGuildUnsafeChannels().remove(event.getChannel().getId());
                event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been disabled").queue();
                dbGuild.saveAsync();
                return;
            }

            guildData.getGuildUnsafeChannels().add(event.getChannel().getId());
            dbGuild.saveAsync();
            event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been enabled.").queue();
        });
    }

    @Override
    public String description() {
        return null;
    }
}
