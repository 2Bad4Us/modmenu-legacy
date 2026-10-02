package modmenu.forge.gui;

import modmenu.forge.ModMenuConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;

/** Mod Menu Legacy's own options screen, reachable from its "Configure" button. */
public class ModMenuConfigScreen extends GuiScreen {
    private static final int DONE = 0, SORT = 1, LIBRARIES = 2, MOD_COUNT = 3;
    private static final int ROW_HEIGHT = 26;
    private static final int PANEL_WIDTH = 300;

    private final GuiScreen parent;
    private int panelLeft, panelTop;

    public ModMenuConfigScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        panelLeft = width / 2 - PANEL_WIDTH / 2;
        panelTop = Math.max(36, height / 2 - 60);
        int buttonX = panelLeft + PANEL_WIDTH - 8 - 70;
        for (int i = 0; i < 3; i++) {
            buttonList.add(new FlatButton(SORT + i, buttonX, panelTop + 8 + i * ROW_HEIGHT + 3, 70, 16, ""));
        }
        buttonList.add(new FlatButton(DONE, width / 2 - 75, panelTop + 8 + 3 * ROW_HEIGHT + 16, 150, 18, "Done"));
        updateLabels();
    }

    private void updateLabels() {
        for (GuiButton b : buttonList) {
            if (b.id == SORT) b.displayString = ModMenuConfig.sortAscending ? "A-Z" : "Z-A";
            if (b.id == LIBRARIES) b.displayString = onOff(ModMenuConfig.showLibraries);
            if (b.id == MOD_COUNT) b.displayString = onOff(ModMenuConfig.showModCount);
        }
    }

    private static String onOff(boolean value) {
        return value ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case DONE:
                mc.displayGuiScreen(parent);
                return;
            case SORT:
                ModMenuConfig.sortAscending = !ModMenuConfig.sortAscending;
                break;
            case LIBRARIES:
                ModMenuConfig.showLibraries = !ModMenuConfig.showLibraries;
                break;
            case MOD_COUNT:
                ModMenuConfig.showModCount = !ModMenuConfig.showModCount;
                break;
        }
        ModMenuConfig.save();
        updateLabels();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, EnumChatFormatting.BOLD + "Mod Menu Legacy", width / 2, panelTop - 24, Theme.TEXT);

        int panelBottom = panelTop + 8 + 3 * ROW_HEIGHT + 4;
        Theme.panel(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelBottom);
        String[][] rows = {
                {"Sorting", "Order of the mod list"},
                {"Show libraries", "List library and API mods"},
                {"Mod count on title screen", "Shows \"Mods (N)\""},
        };
        for (int i = 0; i < rows.length; i++) {
            int y = panelTop + 8 + i * ROW_HEIGHT;
            fontRendererObj.drawString(rows[i][0], panelLeft + 10, y + 2, Theme.TEXT);
            fontRendererObj.drawString(rows[i][1], panelLeft + 10, y + 12, Theme.TEXT_MUTED);
            if (i < rows.length - 1) Theme.divider(panelLeft + 8, panelLeft + PANEL_WIDTH - 8, y + ROW_HEIGHT - 2);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
