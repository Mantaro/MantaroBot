package net.kodehawa.discord.Mantaro.commands;

import java.util.concurrent.CopyOnWriteArrayList;

import com.marcomaldonado.konachan.entities.Tag;
import com.marcomaldonado.konachan.entities.Wallpaper;
import com.marcomaldonado.konachan.service.Konachan;
import com.marcomaldonado.web.callback.WallpaperCallback;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CKonachan implements Command {
	
	boolean sfw = true;
	Konachan konachan = new Konachan(sfw);

	@Override
	@ModuleProperties(level = "user", name = "konachan", type = "special", description = "Gets an image from konachan. ~>konachan help for more details on how to use it.",
			additionalInfo = "Possible args: get/tags/help", takesArgs = true)
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		if(beheaded.startsWith("get"))
		{
			CopyOnWriteArrayList<String> images = new CopyOnWriteArrayList<String>();
			
			String whole1 = whole.replace("~>konachan get ", "");
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
				evt.getChannel().sendMessageAsync("You can get a total of " + String.valueOf(images.size()) + "images in this page.", null);
				evt.getChannel().sendMessageAsync(images.get(number), null);
			}
			catch(ArrayIndexOutOfBoundsException exception)
			{
				evt.getChannel().sendMessageAsync("There aren't more images! Try with a lower number.", null);
			}
		}
		
		else if(beheaded.startsWith("tags"))
		{
			CopyOnWriteArrayList<String> images1 = new CopyOnWriteArrayList<String>();

			
			String whole1 = whole.replace("~>konachan tags ", "");
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
	    				evt.getChannel().sendMessageAsync("You can get a total of " + String.valueOf(images1.size()) + " images in this page.", null);
	    				evt.getChannel().sendMessageAsync(images1.get(number), null);
	    			}
	    			catch(ArrayIndexOutOfBoundsException exception)
	    			{
	    				evt.getChannel().sendMessageAsync("There aren't more images! Try with a lower number.", null);
	    			}
	            }
	            public void onStart() {}
	            public void onFailure(int error, String message) {}
	         });
		}
		
		else if(beheaded.startsWith("help"))
		{
			evt.getChannel().sendMessageAsync(
					"```"
					+ "~>konachan get page:limit:imagenumber gets you an image.\r"
					+ "~>konachan tags page:tag:imagenumber gets you an image with the respective tag.```"
					, null);
		}
		
		else
		{
			evt.getChannel().sendMessageAsync("```Wrong usage. Use ~>konachan help to get help.```", null);;
			
		}
	}
}
