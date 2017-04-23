package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.extensions.CollectionUtils;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.imageboard.e621.main.e621;
import net.kodehawa.lib.imageboard.konachan.main.Konachan;
import net.kodehawa.lib.imageboard.konachan.main.entities.Wallpaper;
import net.kodehawa.lib.imageboard.rule34.main.Rule34;
import net.kodehawa.mantarobot.commands.rpg.TextChannelGround;
import net.kodehawa.mantarobot.commands.utils.data.ImageData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Event;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.modules.commands.Commands;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.events.PostLoadEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.json.JSONObject;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Module
public class ImageCmds {

	public static final URLCache CACHE = new URLCache(20);
	private static final String BASEURL = "http://catgirls.brussell98.tk/api/random";
	private static final String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random";
	private static final String[] responses = {"Aww, take a cat.", "%mention%, are you sad? ;w;, take a cat!", "You should all have a cat in your life, but a image will do.",
		"Am I cute yet?", "%mention%, I think you should have a cat."};
	private static String YANDERE_BASE = "https://yande.re/post.json?limit=60&";
	private static e621 e621 = new e621();
	private static Konachan konachan = new Konachan(true);
	private static BidiMap<String, String> nRating = new DualHashBidiMap<>();
	private static boolean needRating = false;
	private static int number = 0;
	private static int number1;
	private static int page = 0;
	private static Random r = new Random();
	private static String rating = "";
	private static boolean smallRequest = false;
	private static String tagsEncoded = "";
	private static String tagsToEncode = "no";

	@Event
	public static void cat(CommandRegistry cr) {
		cr.register("cat", Commands.newSimple(Category.IMAGE)
			.permission(CommandPermission.USER)
			.onCall((thiz, event, content, args) -> {
				try {
					String url = Unirest.get("http://random.cat/meow").asJsonAsync().get().getBody().getObject().get("file").toString();
					event.getChannel().sendFile(CACHE.getFile(url), "cat.jpg",
						new MessageBuilder().append(CollectionUtils.random(responses).replace("%mention%", event.getAuthor().getAsMention())).build()).queue();
				} catch (Exception e) {
					e.printStackTrace();
				}
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Cat command")
				.setDescription("Sends a random cat image")
				.build())
			.build());
	}

	@Event
	public static void catgirls(CommandRegistry cr) {
		cr.register("catgirl", Commands.newSimple(Category.IMAGE)
			.permission(CommandPermission.USER)
			.onCall((thiz, event, content, args) -> {
				boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");
				if (nsfw && !nsfwCheck(event, true, true, null)) return;

				try {
					JSONObject obj = Unirest.get(nsfw ? NSFWURL : BASEURL)
						.asJson()
						.getBody()
						.getObject();
					if (!obj.has("url")) {
						event.getChannel().sendMessage("Unable to find image").queue();
					} else {
						event.getChannel().sendFile(CACHE.getInput(obj.getString("url")), "catgirl.png", null).queue();
					}
				} catch (UnirestException e) {
					e.printStackTrace();
					event.getChannel().sendMessage("Unable to get image").queue();
				}
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Catgirl command")
				.setDescription("Sends catgirl images")
				.addField("Usage", "`~>catgirl`\n´~>catgirl nsfw´", false)
				.build())
			.build());
	}

	@Event
	public static void e621(CommandRegistry cr) {
		cr.register("e621", Commands.newSimple(Category.IMAGE)
			.permission(CommandPermission.USER)
			.onCall((thiz, event, content, args) -> {
				if (!nsfwCheck(event, true, true, null)) return;
				TextChannelGround.of(event).dropItemWithChance(13, 3);

				String noArgs = content.split(" ")[0];
				switch (noArgs) {
					case "get":
						try {
							String whole1 = content.replace("get ", "");
							String[] wholeBeheaded = whole1.split(" ");
							int page = Integer.parseInt(wholeBeheaded[0]);
							e621.get(page, 60, image -> {
								try {
									int number;
									try {
										number = Integer.parseInt(wholeBeheaded[1]);
									} catch (Exception e) {
										number = r.nextInt(image.size());
									}

									String TAGS = image.get(number).getTags().replace(" ", " ,");
									EmbedBuilder builder = new EmbedBuilder();
									builder.setAuthor("Found image", null, image.get(number - 1).getFile_url())
										.setImage(image.get(number - 1).getFile_url())
										.addField("Width", String.valueOf(image.get(number - 1).getWidth()), true)
										.addField("Height", String.valueOf(image.get(number - 1).getHeight()), true)
										.addField("Tags", "``" + (TAGS == null ? "None" : TAGS) + "``", false)
										.setFooter("If the image doesn't load, click the title.", null);

									event.getChannel().sendMessage(builder.build()).queue();
								} catch (ArrayIndexOutOfBoundsException e) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "There aren't more images! Try with a lower number.").queue();
								}
							});

						} catch (Exception exception) {
							if (exception instanceof NumberFormatException)
								event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help e621").queue();
						}
						break;

					case "tags":
						try {
							String sNoArgs = content.replace("tags ", "");
							String[] expectedNumber = sNoArgs.split(" ");
							String tags = expectedNumber[0];

							boolean isOldFormat = args[1].matches("^[0-9]*$");
							if (isOldFormat) {
								event.getChannel().sendMessage(EmoteReference.WARNING + "Now you don't need to specify the page number. Please use ~>e621 tags <tag>").queue();
								return;
							}
							e621.onSearch(r.nextInt(50), 60, tags, images -> {
								try {
									try {
										number1 = Integer.parseInt(expectedNumber[2]);
									} catch (Exception e) {
										number1 = r.nextInt(images.size() > 0 ? images.size() - 1 : images.size());
									}

									String TAGS = images.get(number).getTags().replace(" ", " ,");

									EmbedBuilder builder = new EmbedBuilder();
									builder.setAuthor("Found image", null, images.get(number1 - 1).getFile_url())
										.setImage(images.get(number1 - 1).getFile_url())
										.addField("Width", String.valueOf(images.get(number1 - 1).getWidth()), true)
										.addField("Height", String.valueOf(images.get(number1 - 1).getHeight()), true)
										.addField("Tags", "``" + (TAGS == null ? "None" : TAGS) + "``", false)
										.setFooter("If the image doesn't load, click the title.", null);

									event.getChannel().sendMessage(builder.build()).queue();
								} catch (ArrayIndexOutOfBoundsException e) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "There aren't more images! Try with a lower number.").queue();
								}
							});
						} catch (Exception exception) {
							if (exception instanceof NumberFormatException)
								event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help e621").queue();
						}
						break;
					default:
						thiz.onHelp(event);
						break;
				}
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "e621 commmand")
				.setColor(Color.PINK)
				.setDescription("Retrieves images from the **e621** (furry) image board.")
				.addField("Usage", "~>e621 get <page> <imagenumber>: Gets an image based in parameters.\n"
					+ "~>e621 tags <tag> <imagenumber>: Gets an image based in the specified tag and parameters.\n", false)
				.addField("Parameters", "page: Can be any value from 1 to the e621 maximum page. Probably around 4000.\n"
					+ "imagenumber: (OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.\n"
					+ "tag: Any valid image tag. For example animal_ears or original.", false)
				.build())
			.build());
	}

	private static EmbedBuilder getImage(int argsCount, String requestType, String url, String rating, String[] messageArray, GuildMessageReceivedEvent event) {
		EmbedBuilder builder = new EmbedBuilder();
		if (!nsfwCheck(event, false, false, "s"))
			return builder.setDescription("Cannot send a lewd image in a non-nsfw channel.");

		String json = Utils.wget(url, event);
		try {
			ImageData[] imageData = GsonDataManager.GSON_PRETTY.fromJson(json, ImageData[].class);
			List<ImageData> filter = new ArrayList<>(Arrays.asList(imageData)).stream().filter(data -> rating.equals(data.rating)).collect(Collectors.toList());
			int get;
			try {
				get = requestType.equals("tags") ? argsCount >= 4 ? number : r.nextInt(filter.size()) : argsCount <= 2 ? Integer.parseInt(messageArray[2]) : r.nextInt(filter.size());
			} catch (IndexOutOfBoundsException e) {
				get = r.nextInt(filter.size());
			} catch (IllegalArgumentException e) {
				if (e.getMessage().equals("bound must be positive"))
					return builder.setDescription("No results found.");
				else return builder.setDescription("Query not valid.");
			}

			String AUTHOR = filter.get(get).getAuthor();
			String tags = filter.get(get).getTags().stream().collect(Collectors.joining(", "));

			if (!smallRequest) {
				return builder.setAuthor("Found image", filter.get(get).getFile_url(), null)
					.setDescription("Image uploaded by: " + (AUTHOR == null ? "not found" : AUTHOR) + ", with a rating of: **" + nRating.inverseBidiMap().get(filter.get(get).getRating()) + "**")
					.setImage(filter.get(get).getFile_url())
					.addField("Height", String.valueOf(filter.get(get).getHeight()), true)
					.addField("Width", String.valueOf(filter.get(get).getWidth()), true)
					.addField("Tags", "``" + (tags == null ? "None" : tags) + "``", false)
					.setFooter("If the image doesn't load, click the title.", null);
			}

			return builder.setAuthor("Found image", filter.get(get).getFile_url(), null)
				.setDescription("Image uploaded by: " + (AUTHOR == null ? "not found" : AUTHOR) + ", with a rating of: **" + nRating.inverseBidiMap().get(filter.get(get).getRating()) + "**")
				.setImage(filter.get(get).getFile_url())
				.addField("Width", String.valueOf(filter.get(get).getHeight()), true)
				.addField("Height", String.valueOf(filter.get(get).getWidth()), true)
				.addField("Tags", "``" + (tags == null ? "None" : tags) + "``", false)
				.setFooter("If the image doesn't load, click the title.", null);
		} catch (Exception ex) {
			if (ex instanceof NullPointerException)
				return builder.setDescription(EmoteReference.ERROR + "Wrong syntax.");
			return builder.setDescription(EmoteReference.ERROR + "There are no images here, just dust.");
		}
	}

	@Event
	public static void kona(CommandRegistry cr) {
		cr.register("konachan", Commands.newSimple(Category.IMAGE)
			.permission(CommandPermission.USER)
			.onCall((thiz, event, content, args) -> {
				TextChannel channel = event.getChannel();

				String noArgs = content.split(" ")[0];
				switch (noArgs) {
					case "get":
						try {
							channel.sendTyping().queue();
							String whole1 = content.replace("get ", "");
							String[] wholeBeheaded = whole1.split(" ");
							int page = Integer.parseInt(wholeBeheaded[0]);
							int number;
							List<Wallpaper> wallpapers = konachan.posts(page, 60);
							try {
								number = Integer.parseInt(wholeBeheaded[1]);
							} catch (Exception e) {
								number = r.nextInt(wallpapers.size() - 1);
							}
							String AUTHOR = wallpapers.get(number - 1).getAuthor();
							String TAGS = wallpapers.get(number - 1).getTags().stream().collect(Collectors.joining(", "));

							EmbedBuilder builder = new EmbedBuilder();
							builder.setAuthor("Found image", null, "https:" + wallpapers.get(number - 1).getJpeg_url())
								.setDescription("Image uploaded by: " + (AUTHOR == null ? "not found" : AUTHOR))
								.setImage("https:" + wallpapers.get(number - 1).getJpeg_url())
								.addField("Width", String.valueOf(wallpapers.get(number - 1).getWidth()), true)
								.addField("Height", String.valueOf(wallpapers.get(number - 1).getHeight()), true)
								.addField("Tags", "``" + (TAGS == null ? "None" : TAGS) + "``", false)
								.setFooter("If the image doesn't load, click the title.", null);

							channel.sendMessage(builder.build()).queue();
						} catch (Exception exception) {
							if (exception instanceof NumberFormatException)
								channel.sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help konachan").queue();
							if (exception instanceof IndexOutOfBoundsException)
								channel.sendMessage(EmoteReference.ERROR + "There aren't more images! Try with a lower number.").queue();
						}
						break;
					case "tags":
						try {
							boolean isOldFormat = args[1].matches("^[0-9]*$");
							if (isOldFormat) {
								event.getChannel().sendMessage(EmoteReference.WARNING + "Now you don't need to specify the page number. Please use ~>konachan tags <tag>").queue();
								return;
							}

							channel.sendTyping().queue();
							String sNoArgs = content.replace("tags ", "");
							String[] expectedNumber = sNoArgs.split(" ");
							String tags = expectedNumber[0];
							konachan.onSearch(r.nextInt(50), 60, tags, (wallpapers1, tags1) -> {
								try {
									number1 = Integer.parseInt(expectedNumber[1]);
								} catch (Exception e) {
									number1 = r.nextInt(wallpapers1.size() > 0 ? wallpapers1.size() - 1 : wallpapers1.size());
								}

								String TAGS1 = wallpapers1.get(number1).getTags().stream().collect(Collectors.joining(", "));

								EmbedBuilder builder = new EmbedBuilder();
								builder.setAuthor("Found image", null, "https:" + wallpapers1.get(number1 - 1).getJpeg_url())
									.setDescription("Image uploaded by: " + (wallpapers1.get(number1 - 1).getAuthor() == null ? "not found" : wallpapers1.get(number1 - 1).getAuthor()))
									.setImage("https:" + wallpapers1.get(number1 - 1).getJpeg_url())
									.addField("Width", String.valueOf(wallpapers1.get(number1 - 1).getWidth()), true)
									.addField("Height", String.valueOf(wallpapers1.get(number1 - 1).getHeight()), true)
									.addField("Tags", "``" + (TAGS1 == null ? "None" : TAGS1) + "``", false)
									.setFooter("If the image doesn't load, click the title.", null);

								channel.sendMessage(builder.build()).queue();
							});
						} catch (Exception exception) {
							if (exception instanceof IndexOutOfBoundsException) {
								channel.sendMessage(EmoteReference.ERROR + "There aren't more images! Try with a lower number.").queue();
								return;
							}

							if (exception instanceof NumberFormatException)
								channel.sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help konachan").queue();
						}
						break;
					default:
						thiz.onHelp(event);
						break;
				}
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Konachan commmand")
				.setColor(Color.PINK)
				.setDescription("Retrieves images from the **Konachan** image board.")
				.addField("Usage", "~>konachan get <page> <imagenumber>: Gets an image based in parameters.\n"
					+ "~>konachan tags <tag> <imagenumber>: Gets an image based in the specified tag and parameters.\n", false)
				.addField("Parameters", "page: Can be any value from 1 to the Konachan maximum page. Probably around 4000.\n"
					+ "imagenumber: (OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.\n"
					+ "tag: Any valid image tag. For example animal_ears or original.", false)
				.build())
			.build());
	}

	private static boolean nsfwCheck(GuildMessageReceivedEvent event, boolean isGlobal, boolean sendMessage, String acceptedRating) {
		String nsfwChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildUnsafeChannels().stream()
			.filter(channel -> channel.equals(event.getChannel().getId())).findFirst().orElse(null);
		String rating1 = rating == null ? "s" : rating;
		boolean trigger = !isGlobal ? ((rating1.equals("s") || (nsfwChannel == null)) ? rating1.equals("s") : nsfwChannel.equals(event.getChannel().getId())) :
			nsfwChannel != null && nsfwChannel.equals(event.getChannel().getId());

		if (!trigger) {
			if (sendMessage)
				event.getChannel().sendMessage(new EmbedBuilder().setDescription("Not on a NSFW channel. Cannot send lewd images.").build()).queue();
			return false;
		}

		return true;
	}

	@Event
	public static void onPostLoad(PostLoadEvent e) {
		nRating.put("safe", "s");
		nRating.put("questionable", "q");
		nRating.put("explicit", "e");
	}

	@Event
	public static void rule34(CommandRegistry cr) {
		cr.register("rule34", Commands.newSimple(Category.IMAGE)
			.permission(CommandPermission.USER)
			.onCall((thiz, event, content, args) -> {
				if (!nsfwCheck(event, true, true, null)) return;

				String noArgs = content.split(" ")[0];
				TextChannelGround.of(event).dropItemWithChance(13, 3);
				switch (noArgs) {
					case "get":
						try {
							String whole1 = content.replace("get ", "");
							String[] wholeBeheaded = whole1.split(" ");
							Rule34.get(60, image -> {
								try {
									int number;
									try {
										number = Integer.parseInt(wholeBeheaded[0]);
									} catch (Exception e) {
										number = r.nextInt(image.size());
									}

									String TAGS = image.get(number).getTags().replace(" ", " ,");
									EmbedBuilder builder = new EmbedBuilder();
									builder.setAuthor("Found image", null, "http:" + image.get(number - 1).getFile_url())
										.setImage("http:" + image.get(number - 1).getFile_url())
										.addField("Width", String.valueOf(image.get(number - 1).getWidth()), true)
										.addField("Height", String.valueOf(image.get(number - 1).getHeight()), true)
										.addField("Tags", "``" + (TAGS == null ? "None" : TAGS) + "``", false)
										.setFooter("If the image doesn't load, click the title.", null);

									event.getChannel().sendMessage(builder.build()).queue();
								} catch (ArrayIndexOutOfBoundsException e) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "There aren't more images! Try with a lower number.").queue();
								}
							});

						} catch (Exception exception) {
							if (exception instanceof NumberFormatException)
								event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help e621").queue();
						}
						break;
					case "tags":
						try {
							try {
								boolean isOldFormat = args[1].matches("^[0-9]*$");
								if (isOldFormat) {
									event.getChannel().sendMessage(EmoteReference.WARNING + "Now you don't need to specify the page number. Please use ~>rule34 tags <tag>").queue();
									return;
								}

								String sNoArgs = content.replace("tags ", "");
								String[] expectedNumber = sNoArgs.split(" ");
								String tags = expectedNumber[0];

								Rule34.onSearch(60, tags, images -> {
									try {
										try {
											number1 = Integer.parseInt(expectedNumber[2]);
										} catch (Exception e) {
											number1 = r.nextInt(images.size() > 0 ? images.size() - 1 : images.size());
										}
										String TAGS = images.get(number).getTags() == null ? tags : images.get(number).getTags()
											.replace(" ", " ,");
										EmbedBuilder builder = new EmbedBuilder();
										builder.setAuthor("Found image", null, "http:" + images.get(number1 - 1).getFile_url())
											.setImage("http:" + images.get(number1 - 1).getFile_url())
											.addField("Width", String.valueOf(images.get(number1 - 1).getWidth()), true)
											.addField("Height", String.valueOf(images.get(number1 - 1).getHeight()), true)
											.addField("Tags", "``" + (TAGS == null ? "None" : TAGS) + "``", false)
											.setFooter("If the image doesn't load, click the title.", null);

										event.getChannel().sendMessage(builder.build()).queue();
									} catch (Exception e) {
										e.printStackTrace();

										event.getChannel().sendMessage(EmoteReference.ERROR + "There aren't more images! Try with a lower number.").queue();
									}

								});
							} catch (Exception exception) {
								if (exception instanceof NumberFormatException)
									event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help rule34").queue();
							}
						} catch (NullPointerException e) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Rule34 decided to not fetch the image. Well, you can try with another number or tag.").queue();
						}
						break;
					default:
						thiz.onHelp(event);
						break;
				}
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "rule34.xxx commmand")
				.setColor(Color.PINK)
				.setDescription("Retrieves images from the **rule34** (hentai) image board.")
				.addField("Usage", "~>rule34 get <imagenumber>: Gets an image based in parameters.\n"
					+ "~>rule34 tags <tag> <imagenumber>: Gets an image based in the specified tag and parameters.\n", false)
				.addField("Parameters", "page: Can be any value from 1 to the rule34 maximum page. Probably around 4000.\n"
					+ "imagenumber: (OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.\n"
					+ "tag: Any valid image tag. For example animal_ears or original.", false)
				.build())
			.build());
	}

	@Event
	public static void yandere(CommandRegistry cr) {
		cr.register("yandere", Commands.newSimple(Category.IMAGE)
			.permission(CommandPermission.USER)
			.onCall((thiz, event, content, args) -> {
				rating = "s";
				needRating = args.length >= 3;
				smallRequest = args.length <= 1;
				TextChannel channel = event.getChannel();
				int argCount = args.length - 1;

				try {
					page = Math.max(r.nextInt(50), 1);
					tagsToEncode = args[1];
					if (needRating) rating = nRating.get(args[2]);
					number = Integer.parseInt(args[3]);
				} catch (Exception ignored) {
				}

				try {
					tagsEncoded = URLEncoder.encode(tagsToEncode, "UTF-8");
				} catch (UnsupportedEncodingException ignored) {
				} //Shouldn't happen.
				TextChannelGround.of(event).dropItemWithChance(13, 3);

				String noArgs = content.split(" ")[0];
				switch (noArgs) {
					case "get":
						String url = String.format(YANDERE_BASE + "page=%2s", String.valueOf(page)).replace(" ", "");
						channel.sendMessage(getImage(argCount, "get", url, rating, args, event).build()).queue();
						break;
					case "tags":
						boolean isOldFormat = args[1].matches("^[0-9]*$");
						if (isOldFormat) {
							event.getChannel().sendMessage(EmoteReference.WARNING + "Now you don't need to specify the page number. Please use ~>yandere tags <tag>").queue();
							return;
						}

						String url1 = String.format(YANDERE_BASE + "page=%2s&tags=%3s", String.valueOf(page), tagsEncoded).replace(" ", "");
						channel.sendMessage(getImage(argCount, "tags", url1, rating, args, event).build()).queue();
						break;
					case "":
						int randomPage = r.nextInt(5);
						String url2 = String.format(YANDERE_BASE + "&page=%2s", String.valueOf(randomPage)).replace(" ", "");
						channel.sendMessage(getImage(argCount, "random", url2, rating, args, event).build()).queue();
						break;
					default:
						thiz.onHelp(event);
						break;
				}
			})
			.help((thiz, event) -> thiz.helpEmbed(event, "Yande.re command")
				.setColor(Color.DARK_GRAY)
				.setDescription("This command fetches images from the image board **yande.re**. Normally used to store *NSFW* images, "
					+ "but tags can be set to safe if you so desire.\n"
					+ "~>yandere: Gets you a completely random image.\n"
					+ "~>yandere get <imagenumber> <rating>: Gets you an image with the specified parameters.\n"
					+ "~>yandere tags <tag> <rating> <imagenumber>: Gets you an image with the respective tag and specified parameters.\n"
					+ "This command can be only used in NSFW channels! (Unless rating has been specified as safe)\n"
					+ "> Parameter explanation:\n"
					+ "imagenumber: (OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.\n"
					+ "tag: Any valid image tag. For example animal_ears or yuri. (only one tag, spaces are separated by underscores)\n"
					+ "rating: (OPTIONAL) Can be either safe, questionable or explicit, depends on the type of image you want to get.")
				.build())
			.build());
	}
}
