# Time Loop Utility

A Minecraft 1.12.2 mod for creating configurable time loops through the use of backups. Works in any dimension and each can have their own start and end time.

### Usage

- Download the mod on [curseforge](https://www.curseforge.com/minecraft/mc-mods/timeloop)
- Run the server to generate ./config/timeloop.json
- modify the config to specify what dimensions, player data, etc should be reset.

### timeloop.json example

```java
{
    "keepInventory": true,
    "serverDisconnectMsg": "End of loop reached. World has been reset.",
    "dimensions": [
        "0": {
            "id": 0,
            "loopStart": 0,
            "loopEnd": 12000,
            "doLoop": true,
        }
    ]
}
```

This config example specifies a loop which only occurs in the overworld (dimension 0) at 12000 ticks and resets the world to 0 ticks when the end of the loop is reached. "keepInventory" is enabled to reset player's data such as inventory items during the loop. More dimensions can be added to the loop config by adding additional entries to the "dimensions" array.
