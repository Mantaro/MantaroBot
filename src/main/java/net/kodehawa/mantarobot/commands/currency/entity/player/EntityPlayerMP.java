package net.kodehawa.mantarobot.commands.currency.entity.player;

import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.Objects;

/**
 * Global {@link net.kodehawa.mantarobot.commands.currency.entity.Entity} wrapper.
 * This contains all the functions necessary to make the Player interact with the TextChannelGround (World).
 *
 * When returned, it will return a {@link java.lang.String}  representation of all the objects here.
 *
 * The user will see a representation of this if the guild is in local mode, else it will be a representation of EntityPlayerMP
 *
 * @see net.kodehawa.mantarobot.commands.currency.entity.Entity
 * @see EntityPlayer
 * @author Kodehawa
 */
public class EntityPlayerMP extends EntityPlayer {
	/**
	 * The birthday date for the specified user. It's *always* global.
	 */
	public String birthdayDate = null;

	/**
	 * (INTERNAL)
	 * Gets the specified EntityPlayer which needs to be seeked.
	 * @param e The user to seek for.
	 * @return The EntityPlayer instance.
	 */
	public static EntityPlayerMP getPlayer(User e){
		Objects.requireNonNull(e,  "Player user cannot be null!");
		return MantaroData.getData().get().getUser(e, true);
	}


	/**
	 * Sets this {@link EntityPlayer} birthday date.
	 * @param birthdayDate The date to set.
	 */
	public EntityPlayerMP setBirthdayDate(String birthdayDate) {
		this.birthdayDate = birthdayDate;
		return this;
	}

	/**
	 * Gets this {@link EntityPlayer} birthday date.
	 */
	public String getBirthdayDate() {
		return birthdayDate;
	}
}
