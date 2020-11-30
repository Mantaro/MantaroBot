Thanks for your interest on contributing to Mantaro. The guidelines for doing such are explained below, they're usually pretty simple, and most of 
what you want to read is the code guidelines to make sure we don't request style changes.

### Features
1. Any and all features have to first be approved on the support server at https://support.mantaro.site. Feel free to discuss about them on #bot-discussions.
2. If a feature was already approved by a developer, but there's no progress on its implementation, you're free to skip the above point.
3. All feature additions have to be done through a PR/MR. Please ask in the support server if you're not sure about it (though I guess most of us are past e-mail diff sending :P)

### Bug fixing 
1. First report the bug on #bug-reports on the support server said above. If the fix is trivial, we can probably push a quick commit.
2. If there's a long-lasting bug you want to fix but nothing has been done about it, feel free to do so in a PR.

### Code guidelines
This is not a definitive guide, and the code does not follow all of them consistently. 
For *required* code guidelines, they'll be marked with a (!). If you fail to met one of them, you'll be asked to change your PR so it does meet them.

* There must be an space after a if statement start, like `if (a != b)`, not `if(a != b)`
* The code uses 4 spaces. If you're used to using tab (like most people), just set your editor to do 4 spaces on a single tab. This is to keep indentation highlighting consistent (!)
* Try to make variable names that make sense, but don't make long ones. Something like `increase` is good, `i` is not good, and `thisIncreasesMoneyBy100` is definitely not good. (!)
* The copyright header should be included in new files. Check other files for guidelines on how it should look.
* Avoid too many indentation levels. If it's necessary it's ok, but just avoid it.
* Commits should follow the following convention: `Action(module): message`. We'll outline it on the section below. Developers (as in, people with commit access) might not need to follow this all the time.

### Commit formatting
Commits should follow the following convention: `Action(module): message`.

Where action can be:

* Fix - Fixes a bug or issue with the code.
* Add - Adds a new thing to an existing feature.
* Feat - Adds a completely new feature.
* Refactor - Refactors a part of Mantaro's code.
* Chore - Probably just a dependency bump-, or fixes to the docker/CI configs.

And module can be:
* Any command (mine, fish, profile, etc)
* A internal part of Mantaro (cc, evalsystem, finderutil, core, command, option)
* A external part included on Mantaro's code (lavalink-client)
* Dependencies (deps), CI stuff (ci) and Docker configs (docker)
* One of the .md files (features, coc, contrib, readme)

If the module isn't clear or found there, you can omit it, your message should look like:

* `Add(mine): Add new drop when mining with sparkle`
* `Fix(equip): Fix equipment disappearing if it's a full moon and you ate a taco`
* `Chore(deps): Update JDA from 4.2.0_167 to 4.2.0_178`
