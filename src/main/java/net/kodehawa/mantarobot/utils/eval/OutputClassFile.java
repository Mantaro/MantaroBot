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
