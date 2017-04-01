package net.kodehawa.dataport;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OldPlayerData {
	public int health = 250;
	public Map<Integer, Integer> inventory = new HashMap<>();
	public long money = 0;
	public int reputation = 0;
	public int stamina = 100;
	public UUID uniqueId;
	private transient boolean processing;
}
