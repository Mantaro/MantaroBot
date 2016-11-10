package net.kodehawa.discord.Mantaro.bot;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.reflections.Reflections;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.kodehawa.discord.Mantaro.listeners.Listener;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.main.Parser;
import net.kodehawa.discord.Mantaro.manager.CommandManager;
import net.kodehawa.discord.Mantaro.utils.LogType;
import net.kodehawa.discord.Mantaro.utils.Logger;
import net.kodehawa.discord.Mantaro.utils.Values;

/**
 * Simple bot for Discord client.
 * @author Yomura
 * @version 0.7
 * @since 14/08/2016
 */
public class MantaroBot {
	
	//Instance of the JDA API.
	private JDA jda;
	
	//Command parser. Basically, what formats the commands so I can use them.
	private Parser parser = new Parser();
	
	private static MantaroBot instance = new MantaroBot();
	
	//Command list. From here, everything gets called, like for example Command.botAction();
	public HashMap<String, Command> mentionCommandList = new HashMap<String, Command>();
	public HashMap<String, Command> commandList = new HashMap<String, Command>();
	
	private final String gameStatus = "Lewd.";
	public final String botPrefix = "~>";
	private final String[] meta = {"8th of November 2016", "0.98a", "Kodehawa"};
	
	//Which OS is the bot running on?
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	public Set<Class<? extends Command>> classes = null;
	
	public boolean debugMode;
	
	public MantaroBot(){
		Reflections reflections = new Reflections("net.kodehawa.discord.Mantaro.commands");
		classes = reflections.getSubTypesOf(Command.class);
	}
	
	/**
	 * Don't kill me, this is the nicest I could code this.
	 * @param args
	 */	
	public static void main(String[] args)
	{
		String botToken = "";

		int i;
		for(i = 0; i < args.length; i++)
		{
		    if(args[i].startsWith("debug")){ getInstance().debugMode = Boolean.parseBoolean(args[i].split(":")[1]); }
		    else if(args[i].startsWith("token")){ botToken = args[i].split(":")[1]; }
		}
						
		if(getInstance().debugMode)
		{
			Logger.instance().print("Starting with Java args:", LogType.INFO);
			for (String s: args) {
				System.out.println(s.replace(":", " = "));
		     }
			
			System.out.println("Date: " + getInstance().meta[0] + " | Version: " + getInstance().meta[1] + " | Creator: " + getInstance().meta[2]);
		}

		Logger.instance().print("MantaroBot starting...", LogType.INFO);
					
		try
		{
			getInstance().jda = new JDABuilder().addListener(new Listener()).setBotToken(botToken).buildBlocking();
			getInstance().jda.setAutoReconnect(true);
			getInstance().jda.getAccountManager().setGame(getInstance().gameStatus);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		new Values();
		
		try {
			getInstance().addCommands();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			Logger.instance().print("Something very bad happened while loading commands! Check stacktrace.", LogType.CRITICAL);
		}
		
	}
	
	private void addCommands() throws InterruptedException, ExecutionException
	{
		  CommandManager cmd = new CommandManager();
	      cmd.start();
	}
	
	/**
	 * Sends the instruction when a command happens.
	 * @param cmd
	 */
	public static void onCommand(Parser.CommandContainer cmd)
	{		
		if(!Listener.isMenction)
		{
			if(getInstance().commandList.containsKey(cmd.invoke))
			{
				boolean enabled = getInstance().commandList.get(cmd.invoke).isAvaliable(cmd.args, cmd.event);
				if(enabled)
				{
					getInstance().commandList.get(cmd.invoke).botAction(cmd.args, cmd.rawCommand, cmd.beheaded1, cmd.event);
				}
			}
		}
		else
		{
			if(getInstance().mentionCommandList.containsKey(cmd.invoke))
			{
				boolean enabled = getInstance().mentionCommandList.get(cmd.invoke).isAvaliable(cmd.args, cmd.event);
				if(enabled)
				{
					getInstance().mentionCommandList.get(cmd.invoke).botAction(cmd.args, cmd.rawCommand, cmd.beheaded1, cmd.event);
				}
			}
		}
	}
	
	/**
	 * Calls the bot instance.
	 * @return this class instance.
	 */
	public static MantaroBot getInstance()
	{
		return instance;
	}
	
	public Parser getParser()
	{
		return parser;
	}
	
	public JDA getSelf()
	{
		return jda;
	}
	
	public String getBuildDate()
	{
		return meta[0];
	}
	
	public String getBotPrefix()
	{
		return botPrefix;
	}
	
	public String getBuild()
	{
		return meta[1];
	}
	
	/**
	 * Do I need to explain it?
	 * @return is windows or no?
	 */
    public boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
    }
}
