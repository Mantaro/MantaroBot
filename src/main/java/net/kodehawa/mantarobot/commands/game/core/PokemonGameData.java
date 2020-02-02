/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.game.core;

import java.util.List;
import java.util.Objects;

public class PokemonGameData {
    private String name;
    private String image;
    private List<String> names;
    
    public PokemonGameData() {
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getImage() {
        return this.image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public List<String> getNames() {
        return this.names;
    }
    
    public void setNames(List<String> names) {
        this.names = names;
    }
    
    protected boolean canEqual(final Object other) {
        return other instanceof PokemonGameData;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.name;
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $image = this.image;
        result = result * PRIME + ($image == null ? 43 : $image.hashCode());
        final Object $names = this.names;
        result = result * PRIME + ($names == null ? 43 : $names.hashCode());
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof PokemonGameData)) return false;
        final PokemonGameData other = (PokemonGameData) o;
        if(!other.canEqual(this)) return false;
        final Object this$name = this.name;
        final Object other$name = other.name;
        if(!Objects.equals(this$name, other$name)) return false;
        final Object this$image = this.image;
        final Object other$image = other.image;
        if(!Objects.equals(this$image, other$image)) return false;
        final Object this$names = this.names;
        final Object other$names = other.names;
        return Objects.equals(this$names, other$names);
    }
    
    public String toString() {
        return "PokemonGameData(name=" + this.name + ", image=" + this.image + ", names=" + this.names + ")";
    }
}
