package lindefense;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Locates bundled asset files (fonts, sound effects, music) without depending on the current working directory,
 * so the game runs the same from a terminal, an IDE or a packaged folder.
 *
 * <p>A relative path such as {@code assets/sfx/ui_click.wav} is looked up, in order, in:</p>
 * <ol>
 *   <li>the folder named by the {@code lindefense.home} system property,</li>
 *   <li>the current working directory,</li>
 *   <li>the directory above the code source (the folder that contains {@code build/} or the jar),</li>
 *   <li>the classpath (only when the resource is an ordinary file, i.e. a classes directory).</li>
 * </ol>
 * <p>File names are case-sensitive on Linux, so callers must use the exact on-disk spelling.</p>
 */
public final class Assets {
    private Assets() { }

    /** The base folders searched, in priority order (duplicates removed). */
    public static List<File> roots() {
        List<File> r = new ArrayList<>();
        String home = System.getProperty("lindefense.home");
        if (home != null && !home.isEmpty()) add(r, new File(home));
        add(r, new File(System.getProperty("user.dir", ".")));
        try {
            File code = new File(Assets.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File above = code.getParentFile();
            if (above != null) add(r, above);
            if (code.isDirectory()) add(r, code);
        } catch (Exception | Error ignored) {
            // code source unavailable (unusual class loader): other roots still apply
        }
        return r;
    }

    private static void add(List<File> list, File f) {
        File abs = f.getAbsoluteFile();
        if (!list.contains(abs)) list.add(abs);
    }

    /**
     * Resolves {@code relPath} (forward slashes, e.g. {@code assets/music/menu.wav}) to an existing file or folder.
     * If nothing exists anywhere, returns the path relative to the first root so callers can test {@code isFile()}.
     */
    public static File file(String relPath) {
        List<File> roots = roots();
        for (File root : roots) {
            File f = new File(root, relPath);
            if (f.exists()) return f;
        }
        try {
            URL u = Assets.class.getResource("/" + relPath);
            if (u != null && "file".equals(u.getProtocol())) {
                File f = new File(u.toURI());
                if (f.exists()) return f;
            }
        } catch (Exception | Error ignored) {
            // not a plain file resource
        }
        return new File(roots.isEmpty() ? new File(".") : roots.get(0), relPath);
    }
}
