package modmenu.forge.gui;

import modmenu.forge.ModBadge;
import modmenu.forge.ModMenu;
import modmenu.forge.ModMenuConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** A recreation of Mod Menu's "Mods" screen for Forge 1.8.9. */
public class ModsScreen extends GuiScreen {
    private static final int DONE = 0, OPEN_FOLDER = 1, FILTERS = 2, SORT = 3, LIBRARIES = 4, CONFIGURE = 5,
            WEBSITE = 6, UPDATES = 7;
    private static final int HEADER_Y = 36;
    private static final String LINK_PREFIX = "" + EnumChatFormatting.BLUE + EnumChatFormatting.UNDERLINE;

    private static String lastSearch = "";
    private static boolean filtersShown = false;
    private static ModContainer lastSelected;

    private final GuiScreen parent;
    private GuiTextField searchBox;
    private ModListWidget list;
    private ModContainer selected;
    private List<ModContainer> allMods;

    private GuiButton configureButton, websiteButton, updatesButton, sortButton, librariesButton;
    private int paneY, paneWidth, rightPaneX, showingY;
    private float descriptionScroll;
    private int descriptionTop;
    private int linkX, linkY, linkWidth = -1;

    /** Hidden title screen used only to draw the rotating panorama behind us. */
    private net.minecraft.client.gui.GuiMainMenu panorama;
    private java.lang.reflect.Method renderSkybox;

    public ModsScreen(GuiScreen parent) {
        this.parent = parent;
        this.selected = lastSelected;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        allMods = ModMenu.getAllMods();
        if (mc.theWorld == null) setupPanorama();

        paneWidth = width / 2 - 8;
        rightPaneX = width - paneWidth;
        showingY = filtersShown ? 64 : 42;
        paneY = showingY + 13;

        int searchBoxX = 8;
        int filterX = paneWidth - 26;
        int searchBoxWidth = filterX - 4 - searchBoxX;
        searchBox = new GuiTextField(99, fontRendererObj, searchBoxX, 18, searchBoxWidth, 18);
        searchBox.setMaxStringLength(64);
        searchBox.setText(lastSearch);
        searchBox.setFocused(true);

        buttonList.clear();
        buttonList.add(new GuiButton(FILTERS, filterX, 17, 20, 20, ""));

        int filterWidth = (filterX + 20 - searchBoxX) / 2 - 1;
        sortButton = new GuiButton(SORT, searchBoxX, 40, filterWidth, 20, "");
        librariesButton = new GuiButton(LIBRARIES, searchBoxX + filterWidth + 2, 40, filterWidth, 20, "");
        sortButton.visible = librariesButton.visible = filtersShown;
        buttonList.add(sortButton);
        buttonList.add(librariesButton);

        configureButton = new GuiButton(CONFIGURE, width - 22, HEADER_Y, 20, 20, "");
        websiteButton = new WideButton(WEBSITE, rightPaneX, HEADER_Y + 37, 100, 20, "Website");
        updatesButton = new WideButton(UPDATES, rightPaneX, HEADER_Y + 37, 100, 20, "Updates");
        buttonList.add(configureButton);
        buttonList.add(websiteButton);
        buttonList.add(updatesButton);

        buttonList.add(new GuiButton(OPEN_FOLDER, width / 2 - 154, height - 28, 150, 20, "Open Mods Folder"));
        buttonList.add(new GuiButton(DONE, width / 2 + 4, height - 28, 150, 20, "Done"));

        list = new ModListWidget(this, 0, paneY, paneWidth, height - 36 - paneY);
        refreshList();
        updateButtons();
    }

    private void setupPanorama() {
        try {
            if (panorama == null) panorama = new net.minecraft.client.gui.GuiMainMenu();
            panorama.setWorldAndResolution(mc, width, height);
            if (renderSkybox == null) {
                renderSkybox = net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(
                        net.minecraft.client.gui.GuiMainMenu.class, panorama,
                        new String[]{"renderSkybox", "func_73971_c"}, int.class, int.class, float.class);
            }
        } catch (Throwable t) {
            renderSkybox = null;
        }
    }

    private void drawBackgroundLayer(int mouseX, int mouseY, float partialTicks) {
        if (mc.theWorld != null) {
            drawDefaultBackground();
            return;
        }
        if (renderSkybox != null) {
            try {
                renderSkybox.invoke(panorama, mouseX, mouseY, partialTicks);
                drawGradientRect(0, 0, width, height, 0x40000000, 0x40000000);
                return;
            } catch (Throwable t) {
                renderSkybox = null;
            }
        }
        drawBackground(0);
    }

    // ---- data -------------------------------------------------------------------------------

    public static String getSummary(ModContainer mod) {
        ModMetadata meta = mod.getMetadata();
        String description = meta == null ? null : meta.description;
        if (mod == Loader.instance().getMinecraftModContainer() && (description == null || description.isEmpty())) {
            return "The base game.";
        }
        if (description == null || description.trim().isEmpty()) return "No description provided.";
        return description.replace("\r", "").replace("\n", " ");
    }

    private static String authors(ModContainer mod) {
        ModMetadata meta = mod.getMetadata();
        if (meta == null || meta.authorList == null || meta.authorList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < meta.authorList.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(meta.authorList.get(i));
        }
        return sb.toString();
    }

    private boolean matchesSearch(ModContainer mod, String query) {
        if (query.isEmpty()) return true;
        String q = query.toLowerCase();
        return mod.getName().toLowerCase().contains(q)
                || mod.getModId().toLowerCase().contains(q)
                || getSummary(mod).toLowerCase().contains(q)
                || authors(mod).toLowerCase().contains(q);
    }

    private void refreshList() {
        String query = searchBox.getText().trim();
        List<ModContainer> filtered = new ArrayList<ModContainer>();
        for (ModContainer mod : allMods) {
            if (!ModMenuConfig.showLibraries && ModBadge.isLibrary(mod) && query.isEmpty()) continue;
            if (matchesSearch(mod, query)) filtered.add(mod);
        }
        Collections.sort(filtered, new Comparator<ModContainer>() {
            @Override
            public int compare(ModContainer a, ModContainer b) {
                // Minecraft always on top, like Mod Menu
                boolean aMc = a == Loader.instance().getMinecraftModContainer();
                boolean bMc = b == Loader.instance().getMinecraftModContainer();
                if (aMc != bMc) return aMc ? -1 : 1;
                int cmp = a.getName().compareToIgnoreCase(b.getName());
                return ModMenuConfig.sortAscending ? cmp : -cmp;
            }
        });
        list.setMods(filtered);
        if (selected == null || !filtered.contains(selected)) {
            select(filtered.isEmpty() ? null : filtered.get(0));
        }
    }

    public ModContainer getSelected() {
        return selected;
    }

    public void select(ModContainer mod) {
        if (mod != selected) descriptionScroll = 0;
        selected = mod;
        lastSelected = mod;
        updateButtons();
    }

    private boolean hasConfig(ModContainer mod) {
        return ConfigCompat.has(mod);
    }

    private String getWebsite(ModContainer mod) {
        if (mod == null) return null;
        if (mod == Loader.instance().getMinecraftModContainer()) return "https://www.minecraft.net/";
        ModMetadata meta = mod.getMetadata();
        if (meta == null || meta.url == null || meta.url.trim().isEmpty()) return null;
        return meta.url.trim();
    }

    private String getUpdateUrl(ModContainer mod) {
        if (mod == null || mod.getMetadata() == null) return null;
        String url = mod.getMetadata().updateUrl;
        return url == null || url.trim().isEmpty() ? null : url.trim();
    }

    private void updateButtons() {
        if (configureButton == null) return;
        configureButton.visible = selected != null && hasConfig(selected);
        websiteButton.visible = getWebsite(selected) != null;
        updatesButton.visible = getUpdateUrl(selected) != null;
        // Like Mod Menu, the link buttons share the right pane's width
        int paneW = width - 2 - rightPaneX;
        if (websiteButton.visible && updatesButton.visible) {
            websiteButton.width = paneW / 2 - 2;
            updatesButton.width = paneW / 2 - 2;
            updatesButton.xPosition = rightPaneX + paneW / 2 + 2;
        } else {
            websiteButton.width = paneW;
            updatesButton.width = paneW;
            updatesButton.xPosition = rightPaneX;
        }
        sortButton.displayString = "Sort: " + (ModMenuConfig.sortAscending ? "A-Z" : "Z-A");
        librariesButton.displayString = "Libraries: " + (ModMenuConfig.showLibraries ? "Shown" : "Hidden");
        boolean hasLinks = websiteButton.visible || updatesButton.visible;
        descriptionTop = HEADER_Y + 37 + (hasLinks ? 25 : 0);
    }

    // ---- input ------------------------------------------------------------------------------

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case DONE:
                mc.displayGuiScreen(parent);
                break;
            case OPEN_FOLDER:
                openFile(new File(mc.mcDataDir, "mods"));
                break;
            case FILTERS:
                filtersShown = !filtersShown;
                initGui();
                break;
            case SORT:
                ModMenuConfig.sortAscending = !ModMenuConfig.sortAscending;
                ModMenuConfig.save();
                refreshList();
                updateButtons();
                break;
            case LIBRARIES:
                ModMenuConfig.showLibraries = !ModMenuConfig.showLibraries;
                ModMenuConfig.save();
                refreshList();
                updateButtons();
                break;
            case CONFIGURE:
                openConfig(selected);
                break;
            case WEBSITE:
                openUrl(getWebsite(selected));
                break;
            case UPDATES:
                openUrl(getUpdateUrl(selected));
                break;
        }
    }

    private void openConfig(ModContainer mod) {
        GuiScreen config = ConfigCompat.create(mod, this);
        if (config == null) return;
        ModMenu.returnHereAfterConfig(this);
        mc.displayGuiScreen(config);
    }

    private static void openFile(File file) {
        file.mkdirs();
        try {
            Desktop.getDesktop().open(file);
        } catch (Throwable t) {
            try {
                org.lwjgl.Sys.openURL(file.toURI().toString());
            } catch (Throwable ignored) {
            }
        }
    }

    private static void openUrl(String url) {
        if (url == null) return;
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable t) {
            try {
                org.lwjgl.Sys.openURL(url);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        List<ModContainer> mods = list.getMods();
        if ((keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) && !mods.isEmpty()) {
            int index = mods.indexOf(selected);
            index = keyCode == Keyboard.KEY_UP ? Math.max(0, index - 1) : Math.min(mods.size() - 1, index + 1);
            select(mods.get(index));
            list.ensureVisible(selected);
            return;
        }
        if (searchBox.textboxKeyTyped(typedChar, keyCode)) {
            if (!searchBox.getText().equals(lastSearch)) {
                lastSearch = searchBox.getText();
                refreshList();
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchBox.mouseClicked(mouseX, mouseY, mouseButton);
        searchBox.setFocused(true);
        list.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && linkWidth > 0 && mouseX >= linkX && mouseX < linkX + linkWidth
                && mouseY >= linkY && mouseY < linkY + 9) {
            openUrl(getWebsite(selected));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        list.mouseReleased();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (list.isMouseOver(mouseX, mouseY)) {
            list.scroll(wheel);
        } else if (mouseX >= rightPaneX && mouseY >= descriptionTop && mouseY < height - 36) {
            descriptionScroll -= wheel > 0 ? 18 : -18;
            descriptionScroll = Math.max(0, descriptionScroll);
        }
    }

    @Override
    public void updateScreen() {
        searchBox.updateCursorCounter();
        if (panorama != null && mc.theWorld == null) panorama.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    // ---- rendering --------------------------------------------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackgroundLayer(mouseX, mouseY, partialTicks);
        list.draw(mouseX, mouseY);

        drawCenteredString(fontRendererObj, "Mods", list.width / 2, 6, 0xFFFFFF);
        searchBox.drawTextBox();
        if (searchBox.getText().isEmpty()) {
            fontRendererObj.drawString(EnumChatFormatting.ITALIC + "Search...", searchBox.xPosition + 4,
                    searchBox.yPosition + 5, 0x808080);
        }

        int shown = list.getMods().size();
        String showing = shown == allMods.size() || (searchBox.getText().isEmpty() && !filtersShown)
                ? "Showing " + shown + " mods"
                : "Showing " + shown + "/" + allMods.size() + " mods";
        fontRendererObj.drawStringWithShadow(showing, 8, showingY, 0xFFFFFF);

        if (selected != null) drawSelectedMod();

        super.drawScreen(mouseX, mouseY, partialTicks);

        drawFilterIcon(buttonList.get(0));
        if (configureButton.visible) drawConfigIcon(configureButton);

        if (configureButton.visible && configureButton.isMouseOver()) {
            drawHoveringText(Collections.singletonList("Configure..."), mouseX, mouseY);
        }
    }

    private void drawSelectedMod() {
        int x = rightPaneX;
        int y = HEADER_Y;
        int maxX = width - (configureButton.visible ? 26 : 4);
        int textX = x + 38;

        ModIcons.draw(selected, x, y, 32);

        String name = RenderUtil.trim(fontRendererObj, selected.getName(), maxX - textX);
        fontRendererObj.drawStringWithShadow(name, textX, y + 1, 0xFFFFFF);

        String version = selected.getDisplayVersion();
        if (version == null || version.isEmpty()) version = selected.getVersion();
        if (version != null && !version.isEmpty()) {
            if (Character.isDigit(version.charAt(0))) version = "v" + version;
            fontRendererObj.drawStringWithShadow(RenderUtil.trim(fontRendererObj, version, maxX - textX), textX, y + 12, 0xAAAAAA);
        }

        String authors = authors(selected);
        if (!authors.isEmpty()) {
            fontRendererObj.drawStringWithShadow(RenderUtil.trim(fontRendererObj, "By " + authors, maxX - textX), textX, y + 23, 0xAAAAAA);
        } else {
            ModBadge.drawAll(fontRendererObj, ModBadge.getBadges(selected), textX, y + 23, maxX);
        }

        drawDescription();
    }

    private void drawDescription() {
        int left = rightPaneX;
        int right = width - 2;
        int top = descriptionTop;
        int bottom = height - 36;
        if (bottom <= top) return;

        RenderUtil.drawListBackground(left, top, right, bottom, descriptionScroll);

        List<String> lines = buildDescriptionLines(right - left - 12);
        int contentHeight = lines.size() * 9 + 8;
        float max = Math.max(0, contentHeight - (bottom - top));
        if (descriptionScroll > max) descriptionScroll = max;

        RenderUtil.scissor(left, top, right - left, bottom - top);
        int y = top + 4 - (int) descriptionScroll;
        linkWidth = -1;
        for (String line : lines) {
            if (y > top - 9 && y < bottom) {
                if (line.startsWith(LINK_PREFIX)) {
                    // Links are indented by position, not spaces, so the underline doesn't run into the indent
                    linkX = left + 4 + fontRendererObj.getStringWidth("  ");
                    linkY = y;
                    linkWidth = fontRendererObj.getStringWidth(line);
                    fontRendererObj.drawStringWithShadow(line, linkX, y, 0xFFFFFF);
                } else {
                    fontRendererObj.drawStringWithShadow(line, left + 4, y, 0xFFFFFF);
                }
            }
            y += 9;
        }
        RenderUtil.endScissor();
    }

    private List<String> buildDescriptionLines(int wrapWidth) {
        List<String> lines = new ArrayList<String>();
        ModMetadata meta = selected.getMetadata();

        for (String paragraph : getSummary(selected).split("\n")) {
            lines.addAll(fontRendererObj.listFormattedStringToWidth(paragraph, wrapWidth));
        }

        if (getWebsite(selected) != null) {
            lines.add("");
            lines.add("Links:");
            lines.add(LINK_PREFIX + "Website");
        }

        String authors = authors(selected);
        boolean hasCredits = meta != null && meta.credits != null && !meta.credits.trim().isEmpty();
        if (!authors.isEmpty() || hasCredits) {
            lines.add("");
            lines.add("Credits:");
            if (!authors.isEmpty()) {
                lines.add("  Authors:");
                for (String l : fontRendererObj.listFormattedStringToWidth(authors, wrapWidth - 16)) lines.add("    " + l);
            }
            if (hasCredits) {
                for (String l : fontRendererObj.listFormattedStringToWidth(meta.credits, wrapWidth - 8)) lines.add("  " + l);
            }
        }

        if (meta != null && meta.childMods != null && !meta.childMods.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ModContainer child : meta.childMods) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(child.getName());
            }
            lines.add("");
            lines.add("Contains:");
            for (String l : fontRendererObj.listFormattedStringToWidth(sb.toString(), wrapWidth - 8)) lines.add("  " + l);
        }
        return lines;
    }

    /** Funnel icon drawn on the filter button, like Mod Menu's. */
    private void drawFilterIcon(GuiButton b) {
        int cx = b.xPosition + 10, y = b.yPosition + 5;
        int c = 0xFFE0E0E0;
        drawRect(cx - 6, y, cx + 6, y + 1, c);
        drawRect(cx - 6, y, cx - 5, y + 2, c);
        drawRect(cx + 5, y, cx + 6, y + 2, c);
        drawRect(cx - 5, y + 2, cx - 3, y + 3, c);
        drawRect(cx + 3, y + 2, cx + 5, y + 3, c);
        drawRect(cx - 3, y + 3, cx - 1, y + 5, c);
        drawRect(cx + 1, y + 3, cx + 3, y + 5, c);
        drawRect(cx - 1, y + 5, cx, y + 10, c);
        drawRect(cx, y + 5, cx + 1, y + 10, c);
    }

    /** Slider-style "settings" icon on the configure button, like Mod Menu's. */
    private void drawConfigIcon(GuiButton b) {
        int x = b.xPosition + 4, y = b.yPosition + 4;
        int c = 0xFFE0E0E0;
        int[] knobs = {8, 3, 6};
        for (int i = 0; i < 3; i++) {
            int ly = y + 2 + i * 5;
            drawRect(x, ly, x + 12, ly + 1, c);
            drawRect(x + knobs[i] - 1, ly - 1, x + knobs[i] + 2, ly + 2, c);
            drawRect(x + knobs[i], ly, x + knobs[i] + 1, ly + 1, 0xFF404040);
        }
    }
}
