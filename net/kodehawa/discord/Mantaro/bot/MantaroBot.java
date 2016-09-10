package net.kodehawa.discord.Mantaro.bot;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.reflections.Reflections;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.kodehawa.discord.Mantaro.annotation.Metadata;
import net.kodehawa.discord.Mantaro.commands.*;
import net.kodehawa.discord.Mantaro.commands.admin.*;
import net.kodehawa.discord.Mantaro.commands.eval.Eval;
import net.kodehawa.discord.Mantaro.commands.mention.*;
import net.kodehawa.discord.Mantaro.commands.osu.Cosu;
import net.kodehawa.discord.Mantaro.commands.placeholder.CommandNotFound;
import net.kodehawa.discord.Mantaro.listeners.MessageListener;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.main.Parser;
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
	
	//So I can call this.
	private static MantaroBot instance = new MantaroBot();
	
	//Command list. From here, everything gets called, like for example Command.botAction();
	public HashMap<String, Command> mentionCommandList = new HashMap<String, Command>();
	public HashMap<String, Command> commandList = new HashMap<String, Command>();
	
	//So I don't hardcode it, just that.
	private final String gameStatus = "Lewd.";
	private final String botPrefix = "~>";
	
	//Which OS is the bot running on?
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	//Find all classes in commands and all subpackages
	public Set<Class<? extends Command>> classes = null;
	
	public MantaroBot(){
		//Set first lookup
		Reflections reflections = new Reflections("net.kodehawa.discord.Mantaro.commands");
		classes = reflections.getSubTypesOf(Command.class);
	}
	
	/**
	 * Don't kill me, this is the nicest I could code this.
	 * @param args
	 */	
	@Metadata(date = "6th of September 2016", build = "0.7.3a", credits = "Kodehawa")
	public static void main(String[] args)
	{
		//Just so you know...
		System.out.println("MantaroBot starting...");
		
		try
		{
			getInstance().jda = new JDABuilder().addListener(new MessageListener()).setBotToken("woah token").buildBlocking();
			System.out.println("MantaroBot succefully started");
			getInstance().jda.setAutoReconnect(true);
			//Default
			getInstance().jda.getAccountManager().setGame(getInstance().gameStatus);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		new Values();

		//Starts command thread
		try {
			getInstance().addCommands();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.out.println("Something very bad happened while loading commands! Check stacktrace.");
		}
	}
	
	private void addCommands() throws InterruptedException, ExecutionException
	{
		  MantaroBot.Commands cmd = new MantaroBot.Commands();
		  //start
	      cmd.start();
	}
	
	/**
	 * Sends the instruction when a command happens.
	 * @param cmd
	 */
	public static void onCommand(Parser.CommandContainer cmd)
	{		
		if(!MessageListener.isMenction)
		{
			if(getInstance().commandList.containsKey(cmd.invoke))
			{
				boolean enabled = getInstance().commandList.get(cmd.invoke).isAvaliable(cmd.args, cmd.event);
				if(enabled)
				{
					getInstance().commandList.get(cmd.invoke).botAction(cmd.args, cmd.rawCommand, cmd.beheaded1, cmd.event);
					getInstance().commandList.get(cmd.invoke).actionResult(enabled, cmd.event);
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
					getInstance().mentionCommandList.get(cmd.invoke).actionResult(enabled, cmd.event);
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
		for (Method m: MantaroBot.class.getMethods()) {
			Metadata meta = m.getAnnotation(Metadata.class);
		    return meta.date();
		}
		
		return null;
	}
	
	public String getBotPrefix()
	{
		return botPrefix;
	}
	
	public String getBuild()
	{
		for (Method m: MantaroBot.class.getMethods()) {
			Metadata meta = m.getAnnotation(Metadata.class);
		    return meta.build();
		}
		
		return null;
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
    		System.out.println("Initializing commands");
    		getInstance().commandList.put("ping", new CPing());
    		getInstance().commandList.put("serverinfo", new CServerInfo());
    		getInstance().commandList.put("marco", new CMarco());
    		getInstance().commandList.put("lewd", new CLewd());
    		getInstance().commandList.put("meow", new CMeow());
    		getInstance().commandList.put("master", new CMaster());
    		getInstance().commandList.put("game", new CChangeGameStatus());
    		getInstance().commandList.put("bleach", new CBleach());
    		getInstance().commandList.put("disconnect", new CDisconnect());
    		getInstance().commandList.put("help", new CHelp());
    		getInstance().commandList.put("restart", new CRestart());
    		getInstance().commandList.put("aaaa", new CBrainPower());
    		getInstance().commandList.put("about", new CAbout());
    		getInstance().commandList.put("tsundere", new CTsundere());
    		getInstance().commandList.put("hi", new CHi());
    		getInstance().commandList.put("roasted", new CRoasted());
    		getInstance().commandList.put("meow2", new CMeow2());
    		getInstance().commandList.put("quote", new CQuotation());
    		getInstance().commandList.put("add", new AddList());
    		getInstance().commandList.put("userinfo", new CUserInfo());
    		getInstance().commandList.put("shrug", new CShrug());
    		getInstance().commandList.put("konachan", new CKonachan());
    		getInstance().commandList.put("time", new CHour());
    		getInstance().commandList.put("osu", new Cosu());
    		getInstance().commandList.put("action", new CAction());
    		getInstance().commandList.put("random", new CRand());
    		getInstance().commandList.put("placeholder", new CommandNotFound());
    		getInstance().commandList.put("bot.status", new Disable());
    		getInstance().commandList.put("kode.eval", new Eval());
    		
    		getInstance().mentionCommandList.put("nya", new MentionMeow());
    		getInstance().mentionCommandList.put("wanna go to bed?", new MentionBed());
    		getInstance().mentionCommandList.put("welcome new", new MentionWelcomeNew());
    		getInstance().mentionCommandList.put("help", new MentionHelp());
    		getInstance().mentionCommandList.put("tell", new MentionSay());
    		getInstance().mentionCommandList.put("talk", new MentionCleverbot());
    		getInstance().mentionCommandList.put("placeholder", new CommandNotFound());
    		
    		
    		int totalCommands = getInstance().commandList.size()+getInstance().mentionCommandList.size();
    		
    		System.out.println("Successfully loaded " + totalCommands + " commands.");

    		
    		this.interrupt();
    	}
    	
    	
    	
    }
}
