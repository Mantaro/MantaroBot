package net.kodehawa.mantarobot.commands.currency.entity.player;

/**
 * Global {@link net.kodehawa.mantarobot.commands.currency.entity.Entity} wrapper.
 * This contains all the functions necessary to make the Player interact with the TextChannelGround (World).
 *
 * When returned, it will return a {@link java.lang.String}  representation of all the objects here.
 *
 * The user will see a representation of this if the guild is in local mode, else it will be a representation of EntityPlayerMP
 *
 * @see net.kodehawa.mantarobot.commands.currency.entity.Entity
 * @see net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayer
 * @author Kodehawa
 */
public class EntityPlayerMP extends EntityPlayer {
	public String birthdayDate = null;
}
