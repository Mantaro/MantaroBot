package net.kodehawa.mantarobot.commands.rpg.entity.world;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.Entity;
import net.kodehawa.mantarobot.commands.rpg.entity.EntityTickable;
import net.kodehawa.mantarobot.commands.rpg.inventory.Inventory;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;

public class EntityTree extends EntityTickable {

	private int health = 100;
	private int stamina = 0;


	@Override
	public void tick(TextChannelWorld world, GuildMessageReceivedEvent event) {
		onDeath();
	}

	@Override
	public void setHealth(int amount) {
		health = amount;
	}

	@Override
	public void setStamina(int amount) {
		stamina = amount;
	}

	@Override
	public TextChannelWorld getWorld() {
		return null;
	}

	@Override
	public int getHealth() {
		return health;
	}

	@Override
	public Inventory getInventory() {
		return null;
	}

	@Override
	public int getMaxHealth() {
		return 100;
	}

	@Override
	public int getMaxStamina() {
		return 0;
	}

	@Override
	public int getStamina() {
		return stamina;
	}

	@Override
	public void behaviour(TextChannelWorld world) {

	}

	@Override
	public Coordinates getCoordinates() {
		return null;
	}

	@Override
	public void setCoordinates(Coordinates coordinates) {

	}

	public void onSpawn(){
		if(getWorld().getActiveEntities().stream().filter(entity -> (entity instanceof EntityTree)).count() > 20) return;
		getWorld().addEntity(this);
		System.out.println(this + " spawned on " + getWorld());
	}

	public void onDeath(){
		if(health == 0){
			getWorld().removeEntity(this);
			System.out.println(this + " died " + getWorld());
		}
	}

	@Override
	public Type getType() {
		return Type.SPECIAL;
	}
}
