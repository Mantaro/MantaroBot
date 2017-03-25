package net.kodehawa.mantarobot.data.entities;

import lombok.Data;
import net.kodehawa.mantarobot.data.db.ManagedObject;

@Data
public class MantaroObj implements ManagedObject {
	public static final String DB_TABLE = "mantaro";
	public final String id = "mantaro";

	@Override
	public void delete() {

	}

	@Override
	public void save() {

	}

	/* TODO Stuff to Add
	Blacklists (User and Guilds)
	 */
}
