package net.kodehawa.mantarobot.cmd;

import java.util.concurrent.CopyOnWriteArrayList;

import com.marcomaldonado.konachan.entities.Tag;
import com.marcomaldonado.konachan.entities.Wallpaper;
import com.marcomaldonado.konachan.service.Konachan;
import com.marcomaldonado.web.callback.WallpaperCallback;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.management.Command;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;

public class Kona extends Module {
	private int number1;

	public Kona()
	{
		this.registerCommands();
	}
	
	@Override
	public void registerCommands(){
		super.register("konachan", "Retrieves images from konachan", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();

				String noArgs = content.split(" ")[0];
				switch(noArgs){
					case "get":
						channel.sendTyping().queue();
						CopyOnWriteArrayList<String> images = new CopyOnWriteArrayList<>();
						Konachan konachan = new Konachan(true);
						String whole1 = content.replace("get ", "");
						String[] wholeBeheaded = whole1.split(" ");
						int page = Integer.parseInt(wholeBeheaded[0]);
						int number;
						try{
							number = Integer.parseInt(wholeBeheaded[1]);
						} catch(Exception e){
							number = 1;
						}

						Wallpaper[] wallpapers = konachan.posts(page, 60);
						for(Wallpaper wallpaper : wallpapers) {
							images.add(wallpaper.getJpeg_url());
						}

						try{
							String toSend = String.format("%s Image found! You can get a total of **%d images// in this page.\n %s", ":mag_right:", images.size(), "http:" + images.get(number-1));
							channel.sendMessage(toSend).queue();
						} catch(ArrayIndexOutOfBoundsException exception){
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
						try{
							number1 = Integer.parseInt(whole2[2]);
						} catch (Exception e){
							number1 = 1;
						}

						konachan1.search(page1, 60, tags, new WallpaperCallback() {
							public void onSuccess(Wallpaper[] wallpapers, Tag[] tags) {
								for(Wallpaper wallpaper : wallpapers) {
									images1.add(wallpaper.getJpeg_url());
								}
								try{
									String toSend = String.format("%s Image found with tags **%s**. You can get a total of **%d images** in this page.\n %s", ":mag_right:", whole2[1], images1.size(), "http:" + images1.get(number1-1));
									channel.sendMessage(toSend).queue();
								}
								catch(ArrayIndexOutOfBoundsException exception){
									channel.sendMessage(":heavy_multiplication_x: " + "There aren't more images! Try with a lower number.").queue();
								}
							}
							public void onStart() {}
							public void onFailure(int error, String message) {}
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
}
