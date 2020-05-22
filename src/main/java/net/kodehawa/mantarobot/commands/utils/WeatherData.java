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

package net.kodehawa.mantarobot.commands.utils;

import java.util.ArrayList;
import java.util.List;

public class WeatherData {
    private Clouds clouds = new Clouds();
    private Main main = new Main();
    private Sys sys = new Sys();
    private List<Weather> weather = new ArrayList<>();
    private Wind wind = new Wind();

    public Clouds getClouds() {
        return clouds;
    }

    public Main getMain() {
        return main;
    }

    public Sys getSys() {
        return sys;
    }

    public List<Weather> getWeather() {
        return weather;
    }

    public Wind getWind() {
        return wind;
    }

    public static class Clouds {
        public int all;
    }

    public static class Main {
        private int humidity; //Humidity in percentage.
        private double pressure = 0;
        private double speed; //Speed in m/h.
        private double temp = 0;

        public int getHumidity() {
            return humidity;
        }

        public double getPressure() {
            return pressure;
        }

        public double getSpeed() {
            return speed;
        }

        public double getTemp() {
            return temp;
        }
    }

    public static class Sys {
        public String country = null;
    }

    public static class Weather {
        public String main = null;
    }

    public static class Wind {
        public double speed = 0;
    }
}
