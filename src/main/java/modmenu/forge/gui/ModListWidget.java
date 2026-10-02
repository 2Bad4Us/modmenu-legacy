package modmenu.forge.gui;

import modmenu.forge.ModBadge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.common.ModContainer;

import java.util.ArrayList;
import java.util.List;

/** Left-hand scrolling list of mods: icon, name + badges, and a two-line summary. */
public class ModListWidget {
    public static final int ENTRY_HEIGHT = 36;
    private static final int PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 6;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ModsScreen screen;
    public int x, y, width, height;
    private List<ModContainer> mods = new ArrayList<ModContainer>();
    private float scroll;
    private boolean draggingScrollbar;

    public ModListWidget(ModsScreen screen, int x, int y, int width, int height) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setMods(List<ModContainer> mods) {
        this.mods = mods;
        clampScroll();
    }

    public List<ModContainer> getMods() {
        return mods;
    }

    private int contentHeight() {
        return mods.size() * ENTRY_HEIGHT + PADDING;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height + PADDING);
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private boolean hasScrollbar() {
        return maxScroll() > 0;
    }

    private int rowLeft() {
        return x + PADDING + 2;
    }

    private int rowRight() {
        return x + width - PADDING - (hasScrollbar() ? SCROLLBAR_WIDTH : 0);
    }

    public void ensureVisible(ModContainer mod) {
        int index = mods.indexOf(mod);
        if (index < 0) return;
        int top = index * ENTRY_HEIGHT;
        if (top < scroll) scroll = top;
        else if (top + ENTRY_HEIGHT + PADDING > scroll + height) scroll = top + ENTRY_HEIGHT + PADDING - height;
        clampScroll();
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void draw(int mouseX, int mouseY) {
        if (draggingScrollbar) updateDrag(mouseY);
        FontRenderer font = mc.fontRendererObj;

        RenderUtil.drawListBackground(x, y, x + width, y + height, scroll);

        RenderUtil.scissor(x, y, width, height);
        int left = rowLeft();
        int right = rowRight();
        int rowWidth = right - left;
        for (int i = 0; i < mods.size(); i++) {
            int top = y + PADDING + i * ENTRY_HEIGHT - (int) scroll;
            if (top + ENTRY_HEIGHT < y || top > y + height) continue;
            ModContainer mod = mods.get(i);

            if (mod == screen.getSelected()) {
                Gui.drawRect(left - 2, top - 2, right + 2, top + ENTRY_HEIGHT - 2, 0xFF808080);
                Gui.drawRect(left - 1, top - 1, right + 1, top + ENTRY_HEIGHT - 3, 0xFF000000);
            }

            ModIcons.draw(mod, left, top, 32);

            int textX = left + 32 + 3;
            int textWidth = right - textX - 2;
            String name = RenderUtil.trim(font, mod.getName(), textWidth);
            font.drawString(name, textX, top + 1, 0xFFFFFF);
            ModBadge.drawAll(font, ModBadge.getBadges(mod), textX + font.getStringWidth(name) + 3, top + 1, right);

            List<String> lines = font.listFormattedStringToWidth(ModsScreen.getSummary(mod), textWidth);
            for (int line = 0; line < Math.min(2, lines.size()); line++) {
                String text = lines.get(line);
                if (line == 1 && lines.size() > 2) text = RenderUtil.trim(font, text + "...", textWidth);
                font.drawString(text, textX, top + 12 + line * 9, 0xA0A0A0);
            }
        }
        RenderUtil.endScissor();


        if (hasScrollbar()) {
            int barLeft = x + width - SCROLLBAR_WIDTH;
            int barRight = x + width;
            int thumbHeight = Math.max(32, height * height / contentHeight());
            thumbHeight = Math.min(thumbHeight, height - 8);
            int thumbTop = y + (int) (scroll * (height - thumbHeight) / maxScroll());
            Gui.drawRect(barLeft, y, barRight, y + height, 0xFF000000);
            Gui.drawRect(barLeft, thumbTop, barRight, thumbTop + thumbHeight, 0xFF808080);
            Gui.drawRect(barLeft, thumbTop, barRight - 1, thumbTop + thumbHeight - 1, 0xFFC0C0C0);
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || button != 0) return false;
        if (hasScrollbar() && mouseX >= x + width - SCROLLBAR_WIDTH) {
            draggingScrollbar = true;
            updateDrag(mouseY);
            return true;
        }
        int relY = mouseY - y - PADDING + (int) scroll;
        if (relY < 0) return true;
        int index = relY / ENTRY_HEIGHT;
        if (index >= 0 && index < mods.size() && mouseX >= rowLeft() - 2 && mouseX <= rowRight() + 2) {
            screen.select(mods.get(index));
        }
        return true;
    }

    public void mouseReleased() {
        draggingScrollbar = false;
    }

    private void updateDrag(int mouseY) {
        float progress = (mouseY - y) / (float) height;
        scroll = progress * contentHeight() - height / 2.0F;
        clampScroll();
    }

    public void scroll(int wheel) {
        scroll -= wheel > 0 ? ENTRY_HEIGHT / 2.0F * 1.5F : -ENTRY_HEIGHT / 2.0F * 1.5F;
        clampScroll();
    }
}
