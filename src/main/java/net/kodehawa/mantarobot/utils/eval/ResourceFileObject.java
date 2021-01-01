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
