/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.modules.commands.help;

import net.kodehawa.mantarobot.core.command.meta.Help;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public record HelpContent(String description, List<Help.Parameter> parameters, String usage, List<String> related,
                          List<String> descriptionList, boolean seasonal) {

    public static class Builder {
        private String description = null;
        private List<Help.Parameter> parameters = new ArrayList<>();
        private String usage = null;
        private List<String> related = new ArrayList<>();
        private boolean seasonal = false;
        private List<String> descriptionList = new ArrayList<>();

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setParameters(List<Help.Parameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder addParameter(String name, String description) {
            parameters.add(getParameter(name, description, false));
            return this;
        }

        public Builder addParameterOptional(String name, String description) {
            parameters.add(getParameter(name, description, true));
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

        // This is *cursed*
        // With this simple trick I'm probably breaking a billion Java conventions
        // Like, you know, the fact you shouldn't be able to initiate an annotation.
        // Though, this isn't **really** an annotation, it shouldn't matter, as this is for backwards compatibility.
        // SO: https://stackoverflow.com/a/16303007
        private Help.Parameter getParameter(String name, String description, boolean optional) {
            return new Help.Parameter() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Help.Parameter.class;
                }

                @Override
                public String name() {
                    return name;
                }

                @Override
                public String description() {
                    return description;
                }

                @Override
                public boolean optional() {
                    return optional;
                }
            };
        }

        public HelpContent build() {
            return new HelpContent(description, parameters, usage, related, descriptionList, seasonal);
        }
    }
}
