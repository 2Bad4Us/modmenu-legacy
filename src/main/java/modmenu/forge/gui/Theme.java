package modmenu.forge.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

/** Shared colours and panel drawing, so every Mod Menu Legacy screen looks the same. */
public final class Theme {
    public static final int TEXT = 0xFFFFFF;
    public static final int TEXT_BODY = 0xD8D8D8;
    public static final int TEXT_MUTED = 0x9A9A9A;
    public static final int TEXT_HEADING = 0x8C8C8C;
    public static final int LINK = 0x6FA8FF;

    public static final int PANEL_BORDER = 0x40FFFFFF;
    public static final int ROW_HOVER = 0x18FFFFFF;
    public static final int ROW_SELECTED = 0x2EFFFFFF;
    public static final int ROW_SELECTED_BORDER = 0x90FFFFFF;
    public static final int DIVIDER = 0x30FFFFFF;

    private Theme() {
    }

    public static int panelFill() {
        // A little darker over the bright title-screen panorama than over the (already dimmed) world
        return Minecraft.getMinecraft().theWorld != null ? 0x80000000 : 0x90000000;
    }

    /** Translucent panel with a thin light border. */
    public static void panel(int left, int top, int right, int bottom) {
        Gui.drawRect(left, top, right, bottom, panelFill());
        outline(left, top, right, bottom, PANEL_BORDER);
    }

    public static void outline(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, top + 1, color);
        Gui.drawRect(left, bottom - 1, right, bottom, color);
        Gui.drawRect(left, top + 1, left + 1, bottom - 1, color);
        Gui.drawRect(right - 1, top + 1, right, bottom - 1, color);
    }

    public static void divider(int left, int right, int y) {
        Gui.drawRect(left, y, right, y + 1, DIVIDER);
    }
}
