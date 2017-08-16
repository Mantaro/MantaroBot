package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.collections.CollectionUtils;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.imageboards.ImageboardAPI;
import net.kodehawa.lib.imageboards.entities.Furry;
import net.kodehawa.lib.imageboards.entities.Hentai;
import net.kodehawa.lib.imageboards.entities.Wallpaper;
import net.kodehawa.lib.imageboards.entities.YandereImage;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.json.JSONObject;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
public class ImageCmds {

	private final BidiMap<String, String> nRating = new DualHashBidiMap<>();
	private final Random r = new Random();

	private final URLCache CACHE = new URLCache(20);

	private static final String[] responses = {"Aww, take a cat.", "%mention%, are you sad? ;w;, take a cat!", "You should all have a cat in your life, but a image will do.",
			"Am I cute yet?", "%mention%, I think you should have a cat."};

	private final String BASEURL = "http://catgirls.brussell98.tk/api/random";
	private final String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random"; //this actually returns more questionable images than explicit tho

	private final ImageboardAPI<Furry> e621 = new ImageboardAPI<>(ImageboardAPI.Boards.E621, ImageboardAPI.Type.JSON, Furry[].class);
	private final ImageboardAPI<Hentai> rule34 = new ImageboardAPI<>(ImageboardAPI.Boards.R34, ImageboardAPI.Type.XML, Hentai[].class);
	private final ImageboardAPI<YandereImage> yandere = new ImageboardAPI<>(ImageboardAPI.Boards.YANDERE, ImageboardAPI.Type.JSON, YandereImage[].class);
	private final ImageboardAPI<Wallpaper> konachan = new ImageboardAPI<>(ImageboardAPI.Boards.KONACHAN, ImageboardAPI.Type.JSON, Wallpaper[].class);

	@Subscribe
	public void cat(CommandRegistry cr) {
		cr.register("cat", new SimpleCommand(Category.IMAGE) {
			final OkHttpClient httpClient = new OkHttpClient();

			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				try {
					Request r = new Request.Builder()
							.url("http://random.cat/meow")
							.build();

					Response response = httpClient.newCall(r).execute();

					String url = new JSONObject(response.body().string()).getString("file");
					response.close();
					event.getChannel().sendFile(CACHE.getFile(url), "cat.jpg",
							new MessageBuilder().append(CollectionUtils.random(responses).replace("%mention%", event.getAuthor().getAsMention())).build()).queue();
				} catch(Exception e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Error retrieving cute cat images :<").queue();
					e.printStackTrace();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Cat command")
						.setDescription("Sends a random cat image.")
						.build();
			}
		});
	}

	@Subscribe
	public void catgirls(CommandRegistry cr) {
		cr.register("catgirl", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");
				if(nsfw && !nsfwCheck(event, true, true, null)) return;

				try {
					JSONObject obj = new JSONObject(Utils.wgetResty(nsfw ? NSFWURL : BASEURL, event));
					if(!obj.has("url")) {
						event.getChannel().sendMessage("Unable to find image.").queue();
					} else {
						event.getChannel().sendFile(CACHE.getInput(obj.getString("url")), "catgirl.png", null).queue();
					}
				} catch(Exception e) {
					e.printStackTrace();
					event.getChannel().sendMessage("Unable to get image.").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Catgirl command")
						.setDescription("**Sends catgirl images**")
						.addField("Usage", "`~>catgirl` - **Returns catgirl images.**" +
								"\n´`~>catgirl nsfw` - **Returns lewd or questionable cargirl images.**", false)
						.build();
			}
		});
	}

	@Subscribe
	public void e621(CommandRegistry cr) {
		cr.register("e621", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if(!nsfwCheck(event, true, true, null)) return;
				TextChannelGround.of(event).dropItemWithChance(13, 3);

				String noArgs = content.split(" ")[0];
				switch(noArgs) {
					case "get":
						try {
							String whole1 = content.replace("get ", "");
							String[] wholeBeheaded = whole1.split(" ");
							e621.get(image1 -> {
								try {
									int number;
									try {
										number = Integer.parseInt(wholeBeheaded[0]);
									} catch(Exception e) {
										number = r.nextInt(image1.size());
									}

									Furry image = image1.get(number);
									String TAGS = image.getTags().replace(" ", " ,");
									if(foundMinorTags(event, TAGS, null)) return;
									EmbedBuilder builder = new EmbedBuilder();
									builder.setAuthor("Found image", null, image.getFile_url())
											.setImage(image.getFile_url())
											.addField("Width", String.valueOf(image.getWidth()), true)
											.addField("Height", String.valueOf(image.getHeight()), true)
											.addField("Tags", "`" + (TAGS == null ? "None" : TAGS) + "`", false)
											.setFooter("If the image doesn't load, click the title.", null);

									event.getChannel().sendMessage(builder.build()).queue();
								} catch(IndexOutOfBoundsException | IllegalArgumentException e) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "**There aren't more images or no results found**! Try with a lower number.").queue();
								}
							});

						} catch(Exception exception) {
							if(exception instanceof NumberFormatException)
								event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help e621").queue(
										message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
								);
						}
						break;

					case "tags":
						try {
							String sNoArgs = content.replace("tags ", "");
							String[] expectedNumber = sNoArgs.split(" ");
							String tags = expectedNumber[0];
							e621.onSearch(tags, images -> {
								try {
									int number1;
									try {
										number1 = Integer.parseInt(expectedNumber[1]);
									} catch(Exception e) {
										number1 = r.nextInt(images.size() > 0 ? images.size() - 1 : images.size());
									}

									Furry image = images.get(number1);
									String TAGS = image.getTags().replace(" ", " ,");
									if(foundMinorTags(event, TAGS, null)) return;

									EmbedBuilder builder = new EmbedBuilder();
									builder.setAuthor("Found image", null, image.getFile_url())
											.setImage(image.getFile_url())
											.addField("Width", String.valueOf(image.getWidth()), true)
											.addField("Height", String.valueOf(image.getHeight()), true)
											.addField("Tags", "``" + (TAGS == null ? "None" : TAGS) + "``", false)
											.setFooter("If the image doesn't load, click the title.", null);

									event.getChannel().sendMessage(builder.build()).queue();
								} catch(IndexOutOfBoundsException | IllegalArgumentException e) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "**There aren't any more images or no results found**! Try with a lower number.").queue();
								}
							});
						} catch(Exception exception) {
							if(exception instanceof NumberFormatException)
								event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help e621").queue(
										message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
								);
						}
						break;
					default:
						onHelp(event);
						break;
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "e621 commmand")
						.setColor(Color.PINK)
						.setDescription("**Retrieves images from the e621 (furry) image board.**")
						.addField("Usage",
								"`~>e621 get <imagenumber>` - **Gets an image based in parameters.**\n"
										+ "`~>e621 tags <tag> <imagenumber>` - **Gets an image based in the specified tag and parameters.**", false)
						.addField("Parameters",
								"`page` - **Can be any value from 1 to the e621 maximum page. Probably around 4000.**\n"
										+ "`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
										+ "`tag` - **Any valid image tag. For example animal_ears or original.**", false)
						.build();
			}
		});
	}

	@Subscribe
	public void kona(CommandRegistry cr) {
		cr.register("konachan", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				TextChannel channel = event.getChannel();

				String noArgs = content.split(" ")[0];
				switch(noArgs) {
					case "get":
						try {
							String whole1 = content.replace("get ", "");
							String[] wholeBeheaded = whole1.split(" ");
							konachan.get(images -> {
								int number;
								List<Wallpaper> wallpapers = images.stream().filter(data -> data.getRating().equals("s")).collect(Collectors.toList());
								try {
									number = Integer.parseInt(wholeBeheaded[0]);
								} catch(Exception e) {
									number = r.nextInt(wallpapers.size());
								}

								Wallpaper wallpaper = wallpapers.get(number);
								String AUTHOR = wallpaper.getAuthor();
								String TAGS = wallpaper.getTags().stream().collect(Collectors.joining(", "));

								EmbedBuilder builder = new EmbedBuilder();
								builder.setAuthor("Found image", "https:" + wallpaper.getJpeg_url(), null)
										.setDescription("Image uploaded by: " + (AUTHOR == null ? "not found" : AUTHOR))
										.setImage("https:" + wallpaper.getJpeg_url())
										.addField("Width", String.valueOf(wallpaper.getWidth()), true)
										.addField("Height", String.valueOf(wallpaper.getHeight()), true)
										.addField("Tags", "`" + (TAGS == null ? "None" : TAGS) + "`", false)
										.setFooter("If the image doesn't load, click the title.", null);

								channel.sendMessage(builder.build()).queue();
							});
						} catch(Exception exception) {
							if(exception instanceof NumberFormatException)
								channel.sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help konachan").queue(
										message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
								);
							if(exception instanceof IndexOutOfBoundsException || exception instanceof IllegalArgumentException)
								channel.sendMessage(EmoteReference.ERROR + "There aren't more images! Try with a lower number.").queue();
						}
						break;
					case "tags":
						try {
							String sNoArgs = content.replace("tags ", "");
							String[] expectedNumber = sNoArgs.split(" ");
							String tags = expectedNumber[0];
							konachan.onSearch(tags, wallpapers1 -> {
								List<Wallpaper> filter = wallpapers1.stream().filter(data -> data.getRating().equals("s")).collect(Collectors.toList());
								int number1;
								try {
									number1 = Integer.parseInt(expectedNumber[1]);
								} catch(Exception e) {
									number1 = r.nextInt(filter.size() > 0 ? filter.size() - 1 : filter.size());
								}

								Wallpaper wallpaper = filter.get(number1);
								String TAGS1 = wallpaper.getTags().stream().collect(Collectors.joining(", "));

								EmbedBuilder builder = new EmbedBuilder();
								builder.setAuthor("Found image", "https:" + wallpaper.getJpeg_url(), null)
										.setDescription("Image uploaded by: " + (wallpaper.getAuthor() == null ? "not found" : wallpaper.getAuthor()))
										.setImage("https:" + wallpaper.getJpeg_url())
										.addField("Width", String.valueOf(wallpaper.getWidth()), true)
										.addField("Height", String.valueOf(wallpaper.getHeight()), true)
										.addField("Tags", "`" + (TAGS1 == null ? "None" : TAGS1) + "`", false)
										.setFooter("If the image doesn't load, click the title.", null);

								channel.sendMessage(builder.build()).queue();
							});
						} catch(Exception exception) {
							if(exception instanceof NumberFormatException)
								channel.sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help konachan").queue(
										message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
								);

							if(exception instanceof IndexOutOfBoundsException || exception instanceof IllegalArgumentException) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "**There aren't any more images or no results found**! Try with a lower number.").queue();
								return;
							}
						}
						break;
					default:
						onHelp(event);
						break;
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Konachan commmand")
						.setColor(Color.PINK)
						.setDescription("**Retrieves images from the Konachan image board.**")
						.addField("Usage",
								"`~>konachan get <page> <imagenumber>` - **Gets an image based in parameters.**\n"
										+ "`~>konachan tags <tag> <imagenumber>` - **Gets an image based in the specified tag and parameters.**\n", false)
						.addField("Parameters",
								"`page` - **Can be any value from 1 to the Konachan maximum page. Probably around 4000.**\n"
										+ "`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
										+ "`tag` - **Any valid image tag. For example animal_ears or original.**", false)
						.build();
			}
		});
	}

	@Subscribe
	public void rule34(CommandRegistry cr) {
		cr.register("rule34", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!nsfwCheck(event, true, true, null)) return;

				String noArgs = content.split(" ")[0];
				TextChannelGround.of(event).dropItemWithChance(13, 3);
				switch (noArgs) {
					case "get":
						try {
							String whole1 = content.replace("get ", "");
							String[] wholeBeheaded = whole1.split(" ");
							rule34.get(images -> {
								try {
									int number;
									try {
										number = Integer.parseInt(wholeBeheaded[0]);
									} catch (Exception e) {
										number = r.nextInt(images.size());
									}
									Hentai image = images.get(number);
									String TAGS = image.getTags().replace(" ", " ,");
									if(foundMinorTags(event, TAGS, null)) return;
									EmbedBuilder builder = new EmbedBuilder();
									builder.setAuthor("Found image", image.getFile_url(), null)
											.setImage(image.getFile_url())
											.addField("Width", String.valueOf(image.getWidth()), true)
											.addField("Height", String.valueOf(image.getHeight()), true)
											.addField("Tags", "``" + (TAGS == null ? "None" : TAGS) + "``", false)
											.setFooter("If the image doesn't load, click the title.", null);

									event.getChannel().sendMessage(builder.build()).queue();
								} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "**There aren't any more images or no results found**! Try with a lower number.").queue();
								}
							});

						} catch (Exception e) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong argument type (or we couldn't complete the query? Maybe try another one?, try again or check ~>help rule34)").queue(
									message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
						}
						break;
					case "tags":
						try {
							String sNoArgs = content.replace("tags ", "");
							String[] expectedNumber = sNoArgs.split(" ");
							String tags = expectedNumber[0];

							rule34.onSearch(tags, images -> {
								try {
									int number1;
									try {
										number1 = Integer.parseInt(expectedNumber[2]);
									} catch (Exception e) {
										number1 = r.nextInt(images.size() > 0 ? images.size() - 1 : images.size());
									}

									Hentai image = images.get(number1);
									String tags1 = image.getTags() == null ? tags : image.getTags();
									if(foundMinorTags(event, tags1, null)) return;

									if(tags1.length() > 980) tags1 = tags1.substring(0, 980) + "...";

									EmbedBuilder builder = new EmbedBuilder();
									builder.setAuthor("Found image", image.getFile_url(), null)
											.setImage(image.getFile_url())
											.addField("Width", String.valueOf(image.getWidth()), true)
											.addField("Height", String.valueOf(image.getHeight()), true)
											.addField("Tags",  "`" + tags1  + "`", false)
											.setFooter("If the image doesn't load, click the title.", null);

									event.getChannel().sendMessage(builder.build()).queue();
								} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "**There aren't any more images or no results found**! Please try with a lower " +
											"number or another search.").queue();
								}

							});
						} catch (Exception e) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong argument type (or we couldn't complete the query? Maybe try another one?, try again or check ~>help rule34)").queue(
									message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
						}
						break;
					default:
						onHelp(event);
						break;
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "rule34.xxx commmand")
						.setColor(Color.PINK)
						.setDescription("**Retrieves images from the rule34 (hentai) image board.**")
						.addField("Usage", "`~>rule34 get <imagenumber>` - **Gets an image based in parameters.**\n"
								+ "`~>rule34 tags <tag> <imagenumber>` - **Gets an image based in the specified tag and parameters.**\n", false)
						.addField("Parameters", "`page` - **Can be any value from 1 to the rule34 maximum page. Probably around 4000.**\n"
								+ "`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
								+ "`tag` - **Any valid image tag. For example animal_ears or original.**", false)
						.build();
			}
		});
	}

	@Subscribe
	public void yandere(CommandRegistry cr) {
		cr.register("yandere", new SimpleCommand(Category.IMAGE) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				String rating = "s";
				boolean needRating = args.length >= 3;
				TextChannel channel = event.getChannel();

				try {
					if(needRating) rating = nRating.get(args[2]);
				} catch(Exception e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid rating!").queue();
					return;
				}

				final String fRating = rating;

				if(!nsfwCheck(event, false, false, fRating)){
					event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot send a lewd image in a non-nsfw channel.").queue();
					return;
				}

				String noArgs = content.split(" ")[0];
				switch(noArgs) {
					case "get":
						try {
							String whole1 = content.replace("get ", "");
							String[] wholeBeheaded = whole1.split(" ");

							yandere.get(images1 -> {
								try{
									int number;
									List<YandereImage> images = images1.stream().filter(data -> data.getRating().equals(fRating)).collect(Collectors.toList());
									try {
										number = Integer.parseInt(wholeBeheaded[0]);
									} catch(Exception e) {
										number = r.nextInt(images.size());
									}
									YandereImage image = images.get(number);
									String tags = image.getTags().stream().collect(Collectors.joining(", "));
									String author = image.getAuthor();
									if(foundMinorTags(event, tags, image.rating)){
										return;
									}

									EmbedBuilder builder = new EmbedBuilder();
									builder.setAuthor("Found image", image.getJpeg_url(), null)
											.setDescription("Image uploaded by: **" + (author == null ? "not found" : author + "** with a rating of " + nRating.getKey(image.rating)))
											.setImage(image.getJpeg_url())
											.addField("Width", String.valueOf(image.getWidth()), true)
											.addField("Height", String.valueOf(image.getHeight()), true)
											.addField("Tags", "`" + (tags == null ? "None" : tags) + "`", false)
											.setFooter("If the image doesn't load, click the title.", null);

									channel.sendMessage(builder.build()).queue();
									TextChannelGround.of(event).dropItemWithChance(13, 3);
								} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "**There aren't any more images or no results found**! Please try with a lower " +
											"number or another search.").queue();
								}
							});
						} catch(Exception exception) {
							if(exception instanceof NumberFormatException)
								channel.sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help yandere").queue(
										message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
								);
						}
						break;
					case "tags":
						try {
							String sNoArgs = content.replace("tags ", "");
							String[] expectedNumber = sNoArgs.split(" ");
							String tags = expectedNumber[0];

							yandere.onSearch(tags, images -> {
								try{
									List<YandereImage> filter = images.stream().filter(data -> data.getRating().equals(fRating)).collect(Collectors.toList());
									int number1;
									try {
										number1 = Integer.parseInt(expectedNumber[1]);
									} catch(Exception e) {
										number1 = r.nextInt(filter.size() > 0 ? filter.size() - 1 : filter.size());
									}
									YandereImage image = filter.get(number1);
									String tags1 = image.getTags().stream().collect(Collectors.joining(", "));

									if(foundMinorTags(event, tags1, image.rating)){
										return;
									}
									String author = image.getAuthor();

									EmbedBuilder builder = new EmbedBuilder();
									builder.setAuthor("Found image", image.getJpeg_url(), null)
											.setDescription("Image uploaded by: **" + (author == null ? "not found" : author + "** with a rating of " + nRating.getKey(image.rating)))
											.setImage(image.getJpeg_url())
											.addField("Width", String.valueOf(image.getWidth()), true)
											.addField("Height", String.valueOf(image.getHeight()), true)
											.addField("Tags", "`" + (tags1 == null ? "None" : tags1) + "`", false)
											.setFooter("If the image doesn't load, click the title.", null);

									channel.sendMessage(builder.build()).queue();
									TextChannelGround.of(event).dropItemWithChance(13, 3);
								} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
									event.getChannel().sendMessage(EmoteReference.ERROR + "**There aren't any more images or no results found**! Please try with a lower " +
											"number or another search.").queue();
								}
							});
						} catch(Exception exception) {
							if(exception instanceof NumberFormatException)
								channel.sendMessage(EmoteReference.ERROR + "Wrong argument type. Check ~>help yandere").queue(
										message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
								);

							exception.printStackTrace();
						}
						break;
					case "":
						yandere.get(images -> {
							List<YandereImage> filter = images.stream().filter(data -> data.getRating().equals(fRating)).collect(Collectors.toList());
							int number = r.nextInt(filter.size());
							YandereImage image = filter.get(number);
							String AUTHOR = image.getAuthor();
							String TAGS = image.getTags().stream().collect(Collectors.joining(", "));
							EmbedBuilder builder = new EmbedBuilder();
							builder.setAuthor("Found image", image.getJpeg_url(), null)
									.setDescription("Image uploaded by: **" + (AUTHOR == null ? "not found" : AUTHOR + "** with a rating of " + nRating.getKey(image.rating)))
									.setImage(image.getJpeg_url())
									.addField("Width", String.valueOf(image.getWidth()), true)
									.addField("Height", String.valueOf(image.getHeight()), true)
									.addField("Tags", "`" + (TAGS == null ? "None" : TAGS) + "`", false)
									.setFooter("If the image doesn't load, click the title.", null);

							channel.sendMessage(builder.build()).queue();
							TextChannelGround.of(event).dropItemWithChance(13, 3);
						});
						break;
					default:
						onHelp(event);
						break;
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Yande.re command")
						.setColor(Color.DARK_GRAY)
						.setDescription("**This command fetches images from the image board yande.re. Normally used to store NSFW images, "
								+ "but tags can be set to safe if you so desire.**")
						.addField("Usage",
								"`~>yandere` - **Gets you a completely random image.**\n"
										+ "`~>yandere get <page> <rating> <imagenumber>` - **Gets you an image with the specified parameters.**\n"
										+ "`~>yandere tags <tag> <rating> <imagenumber>` - **Gets you an image with the respective tag and specified parameters.**\n\n"
										+ "**WARNING**: This command can be only used in NSFW channels! (Unless rating has been specified as safe or not specified at all)", false)
						.addField("Parameters",
								"`imagenumber` - **(OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.**\n"
										+ "`tag` - **Any valid image tag. For example animal_ears or yuri. (only one tag, spaces are separated by underscores)**\n"
										+ "`rating` - **(OPTIONAL) Can be either safe, questionable or explicit, depends on the type of image you want to get.**", false)
						.build();
			}
		});
	}

	private boolean nsfwCheck(GuildMessageReceivedEvent event, boolean isGlobal, boolean sendMessage, String rating) {
		if(event.getChannel().isNSFW()) return true;

		String nsfwChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildUnsafeChannels().stream()
				.filter(channel -> channel.equals(event.getChannel().getId())).findFirst().orElse(null);
		String rating1 = rating == null ? "s" : rating;
		boolean trigger = !isGlobal ? ((rating1.equals("s") || (nsfwChannel == null)) ? rating1.equals("s") : nsfwChannel.equals(event.getChannel().getId())) :
				nsfwChannel != null && nsfwChannel.equals(event.getChannel().getId());

		if(!trigger) {
			if(sendMessage){
				event.getChannel().sendMessage(EmoteReference.ERROR + "Not on a NSFW channel. Cannot send lewd images.\n" +
						"**Reminder:** You can set this channel as NSFW by doing `~>opts nsfw toggle` if you are an administrator on this server.").queue();
			}
			return false;
		}

		return true;
	}

	private boolean foundMinorTags(GuildMessageReceivedEvent event, String tags, String rating){
		boolean trigger = tags.contains("loli") || tags.contains("lolis") ||
				tags.contains("shota") || tags.contains("shotas") ||
				tags.contains("lolicon") || tags.contains("shotacon") &&
				(rating == null || rating.equals("q") || rating.equals("e"));

		if(!trigger){
			return false;
		}

		event.getChannel().sendMessage(EmoteReference.WARNING + "Sadly we cannot display images that allegedly contain `loli` or `shota` lewd/NSFW content because discord" +
				" prohibits it. (Filter ran: Image contains a loli or shota tag and it's NSFW)").queue();
		return true;
	}

	@Subscribe
	public void onPostLoad(PostLoadEvent e) {
		nRating.put("safe", "s");
		nRating.put("questionable", "q");
		nRating.put("explicit", "e");
	}
}