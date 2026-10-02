package modmenu.forge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Mod details with fallbacks. Forge leaves a mod's metadata empty when its mcmod.info is missing
 * or its modid doesn't match the @Mod annotation, so we read the mcmod.info out of the jar ourselves.
 */
public final class ModInfo {
    public final String description;
    public final List<String> authors;
    public final String url;
    public final String credits;
    public final String logoFile;

    private static final Map<String, ModInfo> CACHE = new HashMap<String, ModInfo>();

    private ModInfo(String description, List<String> authors, String url, String credits, String logoFile) {
        this.description = description;
        this.authors = authors;
        this.url = url;
        this.credits = credits;
        this.logoFile = logoFile;
    }

    public static ModInfo get(ModContainer mod) {
        String key = mod.getModId();
        ModInfo info = CACHE.get(key);
        if (info == null) {
            info = load(mod);
            CACHE.put(key, info);
        }
        return info;
    }

    private static ModInfo load(ModContainer mod) {
        ModMetadata meta = mod.getMetadata();
        String description = meta == null ? null : meta.description;
        List<String> authors = meta == null || meta.authorList == null ? new ArrayList<String>() : meta.authorList;
        String url = meta == null ? null : meta.url;
        String credits = meta == null ? null : meta.credits;
        String logo = meta == null ? null : meta.logoFile;

        if (isEmpty(description) || authors.isEmpty() || isEmpty(url) || isEmpty(logo)) {
            JsonObject entry = readJarInfo(mod);
            if (entry != null) {
                if (isEmpty(description)) description = string(entry, "description");
                if (authors.isEmpty()) authors = strings(entry, "authorList", "authors");
                if (isEmpty(url)) url = string(entry, "url");
                if (isEmpty(credits)) credits = string(entry, "credits");
                if (isEmpty(logo)) logo = string(entry, "logoFile");
            }
        }
        return new ModInfo(trimToNull(description), authors, trimToNull(url), trimToNull(credits), trimToNull(logo));
    }

    /** Finds this mod's entry in the mcmod.info inside its jar. */
    private static JsonObject readJarInfo(ModContainer mod) {
        File source = mod.getSource();
        if (source == null || !source.isFile()) return null;
        ZipFile zip = null;
        try {
            zip = new ZipFile(source);
            ZipEntry entry = zip.getEntry("mcmod.info");
            if (entry == null) return null;
            InputStream in = zip.getInputStream(entry);
            JsonElement root = new JsonParser().parse(new InputStreamReader(in, "UTF-8"));
            in.close();
            JsonArray list = null;
            if (root.isJsonArray()) {
                list = root.getAsJsonArray();
            } else if (root.isJsonObject() && root.getAsJsonObject().has("modList")) {
                list = root.getAsJsonObject().getAsJsonArray("modList"); // mcmod.info version 2
            }
            if (list == null || list.size() == 0) return null;
            JsonObject byName = null;
            for (JsonElement element : list) {
                if (!element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();
                if (mod.getModId().equalsIgnoreCase(string(obj, "modid"))) return obj;
                if (byName == null && mod.getName() != null && mod.getName().equalsIgnoreCase(string(obj, "name"))) byName = obj;
            }
            if (byName != null) return byName;
            // A jar with a single mod: its mcmod.info entry is ours even if the modid is wrong
            return list.size() == 1 && list.get(0).isJsonObject() ? list.get(0).getAsJsonObject() : null;
        } catch (Throwable t) {
            return null;
        } finally {
            if (zip != null) try { zip.close(); } catch (Exception ignored) { }
        }
    }

    private static String string(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static List<String> strings(JsonObject obj, String... keys) {
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e == null) continue;
            List<String> out = new ArrayList<String>();
            if (e.isJsonArray()) {
                for (JsonElement s : e.getAsJsonArray()) if (s.isJsonPrimitive()) out.add(s.getAsString());
            } else if (e.isJsonPrimitive()) {
                out.add(e.getAsString());
            }
            if (!out.isEmpty()) return out;
        }
        return Collections.emptyList();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimToNull(String s) {
        return isEmpty(s) ? null : s.trim();
    }
}
