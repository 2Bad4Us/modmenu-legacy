# Mod Menu Legacy

**A modern, searchable mod list for Minecraft Forge 1.8.9, in the style of Fabric's Mod Menu.**

![Mods screen](screenshot%202.png)

Forge's built-in mod list hasn't changed since 2015. Mod Menu Legacy replaces it with the clean two-pane browser you know from modern versions: search your mods, see their details at a glance, and open their settings in one click.

---

## ✨ Features

### 📋 A better mod list
- Every mod shown with its **icon, name and summary**
- Coloured **badges** (Minecraft, Library, Client), like the original Mod Menu
- A details pane with **version, authors, description, credits and links**
- Smooth scrolling, keyboard navigation (↑ / ↓) and remembered selection

### 🔍 Search & filters
- Search by **name, mod ID, description or author**
- Sort **A-Z / Z-A**
- Show or hide **library mods** to keep the list tidy

### ⚙️ One-click config
The **Configure** button finds a mod's settings screen automatically:
1. The standard Forge config screen, for most mods
2. Built-in support for mods that normally open settings only by keybind (e.g. *Custom Crosshair Mod*)
3. A smart search of the mod's files for its settings screen

When you close a mod's settings, you go **back to the Mods screen**, not out to the game.

### 🏠 Menu integration
- **Title screen:** a full-width **Mods (N)** button showing how many mods you have. Realms becomes a small button on the side.
- **Pause menu:** a full-width **Mods** button
- Rotating **panorama background** on the title screen, see-through panels in-game
- **Open Mods Folder** button

### 🪶 Lightweight
- **Client-side only.** Works on any server, including Hypixel and other vanilla servers.
- No dependencies, about 40 KB

---

## 📥 Installation

1. Install **[Minecraft Forge 1.8.9](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html)**
2. Download `modmenu-legacy-1.8.9-<version>.jar`
3. Put it in your `.minecraft/mods` folder
4. Launch the game and click **Mods** on the title screen

---

## 🔧 Configuration

Open **Mods → Mod Menu Legacy → Configure**, or edit `config/modmenu.properties`:

| Option | Default | Description |
|---|---|---|
| `sort_ascending` | `true` | Sort mods A-Z (`false` = Z-A) |
| `show_libraries` | `false` | Show library mods in the list |
| `show_mod_count` | `true` | Show the mod count on the title screen button |

---

## ❓ FAQ

**Does it work with my other mods?**
Yes. Every Forge 1.8.9 mod is listed automatically. If a mod has a settings screen, the Configure button opens it.

**A mod has settings, but there's no Configure button.**
Some mods only open their settings with a keybind or chat command, and nothing else can reach those screens. Open an issue with the mod's name and support can be added.

**Does it work with Fabric or newer versions?**
No. It's for Forge 1.8.9 only. For newer versions, use the original [Mod Menu](https://modrinth.com/mod/modmenu).

**Can I use it in a modpack?**
Yes. It's MIT-licensed.

---

## 🛠️ Building from source

You need Java 17 or newer to run Gradle. The Java 8 toolchain used for compiling is downloaded automatically.

```
./gradlew build
```

The release jar is written to `build/libs/`. Run `./gradlew runClient` to test in a development client.

**Adding config support for a mod:** if a mod only opens settings by keybind or command, add an entry to `KNOWN` in
`src/main/java/modmenu/forge/gui/ConfigCompat.java`.

---

## 📜 Credits & License

- Made by **Craftbyte**
- Inspired by [Mod Menu](https://github.com/TerraformersMC/ModMenu) by TerraformersMC. This is an independent project: no code or assets were copied, and it is not affiliated with or endorsed by TerraformersMC.
- Licensed under the **MIT License**. See [LICENSE](LICENSE).
