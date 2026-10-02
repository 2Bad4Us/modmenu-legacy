package modmenu.forge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public final class ModMenuConfig {
    public static boolean sortAscending = true;
    public static boolean showLibraries = false;
    public static boolean showModCount = true;

    private static File file;

    private ModMenuConfig() {
    }

    public static void load(File configDir) {
        file = new File(configDir, "modmenu.properties");
        if (!file.exists()) {
            save();
            return;
        }
        Properties props = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
            sortAscending = Boolean.parseBoolean(props.getProperty("sort_ascending", "true"));
            showLibraries = Boolean.parseBoolean(props.getProperty("show_libraries", "false"));
            showModCount = Boolean.parseBoolean(props.getProperty("show_mod_count", "true"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) { }
        }
    }

    public static void save() {
        if (file == null) return;
        Properties props = new Properties();
        props.setProperty("sort_ascending", String.valueOf(sortAscending));
        props.setProperty("show_libraries", String.valueOf(showLibraries));
        props.setProperty("show_mod_count", String.valueOf(showModCount));
        OutputStream out = null;
        try {
            file.getParentFile().mkdirs();
            out = new FileOutputStream(file);
            props.store(out, "Mod Menu config");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) try { out.close(); } catch (Exception ignored) { }
        }
    }
}
