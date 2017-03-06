package net.kodehawa.mantarobot.commands.rpg.entity;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;

public abstract class EntityTickable implements Entity {

    public abstract void tick(TextChannelWorld world, GuildMessageReceivedEvent event);

    public boolean check(GuildMessageReceivedEvent event){
        if(!(getWorld() == TextChannelWorld.of(event.getChannel()))) return false;

        tick(getWorld(), event);
        return true;
    }
}
