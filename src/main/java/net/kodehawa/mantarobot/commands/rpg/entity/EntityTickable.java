package net.kodehawa.mantarobot.commands.rpg.entity;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;
import net.kodehawa.mantarobot.core.listeners.OptimizedListener;

public abstract class EntityTickable extends OptimizedListener<GuildMessageReceivedEvent> implements Entity {

    public EntityTickable(){
        super(GuildMessageReceivedEvent.class);
    }

    public abstract void tick(TextChannelWorld world, GuildMessageReceivedEvent event);

    @Override
    public void event(GuildMessageReceivedEvent e){
        if(!(getWorld() == TextChannelWorld.of(e.getChannel()))) return;

        tick(getWorld(), e);
    }
}
