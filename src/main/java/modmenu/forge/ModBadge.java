package modmenu.forge;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.ArrayList;
import java.util.List;

/** The small coloured tags drawn next to mod names, using Mod Menu's colours. */
public enum ModBadge {
    MINECRAFT("Minecraft", 0xff6f6c6a, 0xff31302f),
    LIBRARY("Library", 0xff107454, 0xff093929),
    CLIENT("Client", 0xff2b4b7c, 0xff0e2a55);

    public final String text;
    public final int outline;
    public final int fill;

    ModBadge(String text, int outline, int fill) {
        this.text = text;
        this.outline = outline;
        this.fill = fill;
    }

    public static boolean isMinecraft(ModContainer mod) {
        String id = mod.getModId();
        return mod == Loader.instance().getMinecraftModContainer()
                || "mcp".equals(id) || "FML".equals(id) || "Forge".equals(id) || "forge".equals(id);
    }

    public static boolean isLibrary(ModContainer mod) {
        if (isMinecraft(mod)) return false;
        String id = mod.getModId().toLowerCase();
        String name = mod.getName() == null ? "" : mod.getName().toLowerCase();
        if (mod.getMetadata() != null && mod.getMetadata().parent != null && !mod.getMetadata().parent.isEmpty()) {
            return true;
        }
        return id.endsWith("lib") || id.endsWith("api") || id.endsWith("core")
                || name.contains("library") || name.endsWith(" lib") || name.endsWith(" api") || name.endsWith(" core");
    }

    public static List<ModBadge> getBadges(ModContainer mod) {
        List<ModBadge> badges = new ArrayList<ModBadge>();
        if (isMinecraft(mod)) badges.add(MINECRAFT);
        if (isLibrary(mod)) badges.add(LIBRARY);
        if (ModMenu.MODID.equals(mod.getModId())) badges.add(CLIENT);
        return badges;
    }

    /** Draws the badge and returns its width. */
    public int draw(FontRenderer font, int x, int y) {
        int width = font.getStringWidth(text) + 6;
        int height = font.FONT_HEIGHT + 2;
        Gui.drawRect(x + 1, y - 1, x + width - 1, y, outline);
        Gui.drawRect(x, y, x + 1, y + height - 1, outline);
        Gui.drawRect(x + 1, y + height - 1, x + width - 1, y + height, outline);
        Gui.drawRect(x + width - 1, y, x + width, y + height - 1, outline);
        Gui.drawRect(x + 1, y, x + width - 1, y + height - 1, fill);
        font.drawString(text, x + 3, y + 1, 0xCACACA);
        return width;
    }

    public static int drawAll(FontRenderer font, List<ModBadge> badges, int x, int y, int maxX) {
        int cx = x;
        for (ModBadge badge : badges) {
            int w = font.getStringWidth(badge.text) + 6;
            if (cx + w > maxX) break;
            cx += badge.draw(font, cx, y) + 3;
        }
        return cx;
    }
}
