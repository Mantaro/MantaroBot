package net.kodehawa.mantarobot.core;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import org.reflections.Reflections;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.kodehawa.mantarobot.cmd.Anime;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.cmd.management.Loader;
import net.kodehawa.mantarobot.cmd.parser.Parser;
import net.kodehawa.mantarobot.core.listener.Listener;
import net.kodehawa.mantarobot.util.LogType;
import net.kodehawa.mantarobot.util.Logger;
import net.kodehawa.mantarobot.util.StringArrayFile;
/**
 * Mantaro's main class. Handles reflections, login procedure, etc.
 * This bot is fully modifiable and modular. You can add your own commands to this bot WITHOUT touching the main command package (see {@link setModPath(boolean isModded, String modPackagePath)})
 * This is a rewritten version from the previous 0.9 codebase, but with a lot of its old legacy remaining.
 * I focused on improving code readability and speed, and also in making command loading code fully modular and automatic, without the need of me adding the command to the HashMap manually.
 * This bot is using JDA 3.0 as the backbone and link to the Discord API.
 * I didn't want to reinvent the wheel, so a lot of this bot uses external APIs unless I didn't find them or didn't find them suitable.
 * This will be marked as a release, but I feel I can do more and better.
 * @author Yomura
 * @since 24/11/2016
 */
public class Mantaro {
	
	//Am I debugging this?
	public boolean isDebugEnabled = false;
	
	//Who are mantaining this?
	public static String OWNER_ID = "155867458203287552";
	public static String SERVER_MGR_ID = "155035543984537600";
	
	//Mod parameters.
	private boolean externalClassRequired = false;
	private String externalClasspath = "";
	
	//New instances.
	private static volatile Mantaro instance = new Mantaro();
	private Parser parser = new Parser();
	
	String prefix = "~>";
	
	//JDA and Loader. We need this and they're extremely important.
	JDA jda;
	Loader loader;
	
	public HashMap<String, Command> commands = new HashMap<String, Command>(); //A HashMap of commands, with the key being the command name and the result being the Class extending Command.
	public Set<Class<? extends Command>> classes = null; //A Set of classes, which will be later on loaded on Loader.
	

	//Gets in what OS the bot is running. Useful because my machine is running Windows 10, but the server is running Linux.
	private String OS = System.getProperty("os.name").toLowerCase();
	
	private static Game game = Game.of("It's not a bug, it's a feature!");
	
	//Bot data. Will be used in About command.
	//In that command it returns it as data[0] + data[1]
	public String[] data = {"26112016", "1.0.0a4-1312"};
	
	public Mantaro()
	{
		//Adds all the Classes extending Command to the classes HashMap. They will be later loaded in Loader.
		Reflections reflections = new Reflections("net.kodehawa.mantarobot.cmd");
		classes = reflections.getSubTypesOf(Command.class);
		if(externalClassRequired){
			Reflections extReflections = new Reflections(externalClasspath);
			classes.addAll(extReflections.getSubTypesOf(Command.class));
		}
		
	}
	
	public static void main(String[] args){
		
		Logger.instance().print("MantaroBot starting...", LogType.INFO);
		Logger.instance().print("Starting with Java args:", LogType.INFO);
		for (String s: args) {
			System.out.println(s.replace(":", " = "));
	     }

		
		String botToken = "";

		//Parses and assigns the JVM arguments.
		int i;
		for(i = 0; i < args.length; i++)
		{
		    if(args[i].startsWith("debug")){
		    	instance().isDebugEnabled = Boolean.parseBoolean(args[i].split(":")[1]); 
		    }
		    
		    else if(args[i].startsWith("token")){
		    	botToken = args[i].split(":")[1];
		    }
		    
		    else if(args[i].startsWith("anilist")){
		    	Anime.CLIENT_SECRET = args[i].split(":")[1];
		    }
		}
		
		try
		{
			//Builds a bot and a bot listener to use.
			instance().jda = new JDABuilder(AccountType.BOT).addListener(new Listener()).setToken(botToken)
					.buildBlocking(); //For some reason buildAsync constantly disconnects me.
			instance().jda.setAutoReconnect(true);
			instance().jda.getPresence().setGame(game);
			Logger.instance().print("Started MantaroBot JDA instance on JDA " + JDAInfo.VERSION, LogType.INFO);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//Automatically loads the commands.
		instance().loader = new Loader();
		
		//Random status changer.
		CopyOnWriteArrayList<String> splash = new CopyOnWriteArrayList<String>();
		new StringArrayFile("splash", splash , false);
		TimerTask timerTask = new TimerTask() 
	    {
	         public void run()  
	         { 
	        	 Random r = new Random();
	        	 int i = r.nextInt(splash.size());
	        	 if(!(i == splash.size()))
	        	 {
	     			instance().jda.getPresence().setGame(Game.of(splash.get(i)));
	        	 }
	         } 
	     }; 
		 Timer timer = new Timer(); 
		 timer.schedule(timerTask, 350000, 350000);
	}
	
	//What do do when a command is called?
	public void onCommand(Parser.Container cmd) throws InstantiationException, IllegalAccessException
	{		
		if(instance().commands.containsKey(cmd.invoke))
		{
			instance().commands.get(cmd.invoke).onCommand(cmd.args, cmd.content, cmd.event);
		}
	}

	public synchronized static Mantaro instance()
	{
		return instance;
	}
	
	public String getMetadata(String s)
	{
		int i = -1;
		if(s.equals("date")){ i = 0; }
		if(s.equals("build")){ i = 1; }
		
		return data[i];
	}
	
	public Parser getParser()
	{
		return parser;
	}
	
	public JDA getSelf()
	{
		return jda;
	}
	
    public boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
    }
    
    public String getPrefix()
    {
    	return prefix;
    }
    
    public void setModPath(boolean isModded, String modPackagePath)
    {
    	this.externalClassRequired = isModded;
    	this.externalClasspath = modPackagePath;
    }
    
    protected boolean getModded()
    {
    	return this.externalClassRequired;
    }
    
    protected String getExternalPath()
    {
    	return this.externalClasspath;
    }
}
