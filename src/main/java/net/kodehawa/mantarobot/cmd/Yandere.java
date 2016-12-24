package net.kodehawa.mantarobot.cmd;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.util.Utils;

public class Yandere extends Module {
	
    private int number = 1;
	private int page = 0;
	private String tagsToEncode = "no";
	private String tagsEncoded = "";
	private boolean needRating = false;
	private boolean smallRequest = false;
	private String rating;

	private BidiMap<String, String> nRating = new DualHashBidiMap<>();
	
	public Yandere()
	{
		this.registerCommands();
		enterRatings();
	}

	@Override
	public void registerCommands(){
		super.register("yandere", "", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				rating = "s";
				if(args.length >= 4) needRating = true;
				if(args.length <= 2) smallRequest = true;
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				int argscnt = args.length - 1;

				try{
					page = Integer.parseInt(args[1]);
					tagsToEncode = args[2];
					if(needRating) rating = nRating.get(args[3]);
					System.out.println(rating);
					number = Integer.parseInt(args[4]);
				} catch(Exception ignored){}

				try {
					tagsEncoded = URLEncoder.encode(tagsToEncode, "UTF-8");
				} catch (UnsupportedEncodingException ignored){} //Shouldn't happen.

				String noArgs = content.split(" ")[0];
				switch(noArgs){
					case "get":
						channel.sendMessage(":hourglass: " + author.getName() + " | Fetching data from yandere...").queue(
								sentMessage ->
								{
									String url = String.format("https://yande.re/post.json?limit=60&page=%2s", String.valueOf(page)).replace(" ", "");
									sentMessage.editMessage(getImage(argscnt, "get", url, rating, args, event)).queue();
								});
						break;
					case "tags":
						channel.sendMessage(":hourglass: " + author.getName() + " | Fetching data from yandere...").queue(
								sentMessage ->
								{
									String url = String.format("https://yande.re/post.json?limit=60&page=%2s&tags=%3s",
											String.valueOf(page), tagsEncoded).replace(" ", "");
									sentMessage.editMessage(getImage(argscnt, "tags", url, rating, args, event)).queue();
								});
						break;
					case "":
						Random r = new Random();
						int randomPage = r.nextInt(4);

						channel.sendMessage(":hourglass: " + author.getName() + " | Fetching data from yandere...").queue(
								sentMessage ->
								{
									String url = String.format("https://yande.re/post.json?limit=60&page=%2s",
											String.valueOf(randomPage)).replace(" ", "");
									sentMessage.editMessage(getImage(argscnt, "random", url, rating, args, event)).queue();
								});
						break;
					default:
						channel.sendMessage(help()).queue();
						break;
				}
			}

			private String getImage(int argcount, String requestType, String url, String rating, String[] messageArray, MessageReceivedEvent evt){
				boolean trigger = false;
				String rating1 = "";
				CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<>();
				JSONArray fetchedData = Utils.instance().getJSONArrayFromUrl(url, evt);
				for(int i = 0; i < fetchedData.length(); i++)  {
					JSONObject entry = fetchedData.getJSONObject(i);
					if(entry.getString("rating").equals(rating)){
						urls.add(entry.getString("file_url"));
					}
				}

				if(needRating){
					BidiMap<String, String> iRating = nRating.inverseBidiMap();
					rating1 = iRating.get(rating);
				}

				int get = 1;
				try{
					if(requestType.equals("tags")){
						if(argcount >= 4){
							get = number;
						}
						else {
							Random r = new Random();
							int random = r.nextInt(urls.size());
							if(random >= 1){ get = random; }
						}
					}
					if(requestType.equals("get")){
						System.out.println(argcount);
						if(argcount >= 2){
							get = Integer.parseInt(messageArray[2]);
						} else {
							Random r = new Random();
							int random = r.nextInt(urls.size());
							if(random >= 1){ get = random; }
						}
					}
				} catch(Exception ignored){}

				List<TextChannel> array = channel.getJDA().getTextChannels();
				for(MessageChannel ch : array) {
					try{
						if(ch.getName().contains(Parameters.getNSFWChannelForServer(guild.getId())) && channel.getId().equals(ch.getId())){
							trigger = true;
							break;
						} else if(rating.equals("s")){
							trigger = true;
						}
					} catch(NullPointerException e){
						return ":heavy_multiplication_x: No NSFW channel set for this server.";
					}

				}

				if(trigger) {
					if(!smallRequest){
						try{
							return String.format(":mag_right: " + evt.getAuthor().getAsMention() + " I found an image with rating: **" + rating1 + "** and tag: **" + tagsToEncode + "** | You can get a total of **%1s images**.\n %2s" , urls.size(), urls.get(get - 1));
						} catch(ArrayIndexOutOfBoundsException ex){
							return ":heavy_multiplication_x: There are no images here, just dust.";
						}
					} else {
						try{
							return String.format(":mag_right: " + evt.getAuthor().getAsMention() + " I found an image | You can get a total of **%1s images**.\n %2s" , urls.size(), urls.get(get - 1));
						} catch(ArrayIndexOutOfBoundsException ex){
							return ":heavy_multiplication_x: There are no images here, just dust.";
						}
					}
				} else{
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

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}
	
	private void enterRatings(){
		nRating.put("safe", "s");
		nRating.put("questionable", "q");
		nRating.put("explicit", "e");
	}
}