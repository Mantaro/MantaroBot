package net.kodehawa.mantarobot.commands.currency.inventory;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.Expirator;

import java.util.*;

public class TextChannelGround {
	private static final Map<String, List<ItemStack>> DROPPED_ITEMS = new HashMap<>();
	private static final Expirator EXPIRATOR = new Expirator();
	private static Random r = new Random(System.currentTimeMillis());

	public static TextChannelGround of(String id) {
		return new TextChannelGround(DROPPED_ITEMS.computeIfAbsent(id, k -> new ArrayList<>()));
	}

	public static TextChannelGround of(TextChannel channel) {
		return of(channel.getId());
	}

	public static TextChannelGround of(GuildMessageReceivedEvent event) {
		return of(event.getChannel());
	}

	private final List<ItemStack> stacks;

	private TextChannelGround(List<ItemStack> stacks) {
		this.stacks = stacks;
	}

	public TextChannelGround drop(List<ItemStack> stacks) {
		List<ItemStack> finalStacks = new ArrayList<>(stacks);
		this.stacks.addAll(finalStacks);
		EXPIRATOR.letExpire(System.currentTimeMillis() + 120000, () -> {
			this.stacks.removeAll(finalStacks);
		});

		return this;
	}

	public TextChannelGround drop(ItemStack... stacks) {
		return drop(Arrays.asList(stacks));
	}

	public TextChannelGround drop(Item item) {
		return drop(new ItemStack(item, 1));
	}

	public TextChannelGround drop(int item) {
		return drop(Items.ALL[item]);
	}

	public boolean dropWithChance(Item item, int weight) {
		boolean doDrop = r.nextInt(weight) == 0;
		if (doDrop) drop(item);
		return doDrop;
	}

	public List<ItemStack> collect() {
		List<ItemStack> finalStacks = new ArrayList<>(stacks);
		stacks.clear();
		return finalStacks;
	}

	public boolean dropWithChance(int item, int weight) {
		return dropWithChance(Items.fromId(item), weight);
	}
}
