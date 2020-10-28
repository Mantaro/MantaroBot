package net.kodehawa.mantarobot.utils.eval;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class FileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private final List<OutputClassFile> outputFiles = new ArrayList<>();
    private final MavenClassLoader loader;
    
    FileManager(JavaFileManager fileManager, MavenClassLoader loader) {
        super(fileManager);
        this.loader = loader;
    }
    
    public List<OutputClassFile> outputFiles() {
        return outputFiles;
    }
    
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
        var file = new OutputClassFile(className, kind);
        this.outputFiles.add(file);
        return file;
    }
    
    @Override
    public boolean hasLocation(Location location) {
        return location == StandardLocation.CLASS_PATH || super.hasLocation(location);
    }
    
    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        if(a instanceof ResourceFileObject && !(b instanceof ResourceFileObject)) return false;
        if(!(a instanceof ResourceFileObject) && b instanceof ResourceFileObject) return false;
        if(a instanceof ResourceFileObject) {
            return a.getName().equals(b.getName());
        }
        return super.isSameFile(a, b);
    }
    
    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if(file instanceof ResourceFileObject) {
            var path = file.getName();
            if(path.endsWith(".class")) {
                return path.substring(0, path.length() - ".class".length()).replace("/", ".");
            }
        }
        return super.inferBinaryName(location, file);
    }
    
    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        var s = super.list(location, packageName, kinds, recurse);
        if(location == StandardLocation.CLASS_PATH) {
            return () -> Stream.concat(
                    StreamSupport.stream(s.spliterator(), false),
                    loader.resources(packageName, recurse)
                            .filter(p -> p.endsWith(".class"))
                            .map(path -> new ResourceFileObject(loader, path))
            ).iterator();
        }
        return s;
    }
    
}
