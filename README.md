<p align="center">
  <img src="https://i.imgur.com/b00buRW.png"/>
</p>

**Complete and configurable music, currency and games multipurpose Discord bot**

## Using the Official Mantaro Bot

**Just one [click](https://add.mantaro.site) and you can add Mantaro to your own server and enjoy its full feature set!**

You can see more information about the bot itself by reading the summary on [here](https://github.com/Mantaro/MantaroBot/blob/master/FEATURES.md). This is the file we use to publish our bots on bot lists, too. You're welcome to use it as a guide for your own.
* * *

## Building Mantaro on your own

**WARNING**: The owners of Mantaro do not recommend compiling Mantaro as it is not documented and most builds here will be extremely unstable and (probably) untested, probably including unfinished features.  
If you still want to build your own instance of Mantaro, you will need multiple api keys including **(but not limited to)**
*   osu! API
*   AniList API
*   OpenWeatherMap API.
*   Wolke's Weeb API (For most of the action commands).
*   etc...

**We will not help you with the process of obtaining said api keys nor in the process of building and hosting the bot yourself!**

The bot necessarly doesn't need the keys to function, but some functionality might be limited by the lack of them (ex. without Wolke's API keys, you can't use the action commands). Due to the closed nature of that API, we encourage you to submit a patch that would allow custom images to be used on self-hosted instances if you'd like (ex. by pushing your own -local- API server), but keep them in line with the rest of the code. 

Mantaro isn't a modular bot (sadly), but removing features is fairly easy. You can just remove the respective command or the Module file on the commands directory and everything -should- still work. The exception are some Modules that are required by other Modules.

**Steps for building**

1.  Make sure you have at least JDK or OpenJDK 9, RebirthDB/RethinkDB and Redis installed -and- running.
2.  Clone this repository.
3.  If you are going to edit code, make sure your IDE supports [Lombok](http://projectlombok.org) and enable Annotation Processing!
4.  Open a Terminal in the root folder.
5.  Run `gradlew shadowJar`
6.  Grab the `-all.jar` jar from `build/libs`
7.  Install `rethinkdb` and `redis`
8.  Create the `mantaro` database with the following tables: mantaro, players, users, guilds, keys, commands, seasonalplayers
9.  Run it and prepare yourself to start filling in configs (open the jar on the command line using java -jar name.jar and wait for it to crash, then it'll generate the config.json file for you to fill).
10.  In config.json, set the value needApi to false. (Or clone and run https://github.com/Kodehawa/mantaro-api)

* * *

## Mantaro Uses and loves
*   [JDA by DV8FromTheWorld and MinnDevelopment](https://github.com/DV8FromTheWorld/JDA)
*   [Lavaplayer by sedmelluq](https://github.com/sedmelluq/lavaplayer)
*   [RethinkDB by the RethinkDB team](http://rethinkdb.com)
*   [Redis by the redis team.](https://redis.io)
*   And a lot more!

* * *

Give credit where credit is due. If you wish to use our code in a project, **please** credit us, and take your time to read our full license. We don't mind you using Mantaro code, **as it is** open-source for a reason, as long as you don't blatantly copy it or refrain from crediting us.

## Important Contributors
Many thanks to
[@natanbc](https://github.com/natanbc) - A lot of backend improvements and developing. Active.
[@mrlar](https://github.com/mrlar) - Basically made all of the user-facing documentation.
[@adriantodt](https://github.com/adriantodt) - Backend development on the early stages of the bot. Was crucial to the development and thanks to him this bot is what it is now. Inactive.

And a lot more people. Check the *Contributors* tab!

## License

Copyright (C) 2016-2019 **David Rubio Escares** / **Kodehawa**

>This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
>as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. 
>                                                   
>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
>without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
>                                                   
>See the GNU General Public License for more details. 
>You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/

[The full license can be found here.](https://github.com/Kodehawa/MantaroBot/blob/master/LICENSE)
