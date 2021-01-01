/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils.eval;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.tools.Diagnostic;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class JavaEvaluator implements Closeable {
    private final MavenDependencies dependencies;
    
    public JavaEvaluator(@Nonnull MavenDependencies dependencies) {
        this.dependencies = dependencies;
    }
    
    @Nonnull
    @CheckReturnValue
    public CompilationResult compile(String name, String source) {
        var writer = new StringWriter();
        var diagnostics = new ArrayList<Diagnostic<?>>();
        var cl = dependencies.createClassLoader(ClassLoader.getSystemClassLoader());
        var compiler = ToolProvider.getSystemJavaCompiler();
        var fileManager = new FileManager(compiler.getStandardFileManager(
                diagnostics::add, null, null
        ), cl);
        var task = compiler.getTask(
                writer,
                fileManager,
                diagnostics::add,
                null,
                null,
                List.of(new StringJavaFileObject(name, source))
        );
        if(!task.call()) {
            var output = writer.toString().strip();
            return new CompilationResult(null, output.isEmpty() ? null : output, diagnostics);
        }
        var ecl = new ClassLoader(cl) {
            void def(byte[] b) { defineClass(null, b, 0, b.length); }
        };
        fileManager.outputFiles().forEach(c -> ecl.def(c.getBytes()));
        var output = writer.toString().strip();
        try {
            return new CompilationResult(ecl.loadClass(name), output.isEmpty() ? null : output, diagnostics);
        } catch(ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to find class with given name", e);
        }
    }
    
    @Override
    public void close() throws IOException {
        dependencies.close();
    }
    
    private static class StringJavaFileObject extends SimpleJavaFileObject {
        private final String sourceCode;
        
        public StringJavaFileObject(String className, String sourceCode) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }
        
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return this.sourceCode;
        }
    }
}
