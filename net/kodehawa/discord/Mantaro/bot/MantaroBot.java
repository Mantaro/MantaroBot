package net.kodehawa.discord.Mantaro.bot;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.reflections.Reflections;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.kodehawa.discord.Mantaro.commands.*;
import net.kodehawa.discord.Mantaro.commands.admin.*;
import net.kodehawa.discord.Mantaro.commands.eval.Eval;
import net.kodehawa.discord.Mantaro.commands.mention.*;
import net.kodehawa.discord.Mantaro.commands.osu.Cosu;
import net.kodehawa.discord.Mantaro.listeners.Listener;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.main.Parser;
import net.kodehawa.discord.Mantaro.utils.LogTypes;
import net.kodehawa.discord.Mantaro.utils.Logging;
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
	private final String botPrefix = "~>";
	private final String[] meta = {"15th of September 2016", "0.7.6", "Kodehawa"};
	
	//Which OS is the bot running on?
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	public Set<Class<? extends Command>> classes = null;
	
	private boolean debugMode;
	
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
			Logging.instance().print("Starting with Java args:", LogTypes.INFO);
			for (String s: args) {
				System.out.println(s.replace(":", " = "));
		     }
		}

		Logging.instance().print("MantaroBot starting...", LogTypes.INFO);
				
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
			Logging.instance().print("Something very bad happened while loading commands! Check stacktrace.", LogTypes.CRITICAL);
		}
		
	}
	
	private void addCommands() throws InterruptedException, ExecutionException
	{
		  MantaroBot.Commands cmd = new MantaroBot.Commands();
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
	
	/**
	 * Why is this returning null?
	 * @return build date
	 */
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
    
    /**
     * So this takes way less time. Basically what happens when you can't make your code better lmao
     * @author Yomura
     */
    private class Commands extends Thread {
    	
    	public void run()
    	{
    		//do you expect nice code? no, won't deliver
    		this.setName("Command adding thread");
    		Logging.instance().print("Initializing commands.", LogTypes.INFO);
    		getInstance().commandList.put("ping", new CPing());
    		getInstance().commandList.put("serverinfo", new CServerInfo());
    		getInstance().commandList.put("marco", new CMarco());
    		getInstance().commandList.put("lewd", new CLewd());
    		getInstance().commandList.put("master", new CMaster());
    		getInstance().commandList.put("game", new CChangeGameStatus());
    		getInstance().commandList.put("bleach", new CBleach());
    		getInstance().commandList.put("disconnect", new CDisconnect());
    		getInstance().commandList.put("help", new CHelp());
    		getInstance().commandList.put("restart", new CRestart());
    		getInstance().commandList.put("brainpower", new CBrainPower());
    		getInstance().commandList.put("about", new CAbout());
    		getInstance().commandList.put("tsundere", new CTsundere());
    		getInstance().commandList.put("hi", new CHi());
    		getInstance().commandList.put("roasted", new CRoasted());
    		getInstance().commandList.put("quote", new CQuotation());
    		getInstance().commandList.put("add", new AddList());
    		getInstance().commandList.put("userinfo", new CUserInfo());
    		getInstance().commandList.put("shrug", new CShrug());
    		getInstance().commandList.put("konachan", new CKonachan());
    		getInstance().commandList.put("time", new CHour());
    		getInstance().commandList.put("osu", new Cosu());
    		getInstance().commandList.put("action", new CAction());
    		getInstance().commandList.put("random", new CRand());
    		getInstance().commandList.put("urban", new CUrbanDictionary());
    		getInstance().commandList.put("bot.status", new Disable());
    		getInstance().commandList.put("kode.eval", new Eval());
    		
    		getInstance().mentionCommandList.put("nya", new MentionMeow());
    		getInstance().mentionCommandList.put("wanna go to bed?", new MentionBed());
    		getInstance().mentionCommandList.put("welcome new", new MentionWelcomeNew());
    		getInstance().mentionCommandList.put("help", new MentionHelp());
    		getInstance().mentionCommandList.put("tell", new MentionSay());
    		getInstance().mentionCommandList.put("talk", new MentionCleverbot());
    		
    		
    		int totalCommands = getInstance().commandList.size()+getInstance().mentionCommandList.size();
    		
    		Logging.instance().print("Successfully loaded " + totalCommands + " commands.", LogTypes.INFO);

    		
    		this.interrupt();
    	}
    }
}
