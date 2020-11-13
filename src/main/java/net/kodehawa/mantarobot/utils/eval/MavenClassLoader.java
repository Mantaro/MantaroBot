package net.kodehawa.mantarobot.utils.eval;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

public class MavenClassLoader extends ClassLoader {
    private final List<MavenDependencies.DownloadedJar> jars;
    
    MavenClassLoader(ClassLoader parent, List<MavenDependencies.DownloadedJar> jars) {
        super("MavenLoader", parent);
        this.jars = jars;
    }
    
    @Override
    protected URL findResource(String name) {
        for(var f : jars) {
            var entry = f.jf.getJarEntry(name);
            if(entry != null) {
                try {
                    return new URL(f.url, entry.getRealName());
                } catch(MalformedURLException ignored) {
                }
            }
        }
        return null;
    }
    
    @Override
    public InputStream getResourceAsStream(String name) {
        if(getParent() != null) {
            var is = getParent().getResourceAsStream(name);
            if(is != null) {
                return is;
            }
        }
        for(var f : jars) {
            var entry = f.jf.getJarEntry(name);
            if(entry != null) {
                try {
                    return f.jf.getInputStream(entry);
                } catch(IOException ignored) { }
            }
        }
        return null;
    }
    
    @Override
    @SuppressWarnings("try")
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        var is = getResourceAsStream(name.replace('.', '/') + ".class");
        if(is == null) {
            throw new ClassNotFoundException(name);
        }

        // It's meant to be this way, we just want the InputStream
        // to be closed, but we don't need to use it anymore.
        // Warning is supressed above, else compiler complains :)
        try(var __ = is) {
            var bytes = is.readAllBytes();
            return super.defineClass(name, bytes, 0, bytes.length);
        } catch(IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
    
    public Stream<String> resources(String pkg, boolean recursive) {
        return jars.stream().flatMap(jar -> jar.resourceList.stream(pkg, recursive));
    }
}
