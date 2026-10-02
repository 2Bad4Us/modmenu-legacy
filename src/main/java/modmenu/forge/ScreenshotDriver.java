package modmenu.forge;

import modmenu.forge.gui.ModsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Development helper that takes the store-page screenshots automatically.
 * Only active when the game is started with -Dmodmenulegacy.screenshots=<output dir>.
 */
public class ScreenshotDriver {
    private final File outputDir;
    private int step;
    private int wait;
    private String pendingShot;

    public ScreenshotDriver(File outputDir) {
        this.outputDir = outputDir;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingShot != null) return;
        if (wait > 0) {
            wait--;
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        System.out.println("[Mod Menu Legacy] step " + step + " screen="
                + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName()));
        switch (step) {
            case 0: // title screen
                if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                shot("screenshot 1", 80);
                break;
            case 1: { // Mods screen over the panorama, Mod Menu Legacy selected
                ModsScreen screen = new ModsScreen(mc.currentScreen);
                mc.displayGuiScreen(screen);
                select(screen, ModMenu.MODID);
                shot("screenshot 2", 30);
                break;
            }
            case 2: // another mod selected
                if (mc.currentScreen instanceof ModsScreen) select((ModsScreen) mc.currentScreen, "custom-crosshair-mod");
                shot("screenshot 3", 15);
                break;
            case 3: // load a world
                mc.displayGuiScreen(null);
                mc.launchIntegratedServer("ModMenuScreens", "ModMenuScreens",
                        new WorldSettings(12345L, WorldSettings.GameType.CREATIVE, true, false, WorldType.DEFAULT));
                step++;
                break;
            case 4:
                if (mc.theWorld == null || mc.thePlayer == null || mc.getIntegratedServer() == null) return;
                mc.gameSettings.hideGUI = false;
                // Daytime, looking slightly down at the landscape
                net.minecraft.server.integrated.IntegratedServer server = mc.getIntegratedServer();
                server.getCommandManager().executeCommand(server, "time set 6000");
                server.getCommandManager().executeCommand(server, "gamerule doDaylightCycle false");
                // Hover above the treetops looking out over the landscape
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.sendPlayerAbilities();
                server.getCommandManager().executeCommand(server, "tp " + mc.thePlayer.getName() + " ~ ~30 ~ 30 20");
                mc.gameSettings.gammaSetting = 1.0F;
                step++;
                wait = 160; // let chunks render
                break;
            case 5: // pause menu
                mc.displayGuiScreen(new GuiIngameMenu());
                shot("screenshot 4", 20);
                break;
            case 6: { // Mods screen in-game
                ModsScreen screen = new ModsScreen(new GuiIngameMenu());
                mc.displayGuiScreen(screen);
                select(screen, "custom-crosshair-mod");
                shot("screenshot 5", 30);
                break;
            }
            default:
                mc.shutdown();
        }
    }

    private void select(ModsScreen screen, String modId) {
        ModContainer mod = Loader.instance().getIndexedModList().get(modId);
        if (mod != null) screen.select(mod);
    }

    private void shot(String name, int delayTicks) {
        // Park the mouse in a corner so no button shows its hover highlight
        org.lwjgl.input.Mouse.setCursorPosition(Minecraft.getMinecraft().displayWidth - 1, 1);
        wait = delayTicks;
        pendingShot = name;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingShot == null || wait > 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            BufferedImage image = capture(mc.displayWidth, mc.displayHeight);
            outputDir.mkdirs();
            ImageIO.write(image, "png", new File(outputDir, pendingShot + ".png"));
            System.out.println("[Mod Menu Legacy] Saved " + pendingShot + " of "
                    + (mc.currentScreen == null ? "game" : mc.currentScreen.getClass().getSimpleName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        pendingShot = null;
        step++;
    }

    private static BufferedImage capture(int width, int height) {
        java.nio.IntBuffer buffer = BufferUtils.createIntBuffer(width * height);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, width, height, org.lwjgl.opengl.GL12.GL_BGRA, org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        int[] pixels = new int[width * height];
        buffer.get(pixels);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            // OpenGL rows start at the bottom
            image.setRGB(0, height - 1 - y, width, 1, pixels, y * width, width);
        }
        return image;
    }

    /** The delay must count down even while a shot is pending, so tick it separately. */
    @SubscribeEvent
    public void onTickDelay(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && pendingShot != null && wait > 0) wait--;
    }
}
