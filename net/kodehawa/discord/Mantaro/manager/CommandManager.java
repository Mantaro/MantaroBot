package net.kodehawa.discord.Mantaro.manager;

import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.commands.*;
import net.kodehawa.discord.Mantaro.commands.admin.*;
import net.kodehawa.discord.Mantaro.commands.admin.perms.Permissions;
import net.kodehawa.discord.Mantaro.commands.eval.*;
import net.kodehawa.discord.Mantaro.commands.mention.*;
import net.kodehawa.discord.Mantaro.commands.osu.*;
import net.kodehawa.discord.Mantaro.commands.storm.*;
import net.kodehawa.discord.Mantaro.utils.LogType;
import net.kodehawa.discord.Mantaro.utils.Logger;

/**
 * Manages command adding.
 * @author Yomura
 *
 */
public class CommandManager extends Thread {
	
	//Calls the instance of this class.
	public volatile static CommandManager cmdmgr = new CommandManager();
	
	public CommandManager()
	{
		Logger.instance().print("CMDMGR call recieved.", LogType.INFO);
	}

	public void run()
	{
		//do you expect nice code? no, won't deliver
		this.setName("Command adding thread");
		Logger.instance().print("Initializing commands.", LogType.INFO);
		MantaroBot.getInstance().commandList.put("ping", new CPing());
		MantaroBot.getInstance().commandList.put("serverinfo", new CServerInfo());
		MantaroBot.getInstance().commandList.put("marco", new CMarco());
		MantaroBot.getInstance().commandList.put("lewd", new CLewd());
		MantaroBot.getInstance().commandList.put("master", new CMaster());
		MantaroBot.getInstance().commandList.put("game", new CChangeGameStatus());
		MantaroBot.getInstance().commandList.put("bleach", new CBleach());
		MantaroBot.getInstance().commandList.put("disconnect", new CDisconnect());
		MantaroBot.getInstance().commandList.put("help", new CHelp());
		MantaroBot.getInstance().commandList.put("restart", new CRestart());
		MantaroBot.getInstance().commandList.put("brainpower", new CBrainPower());
		MantaroBot.getInstance().commandList.put("about", new CAbout());
		MantaroBot.getInstance().commandList.put("tsundere", new CTsundere());
		MantaroBot.getInstance().commandList.put("hi", new CHi());
		MantaroBot.getInstance().commandList.put("roasted", new CRoasted());
		MantaroBot.getInstance().commandList.put("quote", new CQuotation());
		MantaroBot.getInstance().commandList.put("add", new AddList());
		MantaroBot.getInstance().commandList.put("userinfo", new CUserInfo());
		MantaroBot.getInstance().commandList.put("shrug", new CShrug());
		MantaroBot.getInstance().commandList.put("konachan", new CKonachan());
		MantaroBot.getInstance().commandList.put("yandere", new CYandere());
		MantaroBot.getInstance().commandList.put("time", new CHour());
		MantaroBot.getInstance().commandList.put("osu", new Cosu());
		MantaroBot.getInstance().commandList.put("action", new CAction());
		MantaroBot.getInstance().commandList.put("misc", new CMisc());
		MantaroBot.getInstance().commandList.put("translate", new CTranslator());
		MantaroBot.getInstance().commandList.put("urban", new CUrbanDictionary());
		MantaroBot.getInstance().commandList.put("8ball", new C8Ball());
		MantaroBot.getInstance().commandList.put("noble", new CNoble());
		MantaroBot.getInstance().commandList.put("addbd", new Birthday());
		MantaroBot.getInstance().commandList.put("bot.status", new Disable());
		MantaroBot.getInstance().commandList.put("kode.eval", new Eval());
		MantaroBot.getInstance().commandList.put("permission", new Permissions());
		
		MantaroBot.getInstance().mentionCommandList.put("nya", new MentionMeow());
		MantaroBot.getInstance().mentionCommandList.put("wanna go to bed?", new MentionBed());
		MantaroBot.getInstance().mentionCommandList.put("welcome new", new MentionWelcomeNew());
		MantaroBot.getInstance().mentionCommandList.put("help", new MentionHelp());
		MantaroBot.getInstance().mentionCommandList.put("tell", new MentionSay());
		MantaroBot.getInstance().mentionCommandList.put("talk", new MentionCleverbot());
		
		
		
		int totalCommands = MantaroBot.getInstance().commandList.size()+MantaroBot.getInstance().mentionCommandList.size();
		
		Logger.instance().print("Successfully loaded " + totalCommands + " commands.", LogType.INFO);

		
		this.interrupt();
	}	
	
}
