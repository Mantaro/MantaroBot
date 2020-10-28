package net.kodehawa.mantarobot.utils.eval;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

class OutputClassFile extends SimpleJavaFileObject {
    private final ByteArrayOutputStream outputStream;
    private final String className;
    
    OutputClassFile(String className, Kind kind) {
        super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
        this.className = className;
        this.outputStream = new ByteArrayOutputStream();
    }
    
    @Override
    public OutputStream openOutputStream() {
        return this.outputStream;
    }
    
    public byte[] getBytes() {
        return this.outputStream.toByteArray();
    }
    
    public String getClassName() {
        return this.className;
    }
}
