package net.kodehawa.mantarobot.cmd;

import com.marcomaldonado.konachan.entities.Tag;
import com.marcomaldonado.konachan.entities.Wallpaper;
import com.marcomaldonado.konachan.service.Konachan;
import com.marcomaldonado.web.callback.WallpaperCallback;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.module.SimpleCommand;
import net.kodehawa.mantarobot.util.GeneralUtils;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Image extends Module {

	private String YANDERE_BASE = "https://yande.re/post.json?limit=60&";
	private BidiMap<String, String> nRating = new DualHashBidiMap<>();
	private boolean needRating = false;
	private int number = 1;
	private int number1;
	private int page = 0;
	private String rating;
	private boolean smallRequest = false;
	private String tagsEncoded = "";
	private String tagsToEncode = "no";

	public Image() {
		super(Category.MISC);
		this.registerCommands();
		enterRatings();
	}

	@Override
	public void registerCommands() {
		super.register("yandere", "", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				rating = "s";
				if (args.length >= 4) needRating = true;
				if (args.length <= 2) smallRequest = true;
				User author = event.getAuthor();
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
						channel.sendMessage(":hourglass: " + author.getName() + " | Fetching data from yandere...").queue(
							sentMessage ->
							{
								String url = String.format(YANDERE_BASE + "page=%2s", String.valueOf(page)).replace(" ", "");
								sentMessage.editMessage(getImage(argscnt, "get", url, rating, args, event)).queue();
							});
						break;
					case "tags":
						channel.sendMessage(":hourglass: " + author.getName() + " | Fetching data from yandere...").queue(
							sentMessage ->
							{
								String url = String.format(YANDERE_BASE + "page=%2s&tags=%3s", String.valueOf(page), tagsEncoded).replace(" ", "");
								sentMessage.editMessage(getImage(argscnt, "tags", url, rating, args, event)).queue();
							});
						break;
					case "":
						Random r = new Random();
						int randomPage = r.nextInt(4);

						channel.sendMessage(":hourglass: " + author.getName() + " | Fetching data from yandere...").queue(
							sentMessage ->
							{
								String url = String.format(YANDERE_BASE + "&page=%2s", String.valueOf(randomPage)).replace(" ", "");
								sentMessage.editMessage(getImage(argscnt, "random", url, rating, args, event)).queue();
							});
						break;
					default:
						channel.sendMessage(help()).queue();
						break;
				}
			}

			private String getImage(int argcount, String requestType, String url, String rating, String[] messageArray, GuildMessageReceivedEvent event) {
				boolean trigger = false;
				String rating1 = "";
				CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<>();
				JSONArray fetchedData = GeneralUtils.instance().getJSONArrayFromUrl(url, event);
				for (int i = 0; i < fetchedData.length(); i++) {
					JSONObject entry = fetchedData.getJSONObject(i);
					if (entry.getString("rating").equals(rating)) {
						urls.add(entry.getString("file_url"));
					}
				}

				if (needRating) {
					BidiMap<String, String> iRating = nRating.inverseBidiMap();
					rating1 = iRating.get(rating);
				}

				int get = 1;
				try {
					if (requestType.equals("tags")) {
						if (argcount >= 4) {
							get = number;
						} else {
							Random r = new Random();
							int random = r.nextInt(urls.size());
							if (random >= 1) {
								get = random;
							}
						}
					}
					if (requestType.equals("get")) {
						System.out.println(argcount);
						if (argcount >= 2) {
							get = Integer.parseInt(messageArray[2]);
						} else {
							Random r = new Random();
							int random = r.nextInt(urls.size());
							if (random >= 1) {
								get = random;
							}
						}
					}
				} catch (Exception ignored) {
				}

				List<TextChannel> array = event.getJDA().getTextChannels();
				for (MessageChannel ch : array) {
					try {
						if (ch.getName().contains(Parameters.getNSFWChannelForServer(event.getGuild().getId())) && event.getChannel().getId().equals(ch.getId())) {
							trigger = true;
							break;
						} else if (rating.equals("s")) {
							trigger = true;
						}
					} catch (NullPointerException e) {
						return ":heavy_multiplication_x: No NSFW channel set for this server.";
					}

				}

				if (trigger) {
					if (!smallRequest) {
						try {
							return String.format(":mag_right: " + event.getAuthor().getAsMention() + " I found an image with rating: **" + rating1 + "** and tag: **" + tagsToEncode + "** | You can get a total of **%1s images**.\n %2s", urls.size(), urls.get(get - 1));
						} catch (ArrayIndexOutOfBoundsException ex) {
							return ":heavy_multiplication_x: There are no images here, just dust.";
						}
					} else {
						try {
							return String.format(":mag_right: " + event.getAuthor().getAsMention() + " I found an image | You can get a total of **%1s images**.\n %2s", urls.size(), urls.get(get - 1));
						} catch (ArrayIndexOutOfBoundsException ex) {
							return ":heavy_multiplication_x: There are no images here, just dust.";
						}
					}
				} else {
					return ":heavy_multiplication_x: " + "You only can use this command with explicit images in nsfw channels!";
				}
			}

			@Override
			public String help() {
				return "This command fetches images from the image board **yande.re**. Normally used to store *NSFW* images, "
					+ "but tags can be set to safe if you so desire.\n"
					+ "~>yandere: Gets you a completely random image.\n"
					+ "~>yandere get [page] [imagenumber] [rating]: Gets you an image with the specified parameters.\n"
					+ "~>yandere tags page [tag] [rating] [imagenumber]: Gets you an image with the respective tag and specified parameters.\n"
					+ "This command can be only used in NSFW channels! (Unless rating has been specified as safe)\n"
					+ "> Parameter explanation:\n"
					+ "[page]: Can be any value from 1 to the yande.re maximum page. Probably around 4000.\n"
					+ "[imagenumber]: (OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.\n"
					+ "[tag]: Any valid image tag. For example animal_ears or yuri."
					+ "[rating]: (OPTIONAL) Can be either safe, questionable or explicit, depends on the type of image you want to get.\n";
			}

		});

		super.register("konachan", "Retrieves images from konachan", new SimpleCommand() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();

				String noArgs = content.split(" ")[0];
				switch (noArgs) {
					case "get":
						channel.sendTyping().queue();
						CopyOnWriteArrayList<String> images = new CopyOnWriteArrayList<>();
						Konachan konachan = new Konachan(true);
						String whole1 = content.replace("get ", "");
						String[] wholeBeheaded = whole1.split(" ");
						int page = Integer.parseInt(wholeBeheaded[0]);
						int number;
						try {
							number = Integer.parseInt(wholeBeheaded[1]);
						} catch (Exception e) {
							number = 1;
						}

						Wallpaper[] wallpapers = konachan.posts(page, 60);
						for (Wallpaper wallpaper : wallpapers) {
							images.add(wallpaper.getJpeg_url());
						}

						try {
							String toSend = String.format("%s Image found! You can get a total of **%d images** on this page.\n %s", ":mag_right:", images.size(), "http:" + images.get(number - 1));
							channel.sendMessage(toSend).queue();
						} catch (ArrayIndexOutOfBoundsException exception) {
							channel.sendMessage(":heavy_multiplication_x: " + "There aren't more images! Try with a lower number.").queue();
						}
						break;
					case "tags":
						channel.sendTyping().queue();
						CopyOnWriteArrayList<String> images1 = new CopyOnWriteArrayList<>();
						Konachan konachan1 = new Konachan(true);
						String whole11 = content.replace("tags ", "");
						String[] whole2 = whole11.split(" ");
						int page1 = Integer.parseInt(whole2[0]);
						String tags = whole2[1];
						try {
							number1 = Integer.parseInt(whole2[2]);
						} catch (Exception e) {
							number1 = 1;
						}

						konachan1.search(page1, 60, tags, new WallpaperCallback() {
							public void onStart() {
							}

							public void onFailure(int error, String message) {
							}

							public void onSuccess(Wallpaper[] wallpapers, Tag[] tags) {
								for (Wallpaper wallpaper : wallpapers) {
									images1.add(wallpaper.getJpeg_url());
								}
								try {
									String toSend = String.format("%s Image found with tags **%s**. You can get a total of **%d images** in this page.\n %s", ":mag_right:", whole2[1], images1.size(), "http:" + images1.get(number1 - 1));
									channel.sendMessage(toSend).queue();
								} catch (ArrayIndexOutOfBoundsException exception) {
									channel.sendMessage(":heavy_multiplication_x: " + "There aren't more images! Try with a lower number.").queue();
								}
							}
						});
						break;
					default:
						channel.sendMessage(help()).queue();
						break;
				}
			}

			@Override
			public String help() {
				return "Retrieves images from the **Konachan** image board.\n"
					+ "Usage:\n"
					+ "~>konachan get [page] [imagenumber]: Gets an image based in parameters.\n"
					+ "~>konachan tags [page] [tag] [imagenumber]: Gets an image based in the specified tag and parameters.\n"
					+ "> Parameter explanation:\n"
					+ "[page]: Can be any value from 1 to the yande.re maximum page. Probably around 4000.\n"
					+ "[imagenumber]: (OPTIONAL) Any number from 1 to the maximum possible images to get, specified by the first instance of the command.\n"
					+ "[tag]: Any valid image tag. For example animal_ears or original.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}

	private void enterRatings() {
		nRating.put("safe", "s");
		nRating.put("questionable", "q");
		nRating.put("explicit", "e");
	}
}