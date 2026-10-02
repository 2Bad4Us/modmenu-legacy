package modmenu.forge.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Loads and caches 32x32 mod icons (the mod's logoFile, or the vanilla pack icon for Minecraft). */
public final class ModIcons {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();

    private ModIcons() {
    }

    public static ResourceLocation get(ModContainer mod) {
        String key = mod.getModId();
        if (CACHE.containsKey(key)) return CACHE.get(key);
        ResourceLocation location = null;
        try {
            BufferedImage image = load(mod);
            if (image != null) {
                location = Minecraft.getMinecraft().getTextureManager()
                        .getDynamicTextureLocation("modmenu_icon_" + key.toLowerCase(), new DynamicTexture(image));
            }
        } catch (Exception ignored) {
        }
        CACHE.put(key, location);
        return location;
    }

    private static BufferedImage load(ModContainer mod) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        if (mod == Loader.instance().getMinecraftModContainer()) {
            return mc.getResourcePackRepository().rprDefaultResourcePack.getPackImage();
        }
        String logoFile = mod.getMetadata() == null ? null : mod.getMetadata().logoFile;
        if (logoFile == null || logoFile.isEmpty()) return null;
        IResourcePack pack = FMLClientHandler.instance().getResourcePackFor(mod.getModId());
        if (pack != null) {
            try {
                BufferedImage image = pack.getPackImage();
                if (image != null) return image;
            } catch (Exception ignored) {
            }
        }
        String path = logoFile.startsWith("/") ? logoFile : "/" + logoFile;
        InputStream in = ModIcons.class.getResourceAsStream(path);
        if (in == null) return null;
        try {
            return ImageIO.read(in);
        } finally {
            in.close();
        }
    }

    public static void draw(ModContainer mod, int x, int y, int size) {
        ResourceLocation icon = get(mod);
        if (icon != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(icon);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
            GlStateManager.disableBlend();
        } else {
            drawUnknown(x, y, size);
        }
    }

    /** Grey "?" placeholder, like Mod Menu's unknown icon. */
    private static void drawUnknown(int x, int y, int size) {
        Gui.drawRect(x, y, x + size, y + size, 0xFF5A5A5A);
        Gui.drawRect(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF3A3A3A);
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        float scale = size / 16.0F;
        GlStateManager.translate(x + size / 2.0F, y + size / 2.0F, 0);
        GlStateManager.scale(scale, scale, 1);
        mc.fontRendererObj.drawString("?", -mc.fontRendererObj.getStringWidth("?") / 2 + 0.5F, -3.5F, 0xFF9A9A9A, false);
        GlStateManager.popMatrix();
    }
}
