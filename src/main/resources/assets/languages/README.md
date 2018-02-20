# How to contribute to Translating

 - Create a Github account
   - We would prefer you to also join [Mantaro Hub](https://discord.gg/ppKeqqh) and contact either MrLar 🌺#0611 or Kodehawa#3457.

 - Fork Mantaro's Repository.

 ![](https://i.imgur.com/Zl7Sr70.png)

 - Download [GitHub Desktop](https://desktop.github.com/) (if you dont know what you are doing).

 - Clone the Fork into GitHub Desktop.

 ![](https://i.imgur.com/jpf8qmo.png)  ![](https://i.imgur.com/KgxBlB2.png) ![](https://i.imgur.com/LPihVzy.png)

 - Choose 4.9 as active Branch.

 ![](https://i.imgur.com/pFqFgh8.png)

 - Browse the Local Folder of it and go to `\src\main\resources\assets\languages`.

 - When creating a new translation, copy the en_US.json and rename the copy to your language (i.e German (Germany) would be de_DE.json) otherwise edit the file you want to contribute to.
    - When editing please use an actual code Editor to edit and not notepad. We suggest [IntelliJ](https://www.jetbrains.com/idea/) or [VSCode](https://code.visualstudio.com/).

 - Replace all english sentences with the coresponding translation. Follow the guide below while translating.
   + Keep the overall feeling of the bot consistent with the english version.
   + Don't "paste old translations" into your translation file. All new translations will be added *at the top of the category*, which means you can just paste from en_US the missing ones until the one you have. (and at the bottom of the command in the case of new command strings)
   + Keep the translation clean.
   + Don't remove or "translate" anything remotely like %1$s, %2$s or %\<number\>$s. [More info](https://docs.oracle.com/javase/9/docs/api/java/util/Formattable.html)
   + Don't change the formatting marks (\*\*, \*, etc). If a word is \*\*like this\*\* translate it \*\*como esto\*\*, for example.
   + We're gonna review translation files before going to production, so no worries if something happens to go wrong we will point it out and assist you.
   + If you're unsure what a parameter is, you can try using the command or contact MrLar 🌺#0611 or Kodehawa#3457.
   + For languages that are read backwards (compared to english), the %\<number\>$s-alike parameters can be moved to accomodate your language reading. You can contact MrLar 🌺#0611 or Kodehawa#3457 on Discord if you're lost.
   
 - After you are done, send the changed file to Kodehawa#3457 or MrLar 🌺#0611 on Discord for review (Please include a message on why you are sending it, aswell as the langauge you translated to).
 
 - Once reviewed and being given the ok, go back to your Github desktop client.

 - You'll see changes made. UNTICK everything thats not your langauge file (or the README incase you edited the table below).

 - Give the commit a name and hit commit to.

 - Go to the top and click push to origin.

 - Go back to your Fork on github and click pull request.

 ![](https://i.imgur.com/HROt9B4.png)

 - Make sure you are making a pull request from your Fork 4.9 to MantaroBot 4.9.
![](https://desii.is-a-good-waifu.com/659de7.gif)

 - Give the Pull request a reasonable name.

 ![](https://i.imgur.com/Y7sTIGw.png)

 - You are done.

# Credits
You may add yourself to this table while making a translation.

| Language | Name |
|:--------:|:-----|
| Spanish (CL) | Kodehawa#3457 ([@Kodehawa](https://github.com/Kodehawa)) |
| Portuguese (BR) | Natan#1337 ([@Natanbc](https://github.com/natanbc)) |
| French (FR) | Desiree 🌺#0611 ([@Desiiii](https://github.com/Desiiii)) |
| German (DE) | MrLar 🌺#0611 ([@MrLar](https://github.com/MrLar)) |
| Chinese (TW) | edisonlee55#9058 ([@edisonlee55](https://github.com/edisonlee55))