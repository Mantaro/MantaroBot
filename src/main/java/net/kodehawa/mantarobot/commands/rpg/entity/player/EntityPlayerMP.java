package net.kodehawa.mantarobot.commands.rpg.entity.player;

import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.data.MantaroData;

/**
 * Global {@link net.kodehawa.mantarobot.commands.rpg.entity.Entity} wrapper.
 * This contains all the functions necessary to make the Player interact with the TextChannelGround (World).
 * <p>
 * When returned, it will return a {@link java.lang.String}  representation of all the objects here.
 * <p>
 * The user will see a representation of this if the guild isn't in local mode, else it will be a representation of EntityPlayer
 *
 * @author Kodehawa
 * @see net.kodehawa.mantarobot.commands.rpg.entity.Entity
 * @see EntityPlayer
 */
public class EntityPlayerMP extends EntityPlayer {
	/**
	 * (INTERNAL)
	 * Gets the specified EntityPlayer which needs to be seeked.
	 *
	 * @param e The user to seek for.
	 * @return The EntityPlayer instance.
	 */
	public static EntityPlayerMP getPlayer(User e) {
		if (e == null) {
			return null;
		}
		return MantaroData.getData().get().getUser(e, true);
	}

	/**
	 * The birthday date for the specified user. It's *always* global.
	 */
	public String birthdayDate = null;

	/**
	 * Gets this {@link EntityPlayer} birthday date.
	 */
	public String getBirthdayDate() {
		return birthdayDate;
	}

	/**
	 * Sets this {@link EntityPlayer} birthday date.
	 *
	 * @param birthdayDate The date to set.
	 */
	public EntityPlayerMP setBirthdayDate(String birthdayDate) {
		this.birthdayDate = birthdayDate;
		return this;
	}
}
