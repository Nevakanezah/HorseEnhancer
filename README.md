
#  ![HorseEnhancer logo](https://www.spigotmc.org/data/resource_icons/75/75692.jpg?1583213795) HorseEnhancer by Nevakanezah
*All-natural horse enhancement, adding horse genders, lineage tracking, better breeding, and powerful commands!*

# What is HorseEnhancer?
Horse enhancer is a Spigot/Bukkit plugin that modifies the behaviour of horses in Minecraft in order to enhance horse-related gameplay, and enable an equestrian economy.
The plugin has been tested for Minecraft versions 1.12 - 1.14.
Improvements include:
* Horses now have genders, and require compatible mates to procreate.
* Foal attributes use a more deterministic formula, with a configurable potential for improvement/regression over their parents' attributes.
* Configure the rate at which males spawn, increasing the desirability of stallions.
* View detailed horse information by shift-right-clicking with a clock, including parentage, attributes, and owner.
* Geld males you've tamed by shift-right-clicking with shears, ensuring control over your prized bloodlines.
* Horses track who their parents are, and inbreeding is... inadvisable.
* Disables shooting your own mount in the back of the head.

# How to install
Download the [latest](https://github.com/Nevakanezah/HorseEnhancer/releases/latest) `HorseEnhancer.jar` file, and copy it into your server's `plugins` folder.

# Commands

|         Command         | Description                                          | Permission                      |
|:-----------------------:|------------------------------------------------------|---------------------------------|
|       `/he help`        | Shows the list of subcommands and their description. | ---                             |
|  `/he summon [gender]`  | Summon horse with specified gender and attributes.   | `horseenhancer.command.summon`  |
| `/he update [horseID]`  | Modify an existing horse's attributes.               | `horseenhancer.command.update`  |
| `/he inspect [horseID]` | Show inspection details for the specified horse.     | `horseenhancer.command.inspect` |
|       `/he list`        | List all registered horse IDs.                       | `horseenhancer.command.list`    |
|      `/he reload`       | Reload plugin configuration.                         | `horseenhancer.command.reload`  |
|        `/he tp`         | Teleport yourself to the specified horse.            | `horseenhancer.command.tp`      |
|      `/he tphere`       | Teleport the specified horse to you.                 | `horseenhancer.command.tphere`  |

# Permissions
The following table includes all the permissions used within the plugin:

|              Permission               | Description                                                                                       |
|:-------------------------------------:|---------------------------------------------------------------------------------------------------|
|    `horseenhancer.command.summon`     | Ability to use `summon` subcommand.                                                               |
|    `horseenhancer.command.update`     | Ability to use `update` subcommand.                                                               |
|    `horseenhancer.command.inspect`    | Ability to use `inspect` subcommand.                                                              |
|     `horseenhancer.command.list`      | Ability to use `list` subcommand.                                                                 |
|    `horseenhancer.command.reload`     | Ability to use `reload` subcommand.                                                               |
|      `horseenhancer.command.tp`       | Ability to use `teleport` subcommand.                                                             |
|    `horseenhancer.command.tphere`     | Ability to use `teleporthere` subcommand.                                                         |
|    `horseenhancer.inspection.wild`    | Informs the player if the horse has a gender but is not tamed.                                    |
|   `horseenhancer.inspection.others`   | Allows player to inspect horses that does not belong to them.                                     |
| `horseenhancer.inspection.attributes` | Shows additional detailed stats for the horse during inspection.                                  |
|     `horseenhancer.testing.reset`     | Used by admins, allows access to reset the horse's owner and tamed state to wild.                 |
|    `horseenhancer.testing.taming`     | Used by admins, allows access to make horses easier to tame by setting taming meter to `max - 1`. |

# Issues
For bug reports and feature suggestions, please [submit an issue via GitHub.](https://github.com/Nevakanezah/HorseEnhancer/issues)

# Future Releases
The following features are planned for addition in later updates to the plugin:
* Horse tamer can control who may ride their horse, or transfer ownership.
* Option to refund the breeding item when incompatible horses attempt to breed.
* Different horse breeds have different attribute trends.

# Special Thanks
Special thanks to the following plugin authors, whose work was an invaluable resource:
* A Flying Poro, for her extensive assistance.
* [AnomalyTea's Horse Inspector](https://github.com/AnomalyTea/Horse-Inspector) - From whom I drew much inspiration on how to design this plugin.
* [Soiyeruda's Better Horses](https://www.spigotmc.org/resources/better-horses.2477/) - Whose attribute determination formula I used in this plugin.
* The human-readable [ProQuints IDs](https://arxiv.org/html/0901.4016) project on Arxiv; whose work was adapted for use in generating horseIDs, and whose authors I've avoided naming for fear of impinging upon their software license.
