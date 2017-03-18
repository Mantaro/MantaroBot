<img align="right" src="https://i.imgur.com/SWDen2V.png" height="220" width="220">

# MantaroBot
**Anime, Music, Moderation, weather information, osu! and everything you might want in a customizable and stable bot**

### Using the Official Mantaro Bot
[***Just one click and you can add Mantaro to your own server ^_^***](http://polr.me/mantaro)

### Building it your own
**WARNING**: Both [**@AdrianTodt**](https://github.com/adriantodt) and [**@Kodehawa**](https://github.com/Kodehawa)
**do not** recommend compiling Mantaro, as it is **not** documented and some builds here might be extremely unstable or untested.
If you still want to build your own Mantaro instance, you will need API keys from the following services, **but not limited to**: osu!API, AniList API, and OpenWeatherMap API keys, which are *your* responsability to obtain.
__We will **not** help you with this process__.

1. Clone this repository.
2. Open a Terminal in the root folder.
3. Run `gradlew build`
4. Grab the `-all.jar` jar in `build/libs`
5. Run it and prepare yourself to start filling configs [rip].


###Give credit where credit is due. If you wish to use our code in a project, **please** credit us, and take your time to read our full license. We don't mind you using Mantaro code, as it *is* open-source for a reason, as long as you don't blatantly copy it or refrain from crediting us.

#### Konachan notice (net.kodehawa.lib.imageboard.konachan)
That package is a port of the [**original konachan library**](https://github.com/Mxrck/KonachanLib) to Java 8 and to better suit our needs. I just didn't want to post it on GitHub as a separate lib, but if anyone wants I'll do so.

### Demo:
<img align="center" src="http://i.imgur.com/QgPQE8J.png" height="500" width="1000">

### License
_Copyright (C) 2016-2017 **David Alejandro Rubio Escares**/**Kodehawa**_

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

([The Full License can be found here](https://github.com/Kodehawa/MantaroBot/blob/master/LICENSE))
