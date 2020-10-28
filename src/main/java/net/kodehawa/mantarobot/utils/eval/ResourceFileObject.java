package net.kodehawa.mantarobot.utils.eval;

import javax.tools.SimpleJavaFileObject;
import java.io.InputStream;
import java.net.URI;

class ResourceFileObject extends SimpleJavaFileObject {
    private final ClassLoader source;
    private final String name;
    
    ResourceFileObject(ClassLoader source, String path) {
        super(URI.create("resource:///" + path), Kind.CLASS);
        this.source = source;
        this.name = path;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public InputStream openInputStream() {
        return source.getResourceAsStream(name);
    }
}
