package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.entities.Game;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.State;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.thread.AsyncHelper;
import net.kodehawa.mantarobot.util.StringArrayUtils;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Threaded automatic command loader.
 * This class handles everything related to loading commands in the following order:
 * -> Loops through the classes in the {@link net.kodehawa.mantarobot.cmd} package and its subpackages which extends the {@link net.kodehawa.mantarobot.management.Command} class.
 * -> Initializes the instance of the command.
 * -> Puts the command in a {@link java.util.HashMap} containing the command name (cmd.getName()) and the command instance itself.
 * <p>
 * It is completely automated and needs no input from the programmer when a class which extends {@link net.kodehawa.mantarobot.management.Command} is added to the classpath.
 * <p>
 * Under the hood the  HashMap is using the Reflections library, which puts all the classes on the specified package which
 * extends or implements the specified method, but without actually loading them.
 * <p>
 * Seen in Mantaro.class line 73 to 77.
 * <p>
 * This is a task which benefits from multiple cores and tries to be done as quick as possible.
 *
 * @author Yomura
 * @see org.reflections.Reflections
 * @since 24/11/2016
 */
public final class Loader {

	public Loader() {
		Runnable loaderthr = () ->
		{
			for (Class<? extends Module> c : Mantaro.instance().classes) {
				try {
					c.newInstance();
				} catch (InstantiationException e) {
					Log.instance().print("Cannot initialize a command", this.getClass(), Type.CRITICAL, e);
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					Log.instance().print("Cannot access a command class!", this.getClass(), Type.CRITICAL, e);
					e.printStackTrace();
				}
			}
			Mantaro.instance().classes.clear();
			Mantaro.instance().setStatus(State.POSTLOAD);
			Log.instance().print("Finished loading basic components. Status is now set to POSTLOAD", getClass(), Type.INFO);

			Log.instance().print("Loaded " + Module.modules.size() + " commands", this.getClass(), Type.INFO);

			//Random status changer.
			CopyOnWriteArrayList<String> splash = new CopyOnWriteArrayList<>();
			new StringArrayUtils("splash", splash, false);
			Runnable splashTask = () -> {
				Random r = new Random();
				int i = r.nextInt(splash.size());
				if (!(i == splash.size())) {
					Mantaro.instance().getSelf().getPresence().setGame(Game.of("~>help | " + splash.get(i)));
					Log.instance().print("Changed status to: " + splash.get(i), Type.INFO);
				}
			};
			AsyncHelper.instance().startAsyncTask("Splash Thread", splashTask, 600);
			Mantaro.instance().runScheduled();
		};
		new Thread(loaderthr).start();
	}
}
