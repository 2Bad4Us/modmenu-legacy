package modmenu.forge.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/** A GuiButton that can be wider than the 200px button texture (drawn as left edge + repeated middle + right edge). */
public class WideButton extends GuiButton {
    private static final int TEXTURE_WIDTH = 200;
    private static final int EDGE = 4;

    public WideButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        if (width <= TEXTURE_WIDTH) {
            super.drawButton(mc, mouseX, mouseY);
            return;
        }
        FontRenderer font = mc.fontRendererObj;
        mc.getTextureManager().bindTexture(buttonTextures);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        int v = 46 + getHoverState(hovered) * 20;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.blendFunc(770, 771);

        int topHalf = height / 2;
        int bottomHalf = height - topHalf;
        // Left edge
        drawTexturedModalRect(xPosition, yPosition, 0, v, EDGE, topHalf);
        drawTexturedModalRect(xPosition, yPosition + topHalf, 0, v + 20 - bottomHalf, EDGE, bottomHalf);
        // Middle, repeated
        int x = xPosition + EDGE;
        int end = xPosition + width - EDGE;
        int middle = TEXTURE_WIDTH - EDGE * 2;
        while (x < end) {
            int w = Math.min(middle, end - x);
            drawTexturedModalRect(x, yPosition, EDGE, v, w, topHalf);
            drawTexturedModalRect(x, yPosition + topHalf, EDGE, v + 20 - bottomHalf, w, bottomHalf);
            x += w;
        }
        // Right edge
        drawTexturedModalRect(end, yPosition, TEXTURE_WIDTH - EDGE, v, EDGE, topHalf);
        drawTexturedModalRect(end, yPosition + topHalf, TEXTURE_WIDTH - EDGE, v + 20 - bottomHalf, EDGE, bottomHalf);

        mouseDragged(mc, mouseX, mouseY);
        int color = !enabled ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xE0E0E0;
        drawCenteredString(font, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, color);
    }
}
