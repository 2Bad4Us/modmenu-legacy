package modmenu.forge;

import modmenu.forge.gui.ModsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = ModMenu.MODID, name = "Mod Menu Legacy", version = "1.0.0", clientSideOnly = true,
        acceptedMinecraftVersions = "[1.8.9]", guiFactory = "modmenu.forge.ModMenuGuiFactory")
public class ModMenu {
    public static final String MODID = "modmenulegacy";

    private static final int MAIN_MENU_MODS_BUTTON = 6;
    private static final int INGAME_MOD_OPTIONS_BUTTON = 12;
    private static final int INGAME_LAN_BUTTON = 7;
    private static final int MAIN_MENU_REALMS_BUTTON = 14;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModMenuConfig.load(event.getModConfigurationDirectory());
        MinecraftForge.EVENT_BUS.register(this);
        String screenshotDir = System.getProperty("modmenulegacy.screenshots");
        if (screenshotDir != null) {
            net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(new ScreenshotDriver(new java.io.File(screenshotDir)));
        }
    }

    /** All mods shown in the menu, including Minecraft itself. */
    public static List<ModContainer> getAllMods() {
        List<ModContainer> mods = new ArrayList<ModContainer>();
        ModContainer minecraft = Loader.instance().getMinecraftModContainer();
        if (minecraft != null) mods.add(minecraft);
        for (ModContainer mod : Loader.instance().getModList()) {
            if (!mods.contains(mod)) mods.add(mod);
        }
        return mods;
    }

    public static int getDisplayedModCount() {
        int count = 0;
        for (ModContainer mod : getAllMods()) {
            if (!ModBadge.isLibrary(mod) && !ModBadge.isMinecraft(mod)) count++;
        }
        return count;
    }

    public static String getModsButtonText() {
        if (!ModMenuConfig.showModCount) return "Mods";
        return "Mods (" + getDisplayedModCount() + ")";
    }

    /** The Mods screen to come back to once a config screen opened from it is closed. */
    private static ModsScreen returnScreen;

    public static void returnHereAfterConfig(ModsScreen screen) {
        returnScreen = screen;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (returnScreen != null) {
            if (event.gui == returnScreen) {
                returnScreen = null;
            } else if (event.gui == null || event.gui instanceof GuiMainMenu) {
                // Mods that close their settings with displayGuiScreen(null) land here instead of in-game/title
                event.gui = returnScreen;
                returnScreen = null;
                return;
            }
            // Anything else is a sub-page of the config screen; keep waiting
        }
        // Forge's own mod list (main menu "Mods" and in-game "Mod Options...") is replaced by ours
        if (event.gui instanceof GuiModList) {
            event.gui = new ModsScreen(Minecraft.getMinecraft().currentScreen);
        } else if (isRealmsErrorScreen(event.gui)) {
            // Realms' error screen crashes the game when you press its button; go back to the title screen instead
            event.gui = new GuiMainMenu();
        }
    }

    private static boolean isRealmsErrorScreen(net.minecraft.client.gui.GuiScreen gui) {
        if (!(gui instanceof net.minecraft.client.gui.GuiScreenRealmsProxy)) return false;
        for (java.lang.reflect.Field field : gui.getClass().getDeclaredFields()) {
            if (!net.minecraft.realms.RealmsScreen.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object screen = field.get(gui);
                return screen != null && screen.getClass().getSimpleName().contains("Error");
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiMainMenu) {
            GuiButton mods = null, realms = null;
            for (GuiButton button : event.buttonList) {
                if (button.id == MAIN_MENU_MODS_BUTTON) mods = button;
                if (button.id == MAIN_MENU_REALMS_BUTTON) realms = button;
            }
            if (mods != null) {
                // Full-width "Mods" in the Realms row; Realms shrinks to a small button on the side
                mods.displayString = getModsButtonText();
                mods.xPosition = event.gui.width / 2 - 100;
                mods.width = 200;
                if (realms != null) {
                    realms.xPosition = event.gui.width / 2 + 104;
                    realms.yPosition = mods.yPosition;
                    realms.width = 20;
                    realms.displayString = "R";
                }
            }
        } else if (event.gui instanceof GuiIngameMenu) {
            GuiButton mods = null, lan = null;
            for (GuiButton button : event.buttonList) {
                if (button.id == INGAME_MOD_OPTIONS_BUTTON) mods = button;
                if (button.id == INGAME_LAN_BUTTON) lan = button;
            }
            if (mods != null) mods.displayString = "Mods";
            if (mods != null && lan != null) {
                // Swap places so "Mods" sits where "Open to LAN" was
                int x = mods.xPosition, y = mods.yPosition, w = mods.width;
                mods.xPosition = lan.xPosition;
                mods.yPosition = lan.yPosition;
                mods.width = lan.width;
                lan.xPosition = x;
                lan.yPosition = y;
                lan.width = w;
            }
        }
    }

    @SubscribeEvent
    public void onButtonClicked(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        boolean modsButton = (event.gui instanceof GuiMainMenu && event.button.id == MAIN_MENU_MODS_BUTTON)
                || (event.gui instanceof GuiIngameMenu && event.button.id == INGAME_MOD_OPTIONS_BUTTON);
        if (modsButton) {
            Minecraft.getMinecraft().displayGuiScreen(new ModsScreen(event.gui));
            event.setCanceled(true);
        }
    }
}
