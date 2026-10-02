package modmenu.forge.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public final class RenderUtil {
    private RenderUtil() {
    }

    /** Darkened dirt background, the same look as vanilla/Mod Menu scrolling lists. */
    public static void drawDirt(int left, int top, int right, int bottom, float scroll, int brightness) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(Gui.optionsBackground);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        float f = 32.0F;
        wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        wr.pos(left, bottom, 0).tex(left / f, (bottom + scroll) / f).color(brightness, brightness, brightness, 255).endVertex();
        wr.pos(right, bottom, 0).tex(right / f, (bottom + scroll) / f).color(brightness, brightness, brightness, 255).endVertex();
        wr.pos(right, top, 0).tex(right / f, (top + scroll) / f).color(brightness, brightness, brightness, 255).endVertex();
        wr.pos(left, top, 0).tex(left / f, (top + scroll) / f).color(brightness, brightness, brightness, 255).endVertex();
        tess.draw();
    }

    /** List background: see-through over a world (like modern Mod Menu), dark dirt on the title screen. */
    public static void drawListBackground(int left, int top, int right, int bottom, float scroll) {
        Gui.drawRect(left, top, right, bottom, Minecraft.getMinecraft().theWorld != null ? 0x44000000 : 0x66000000);
        // Header / footer separator lines
        Gui.drawRect(left, top - 2, right, top - 1, 0x55FFFFFF);
        Gui.drawRect(left, top - 1, right, top, 0xFF000000);
        Gui.drawRect(left, bottom, right, bottom + 1, 0xFF000000);
        Gui.drawRect(left, bottom + 1, right, bottom + 2, 0x55FFFFFF);
    }

    public static void drawGradient(int left, int top, int right, int bottom, int startColor, int endColor) {
        float a1 = (startColor >> 24 & 255) / 255.0F, r1 = (startColor >> 16 & 255) / 255.0F;
        float g1 = (startColor >> 8 & 255) / 255.0F, b1 = (startColor & 255) / 255.0F;
        float a2 = (endColor >> 24 & 255) / 255.0F, r2 = (endColor >> 16 & 255) / 255.0F;
        float g2 = (endColor >> 8 & 255) / 255.0F, b2 = (endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(right, top, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(left, top, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(left, bottom, 0).color(r2, g2, b2, a2).endVertex();
        wr.pos(right, bottom, 0).color(r2, g2, b2, a2).endVertex();
        tess.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void scissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        int scale = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, mc.displayHeight - (y + height) * scale,
                Math.max(0, width * scale), Math.max(0, height * scale));
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** Cuts a string to the given width, adding "..." if it had to be shortened. */
    public static String trim(net.minecraft.client.gui.FontRenderer font, String text, int width) {
        if (text == null) return "";
        if (font.getStringWidth(text) <= width) return text;
        return font.trimStringToWidth(text, Math.max(0, width - font.getStringWidth("..."))) + "...";
    }
}
