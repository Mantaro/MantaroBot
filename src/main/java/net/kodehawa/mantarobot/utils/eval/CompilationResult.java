package net.kodehawa.mantarobot.utils.eval;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.tools.Diagnostic;
import java.util.List;

public class CompilationResult {
    private final Class<?> clazz;
    private final String output;
    private final List<Diagnostic<?>> diagnostics;
    
    CompilationResult(Class<?> clazz, String output, List<Diagnostic<?>> diagnostics) {
        this.clazz = clazz;
        this.output = output;
        this.diagnostics = diagnostics;
    }
    
    @CheckReturnValue
    public boolean isSuccessful() {
        return clazz != null;
    }
    
    @Nonnull
    public Class<?> resultingClass() {
        if(!isSuccessful()) {
            throw new IllegalStateException("Compilation failed");
        }
        return clazz;
    }
    
    @Nullable
    @CheckReturnValue
    public String output() {
        return output;
    }
    
    @Nonnull
    @CheckReturnValue
    public List<Diagnostic<?>> diagnostics() {
        return diagnostics;
    }
}
