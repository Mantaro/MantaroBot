package net.kodehawa.mantarobot.commands.currency.entity.player;

/**
 * Global {@link net.kodehawa.mantarobot.commands.currency.entity.Entity} wrapper.
 * This contains all the functions necessary to make the Player interact with the TextChannelGround (World).
 * <p>
 * When returned, it will return a {@link java.lang.String}  representation of all the objects here.
 * <p>
 * The user will see a representation of this if the guild is in local mode, else it will be a representation of EntityPlayerMP
 *
 * @author Kodehawa
 * @see net.kodehawa.mantarobot.commands.currency.entity.Entity
 * @see net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayer
 */
public class EntityPlayerMP extends EntityPlayer {
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
	public void setBirthdayDate(String birthdayDate) {
		this.birthdayDate = birthdayDate;
	}
}
