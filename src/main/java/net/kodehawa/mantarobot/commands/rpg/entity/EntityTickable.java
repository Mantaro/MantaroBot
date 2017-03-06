package net.kodehawa.mantarobot.commands.rpg.entity;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;

public abstract class EntityTickable implements Entity {

    public abstract void tick(TextChannelWorld world, GuildMessageReceivedEvent event);

    public boolean check(GuildMessageReceivedEvent event){
        return getWorld() == TextChannelWorld.of(event.getChannel());
    }

    public void add(TextChannelWorld world){
        if(!world.getActiveEntities().contains(this)){
            world.addEntity(this);
        }
    }
}
