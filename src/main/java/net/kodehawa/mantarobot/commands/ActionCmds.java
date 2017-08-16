package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.collections.CollectionUtils;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.action.ImageActionCmd;
import net.kodehawa.mantarobot.commands.action.ImageCmd;
import net.kodehawa.mantarobot.commands.action.TextActionCmd;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.awt.*;
import java.util.List;

@Module
public class ActionCmds {

	//File declarations
	private final DataManager<List<String>> BITES = new SimpleFileDataManager("assets/mantaro/texts/bites.txt");
	private final DataManager<List<String>> GREETINGS = new SimpleFileDataManager("assets/mantaro/texts/greetings.txt");
	private final DataManager<List<String>> HUGS = new SimpleFileDataManager("assets/mantaro/texts/hugs.txt");
	private final DataManager<List<String>> PATS = new SimpleFileDataManager("assets/mantaro/texts/pats.txt");
	private final DataManager<List<String>> POKES = new SimpleFileDataManager("assets/mantaro/texts/pokes.txt");
	private final DataManager<List<String>> SLAPS = new SimpleFileDataManager("assets/mantaro/texts/slaps.txt");
	private final DataManager<List<String>> BLEACH = new SimpleFileDataManager("assets/mantaro/texts/bleach.txt");
	private final DataManager<List<String>> HIGHFIVES = new SimpleFileDataManager("assets/mantaro/texts/highfives.txt");
	private final DataManager<List<String>> KISSES = new SimpleFileDataManager("assets/mantaro/texts/kisses.txt");
	private final DataManager<List<String>> POUTS = new SimpleFileDataManager("assets/mantaro/texts/pouts.txt");
	private final DataManager<List<String>> TICKLES = new SimpleFileDataManager("assets/mantaro/texts/tickles.txt");
	private final DataManager<List<String>> TSUNDERE = new SimpleFileDataManager("assets/mantaro/texts/tsundere.txt");
	private final DataManager<List<String>> LEWD = new SimpleFileDataManager("assets/mantaro/texts/lewd.txt");
	private final DataManager<List<String>> FACEDESK = new SimpleFileDataManager("assets/mantaro/texts/facedesk.txt");
	private final DataManager<List<String>> NUZZLE = new SimpleFileDataManager("assets/mantaro/texts/nuzzle.txt");
	private final DataManager<List<String>> MEOW = new SimpleFileDataManager("assets/mantaro/texts/meow.txt");
	private final DataManager<List<String>> BLOODSUCK = new SimpleFileDataManager("assets/mantaro/texts/bloodsuck.txt");
	private final DataManager<List<String>> NOMS = new SimpleFileDataManager("assets/mantaro/texts/nom.txt");

	@Subscribe
	public void action(CommandRegistry registry) {
		registry.register("action", new SimpleTreeCommand(Category.ACTION) {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Action commands")
						.addField("Usage", "`~>action bleach` - **Random bleach picture**.\n" +
								"`~>action nom` - **nom nom**.", false)
						.setColor(Color.PINK)
						.build();
			}
		}.addSubCommand("nom", new SubCommand() {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content) {
				event.getChannel().sendMessage(CollectionUtils.random(NOMS.get())).queue();
			}
		}).addSubCommand("bleach", new SubCommand() {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content) {
				event.getChannel().sendMessage(CollectionUtils.random(BLEACH.get())).queue();
			}
		}));
	}

	@Subscribe
	public void register(CommandRegistry cr) {
		//pat();
		cr.register("pat", new ImageActionCmd(
			"Pat", "Pats the specified user.", Color.PINK,
			"pat.gif", EmoteReference.TALKING + "%s you have been patted by %s", PATS.get(), "Aww, I see you are lonely, take a pat <3"
		));

		//hug();
		cr.register("hug", new ImageActionCmd(
			"Hug", "Hugs the specified user.", Color.PINK,
			"hug.gif", EmoteReference.TALKING + "%s you have been hugged by %s", HUGS.get(), "Aww, I see you are lonely, take a hug <3"
		));

		//kiss();
		cr.register("kiss", new ImageActionCmd(
			"Kiss", "Kisses the specified user.", Color.PINK,
			"kiss.gif", EmoteReference.TALKING + "%s you have been kissed by %s", KISSES.get(), "Aww, I see you are lonely, *kisses*"
		));

		//poke();
		cr.register("poke", new ImageActionCmd(
				"Poke", "Pokes the specified user.", Color.PINK,
				"poke.gif", EmoteReference.TALKING + "%s you have been poked by %s :eyes:", POKES.get(), "Aww, I see you are lonely, *pokes you*"
		));

		//slap();
		cr.register("slap", new ImageActionCmd(
				"Slap", "Slaps the specified user ;).", Color.PINK,
				"slap.gif", EmoteReference.TALKING + "%s you have been slapped by %s!", SLAPS.get(), "Hmm, why do you want this? Uh, I guess... *slaps you*"
		));

		//bite();
		cr.register("bite", new ImageActionCmd(
				"Bite", "Bites the specified user.", Color.PINK,
				"bite.gif", EmoteReference.TALKING + "%s you have been bitten by %s :eyes:", BITES.get(), "*bites you*"
		));

		//tickle();
		cr.register("tickle", new ImageActionCmd(
				"Tickle", "Tickles the specified user.", Color.PINK,
				"tickle.gif", EmoteReference.JOY + "%s you have been tickled by %s", TICKLES.get(), "*tickles you*"
		));

		//highfive();
		cr.register("highfive", new ImageActionCmd(
				"Highfive", "Highfives with the specified user.", Color.PINK,
				"highfive.gif", EmoteReference.TALKING + "%s highfives %s :heart:", HIGHFIVES.get(), "*highfives*", true
		));

		//pout();
		cr.register("pout", new ImageActionCmd(
				"Pout", "Pouts at the specified user.", Color.PINK,
				"pout.gif", EmoteReference.TALKING + "%s pouts at %s *hmph*", POUTS.get(), "*pouts*, **hmph**", true
		));

		//greet();
		cr.register("greet", new TextActionCmd(
			"Greet", "Sends a random greeting", Color.DARK_GRAY,
			EmoteReference.TALKING + "%s", GREETINGS.get()
		));

		//tsundere();
		cr.register("tsundere", new TextActionCmd(
			"Tsundere Command", "Y-You baka!", Color.PINK,
			EmoteReference.MEGA + "%s", TSUNDERE.get()
		));

		//nuzzle()
		cr.register("nuzzle", new ImageActionCmd(
		        "Nuzzle Command", "Nuzzles the specified user.", Color.PINK,
                "nuzzle.gif", EmoteReference.TALKING + "%s is nuzzling %s", NUZZLE.get(), "*nuzzles you*", true
        ));

		//bloodsuck()
		cr.register("bloodsuck", new ImageActionCmd("Bloodsuck command", "Sucks the blood of an user", Color.PINK, "bloodsuck.gif",
				EmoteReference.TALKING + "%s is sucking the blood of %s!", BLOODSUCK.get(), "J-Just how am I meant to? Oh well.. *sucks your blood*", true));

		//lewd()
		cr.register("lewd", new ImageCmd("Lewd", "T-Too lewd!", "lewd", LEWD.get(), "Y-You lewdie!"));

		//meow()
		cr.register("meow", new ImageCmd("Meow", "Meows at the specified user.", "meow", MEOW.get(), "Meow."));
		cr.registerAlias("meow", "mew");

		//facedesk()
		cr.register("facedesk", new ImageCmd("Facedesk", "When it's just too much to handle.", "facedesk", FACEDESK.get(),
				"*facedesks*", true));
	}
}
