package com.marcomaldonado.konachan.service;

import com.google.gson.Gson;
import com.marcomaldonado.konachan.entities.Tag;
import com.marcomaldonado.konachan.entities.Wallpaper;
import com.marcomaldonado.web.callback.DownloadCallback;
import com.marcomaldonado.web.callback.WallpaperCallback;
import com.marcomaldonado.web.tools.helpers.HTMLHelper;
import us.monoid.web.BinaryResource;
import us.monoid.web.Resty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Mxrck on 22/11/2015.
 */
public class Konachan {

	private HTMLHelper htmlHelper = HTMLHelper.getInstance();
	private int limitRelatedTags = 5;
	private HashMap<String, Object> queryParams;
	private Resty resty;
	private boolean safeForWork = false;

	public Konachan(boolean safeForWork) {
		queryParams = new HashMap<>();
		resty = new Resty();
		resty.identifyAsMozilla();
		this.safeForWork = safeForWork;
	}

	private String cleanTag(String tagname) {
		return tagname.toLowerCase().trim().replace(' ', '_');
	}

	private int getLimitRelatedTags() {
		return limitRelatedTags;
	}

	public void setLimitRelatedTags(int limitRelatedTags) {
		this.limitRelatedTags = limitRelatedTags;
	}

	public boolean isSafeForWork() {
		return safeForWork;
	}

	public void setSafeForWork(boolean safeForWork) {
		this.safeForWork = safeForWork;
	}

	public Wallpaper[] posts() {
		return this.posts(1, 25);
	}

	public Wallpaper[] posts(int limit) {
		return this.posts(1, limit);
	}

	public Wallpaper[] posts(int page, int limit) {
		return this.posts(page, limit, (String) null);
	}

	public Thread posts(WallpaperCallback callback) {
		return this.posts(1, 25, null, callback);
	}

	public Thread posts(int limit, WallpaperCallback callback) {
		return this.posts(1, limit, null, callback);
	}

	public Thread posts(int page, int limit, WallpaperCallback callback) {
		return this.posts(page, limit, null, callback);
	}

	private Thread posts(final int page, final int limit, final String search, final WallpaperCallback callback) {
		final Konachan self = this;
		Thread thread = new Thread(() -> {
			try {
				if (callback != null) {
					callback.onStart();
				}
				Wallpaper[] wallpapers = self.posts(page, limit, search);
				Tag[] tags = null;
				if (search != null) {
					tags = self.tags(search, 1, self.getLimitRelatedTags());
				}
				if (callback != null) {
					callback.onSuccess(wallpapers, tags);
				}
			} catch (Exception ex) {
				if (callback != null) callback.onFailure(
					KonachanErrors.GENERIC_ERROR,
					KonachanErrors.message(KonachanErrors.GENERIC_ERROR)
				);
			}
		});
		thread.start();
		return thread;
	}

	private Wallpaper[] posts(int page, int limit, String search) {
		this.queryParams.put("limit", limit);
		this.queryParams.put("page", page);
		if (search != null) {
			this.queryParams.put("tags", this.cleanTag(search));
		}
		String response = "[]";
		try {
			String postsUrl = "http://konachan.com/post.json";
			response = this.resty.text(
				postsUrl + "?" + htmlHelper.urlEncodeUTF8(this.queryParams)
			).toString();
			System.out.println(postsUrl + "?" + htmlHelper.urlEncodeUTF8(this.queryParams));
		} catch (Exception ignored) {
		} finally {
			queryParams.clear();
		}
		Gson gson = new Gson();
		Wallpaper[] wallpapers = gson.fromJson(response, Wallpaper[].class);

		if (this.safeForWork) {
			ArrayList<Wallpaper> wallpapersSafe = new ArrayList<>();
			for (Wallpaper wallpaper : wallpapers) {
				if (wallpaper.getRating().equalsIgnoreCase("s")) {
					wallpapersSafe.add(wallpaper);
				}
			}
			wallpapers = wallpapersSafe.toArray(new Wallpaper[0]);
		}

		return wallpapers;
	}

	private String saveWallpaper(String filename, String folderPath, String imageURL) {
		if (filename == null) {
			filename = imageURL.substring(imageURL.lastIndexOf('/') + 1, imageURL.length());
		}
		Resty downloader = new Resty();
		downloader.identifyAsMozilla();
		File imageFile = new File(folderPath + File.separator + filename);
		if (imageFile.exists()) return imageFile.getPath();
		BinaryResource binaryResource;
		try {
			binaryResource = downloader.bytes(imageURL);
			binaryResource.save(imageFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return folderPath + File.separator + filename;
	}

	public Thread saveWallpaper(final String filename, final String folderPath, final String imageURL, final DownloadCallback callback) {
		Thread thread = new Thread(() -> {
			try {
				if (callback != null) {
					callback.onStart();
				}
				String save = saveWallpaper(filename, folderPath, imageURL);
				if (save != null) {
					if (callback != null) {
						callback.onSuccess(save);
					}
				} else {
					if (callback != null) {
						callback.onFailure(
							KonachanErrors.UNKNOW_ERROR,
							KonachanErrors.message(KonachanErrors.UNKNOW_ERROR
							)
						);
					}
				}
			} catch (Exception ex) {
				if (callback != null) {
					callback.onFailure(
						KonachanErrors.GENERIC_ERROR,
						KonachanErrors.message(KonachanErrors.GENERIC_ERROR
						)
					);
				}
			}
		});
		thread.start();
		return thread;
	}

	public Wallpaper[] search(String search) {
		return this.posts(1, 25, search);
	}

	public Wallpaper[] search(int page, int limit, String search) {
		return this.posts(page, limit, search);
	}

	public Thread search(String search, WallpaperCallback callback) {
		return this.posts(1, 25, search, callback);
	}

	public Thread search(int page, int limit, String search, WallpaperCallback callback) {
		return this.posts(page, limit, search, callback);
	}

	private Tag[] tags(String tagname, int page, int limit) {
		this.queryParams.put("order", "count");
		this.queryParams.put("limit", limit);
		this.queryParams.put("page", page);
		this.queryParams.put("name", this.cleanTag(tagname));
		String response = "[]";
		try {
			String tagsUrl = "http://konachan.com/tag.json";
			response = this.resty.text(
				tagsUrl + "?" + htmlHelper.urlEncodeUTF8(this.queryParams)
			).toString();
		} catch (Exception ignored) {
		} finally {
			queryParams.clear();
		}
		Gson gson = new Gson();
		return gson.fromJson(response, Tag[].class);
	}

}