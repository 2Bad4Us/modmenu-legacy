package modmenu.forge.gui;

import modmenu.forge.ModBadge;
import modmenu.forge.ModInfo;
import modmenu.forge.ModMenu;
import modmenu.forge.ModMenuConfig;
import net.minecraft.client.gui.Gui;
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
    private static final int MARGIN = 8;
    private static final int GAP = 8;
    private static final int PAD = 8;
    private static final int LINE = 10;

    private static String lastSearch = "";
    private static boolean filtersShown = false;
    private static ModContainer lastSelected;

    private final GuiScreen parent;
    private GuiTextField searchBox;
    private ModListWidget list;
    private ModContainer selected;
    private List<ModContainer> allMods;

    private FlatButton filterButton, configureButton, websiteButton, updatesButton, sortButton, librariesButton;

    // Layout
    private int leftX, leftRight, rightX, rightRight, panelTop, panelBottom;
    private int searchX, searchY, searchRight, searchBottom;
    private int descriptionTop;

    private float descriptionScroll, descriptionTarget;
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

        int columnWidth = (width - MARGIN * 2 - GAP) / 2;
        leftX = MARGIN;
        leftRight = leftX + columnWidth;
        rightX = leftRight + GAP;
        rightRight = width - MARGIN;

        // Search bar + filter toggle
        searchX = leftX;
        searchY = 24;
        searchBottom = searchY + 18;
        searchRight = leftRight - 22;
        searchBox = new GuiTextField(99, fontRendererObj, searchX + 18, searchY + 5, searchRight - searchX - 18 - 52, 10);
        searchBox.setEnableBackgroundDrawing(false);
        searchBox.setMaxStringLength(64);
        searchBox.setText(lastSearch);
        searchBox.setFocused(true);

        buttonList.clear();
        filterButton = new FlatButton(FILTERS, leftRight - 18, searchY, 18, 18, "").withIcon(new FlatButton.Icon() {
            @Override
            public void draw(int x, int y, int w, int h, int color) {
                drawFilterIcon(x, y, w, h, color);
            }
        });
        filterButton.active = filtersShown;
        buttonList.add(filterButton);

        int half = (leftRight - leftX - 4) / 2;
        sortButton = new FlatButton(SORT, leftX, searchBottom + 4, half, 16, "");
        librariesButton = new FlatButton(LIBRARIES, leftX + half + 4, searchBottom + 4, leftRight - leftX - half - 4, 16, "");
        sortButton.visible = librariesButton.visible = filtersShown;
        buttonList.add(sortButton);
        buttonList.add(librariesButton);

        panelTop = filtersShown ? searchBottom + 26 : searchBottom + 6;
        panelBottom = height - 32;

        configureButton = new FlatButton(CONFIGURE, rightRight - PAD - 18, panelTop + PAD, 18, 18, "")
                .withIcon(new FlatButton.Icon() {
                    @Override
                    public void draw(int x, int y, int w, int h, int color) {
                        drawConfigIcon(x, y, color);
                    }
                });
        websiteButton = new FlatButton(WEBSITE, rightX + PAD, panelTop + 48, 100, 18, "Website");
        updatesButton = new FlatButton(UPDATES, rightX + PAD, panelTop + 48, 100, 18, "Updates");
        buttonList.add(configureButton);
        buttonList.add(websiteButton);
        buttonList.add(updatesButton);

        buttonList.add(new FlatButton(OPEN_FOLDER, width / 2 - 154, height - 25, 150, 18, "Open Mods Folder"));
        buttonList.add(new FlatButton(DONE, width / 2 + 4, height - 25, 150, 18, "Done"));

        list = new ModListWidget(this, leftX, panelTop, leftRight - leftX, panelBottom - panelTop);
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
            drawGradientRect(0, 0, width, height, 0x90101010, 0xB0101010);
            return;
        }
        if (renderSkybox != null) {
            try {
                renderSkybox.invoke(panorama, mouseX, mouseY, partialTicks);
                drawGradientRect(0, 0, width, height, 0x50000000, 0x70000000);
                return;
            } catch (Throwable t) {
                renderSkybox = null;
            }
        }
        drawBackground(0);
    }

    // ---- data -------------------------------------------------------------------------------

    public static String getSummary(ModContainer mod) {
        String description = ModInfo.get(mod).description;
        if (mod == Loader.instance().getMinecraftModContainer() && (description == null || description.isEmpty())) {
            return "The base game.";
        }
        if (description == null || description.trim().isEmpty()) return "No description provided.";
        return description.replace("\r", "").replace("\n", " ");
    }

    private static String authors(ModContainer mod) {
        List<String> list = ModInfo.get(mod).authors;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i));
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
        if (mod != selected) descriptionScroll = descriptionTarget = 0;
        selected = mod;
        lastSelected = mod;
        if (list != null && mod != null) list.ensureVisible(mod);
        updateButtons();
    }

    private String getWebsite(ModContainer mod) {
        if (mod == null) return null;
        if (mod == Loader.instance().getMinecraftModContainer()) return "https://www.minecraft.net/";
        return ModInfo.get(mod).url;
    }

    private String getUpdateUrl(ModContainer mod) {
        if (mod == null || mod.getMetadata() == null) return null;
        String url = mod.getMetadata().updateUrl;
        return url == null || url.trim().isEmpty() ? null : url.trim();
    }

    private void updateButtons() {
        if (configureButton == null) return;
        configureButton.visible = selected != null && ConfigCompat.has(selected);
        websiteButton.visible = getWebsite(selected) != null;
        updatesButton.visible = getUpdateUrl(selected) != null;
        // The link buttons share the details panel's width
        int left = rightX + PAD;
        int full = rightRight - PAD - left;
        if (websiteButton.visible && updatesButton.visible) {
            websiteButton.width = (full - 4) / 2;
            updatesButton.width = full - websiteButton.width - 4;
            updatesButton.xPosition = left + websiteButton.width + 4;
        } else {
            websiteButton.width = full;
            updatesButton.width = full;
            updatesButton.xPosition = left;
        }
        sortButton.displayString = "Sort: " + (ModMenuConfig.sortAscending ? "A-Z" : "Z-A");
        librariesButton.displayString = "Libraries: " + (ModMenuConfig.showLibraries ? "Shown" : "Hidden");
        boolean hasLinks = websiteButton.visible || updatesButton.visible;
        descriptionTop = panelTop + 48 + (hasLinks ? 26 : 0);
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
                && mouseY >= linkY - 1 && mouseY < linkY + 9) {
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
        } else if (mouseX >= rightX && mouseX < rightRight && mouseY >= descriptionTop && mouseY < panelBottom) {
            descriptionTarget = Math.max(0, descriptionTarget + (wheel > 0 ? -LINE * 3 : LINE * 3));
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

        drawCenteredString(fontRendererObj, EnumChatFormatting.BOLD + "Mods", width / 2, 9, Theme.TEXT);

        drawSearchBar();
        list.draw(mouseX, mouseY);

        Theme.panel(rightX, panelTop, rightRight, panelBottom);
        if (selected != null) {
            drawSelectedMod(mouseX, mouseY);
        } else {
            drawCenteredString(fontRendererObj, "No mods found", (rightX + rightRight) / 2, (panelTop + panelBottom) / 2 - 4, Theme.TEXT_MUTED);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (configureButton.visible && configureButton.isMouseOver()) {
            drawHoveringText(Collections.singletonList("Configure..."), mouseX, mouseY);
        } else if (filterButton.isMouseOver()) {
            drawHoveringText(Collections.singletonList(filtersShown ? "Hide filters" : "Show filters"), mouseX, mouseY);
        }
    }

    private void drawSearchBar() {
        boolean focused = searchBox.isFocused();
        drawRect(searchX, searchY, searchRight, searchBottom, 0xB0000000);
        Theme.outline(searchX, searchY, searchRight, searchBottom, focused ? 0xA0FFFFFF : 0x50FFFFFF);
        drawSearchIcon(searchX + 6, searchY + 5, 0xFF9A9A9A);

        searchBox.drawTextBox();
        if (searchBox.getText().isEmpty()) {
            fontRendererObj.drawString(EnumChatFormatting.ITALIC + "Search mods...", searchX + 18, searchY + 5, 0x6E6E6E);
        }

        int shown = list.getMods().size();
        String count = shown == allMods.size() || (searchBox.getText().isEmpty() && !filtersShown)
                ? shown + (shown == 1 ? " mod" : " mods")
                : shown + " / " + allMods.size();
        fontRendererObj.drawString(count, searchRight - 6 - fontRendererObj.getStringWidth(count), searchY + 5, Theme.TEXT_MUTED);
    }

    private void drawSelectedMod(int mouseX, int mouseY) {
        int x = rightX + PAD;
        int y = panelTop + PAD;
        int maxX = rightRight - PAD - (configureButton.visible ? 24 : 0);
        int textX = x + 40;

        ModIcons.draw(selected, x, y, 32);

        String name = RenderUtil.trim(fontRendererObj, selected.getName(), maxX - textX);
        fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + name, textX, y + 1, Theme.TEXT);

        String version = selected.getDisplayVersion();
        if (version == null || version.isEmpty()) version = selected.getVersion();
        int lineX = textX;
        if (version != null && !version.isEmpty()) {
            if (Character.isDigit(version.charAt(0))) version = "v" + version;
            version = RenderUtil.trim(fontRendererObj, version, (maxX - textX) / 2);
            fontRendererObj.drawString(version, textX, y + 12, Theme.TEXT_MUTED);
            lineX += fontRendererObj.getStringWidth(version) + 5;
        }
        ModBadge.drawAll(fontRendererObj, ModBadge.getBadges(selected), lineX, y + 12, maxX);

        String authors = authors(selected);
        if (!authors.isEmpty()) {
            fontRendererObj.drawString(RenderUtil.trim(fontRendererObj, "by " + authors, maxX - textX), textX, y + 23, Theme.TEXT_MUTED);
        }

        drawDescription(mouseX, mouseY);
    }

    /** One line of the details panel. */
    private static final class Line {
        final String text;
        final int color;
        final int indent;
        final boolean link;

        Line(String text, int color, int indent, boolean link) {
            this.text = text;
            this.color = color;
            this.indent = indent;
            this.link = link;
        }
    }

    private void drawDescription(int mouseX, int mouseY) {
        int left = rightX + 1;
        int right = rightRight - 1;
        int top = descriptionTop;
        int bottom = panelBottom - 1;
        if (bottom <= top) return;

        Theme.divider(rightX + PAD, rightRight - PAD, top - 1);

        List<Line> lines = buildDescriptionLines(right - left - PAD * 2 - 6);
        int contentHeight = lines.size() * LINE + PAD * 2;
        float max = Math.max(0, contentHeight - (bottom - top));
        descriptionTarget = Math.min(descriptionTarget, max);
        descriptionScroll += (descriptionTarget - descriptionScroll) * 0.45F;
        if (Math.abs(descriptionTarget - descriptionScroll) < 0.5F) descriptionScroll = descriptionTarget;

        RenderUtil.scissor(left, top, right - left, bottom - top);
        int y = top + PAD - Math.round(descriptionScroll);
        linkWidth = -1;
        for (Line line : lines) {
            if (y > top - LINE && y < bottom) {
                int x = left + PAD - 1 + line.indent;
                if (line.link) {
                    linkX = x;
                    linkY = y;
                    linkWidth = fontRendererObj.getStringWidth(line.text);
                    boolean hover = mouseX >= linkX && mouseX < linkX + linkWidth && mouseY >= y - 1 && mouseY < y + 9;
                    fontRendererObj.drawString((hover ? EnumChatFormatting.UNDERLINE : "") + line.text, x, y, line.color);
                } else {
                    fontRendererObj.drawString(line.text, x, y, line.color);
                }
            }
            y += LINE;
        }
        RenderUtil.endScissor();

        if (max > 0) {
            int trackTop = top + 4;
            int trackHeight = bottom - top - 8;
            int thumb = Math.max(16, trackHeight * (bottom - top) / contentHeight);
            int thumbTop = trackTop + Math.round(descriptionScroll * (trackHeight - thumb) / max);
            Gui.drawRect(right - 5, trackTop, right - 2, trackTop + trackHeight, 0x20FFFFFF);
            Gui.drawRect(right - 5, thumbTop, right - 2, thumbTop + thumb, 0x70FFFFFF);
        }
    }

    private void heading(List<Line> lines, String title) {
        if (!lines.isEmpty()) lines.add(new Line("", 0, 0, false));
        lines.add(new Line(EnumChatFormatting.BOLD + title.toUpperCase(), Theme.TEXT_HEADING, 0, false));
    }

    private void wrapped(List<Line> lines, String text, int color, int indent, int wrapWidth) {
        for (String paragraph : text.replace("\r", "").split("\n")) {
            for (String l : fontRendererObj.listFormattedStringToWidth(paragraph, wrapWidth - indent)) {
                lines.add(new Line(l, color, indent, false));
            }
        }
    }

    private List<Line> buildDescriptionLines(int wrapWidth) {
        List<Line> lines = new ArrayList<Line>();
        ModMetadata meta = selected.getMetadata();

        String description = ModInfo.get(selected).description;
        if (description == null) description = getSummary(selected);
        wrapped(lines, description, Theme.TEXT_BODY, 0, wrapWidth);

        if (getWebsite(selected) != null) {
            heading(lines, "Links");
            lines.add(new Line("Website", Theme.LINK, 0, true));
        }

        String authors = authors(selected);
        if (!authors.isEmpty()) {
            heading(lines, "Authors");
            wrapped(lines, authors, Theme.TEXT_BODY, 0, wrapWidth);
        }

        String credits = ModInfo.get(selected).credits;
        if (credits != null) {
            heading(lines, "Credits");
            wrapped(lines, credits, Theme.TEXT_BODY, 0, wrapWidth);
        }

        if (meta != null && meta.childMods != null && !meta.childMods.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ModContainer child : meta.childMods) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(child.getName());
            }
            heading(lines, "Contains");
            wrapped(lines, sb.toString(), Theme.TEXT_BODY, 0, wrapWidth);
        }

        heading(lines, "Mod ID");
        lines.add(new Line(selected.getModId(), Theme.TEXT_MUTED, 0, false));
        return lines;
    }

    // ---- icons ------------------------------------------------------------------------------

    private static void drawSearchIcon(int x, int y, int c) {
        // Small magnifying glass
        drawRect(x + 1, y, x + 5, y + 1, c);
        drawRect(x + 1, y + 5, x + 5, y + 6, c);
        drawRect(x, y + 1, x + 1, y + 5, c);
        drawRect(x + 5, y + 1, x + 6, y + 5, c);
        drawRect(x + 5, y + 5, x + 7, y + 7, c);
        drawRect(x + 6, y + 6, x + 8, y + 8, c);
    }

    private static void drawFilterIcon(int bx, int by, int bw, int bh, int c) {
        int cx = bx + bw / 2, y = by + 5;
        drawRect(cx - 5, y, cx + 5, y + 1, c);
        drawRect(cx - 4, y + 1, cx + 4, y + 2, c);
        drawRect(cx - 3, y + 2, cx + 3, y + 3, c);
        drawRect(cx - 2, y + 3, cx + 2, y + 4, c);
        drawRect(cx - 1, y + 4, cx + 1, y + 8, c);
    }

    private static void drawConfigIcon(int bx, int by, int c) {
        int x = bx + 4, y = by + 4;
        int[] knobs = {7, 3, 6};
        for (int i = 0; i < 3; i++) {
            int ly = y + 1 + i * 4;
            drawRect(x, ly, x + 10, ly + 1, c);
            drawRect(x + knobs[i] - 1, ly - 1, x + knobs[i] + 2, ly + 2, c);
        }
    }
}
