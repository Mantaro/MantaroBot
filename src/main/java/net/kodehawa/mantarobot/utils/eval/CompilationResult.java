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
