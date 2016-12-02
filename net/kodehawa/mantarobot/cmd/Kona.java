package net.kodehawa.mantarobot.cmd;

import java.util.concurrent.CopyOnWriteArrayList;

import com.marcomaldonado.konachan.entities.Tag;
import com.marcomaldonado.konachan.entities.Wallpaper;
import com.marcomaldonado.konachan.service.Konachan;
import com.marcomaldonado.web.callback.WallpaperCallback;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Kona extends Command {

	public Kona()
	{
		setName("konachan");
		setCommandType("user");
		setDescription("Retrieves images from konachan. For usage examples see ~>konachan help.");
		setExtendedHelp(
				"Retrieves images from the **Konachan** image board.\r"
				+ "Usage:\r"
				+ "~>konachan get page limit imagenumber: Gets an image based in parameters.\r"
				+ "~>konachan tags page tag imagenumber: Gets an image based in the specified tag and parameters.\r"
				+ "> Parameter explanation:\r"
				+ "*page*: Can be any value from 1 to the yande.re maximum page. Probably around 4000.\r"
				+ "*limit*: Can handle any value from 1 to 60 (values higher than 60 just default to 60)\r"
				+ "*imagenumber*: Any number from 1 to the maximum possible images to get, specified by the first instance of the command.\r"
				+ "*tag*: Any valid image tag. For example animal_ears or original."
				);
	}
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
        
		String noArgs = beheadedMessage.split(" ")[0];
		switch(noArgs){
		case "get":
			CopyOnWriteArrayList<String> images = new CopyOnWriteArrayList<String>();
			Konachan konachan = new Konachan(true);
			String whole1 = beheadedMessage.replace("get ", "");
			String[] wholeBeheaded = whole1.split(" ");
			int page = Integer.parseInt(wholeBeheaded[0]);
			int limit = Integer.parseInt(wholeBeheaded[1]);
			int number = Integer.parseInt(wholeBeheaded[2]);
						
			Wallpaper[] wallpapers = konachan.posts(page, limit);
			for( Wallpaper wallpaper : wallpapers ) {
				images.add(wallpaper.getJpeg_url());
			}
			
			try{
				channel.sendMessage(":thumbsup: " + "Image found! You can get a total of " + String.valueOf(images.size()) + " images in this page.").queue();
				channel.sendMessage(images.get(number-1)).queue();
			}
			catch(ArrayIndexOutOfBoundsException exception){
				channel.sendMessage(":heavy_multiplication_x: " + "There aren't more images! Try with a lower number.").queue();
			}
			break;
		case "tags":
			CopyOnWriteArrayList<String> images1 = new CopyOnWriteArrayList<String>();
			Konachan konachan1 = new Konachan(true);
			String whole11 = beheadedMessage.replace("tags ", "");
			String[] whole2 = whole11.split(" ");
			int page1 = Integer.parseInt(whole2[0]);
			String tags = whole2[1];
			int number1 = Integer.parseInt(whole2[2]);
			
	        konachan1.search(page1, 60, tags, new WallpaperCallback() {
	            public void onSuccess(Wallpaper[] wallpapers, Tag[] tags) {
	                for(Wallpaper wallpaper : wallpapers) {
	                	images1.add(wallpaper.getJpeg_url());
	                }
	                try{
	    				channel.sendMessage(":thumbsup: " + "Image found! You can get a total of " + String.valueOf(images1.size()) + " images in this page.").queue();
	    				channel.sendMessage(images1.get(number1-1)).queue();
	    			}
	    			catch(ArrayIndexOutOfBoundsException exception){
	    				channel.sendMessage(":heavy_multiplication_x: " + "There aren't more images! Try with a lower number.").queue();
	    			}
	            }
	            public void onStart() {}
	            public void onFailure(int error, String message) {}
	         });
	        break;
		}
	}
}
