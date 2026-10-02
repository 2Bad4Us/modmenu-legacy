package modmenu.forge.gui;

import modmenu.forge.ModMenuConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/** Mod Menu's own options screen, reachable from its "Configure" button. */
public class ModMenuConfigScreen extends GuiScreen {
    private static final int DONE = 0, SORT = 1, LIBRARIES = 2, MOD_COUNT = 3;

    private final GuiScreen parent;

    public ModMenuConfigScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int x = width / 2 - 155;
        buttonList.add(new GuiButton(SORT, x, height / 6, 150, 20, ""));
        buttonList.add(new GuiButton(LIBRARIES, x + 160, height / 6, 150, 20, ""));
        buttonList.add(new GuiButton(MOD_COUNT, x, height / 6 + 24, 150, 20, ""));
        buttonList.add(new GuiButton(DONE, width / 2 - 100, height - 28, 200, 20, "Done"));
        updateLabels();
    }

    private void updateLabels() {
        for (GuiButton b : buttonList) {
            if (b.id == SORT) b.displayString = "Sorting: " + (ModMenuConfig.sortAscending ? "A-Z" : "Z-A");
            if (b.id == LIBRARIES) b.displayString = "Show Libraries: " + (ModMenuConfig.showLibraries ? "ON" : "OFF");
            if (b.id == MOD_COUNT) b.displayString = "Title Mod Count: " + (ModMenuConfig.showModCount ? "ON" : "OFF");
        }
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
        drawCenteredString(fontRendererObj, "Mod Menu Legacy Options", width / 2, 15, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
