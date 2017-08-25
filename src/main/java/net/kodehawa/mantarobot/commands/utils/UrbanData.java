/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.utils;

import java.util.ArrayList;

public class UrbanData {

	public class List {
		public String author = null;
		public String current_vote = null;
		public Integer defid = null;
		public String definition = null;
		public String example = null;
		public String permalink = null;
		public String thumbs_down = null;
		public String thumbs_up = null;
	}

	public final ArrayList<List> list = null;
	public String result_type = null;
	public ArrayList<String> tags = null;
}
