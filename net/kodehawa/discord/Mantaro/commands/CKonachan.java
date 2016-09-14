package net.kodehawa.discord.Mantaro.commands;

import com.marcomaldonado.konachan.entities.Tag;
import com.marcomaldonado.konachan.entities.Wallpaper;
import com.marcomaldonado.konachan.service.Konachan;
import com.marcomaldonado.web.callback.WallpaperCallback;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.utils.Values;

public class CKonachan implements Command {

	Konachan konachan = new Konachan(Values.values.get("kona.sfw").booleanValue());

	@Override
	@ModuleProperties(level = "user", name = "konachan", type = "special", description = "Gets an image from konachan. ~>konachan help for more details on how to use it.",
			additionalInfo = "Possible args: get/tags/help", takesArgs = true)
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		System.out.println(Values.values.get("kona.sfw"));
		String replaced = whole.replace("~>konachan ", "");

		if(replaced.startsWith("get"))
		{
			String whole1 = whole.replace("~>konachan get ", "");
			String[] wholeBeheaded = whole1.split(":");
			int page = Integer.parseInt(wholeBeheaded[0]);
			int limit = Integer.parseInt(wholeBeheaded[1]);
			konachan.posts(page, limit, new WallpaperCallback() {
		        public void onSuccess(Wallpaper[] wallpapers, Tag[] tags) {
		            for(Wallpaper wallpaper : wallpapers) {
		              evt.getChannel().sendMessageAsync(wallpaper.getJpeg_url(), null);
		              break;
		            }
		        }
				public void onStart() {}
				public void onFailure(int error, String message) {}
		        });
		}
		
		else if(replaced.startsWith("tags"))
		{
			String whole1 = whole.replace("~>konachan tags ", "");
			String[] whole2 = whole1.split(":");
			int page = Integer.parseInt(whole2[0]);
			String tags = whole2[1];
			
	        konachan.search(60, page, tags, new WallpaperCallback() {
	            public void onSuccess(Wallpaper[] wallpapers, Tag[] tags) {
	                for(Wallpaper wallpaper : wallpapers) {
	                	evt.getChannel().sendMessageAsync(wallpaper.getJpeg_url(), null);
	                	break;
	                }
	            }
	            public void onStart() {}
	            public void onFailure(int error, String message) {}
	         });
		}
		
		else if(replaced.startsWith("help"))
		{
			evt.getChannel().sendMessageAsync(
					"```"
					+ "~>konachan get number:number gets you an image.\r"
					+ "~>konachan tags number:tag gets you an image with the respective tag.```"
					, null);
		}
		
		else
		{
			evt.getChannel().sendMessageAsync("```Wrong usage. Use ~>konachan help to get help.```", null);;
			
		}
	}
}
