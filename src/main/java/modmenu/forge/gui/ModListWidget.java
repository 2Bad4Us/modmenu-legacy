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
    public static final int ROW_HEIGHT = 40;
    private static final int PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int ICON = 32;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ModsScreen screen;
    public int x, y, width, height;
    private List<ModContainer> mods = new ArrayList<ModContainer>();
    /** Where the list is drawn (eases towards targetScroll for smooth scrolling). */
    private float scroll;
    private float targetScroll;
    private boolean draggingScrollbar;
    private int dragOffset;

    public ModListWidget(ModsScreen screen, int x, int y, int width, int height) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setMods(List<ModContainer> mods) {
        this.mods = mods;
        targetScroll = clamp(targetScroll);
        scroll = clamp(scroll);
    }

    public List<ModContainer> getMods() {
        return mods;
    }

    private int contentHeight() {
        return mods.size() * ROW_HEIGHT + PADDING * 2;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    private float clamp(float value) {
        return Math.max(0, Math.min(value, maxScroll()));
    }

    private boolean hasScrollbar() {
        return maxScroll() > 0;
    }

    private int rowLeft() {
        return x + PADDING;
    }

    private int rowRight() {
        return x + width - PADDING - (hasScrollbar() ? SCROLLBAR_WIDTH + 3 : 0);
    }

    private int rowTop(int index) {
        return y + PADDING + index * ROW_HEIGHT - Math.round(scroll);
    }

    public void ensureVisible(ModContainer mod) {
        int index = mods.indexOf(mod);
        if (index < 0) return;
        int top = PADDING + index * ROW_HEIGHT;
        if (top < targetScroll) targetScroll = top - PADDING;
        else if (top + ROW_HEIGHT + PADDING > targetScroll + height) targetScroll = top + ROW_HEIGHT + PADDING - height;
        targetScroll = clamp(targetScroll);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int indexAt(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY) || mouseX < rowLeft() || mouseX >= rowRight()) return -1;
        int rel = mouseY - y - PADDING + Math.round(scroll);
        if (rel < 0) return -1;
        int index = rel / ROW_HEIGHT;
        if (rel % ROW_HEIGHT >= ROW_HEIGHT - 2) return -1; // gap between rows
        return index < mods.size() ? index : -1;
    }

    public void draw(int mouseX, int mouseY) {
        if (draggingScrollbar) updateDrag(mouseY);
        // Ease towards the target; snaps once close enough
        scroll += (targetScroll - scroll) * 0.45F;
        if (Math.abs(targetScroll - scroll) < 0.5F) scroll = targetScroll;

        FontRenderer font = mc.fontRendererObj;
        Theme.panel(x, y, x + width, y + height);

        RenderUtil.scissor(x + 1, y + 1, width - 2, height - 2);
        int left = rowLeft();
        int right = rowRight();
        int hover = draggingScrollbar ? -1 : indexAt(mouseX, mouseY);
        for (int i = 0; i < mods.size(); i++) {
            int top = rowTop(i);
            if (top + ROW_HEIGHT < y || top > y + height) continue;
            ModContainer mod = mods.get(i);
            int bottom = top + ROW_HEIGHT - 2;

            if (mod == screen.getSelected()) {
                Gui.drawRect(left, top, right, bottom, Theme.ROW_SELECTED);
                Theme.outline(left, top, right, bottom, Theme.ROW_SELECTED_BORDER);
            } else if (i == hover) {
                Gui.drawRect(left, top, right, bottom, Theme.ROW_HOVER);
            }

            ModIcons.draw(mod, left + 3, top + 3, ICON);

            int textX = left + ICON + 9;
            int textWidth = right - textX - 4;
            String name = RenderUtil.trim(font, mod.getName(), textWidth);
            font.drawStringWithShadow(name, textX, top + 5, Theme.TEXT);
            ModBadge.drawAll(font, ModBadge.getBadges(mod), textX + font.getStringWidth(name) + 4, top + 5, right - 2);

            List<String> lines = font.listFormattedStringToWidth(ModsScreen.getSummary(mod), textWidth);
            for (int line = 0; line < Math.min(2, lines.size()); line++) {
                String text = lines.get(line);
                if (line == 1 && lines.size() > 2) text = RenderUtil.trim(font, text + "...", textWidth);
                font.drawString(text, textX, top + 17 + line * 9, Theme.TEXT_MUTED);
            }
        }
        RenderUtil.endScissor();

        if (hasScrollbar()) {
            int barX = x + width - PADDING - SCROLLBAR_WIDTH;
            int trackTop = y + PADDING;
            int trackHeight = height - PADDING * 2;
            int thumbHeight = Math.max(20, trackHeight * height / contentHeight());
            int thumbTop = trackTop + Math.round(scroll * (trackHeight - thumbHeight) / maxScroll());
            boolean barHover = draggingScrollbar || (mouseX >= barX - 3 && mouseX < x + width && isMouseOver(mouseX, mouseY));
            Gui.drawRect(barX, trackTop, barX + SCROLLBAR_WIDTH, trackTop + trackHeight, 0x20FFFFFF);
            Gui.drawRect(barX, thumbTop, barX + SCROLLBAR_WIDTH, thumbTop + thumbHeight, barHover ? 0xC0FFFFFF : 0x70FFFFFF);
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || button != 0) return false;
        if (hasScrollbar() && mouseX >= x + width - PADDING - SCROLLBAR_WIDTH - 3) {
            draggingScrollbar = true;
            dragOffset = -1;
            updateDrag(mouseY);
            return true;
        }
        int index = indexAt(mouseX, mouseY);
        if (index >= 0) screen.select(mods.get(index));
        return true;
    }

    public void mouseReleased() {
        draggingScrollbar = false;
    }

    private void updateDrag(int mouseY) {
        float progress = (mouseY - y - PADDING) / (float) (height - PADDING * 2);
        targetScroll = clamp(progress * contentHeight() - height / 2.0F);
        scroll = targetScroll;
    }

    public void scroll(int wheel) {
        targetScroll = clamp(targetScroll + (wheel > 0 ? -ROW_HEIGHT : ROW_HEIGHT));
    }
}
