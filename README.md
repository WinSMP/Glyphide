# Glyphide

Glyphide is a small, focused Minecraft plugin for Paper (and forks such as Folia) that gives you flexible, permission-driven chat formatting — including MiniMessage support, legacy color codes, automatic link highlighting with previews, and LuckPerms integration.

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
| Link highlighting & previews | Detects links, colors them, and shows the page title on hover; URL info is cached. |
| Configurable                 | Everything in `config.yml`: change formats and permissions without recompiling.    |
| Admin tools                  | `/ghreload` to reload config at runtime.                                           |
| Permission-based styles      | Apply special formatting based on player permissions.                              |

## Quick start

1. Drop the `Glyphide.jar` into your server's `plugins/` folder.
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
linkPreview:
  enabled: true
  cacheDurationSeconds: 3600
```

</details>

### Key options

| Option | Type  | Default / example | Notes |
| ---: | :---: | :--- | --- |
| `chat.format` | string | `"$prefix $username > $message"` | Use placeholders (see below). Supports MiniMessage and legacy color codes. |
| `linkPreview.enabled` | boolean | `true` | Toggle fetching page titles for links. |
| `linkPreview.cacheDurationSeconds` | int | `3600` | How long to cache fetched link metadata. |

> [!IMPORTANT]
> When editing `config.yml`, make sure spacing and indentation are preserved. YAML is whitespace-sensitive.

## Placeholders

Use these in `chat.format`:

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

## Commands & Permissions

|   Command   |                  What it does                      |                  Permission                    |
| ----------- | -------------------------------------------------: | ---------------------------------------------- |
| `/ghreload` | Reloads `config.yml` without restarting the server | `glyphide.reload` (recommended for ops/admins) |

You can gate formatting options behind your existing permission setup (LuckPerms or other permission plugins) by checking for specific permission nodes in your config or chat pipeline.

## Requirements

- A Paper-compatible Minecraft server (Paper, Folia, etc.)
- Java version compatible with your Paper build
- LuckPerms (optional **but recommended** for prefix/suffix fetching)

## Link previews & caching

Glyphide highlights links and fetches the page title to display on hover. To improve performance:

- Keep `linkPreview.cacheDurationSeconds` at a sensible value (default `3600`).
- If you run into rate limits or want to disable preview fetching, set `linkPreview.enabled: false`.

## Troubleshooting

> [!INFO]
> If chat formatting doesn't appear:
>
> * Check that `config.yml` syntax is valid YAML.
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
