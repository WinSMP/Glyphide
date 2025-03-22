# ChatFormatter

ChatFormatter is a Minecraft plugin designed to enhance and customize the chat experience on Minecraft servers running Paper or its forks, such as Folia.

It integrates with LuckPerms to dynamically format chat messages based on player permissions, prefixes, and suffixes.

The plugin supports advanced text formatting using MiniMessage and legacy Minecraft formatting codes.

## Features

- **Dynamic Chat Formatting**: Customize chat messages with placeholders for player prefixes, suffixes, usernames, and world names.
- **LuckPerms Integration**: Automatically fetches player prefixes and suffixes from LuckPerms.
- **Advanced Text Formatting**: Supports MiniMessage and legacy Minecraft formatting codes for rich text formatting.
- **Customizable Configuration**: Easily configure chat formats and permissions via `config.yml`.
- **Admin Commands**: Includes a `/chatformatter reload` command to reload the configuration without restarting the server.
- **Permission-Based Formatting**: Allows administrators to apply special formatting to their messages.

## Requirements

- Minecraft server running a Paper server, or any fork, such as Folia.
- LuckPerms plugin (required for fetching player prefixes and suffixes).

## Configuration

The configuration file (`config.yml`) should look like this:

```yaml
chat:
  format: "$prefix $username > $message"
```

### Configuration Parameters

- **chat.format**: The format of the chat message. Supports the following placeholders:
  - `$prefix`: Player's prefix (fetched from LuckPerms).
  - `$suffix`: Player's suffix (fetched from LuckPerms).
  - `$username`: Player's username.
  - `$world`: The world the player is currently in.
  - `$message`: The message sent by the player.

## Contributing

Contributions are welcome! If you have suggestions for improvements or new features, feel free to open an issue or submit a pull request.

### Roadmap

- [ ] Add support for two-way private messages between Discord and Minecraft.
- [ ] Reduce JAR size (since now it's ~6MB).

For any questions or support, please contact me or open an issue on this GitHub repository.

## License

This project is licensed under the Apache-2.0 License. See the [LICENSE](LICENSE) file for more details.
