# CustomItemsPlugin

CustomItemsPlugin is a Minecraft plugin that adds custom weapons and abilities, along with a GUI selector for players to easily access them.

## Features

- **Custom Weapons and Abilities**:
    - **Stormwalker’s Blade**: Launch enemies upward with water.
    - **Infernoheart Chestplate**: Ignite enemies and gain regeneration/fire resistance when under 4 hearts.
    - **Phantomstep Boots**: Double jump to dash where you're looking.
    - **Celestial Fang**: Call a meteor strike.
    - **Glacierbound Greaves**: Gain Speed II while wearing.
    - **Tempest Helm**: Boost upward.
    - **Voided Scythe**: Pull the nearest enemy.
    - **Bloodthorn Blade**: Enter a frenzy with 100x attack speed.
    - **Sunforged Aegis**: Knock back enemies and block projectiles.
    - **Serpentfang Bow**: Shoot explosive TNT.

- **GUI Weapon Selector**: Use `/weaponsgui` to open a GUI and select weapons.

## Commands

| Command       | Description                          | Permission                |
|---------------|--------------------------------------|---------------------------|
| `/weaponsgui` | Opens the weapon selector GUI.       | `customitems.weaponsgui`  |

## Permissions

| Permission                | Description                              | Default |
|---------------------------|------------------------------------------|---------|
| `customitems.weaponsgui`  | Allows the player to open the weapons GUI. | `true`  |

## Installation

1. Download the plugin `.jar` file.
2. Place it in the `plugins` folder of your Minecraft server.
3. Restart the server.

## Usage

1. Use `/weaponsgui` to open the weapon selector.
2. Select a weapon from the GUI to add it to your inventory.
3. Use the weapons to activate their unique abilities.

## Requirements

- Minecraft server version 1.21 or higher.
- Java 17 or higher.

## Development

This plugin is built using:
- **Java**
- **Maven**

### Building the Plugin

1. Clone the repository.
2. Run `mvn clean package` to build the `.jar` file.
3. The compiled `.jar` will be located in the `target` directory.

## License

This project is licensed under the MIT License.