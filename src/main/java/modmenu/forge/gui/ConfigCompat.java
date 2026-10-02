package modmenu.forge.gui;

import modmenu.forge.ModBadge;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.common.ModContainer;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Finds a config screen for any mod, in order:
 * 1. the mod's Forge {@link IModGuiFactory} (the standard way),
 * 2. hand-written support for popular mods that only open settings via keybind/command,
 * 3. a scan of the mod's jar for a settings GuiScreen we can construct.
 * Everything is reflective, so none of these mods are required.
 */
public final class ConfigCompat {
    public interface ScreenFactory {
        GuiScreen create(GuiScreen parent) throws Exception;
    }

    private static final Map<String, ScreenFactory> KNOWN = new HashMap<String, ScreenFactory>();
    /** Discovered screen per mod id; a null value means "scanned, nothing found". */
    private static final Map<String, Class<? extends GuiScreen>> DISCOVERED = new HashMap<String, Class<? extends GuiScreen>>();

    private static final Pattern SETTINGS_WORD = Pattern.compile("(?i).*(config|setting|option|preference|customi[sz]).*");
    private static final Pattern SCREEN_WORD = Pattern.compile("(?i).*(gui|screen|menu).*");

    static {
        // Custom Crosshair Mod: new EditCrosshairGuiScreen(CustomCrosshairMod.INSTANCE.properties().getCrosshair())
        KNOWN.put("custom-crosshair-mod", new ScreenFactory() {
            @Override
            public GuiScreen create(GuiScreen parent) throws Exception {
                Class<?> modClass = Class.forName("com.wjbaker.ccm.CustomCrosshairMod");
                Object mod = modClass.getField("INSTANCE").get(null);
                Object properties = modClass.getMethod("properties").invoke(mod);
                Object crosshair = properties.getClass().getMethod("getCrosshair").invoke(properties);
                Class<?> screenClass = Class.forName(
                        "com.wjbaker.ccm.render.gui.screen.screens.editCrosshair.EditCrosshairGuiScreen");
                return (GuiScreen) screenClass.getConstructor(crosshair.getClass()).newInstance(crosshair);
            }
        });
    }

    private ConfigCompat() {
    }

    public static boolean has(ModContainer mod) {
        if (mod == null) return false;
        return forgeConfigClass(mod) != null || KNOWN.containsKey(mod.getModId()) || discover(mod) != null;
    }

    public static GuiScreen create(ModContainer mod, GuiScreen parent) {
        if (mod == null) return null;
        try {
            Class<? extends GuiScreen> forge = forgeConfigClass(mod);
            if (forge != null) {
                GuiScreen screen = instantiate(forge, parent);
                if (screen != null) return screen;
            }
            ScreenFactory known = KNOWN.get(mod.getModId());
            if (known != null) return known.create(parent);
            Class<? extends GuiScreen> found = discover(mod);
            if (found != null) return instantiate(found, parent);
        } catch (Throwable t) {
            System.err.println("[Mod Menu] Could not open config screen for " + mod.getModId());
            t.printStackTrace();
        }
        return null;
    }

    private static Class<? extends GuiScreen> forgeConfigClass(ModContainer mod) {
        try {
            IModGuiFactory factory = FMLClientHandler.instance().getGuiFactoryFor(mod);
            return factory == null ? null : factory.mainConfigGuiClass();
        } catch (Throwable t) {
            return null;
        }
    }

    private static GuiScreen instantiate(Class<? extends GuiScreen> cls, GuiScreen parent) throws Exception {
        for (Constructor<?> ctor : cls.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(parent.getClass())) {
                ctor.setAccessible(true);
                return (GuiScreen) ctor.newInstance(parent);
            }
        }
        Constructor<? extends GuiScreen> noArg = cls.getDeclaredConstructor();
        noArg.setAccessible(true);
        return noArg.newInstance();
    }

    private static boolean constructible(Class<?> cls) {
        for (Constructor<?> ctor : cls.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 0) return true;
            if (params.length == 1 && params[0].isAssignableFrom(GuiScreen.class)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends GuiScreen> discover(ModContainer mod) {
        String id = mod.getModId();
        if (DISCOVERED.containsKey(id)) return DISCOVERED.get(id);
        Class<? extends GuiScreen> result = null;
        try {
            result = scan(mod);
        } catch (Throwable ignored) {
        }
        DISCOVERED.put(id, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends GuiScreen> scan(ModContainer mod) throws Exception {
        if (ModBadge.isMinecraft(mod)) return null;
        Object instance = mod.getMod();
        File source = mod.getSource();
        if (instance == null || source == null || !source.isFile()) return null;

        // Only look inside the mod's own package, so we never pick up a bundled library's screens
        String pkg = instance.getClass().getName();
        pkg = pkg.contains(".") ? pkg.substring(0, pkg.lastIndexOf('.')) : "";
        String prefix = pkg.replace('.', '/') + "/";
        ClassLoader loader = instance.getClass().getClassLoader();

        List<String> candidates = new ArrayList<String>();
        ZipFile zip = new ZipFile(source);
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class") || name.indexOf('$') >= 0 || !name.startsWith(prefix)) continue;
                if (name.toLowerCase().contains("mixin")) continue; // loading mixin classes directly crashes
                String simple = name.substring(name.lastIndexOf('/') + 1, name.length() - 6);
                if (SETTINGS_WORD.matcher(simple).matches() && SCREEN_WORD.matcher(simple).matches()) {
                    candidates.add(name.substring(0, name.length() - 6).replace('/', '.'));
                }
            }
        } finally {
            zip.close();
        }

        Class<? extends GuiScreen> best = null;
        int bestScore = Integer.MIN_VALUE;
        for (String name : candidates) {
            try {
                Class<?> cls = Class.forName(name, false, loader);
                if (!GuiScreen.class.isAssignableFrom(cls) || cls.isInterface()
                        || Modifier.isAbstract(cls.getModifiers()) || !constructible(cls)) continue;
                String simple = cls.getSimpleName().toLowerCase();
                // Prefer top-level "main" config screens over sub-pages
                int score = -simple.length() - name.split("\\.").length * 2;
                if (simple.contains("config") || simple.contains("setting")) score += 20;
                if (simple.contains("main") || simple.contains("home")) score += 10;
                if (simple.contains("edit") || simple.contains("color") || simple.contains("colour")
                        || simple.contains("list") || simple.contains("entry")) score -= 15;
                if (score > bestScore) {
                    bestScore = score;
                    best = (Class<? extends GuiScreen>) cls;
                }
            } catch (Throwable ignored) {
            }
        }
        return best;
    }
}
