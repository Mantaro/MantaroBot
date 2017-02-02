package net.kodehawa.mantarobot.commands;

import net.kodehawa.lib.konachan.konachan.entities.Wallpaper;
import net.kodehawa.lib.konachan.konachan.service.Konachan;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.utils.ImageData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.GsonDataManager;
import net.kodehawa.mantarobot.utils.Utils;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ImageCmds extends Module {

	Konachan konachan = new Konachan(true);
	private String YANDERE_BASE = "https://yande.re/post.json?limit=60&";
	private BidiMap<String, String> nRating = new DualHashBidiMap<>();
	private boolean needRating = false;
	private int number = 0;
	private int number1;
	private int page = 0;
	private String rating = "";
	private boolean smallRequest = false;
	private String tagsEncoded = "";
	private String tagsToEncode = "no";

	public ImageCmds() {
		super(Category.MISC);
		yandere();
		kona();
		enterRatings();
	}

	private void enterRatings() {
		nRating.put("safe", "s");
		nRating.put("questionable", "q");
		nRating.put("explicit", "e");
	}

	private EmbedBuilder getImage(int argsCount, String requestType, String url, String rating, String[] messageArray, GuildMessageReceivedEvent event) {
		String nsfwChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).nsfwChannel;
		boolean trigger = (rating.equals("s") || (nsfwChannel == null)) ? rating.equals("s") : nsfwChannel.equals(event.getChannel().getId());
		if (!trigger)
			return new EmbedBuilder().setDescription("Not on NSFW channel. Cannot send lewd images.");

		String json = Utils.wget(url, event);
		ImageData[] imageData = GsonDataManager.GSON.fromJson(json, ImageData[].class);
		List<ImageData> filter = new ArrayList<>(Arrays.asList(imageData)).stream().filter(data -> rating.equals(data.rating)).collect(Collectors.toList());
		int get;
		try {
			get = requestType.equals("tags") ? argsCount >= 4 ? number : new Random().nextInt(filter.size()) : argsCount <= 2 ?
				Integer.parseInt(messageArray[2]) : new Random().nextInt(filter.size());
		} catch (ArrayIndexOutOfBoundsException e) {
			get = new Random().nextInt(filter.size());
		}
		String URL = filter.get(get).getFile_url();
		String AUTHOR = filter.get(get).getAuthor();
		String RATING = filter.get(get).getRating();
		int HEIGHT = filter.get(get).getHeight();
		int WIDTH = filter.get(get).getWidth();
		String tags = filter.get(get).getTags().stream().collect(Collectors.joining(", "));

		if (!smallRequest) {
			try {
				return new EmbedBuilder().setAuthor("Found image", null, null)
					.setDescription("Image uploaded by: " + (AUTHOR == null ? "not found" : AUTHOR) + ", with a rating of: **" + nRating.inverseBidiMap().get(RATING) + "**")
					.setImage(URL)
					.addField("Width", String.valueOf(WIDTH), true)
					.addField("Height", String.valueOf(HEIGHT), true)
					.addField("Tags", "``" + (tags == null ? "None" : tags) + "``", false);
			} catch (ArrayIndexOutOfBoundsException ex) {
				return new EmbedBuilder().setDescription(":heavy_multiplication_x: There are no images here, just dust.");
			}
		}

		try {
			return new EmbedBuilder().setAuthor("Found image", null, null)
				.setDescription("Image uploaded by: " + (AUTHOR == null ? "not found" : AUTHOR) + ", with a rating of: **" + nRating.inverseBidiMap().get(RATING) + "**")
				.setImage(URL)
				.addField("Width", String.valueOf(WIDTH), true)
				.addField("Height", String.valueOf(HEIGHT), true)
				.addField("Tags", "``" + (tags == null ? "None" : tags) + "``", false);
		} catch (ArrayIndexOutOfBoundsException ex) {
			return new EmbedBuilder().setDescription(":heavy_multiplication_x: There are no images here, just dust.");
		}
	}

	private void kona() {
		super.register("konachan", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();

				String noArgs = content.split(" ")[0];
				switch (noArgs) {
					case "get":
						channel.sendTyping().queue();
						String whole1 = content.replace("get ", "");
						String[] wholeBeheaded = whole1.split(" ");
						int page = Integer.parseInt(wholeBeheaded[0]);
						int number;

						List<Wallpaper> wallpapers = konachan.posts(page, 60);
						try {
							number = Integer.parseInt(wholeBeheaded[1]);
						} catch (Exception e) {
							number = new Random().nextInt(wallpapers.size() - 1);
						}
						String URL = wallpapers.get(number - 1).getFile_url();
						String AUTHOR = wallpapers.get(number - 1).getAuthor();
						String TAGS = wallpapers.get(number - 1).getTags().stream().collect(Collectors.joining(", "));
						Integer WIDTH = wallpapers.get(number - 1).getWidth();
						Integer HEIGHT = wallpapers.get(number - 1).getHeight();

						try {
							EmbedBuilder builder = new EmbedBuilder();
							builder.setAuthor("Found image", null, null)
								.setDescription("Image uploaded by: " + (AUTHOR == null ? "not found" : AUTHOR))
								.setImage("https:" + URL)
								.addField("Width", String.valueOf(WIDTH), true)
								.addField("Height", String.valueOf(HEIGHT), true)
								.addField("Tags", "``" + (TAGS == null ? "None" : TAGS) + "``", false);
							channel.sendMessage(builder.build()).queue();
						} catch (Exception exception) {
							if(exception instanceof ArrayIndexOutOfBoundsException) channel.sendMessage(":heavy_multiplication_x: " + "There aren't more images! Try with a lower number.").queue();
						}
						break;

					case "tags":
						channel.sendTyping().queue();
						String whole11 = content.replace("tags ", "");
						String[] whole2 = whole11.split(" ");
						int page1 = Integer.parseInt(whole2[0]);
						String tags = whole2[1];

						konachan.onSearch(page1, 60, tags, (wallpapers1, tags1) -> {
							try {
								number1 = Integer.parseInt(whole2[2]);
							} catch (Exception e) {
								number1 = new Random().nextInt(wallpapers1.size() - 1);
							}

							String URL1 = wallpapers1.get(number1 - 1).getFile_url();
							String AUTHOR1 = wallpapers1.get(number1 - 1).getAuthor();
							String TAGS1 = wallpapers1.get(number1 - 1).getTags().stream().collect(Collectors.joining(", "));
							Integer WIDTH1 = wallpapers1.get(number1 - 1).getWidth();
							Integer HEIGHT1 = wallpapers1.get(number1 - 1).getHeight();
							try {
								EmbedBuilder builder = new EmbedBuilder();
								builder.setAuthor("Found image", null, null)
									.setDescription("Image uploaded by: " + (AUTHOR1 == null ? "not found" : AUTHOR1))
									.setImage("https:" + URL1)
									.addField("Width", String.valueOf(WIDTH1), true)
									.addField("Height", String.valueOf(HEIGHT1), true)
									.addField("Tags", "``" + (TAGS1 == null ? "None" : TAGS1) + "``", false);
								channel.sendMessage(builder.build()).queue();
							} catch (Exception exception) {
								if(exception instanceof ArrayIndexOutOfBoundsException) channel.sendMessage(":heavy_multiplication_x: " + "There aren't more images! Try with a lower number.").queue();
							}
						});
						break;
					default:
						onHelp(event);
						break;
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "konachan.com commmand")
					.setColor(Color.PINK)
					.setDescription("Retrieves images from the **Konachan** image board.\n"
						+ "Usage:\n"
						+ "~>konachan get [page] [imagenumber]: Gets an image based in parameters.\n"
						+ "~>konachan tags [page] [tag] [imagenumber]: Gets an image based in the specified tag and parameters.\n"
						+ "> Parameter explanation:\n"
						+ "[page]: Can be any value from 1 to the Konachan maximum page. Probably around 4000.\n"
						+ "[imagenumber]: (OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.\n"
						+ "[tag]: Any valid image tag. For example animal_ears or original.")
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	private void yandere() {
		super.register("yandere", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				rating = "s";
				needRating = args.length >= 4;
				smallRequest = args.length <= 2;
				TextChannel channel = event.getChannel();
				int argscnt = args.length - 1;

				try {
					page = Integer.parseInt(args[1]);
					tagsToEncode = args[2];
					if (needRating) rating = nRating.get(args[3]);
					number = Integer.parseInt(args[4]);
				} catch (Exception ignored) {
				}

				try {
					tagsEncoded = URLEncoder.encode(tagsToEncode, "UTF-8");
				} catch (UnsupportedEncodingException ignored) {
				} //Shouldn't happen.

				String noArgs = content.split(" ")[0];
				switch (noArgs) {
					case "get":
						String url = String.format(YANDERE_BASE + "page=%2s", String.valueOf(page)).replace(" ", "");
						channel.sendMessage(getImage(argscnt, "get", url, rating, args, event).build()).queue();
						break;
					case "tags":
						String url1 = String.format(YANDERE_BASE + "page=%2s&tags=%3s", String.valueOf(page), tagsEncoded).replace(" ", "");
						channel.sendMessage(getImage(argscnt, "tags", url1, rating, args, event).build()).queue();
						break;
					case "":
						int randomPage = new Random().nextInt(5);
						String url2 = String.format(YANDERE_BASE + "&page=%2s", String.valueOf(randomPage)).replace(" ", "");
						channel.sendMessage(getImage(argscnt, "random", url2, rating, args, event).build()).queue();
						break;
					default:
						onHelp(event);
						break;
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Yande.re command")
					.setColor(Color.DARK_GRAY)
					.setDescription("This command fetches images from the image board **yande.re**. Normally used to store *NSFW* images, "
						+ "but tags can be set to safe if you so desire.\n"
						+ "~>yandere: Gets you a completely random image.\n"
						+ "~>yandere get [page] [imagenumber] [rating]: Gets you an image with the specified parameters.\n"
						+ "~>yandere tags page [tag] [rating] [imagenumber]: Gets you an image with the respective tag and specified parameters.\n"
						+ "This command can be only used in NSFW channels! (Unless rating has been specified as safe)\n"
						+ "> Parameter explanation:\n"
						+ "[page]: Can be any value from 1 to the yande.re maximum page. Probably around 4000.\n"
						+ "[imagenumber]: (OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.\n"
						+ "[tag]: Any valid image tag. For example animal_ears or yuri.\n"
						+ "[rating]: (OPTIONAL) Can be either safe, questionable or explicit, depends on the type of image you want to get.")
					.build();
			}
		});
	}
}
