package net.kodehawa.discord.Mantaro.reflection;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.kodehawa.discord.Mantaro.bot.MantaroBot;

public class ClassFinder {

    private static final char PKG_SEPARATOR = '.';

    private static final char DIR_SEPARATOR = '/';

    private static final String CLASS_FILE_SUFFIX = ".class";

    private static final String BAD_PACKAGE_ERROR = "Unable to get resources from path '%s'. Are you sure the package '%s' exists?";

    static Boolean desenv = null;

    static boolean isDevelopment() {
        if (desenv != null) return desenv;

        try {
            desenv = new File(".").getCanonicalPath().contains("workspace");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return desenv;
    }
    
    public static List<Class<?>> find(String scannedPackage) {
        String scannedPath = scannedPackage.replace(PKG_SEPARATOR, DIR_SEPARATOR);
        URL scannedUrl = Thread.currentThread().getContextClassLoader().getResource(scannedPath);
        if (scannedUrl == null) {
            throw new IllegalArgumentException(String.format(BAD_PACKAGE_ERROR, scannedPath, scannedPackage));
        }
        File scannedDir = null;
        if(!isDevelopment())
        {
        	CodeSource src = MantaroBot.class.getProtectionDomain().getCodeSource();
        	if (src != null) {
        	  URL jar = src.getLocation();
        	  ZipInputStream zip = null;
        	  try {
        		  zip = new ZipInputStream(jar.openStream());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
				}
        	  while(true) {
        	    ZipEntry e = null;
				try {
					e = zip.getNextEntry();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
        	    if (e == null)
        	      break;
        	    String name = e.getName();
        	    try {
			            scannedDir = new File(MantaroBot.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
        	  }
        	} 
        	else {
        		System.out.println("Source is null?");
        	}
        }
        else
        {
            scannedDir = new File(scannedUrl.getFile());
        }
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (File file : scannedDir.listFiles()) {
            classes.addAll(find(file, scannedPackage));
        }
        return classes;
    }

    private static List<Class<?>> find(File file, String scannedPackage) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        String resource = scannedPackage + PKG_SEPARATOR + file.getName();
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                classes.addAll(find(child, resource));
            }
        } else if (resource.endsWith(CLASS_FILE_SUFFIX)) {
            int endIndex = resource.length() - CLASS_FILE_SUFFIX.length();
            String className = resource.substring(0, endIndex);
            try {
                classes.add(Class.forName(className));
            } catch (ClassNotFoundException ignore) {
            }
        }
        return classes;
    }

}
