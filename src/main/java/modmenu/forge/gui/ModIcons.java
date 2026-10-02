package modmenu.forge.gui;

import modmenu.forge.ModBadge;
import modmenu.forge.ModInfo;
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
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
        if ("FML".equals(mod.getModId())) {
            // FML has no logo of its own; it ships inside Forge, so borrow Forge's
            ModContainer forge = Loader.instance().getIndexedModList().get("Forge");
            if (forge != null) return load(forge);
        }
        String logoFile = ModInfo.get(mod).logoFile;
        if (logoFile != null) {
            // Forge's own way first (the mod's resource pack), then the classpath
            IResourcePack pack = FMLClientHandler.instance().getResourcePackFor(mod.getModId());
            if (pack != null && logoFile.equals(mod.getMetadata().logoFile)) {
                try {
                    BufferedImage image = pack.getPackImage();
                    if (image != null) return image;
                } catch (Exception ignored) {
                }
            }
            BufferedImage image = readFromSource(mod, Collections.singletonList(logoFile));
            if (image != null) return image;
            InputStream in = ModIcons.class.getResourceAsStream(logoFile.startsWith("/") ? logoFile : "/" + logoFile);
            if (in != null) {
                try {
                    return ImageIO.read(in);
                } finally {
                    in.close();
                }
            }
        }
        // No (working) logoFile: look for the usual icon names in the mod's jar
        String id = mod.getModId().toLowerCase();
        return readFromSource(mod, Arrays.asList(
                "assets/" + id + "/icon.png", "assets/" + id + "/logo.png", "assets/" + id + "/textures/icon.png",
                "assets/" + id + "/textures/logo.png", "icon.png", "logo.png", "pack.png", id + ".png"));
    }

    /** Reads the first of the given paths that exists in the mod's jar or folder. */
    private static BufferedImage readFromSource(ModContainer mod, List<String> paths) {
        File source = mod.getSource();
        if (source == null || !source.exists()) return null;
        if (ModBadge.isMinecraft(mod) && !"Forge".equals(mod.getModId()) && !"mcp".equals(mod.getModId())) return null;
        for (String raw : paths) {
            String path = raw.startsWith("/") ? raw.substring(1) : raw;
            try {
                if (source.isDirectory()) {
                    File file = new File(source, path);
                    if (file.isFile()) return ImageIO.read(file);
                } else {
                    ZipFile zip = new ZipFile(source);
                    try {
                        ZipEntry entry = zip.getEntry(path);
                        if (entry != null) {
                            InputStream in = zip.getInputStream(entry);
                            try {
                                BufferedImage image = ImageIO.read(in);
                                if (image != null) return image;
                            } finally {
                                in.close();
                            }
                        }
                    } finally {
                        zip.close();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
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
