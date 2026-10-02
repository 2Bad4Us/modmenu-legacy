package modmenu.forge.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Clean flat button: translucent dark fill, thin border that lights up on hover. Works at any width. */
public class FlatButton extends GuiButton {
    /** Draws an icon on top of the button; may be null. */
    public interface Icon {
        void draw(int x, int y, int width, int height, int color);
    }

    private Icon icon;
    /** Highlighted look for toggle buttons that are "on" (e.g. the filter button while filters are open). */
    public boolean active;

    public FlatButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    public FlatButton withIcon(Icon icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        boolean lit = enabled && (hovered || active);

        int fill = !enabled ? 0x60000000 : hovered ? 0xA0303030 : active ? 0x90262626 : 0x90000000;
        int border = !enabled ? 0x30FFFFFF : hovered ? 0xE0FFFFFF : active ? 0xA0FFFFFF : 0x50FFFFFF;
        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, fill);
        Theme.outline(xPosition, yPosition, xPosition + width, yPosition + height, border);

        int textColor = !enabled ? 0x707070 : lit ? 0xFFFFFF : 0xD0D0D0;
        if (icon != null) icon.draw(xPosition, yPosition, width, height, 0xFF000000 | textColor);
        if (displayString != null && !displayString.isEmpty()) {
            drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, textColor);
        }
    }
}
