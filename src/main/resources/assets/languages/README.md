# How to contribute to Translating
If this way is too complicated for you scroll a little bit down and there is an alternative and more easy way.

 - Create a Github account
   - We would prefer you to also join [Mantaro Hub](https://discord.gg/ppKeqqh) and contact Kodehawa#3457.

 - Fork Mantaro's Repository.

 ![](https://i.imgur.com/Zl7Sr70.png)

 - Download [GitHub Desktop](https://desktop.github.com/) (if you don't know what you are doing).

 - Clone the Fork into GitHub Desktop.

 ![](https://i.imgur.com/jpf8qmo.png)  ![](https://i.imgur.com/KgxBlB2.png) ![](https://i.imgur.com/LPihVzy.png)

 - Browse the Local Folder of it and go to `\src\main\resources\assets\languages`.

 - When creating a new translation, copy the en_US.json and rename the copy to your language (i.e German (Germany) would be de_DE.json) otherwise edit the file you want to contribute to.
    - When editing please use an actual code Editor to edit and not notepad. We suggest [IntelliJ](https://www.jetbrains.com/idea/) or [VSCode](https://code.visualstudio.com/).

 - Replace all english sentences with the corresponding translation. Follow the guide below while translating.
   + Keep the overall feeling of the bot consistent with the english version.
   + Don't "paste old translations" into your translation file. All new translations will be added *at the top of the category*, which means you can just paste from en_US the missing ones until the one you have. (and at the bottom of the command in the case of new command strings)
   + Keep the translation clean.
   + Don't remove or "translate" anything remotely like %1$s, %2$s or %\<number\>$s. [More info](https://docs.oracle.com/javase/9/docs/api/java/util/Formattable.html)
   + Don't change the formatting marks (\*\*, \*, etc). If a word is \*\*like this\*\* translate it \*\*como esto\*\*, for example.
   + We're gonna review translation files before going to production, so no worries if something happens to go wrong we will point it out and assist you.
   + If you're unsure what a parameter is, you can try using the command or contact Kodehawa#3457.
   + For languages that are read backwards (compared to english), the %\<number\>$s-alike parameters can be moved to accommodate your language reading. You can contact Kodehawa#3457 on Discord if you're lost.
 
 - (if applicable) Multiple versions of the same string can be provided by making the value an array of strings. A random element of the array is selected when used.
    - To make it an array, simply surround it with `[]` (`"key": "value"` -> `"key": ["value"]`).
    - To add elements to the array, separate them with a comma (`"key": ["value"]` -> `"key": ["value", "other value"]`).
   
 - After you are done, send the changed file to Kodehawa#3457 on Discord for review (Please include a message on why you are sending it, as well as the language you translated to).
 
 - Once reviewed and being given the ok, go back to your Github desktop client.

 - You'll see changes made. UNTICK everything that's not your language file (or the README in case you edited the table below).

 - Give the commit a name and hit commit to.

 - Go to the top and click push to origin.

 - Go back to your Fork on github and click pull request.

 ![](https://i.imgur.com/HROt9B4.png)

 - **Make sure you are making a pull request from your Fork master to MantaroBot master.** (Screenshots say 4.9 but you want to do master anyway)

 - Give the Pull request a reasonable name.

 ![](https://i.imgur.com/Y7sTIGw.png)

 - You are done.

# Alternative way

## If you are creating a new language: 
- Create a text document anywhere on your computer and rename it to "\<your lang code\>.json" (i.e. for german de_DE.json). Yes that means the .txt becomes .json

- Go to en_US.json in the above directory and copy everything you see in the box

- Paste it into the newly created file

- Replace all english sentences with the corresponding translation. Follow the guide below while translating.

- Once think the language is ready DM (Direct message) Kodehawa#3457 on Discord and tell him what language you translated to and upload the file along side it

## If you are adding to an existing langauge:
- Create a text document anywhere on your computer and rename it to "\<the code of the language\>.json" (i.e. for german de_DE.json). Yes that means the .txt becomes .json

- Go to \<the code of the language\>.json in the above directory and copy everything you see in the box

- Paste it into the newly created file

- Replace all still english sentences with the corresponding translation (Or correct exiting translations if applicable). Follow the guide below while translating.

- Once think the language is ready DM (Direct message) Kodehawa#3457 on Discord and tell him what language you translated to and upload the file along side it

## Guide:
   + Keep the overall feeling of the bot consistent with the english version.
   + Don't "paste old translations" into your translation file. All new translations will be added *at the top of the category*, which means you can just paste from en_US the missing ones until the one you have. (and at the bottom of the command in the case of new command strings)
   + Keep the translation clean.
   + Don't remove or "translate" anything remotely like %1$s, %2$s or %\<number\>$s. [More info](https://docs.oracle.com/javase/9/docs/api/java/util/Formattable.html)
   + Don't change the formatting marks (\*\*, \*, etc). If a word is \*\*like this\*\* translate it \*\*como esto\*\*, for example.
   + We're gonna review translation files before going to production, so no worries if something happens to go wrong we will point it out and assist you.
   + If you're unsure what a parameter is, you can try using the command or contact Kodehawa#3457.
   + For languages that are read backwards (compared to english), the %\<number\>$s-alike parameters can be moved to accommodate your language reading. You can contact Kodehawa#3457 on Discord if you're lost.
 
 - (if applicable) Multiple versions of the same string can be provided by making the value an array of strings. A random element of the array is selected when used.
    - To make it an array, simply surround it with `[]` (`"key": "value"` -> `"key": ["value"]`).
    - To add elements to the array, separate them with a comma (`"key": ["value"]` -> `"key": ["value", "other value"]`).

# To have in mind
If you can't DM Kodehawa, add him as a friend. Please ping me in Mantaro Hub before adding so I know who you are. 

Thanks you.

# Credits
You may add yourself to this table while making a translation.

| Language | Name |
|:--------:|:-----|
| Spanish (CL) | Kodehawa#3457 ([@Kodehawa](https://github.com/Kodehawa)) |
| German (DE) | Vrontis#8513 ([@Vrontis](https://github.com/Vrontis)) |
| Lithuanian (LT) | Ghostwolf#0001 ([@TheOnlyGhostwolf](https://github.com/TheOnlyGhostwolf)) |
| Dutch (NL) | JynXi#4043 (No GitHub) |
| French (FR) | Baba#3647 ([@thebaba98](https://github.com/thebaba98)) |
