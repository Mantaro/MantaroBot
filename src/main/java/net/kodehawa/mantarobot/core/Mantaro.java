package net.kodehawa.mantarobot.core;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.listeners.BirthdayListener;
import net.kodehawa.mantarobot.log.State;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.module.Loader;
import net.kodehawa.mantarobot.module.Module;
import org.reflections.Reflections;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.kodehawa.mantarobot.module.Parser;
import net.kodehawa.mantarobot.config.Config;
import net.kodehawa.mantarobot.listeners.Listener;
import net.kodehawa.mantarobot.listeners.LogListener;
import net.kodehawa.mantarobot.thread.AsyncHelper;
import net.kodehawa.mantarobot.thread.ThreadPoolHelper;
import net.kodehawa.mantarobot.util.StringArrayUtils;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Mantaro {
	
	//Am I debugging this?
	public boolean isDebugEnabled = false;
	private State status = State.PRELOAD;
	//Who is maintaining this?
	public final static String OWNER_ID = "155867458203287552";

	//New instances.
	private static volatile Mantaro instance = new Mantaro();
	private final Parser parser = new Parser();

    //JDA and Loader. We need this and they're extremely important.
	private JDA jda;
	private Config cl;
	
	public Set<Class<? extends Module>> classes = null; //A Set of classes, which will be later on loaded on Loader.

	//Gets in what OS the bot is running. Useful because my machine is running Windows 10, but the server is running Linux.
	private String OS = System.getProperty("os.name").toLowerCase();
	
	private static Game game = Game.of("It's not a bug, it's a feature!");
	
	//Bot data. Will be used in About command.
	//In that command it returns it as data[0] + data[1]. Will be displayed as 1.0.0a5-2102.26112016, for example.
	//The data after the dash is the hour (4 numbers) and the date.
	private final String[] data = {"24122016", "1.1.1a1-1217"};
	
	private Mantaro()
	{
		cl = Config.load();
		this.addClasses();
	}	
	
	public static void main(String[] args){
		Log.instance().print("MantaroBot starting...", Type.INFO);
		String botToken = instance().getConfig().values().get("token").toString();
		instance().isDebugEnabled = (Boolean)instance().getConfig().values().get("debug");
		
		try{
			instance().status = State.LOADING;
			instance().jda = new JDABuilder(AccountType.BOT)
					.setToken(botToken)
					.addListener(new Listener())
					.addListener(new LogListener())
					.addListener(new BirthdayListener())
					.setAutoReconnect(true)
					.setGame(game)
					.buildBlocking();
			Log.instance().print("Started bot instance.", Type.INFO);
		} catch(LoginException | InterruptedException | RateLimitedException e){
			e.printStackTrace();
			Log.instance().print("Cannot build JDA instance! This is normally very bad. Error: " + e.getCause(), Type.CRITICAL);
			Log.instance().print("Exiting program...", Type.CRITICAL);
			System.exit(-1);
		}
		
		new Loader();
		instance().status = State.LOADED;
		Log.instance().print("Started MantaroBot " + instance().data[1] + " on JDA " + JDAInfo.VERSION, Type.INFO);

		//Random status changer.
		CopyOnWriteArrayList<String> splash = new CopyOnWriteArrayList<>();
		new StringArrayUtils("splash", splash , false);
		Runnable splashTask = () -> {
			Random r = new Random();
       	 	int i = r.nextInt(splash.size());
       	 	if(!(i == splash.size()))
       	 	{
    			instance().jda.getPresence().setGame(Game.of("~>help | " + splash.get(i)));
				Log.instance().print("Changed status to: " + splash.get(i), Type.INFO);
			}
		};
		AsyncHelper.instance().startAsyncTask("Splash Thread", splashTask, 600);

		instance().status = State.POSTLOAD;
	}
	
	//What to do when a command is called?
	public void onCommand(Parser.Container cmd) {
		if(Module.modules.containsKey(cmd.invoke))
		{
			new Thread(() -> Module.modules.get(cmd.invoke).onCommand(cmd.args, cmd.content, cmd.event)).start();
		}
	}
	
	private void addClasses(){
		Runnable classThr = () -> {
			//Adds all the Classes extending Module to the classes HashMap. They will be later loaded in Loader.
			Reflections reflections = new Reflections("net.kodehawa.mantarobot.cmd");
			classes = reflections.getSubTypesOf(Module.class);
		};
		ThreadPoolHelper.instance().startThread("Load", classThr);
	}
	
	public synchronized static Mantaro instance(){
		return instance;
	}
	
	public String getMetadata(String s){
		int i = -1;
		if(s.equals("date")){ i = 0; }
		if(s.equals("build")){ i = 1; }
		
		return data[i];
	}
	
	public Parser getParser(){
		return parser;
	}
	
	public JDA getSelf(){
		return jda;
	}
	
	public boolean isWindows() {
        return (OS.contains("win"));
    }

    public boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix") );
    }
    
    public Config getConfig(){
    	return cl;
    }

    public State getState(){
    	return status;
	}
}