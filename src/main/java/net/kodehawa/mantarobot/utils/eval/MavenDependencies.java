package net.kodehawa.mantarobot.utils.eval;

import net.kodehawa.mantarobot.utils.Utils;
import okhttp3.Request;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class MavenDependencies implements Closeable {
    private final List<String> repos = new ArrayList<>();
    private final List<DownloadedJar> jars = new ArrayList<>();
    private final Path dir;
    private final boolean delete;
    
    public MavenDependencies() throws IOException {
        this(Files.createTempDirectory("maven-deps"), true);
    }
    
    public MavenDependencies(Path dir) {
        this(dir, false);
    }
    
    public MavenDependencies(Path dir, boolean delete) {
        this.dir = dir;
        if(Files.exists(dir) && !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Provided path exists and is not a directory");
        }
        this.delete = delete;
    }
    
    protected void downloadJar(String url, Path path) throws IOException {
        try(var res = Utils.httpClient.newCall(
                new Request.Builder()
                        .url(url)
                        .build()
        ).execute()) {
            var body = res.body();
            if(!res.isSuccessful() || body == null) throw new IOException("Request was not successful");
            try(var out = Files.newOutputStream(path)) {
                body.byteStream().transferTo(out);
            }
        }
    }
    
    public MavenDependencies addRepository(String repositoryUrl) {
        if(!repositoryUrl.endsWith("/")) {
            repositoryUrl = repositoryUrl + "/";
        }
        repos.add(repositoryUrl);
        return this;
    }
    
    public MavenDependencies addDependency(String name) throws IOException {
        var parts = name.split(":");
        if(parts.length != 3) {
            throw new IllegalArgumentException("Malformed dependency '" + name + "'");
        }
        return addDependency(parts[0], parts[1], parts[2]);
    }
    
    public MavenDependencies addDependency(String group, String artifact, String version) throws IOException {
        for(var s : repos) {
            var url = s + String.join("/",
                    group.replace('.', '/'),
                    artifact.replace('.', '/'),
                    version,
                    artifact + "-" + version + ".jar");
            var path = dir.resolve(group + "-" + artifact + "-" + version + ".jar");
            if(!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                downloadJar(url, path);
                try {
                    new JarFile(path.toFile()).close();
                } catch(IOException e) {
                    Files.delete(path);
                    throw new IOException("Malformed response", e);
                }
            }
            jars.add(new DownloadedJar(path));
            return this;
        }
        throw new IOException("Unable to find dependency " + String.join(":", group, artifact, version));
    }
    
    public MavenClassLoader createClassLoader(ClassLoader parent) {
        return new MavenClassLoader(parent, new ArrayList<>(jars));
    }
    
    @Override
    public void close() throws IOException {
        for(var j : jars) {
            j.close();
            if(delete) {
                Files.delete(j.path);
            }
        }
        if(delete) {
            Files.delete(dir);
        }
    }
    
    static class DownloadedJar {
        private final Path path;
        final URL url;
        final JarFile jf;
        final ResourceList resourceList;
        
        private DownloadedJar(Path path) throws IOException {
            this.path = path;
            try {
                this.url = new URL("jar:" + path.toFile().toURI().toURL() + "!/");
            } catch(MalformedURLException e) {
                throw new IOException("Unable to create jar url", e);
            }
            this.jf = new JarFile(path.toFile());
            this.resourceList = ResourceList.fromJar(jf);
        }
        
        public void close() throws IOException {
            jf.close();
        }
    }
    
}
