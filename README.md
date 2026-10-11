# Glyphide

Glyphide is a small, focused Minecraft plugin for Paper (and forks such as Folia) that gives you flexible, permission-driven chat formatting, including MiniMessage support, legacy color codes, automatic link highlighting with previews, and LuckPerms integration.

> [!TIP]
> Prefer a quick look? Jump to **Usage** or **Configuration** below.

## Table of contents

- [Features](#features)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Placeholders](#placeholders)
- [Commands & Permissions](#commands--permissions)
- [Requirements](#requirements)
- [Contributing & roadmap](#contributing--roadmap)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Features

| Feature                      | What it does                                                                       |
| ---------------------------- | ---------------------------------------------------------------------------------- |
| Dynamic chat formatting      | Format messages with placeholders for prefix/suffix, username, world and message.  |
| LuckPerms integration        | Automatically reads player prefixes/suffixes from LuckPerms.                       |
| MiniMessage & legacy codes   | Use MiniMessage syntax or legacy `§` color codes.                                  |
| Chat token placeholders      | Turn tokens players type, such as `[item]`, into hoverable or clickable components. |
| Link highlighting & previews | Detects links, colors them, and shows the page title on hover; URL info is cached. |
| Configurable                 | Everything in `config.yml`: change formats and permissions without recompiling.    |
| Admin tools                  | `/ghreload` to reload config at runtime.                                           |
| Permission-based styles      | Apply special formatting based on player permissions.                              |

## Quick start

1. Drop the `Glyphide.jar` file into your server's `plugins/` folder.
2. Ensure LuckPerms is installed if you want prefix/suffix support.
3. Start the server (or reload), then edit `plugins/Glyphide/config.yml`.
4. Reload the plugin:

   ```bash
   /ghreload
   ```

## Configuration

Place your formatting in `plugins/Glyphide/config.yml`. A common minimal example:

<details>
<summary>Example `config.yml`</summary>

```yaml
chat:
  format: "$prefix $username > $message"
url:
  color: "#4430cc"
  hover: true
  description-max-length: 15
placeholder:
  item:
    enabled: true
    token: ["[item]", "[i]"]
```

</details>

### Key options

| Option                         | Type            | Default / example                     | Notes                                                               |
| ------------------------------ | --------------- | ------------------------------------- | ------------------------------------------------------------------- |
| `chat.format`                  | string          | `"$prefix $username > $message"`      | See [Placeholders](#placeholders). MiniMessage and `&` codes work.  |
| `url.color`                    | string          | `"#4430cc"`                           | Hex color of highlighted links.                                     |
| `url.hover`                    | boolean         | `true`                                | Show the page title on hover (needs the link to be cached).         |
| `url.description-max-length`   | int             | `15`                                  | Truncation point for the page description in the hover preview.     |
| `placeholder.<id>.enabled`     | boolean         | `false`                               | Whether that placeholder's tokens are substituted at all.           |
| `placeholder.<id>.token`       | list of strings | `["[item]"]`                          | Literal strings players type in chat. Omit to use the shipped list. |
| `use-hypixel-placeholders`     | boolean         | `false`                               | Replaces strings like `o/` and `<3` with better-formatted emoticons.|

> [!IMPORTANT]
> When editing `config.yml`, make sure spacing and indentation are preserved. YAML is whitespace-sensitive.

> [!NOTE]
> Keys you do not recognise are ignored, and any `placeholder.<id>` section Glyphide does not
> implement is reported in the server log rather than breaking chat.

## Placeholders

There are two kinds, and they live in different places.

**Format placeholders** go in `chat.format` and are replaced before the message is parsed:

| Placeholder | Meaning                                     |
| ----------- | ------------------------------------------- |
| `$prefix`   | Player prefix from LuckPerms (if available) |
| `$suffix`   | Player suffix from LuckPerms (if available) |
| `$username` | Player's name                               |
| `$world`    | Current world name                          |
| `$message`  | The message text sent by the player         |

Example format with MiniMessage:

```text
<gold>$prefix</gold> <white>$username</white>: <italic>$message</italic>
```

**Chat token placeholders** are typed by players inside a message and are replaced with rich
components. Each one is configured under its own `placeholder.<id>` section:

| Placeholder | Meaning                                                                     |
| ----------- | --------------------------------------------------------------------------- |
| `item`      | The sender's main-hand item, with a vanilla `SHOW_ITEM` hover.              |
| `hotbar`    | Reserved for a clickable button that opens the sender's hotbar. Unimplemented — do not enable it yet. |

A token only resolves when the placeholder can produce something for that sender: with `item`
enabled but an empty hand, `[item]` stays as typed text instead of being blanked out.

## Commands & Permissions

|   Command   |                  What it does                      |                  Permission                    |
| ----------- | -------------------------------------------------: | ---------------------------------------------- |
| `/ghreload` | Reloads `config.yml` without restarting the server | `glyphide.admin.reload`                        |

`glyphide.admin` additionally unlocks MiniMessage formatting in chat messages for that player;
everyone else gets `&` color codes only.

You can gate formatting options behind your existing permission setup (LuckPerms or other permission plugins) by checking for specific permission nodes in your config or chat pipeline.

## Requirements

- A Paper-compatible Minecraft server (Paper, Folia, etc.)
- Java version compatible with your Paper build
- LuckPerms (optional **but recommended** for prefix/suffix fetching)

## Link previews & caching

Glyphide highlights links and fetches the page title to display on hover. To improve performance:

- Keep `url.description-max-length` short: only that much of the page description is shown.
- Turn previews off entirely with `url.hover: false`.
- Metadata is cached in memory (the last 1000 links); the first sighting of a link has no preview,
  and it appears from the second message onward.

## Troubleshooting

> [!NOTE]
> If chat formatting doesn't appear:
>
> * Check that `config.yml` syntax is valid YAML. A file that does not parse is loaded as an *empty*
>   config, so every setting silently falls back to its default.
> * Ensure LuckPerms is installed and the player has a prefix set (if relying on `$prefix`).
> * Inspect server logs for plugin errors on startup.

If link previews fail intermittently, it’s often due to network restrictions on your server or remote sites blocking requests. Consider disabling previews or increasing cache duration.

## Contributing & roadmap

Contributions are welcome — open an issue or a PR.

**Roadmap**

- [x] Reduce JAR size (current build was ≈ 6 MB)
- [ ] Add per-world format overrides
- [ ] Add configurable mention highlighting

If you open an issue, please include:

- Server version (Paper/Folia + build)
- Java version
- `config.yml` snippet (sanitized)

## License

Glyphide is licensed under MPL-2.0. See the [`LICENSE`](LICENSE) file for details.
