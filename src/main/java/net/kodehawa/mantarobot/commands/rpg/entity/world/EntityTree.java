package net.kodehawa.mantarobot.commands.rpg.entity.world;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.EntityTickable;
import net.kodehawa.mantarobot.commands.rpg.inventory.Inventory;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;

import java.util.Random;

public class EntityTree extends EntityTickable {

	private int health = 100;
	private int stamina = 0;
	private Coordinates coordinates;
	private TextChannelWorld world;

	@Override
	public void tick(TextChannelWorld world, GuildMessageReceivedEvent event) {
		onSpawn(world);
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
		return world;
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
	public void behaviour(TextChannelWorld world) {}

	@Override
	public Coordinates getCoordinates() {
		return coordinates;
	}

	@Override
	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}

	public void onSpawn(TextChannelWorld world){
		this.world = world;
		if(getWorld().getActiveEntities().stream().filter(entity -> (entity instanceof EntityTree)).count() >= 10){
			System.out.println("Entity cap reached.");
			return;
		}
		Random random = new Random();
		int base = random.nextInt(350);
		this.setCoordinates(new Coordinates(Math.abs(base - random.nextInt(200)), 0, Math.abs(base - random.nextInt(30)), getWorld()));
		getWorld().addEntity(this);
		System.out.println(this + " spawned on " + getWorld());
	}

	public void onDeath(){
		if(health == 0){
			getWorld().removeEntity(this);
			System.out.println(this + " died on " + getWorld());
		}
	}

	@Override
	public Type getType() {
		return Type.SPECIAL;
	}

	public String toString(){
		return String.format("Tree({health: %s, coordinates: %s, world: %s})", getHealth(), getCoordinates(), getWorld());
	}
}
