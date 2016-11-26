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
		setDescription("");
	}
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		
		if(beheadedMessage.startsWith("get"))
		{
	        guild = evt.getGuild();
	        author = evt.getAuthor();
	        channel = evt.getChannel();
	        receivedMessage = evt.getMessage();

			CopyOnWriteArrayList<String> images = new CopyOnWriteArrayList<String>();
			Konachan konachan = new Konachan(true);

			String whole1 = beheadedMessage.replace("get ", "");
			String[] wholeBeheaded = whole1.split(":");
			int page = Integer.parseInt(wholeBeheaded[0]);
			int limit = Integer.parseInt(wholeBeheaded[1]);
			int number = Integer.parseInt(wholeBeheaded[2]);
						
			Wallpaper[] wallpapers = konachan.posts(page, limit);
			for( Wallpaper wallpaper : wallpapers ) {
				images.add(wallpaper.getJpeg_url());
			}
			
			try
			{
				channel.sendMessage(":thumbsup: " + "You can get a total of " + String.valueOf(images.size()) + "images in this page.").queue();
				channel.sendMessage(images.get(number)).queue();
			}
			catch(ArrayIndexOutOfBoundsException exception)
			{
				channel.sendMessage(":heavy_multiplication_x: " + "There aren't more images! Try with a lower number.").queue();
			}
		}
		
		else if(beheadedMessage.startsWith("tags"))
		{
			CopyOnWriteArrayList<String> images1 = new CopyOnWriteArrayList<String>();
			Konachan konachan = new Konachan(true);

			String whole1 = beheadedMessage.replace("tags ", "");
			String[] whole2 = whole1.split(":");
			int page = Integer.parseInt(whole2[0]);
			String tags = whole2[1];
			int number = Integer.parseInt(whole2[2]);
			
	        konachan.search(page, 60, tags, new WallpaperCallback() {
	            public void onSuccess(Wallpaper[] wallpapers, Tag[] tags) {
	                for(Wallpaper wallpaper : wallpapers) {
	                	
	                	images1.add(wallpaper.getJpeg_url());
	                }
	                try
	    			{
	    				channel.sendMessage(":thumbsup: " + "You can get a total of " + String.valueOf(images1.size()) + " images in this page.").queue();
	    				channel.sendMessage(images1.get(number)).queue();
	    			}
	    			catch(ArrayIndexOutOfBoundsException exception)
	    			{
	    				channel.sendMessage(":heavy_multiplication_x: " + "There aren't more images! Try with a lower number.").queue();
	    			}
	            }
	            public void onStart() {}
	            public void onFailure(int error, String message) {}
	         });
		}
		
		else if(beheadedMessage.startsWith("help"))
		{
			channel.sendMessage(
					"```"
					+ "~>konachan get page:limit:imagenumber gets you an image.\r"
					+ "~>konachan tags page:tag:imagenumber gets you an image with the respective tag.```"
					).queue();
		}
		
		else
		{
			channel.sendMessage(":heavy_multiplication_x: " + "Wrong usage. Use ~>konachan help to get help.").queue();
			
		}
	}
}
