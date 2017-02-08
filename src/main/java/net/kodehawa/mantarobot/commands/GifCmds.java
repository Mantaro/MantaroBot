package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.giphy.main.Giphy;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;

import java.util.Optional;
import java.util.Random;

public class GifCmds extends Module {

	Giphy giphy = new Giphy();

	public GifCmds(){
		super(Category.GIF);
		random();
		search();
		trending();
	}

	private void search(){
		super.register("gifsearch", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if(args.length <= 0){
					help(event);
					return;
				}
				String rating = null, image = null;
				try { image = args[1]; } catch (Exception ignored){}
				try { rating = args[2]; } catch (Exception ignored){}
				int image1 = Optional.ofNullable(image).isPresent() ? Integer.parseInt(image) : 0;
				giphy.search(event, args[0], null, null, rating,
						(query) -> {
							String nsfwChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).nsfwChannel;
							boolean trigger = (!query.getData()[image1].getRating().equals("r") || (nsfwChannel == null)) ?
									!query.getData()[image1].getRating().equals("r") : nsfwChannel.equals(event.getChannel().getId());
							//Start the embed
							MessageEmbed embed = new EmbedBuilder()
									.setAuthor("Gif lookup result for " + args[0], query.getData()[image1].getImages().original.getUrl(),
											//Gifs are *always* cats, kappa.
											"http://rollycat.com/wp-content/uploads/2014/09/apple-mac-cat-face-like-angel-soul_342655.jpg")
									.setImage(query.getData()[image1].getImages().original.getUrl())
									.addField("Width", query.getData()[image1].getImages().original.getWidth(), true)
									.addField("Height", query.getData()[image1].getImages().original.getHeight(), true)
									.setFooter("If the image doesn't load, click the title.", null)
									.build();

							//If the channel is nsfw or the rating is not explicit, go ahead.
							if(trigger) event.getChannel().sendMessage(embed).queue();
							//Try to explain what went wrong.
							else event.getChannel().sendMessage(":heavy_multiplication_x: Image queried was explicit and called in channel " +
									"besides the NSFW channel. Maybe try with a higher number or another query?").queue();
						});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Gif search")
						.addField("Description", "Searches for gifs on the internet using Giphy", false)
						.addField("Usage", "~>gifsearch <tag> <number> <rating>)\n", false)
						.addField("Parameters",
								"tag: The search query.\n" +
								"image: (OPTIONAL) Image number, from 1 to around 5.\n" +
								"rating: y, g, pg, pg-13 or r (r is explicit and can be only be used on the nsfw channel of the guild).", false)
						.build();
			}
		});
	}

	private void random(){
		super.register("randomgif", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String tags = null;
				try { tags = args[0]; } catch (Exception ignored) {}
				//Start the embed
				giphy.random(tags, event, (result) -> {
					MessageEmbed embed = new EmbedBuilder()
							.setAuthor("Gif lookup result for random", result.getData().getImage_url(),
									//Gifs are *always* cats, kappa.
									"http://rollycat.com/wp-content/uploads/2014/09/apple-mac-cat-face-like-angel-soul_342655.jpg")
							.setImage(result.getData().getImage_url())
							.addField("Width", result.getData().getImage_width(), true)
							.addField("Height", result.getData().getImage_height(), true)
							.setFooter("If the image doesn't load, click the title. Provided by Giphy", null)
							.build();
					event.getChannel().sendMessage(embed).queue();
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Random gif")
						.setDescription("Gets a random gif.")
						.build();
			}
		});
	}

	private void trending(){
		super.register("trendinggif", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				giphy.trending(event, (query -> {
					int image1 = new Random().nextInt(query.getData().length - 1);
					String nsfwChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).nsfwChannel;
					boolean trigger = (!query.getData()[image1].getRating().equals("r") || (nsfwChannel == null)) ?
							!query.getData()[image1].getRating().equals("r") : nsfwChannel.equals(event.getChannel().getId());
					//Start the embed
					MessageEmbed embed = new EmbedBuilder()
							.setAuthor("Gif lookup result for trending", query.getData()[image1].getImages().original.getUrl(),
									//Gifs are *always* cats, kappa.
									"http://rollycat.com/wp-content/uploads/2014/09/apple-mac-cat-face-like-angel-soul_342655.jpg")
							.setImage(query.getData()[image1].getImages().original.getUrl())
							.addField("Width", query.getData()[image1].getImages().original.getWidth(), true)
							.addField("Height", query.getData()[image1].getImages().original.getHeight(), true)
							.setFooter("If the image doesn't load, click the title. Provided by Giphy", null)
							.build();

					//If the channel is nsfw or the rating is not explicit, go ahead.
					if(trigger) event.getChannel().sendMessage(embed).queue();
						//Try to explain what went wrong.
					else event.getChannel().sendMessage(":heavy_multiplication_x: Image queried was explicit and called in channel " +
							"besides the NSFW channel. Maybe try with a higher number or another query?").queue();
				}));
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Trending gif.")
						.setDescription("Return a random gif from the trending section.")
						.build();
			}
		});
	}
}
