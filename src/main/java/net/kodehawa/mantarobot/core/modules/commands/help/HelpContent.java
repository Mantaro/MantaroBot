/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.modules.commands.help;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpContent {
    private final String description;
    private final Map<String, String> parameters;
    private final String usage;
    private final List<String> related;
    private final boolean seasonal;
    private final List<String> descriptionList;

    public HelpContent(String description, Map<String, String> parameters, String usage, List<String> related, List<String> descriptionList, boolean seasonal) {
        this.description = description;
        this.parameters = parameters;
        this.usage = usage;
        this.related = related;
        this.seasonal = seasonal;
        this.descriptionList = descriptionList;
    }

    public String getDescription() {
        return this.description;
    }

    public Map<String, String> getParameters() {
        return this.parameters;
    }

    public String getUsage() {
        return this.usage;
    }

    public List<String> getRelated() {
        return this.related;
    }

    public List<String> getDescriptionList() {
        return descriptionList;
    }

    public boolean isSeasonal() {
        return this.seasonal;
    }

    public static class Builder {
        private String description = null;
        private final Map<String, String> parameters = new HashMap<>();
        private String usage = null;
        private List<String> related = new ArrayList<>();
        private boolean seasonal = false;
        private List<String> descriptionList = new ArrayList<>();

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder addParameter(String parameterName, String content) {
            parameters.put(parameterName, content);
            return this;
        }

        //I was lazy to make last one take a boolean bc that'd mean replacing existing ones, bleh.
        public Builder addParameterOptional(String parameterName, String content) {
            parameters.put(parameterName, content + " This is optional");
            return this;
        }

        public Builder setUsage(String usage) {
            this.usage = usage;
            return this;
        }

        public Builder setUsagePrefixed(String usage) {
            this.usage = "~>" + usage;
            return this;
        }

        public Builder setRelated(List<String> related) {
            this.related = related;
            return this;
        }

        public Builder setDescriptionList(List<String> descriptionList) {
            this.descriptionList = descriptionList;
            return this;
        }

        public Builder setSeasonal(boolean seasonal) {
            this.seasonal = seasonal;
            return this;
        }

        public HelpContent build() {
            return new HelpContent(description, parameters, usage, related, descriptionList, seasonal);
        }
    }
}
