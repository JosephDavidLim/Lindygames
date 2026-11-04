package view;

import acm.graphics.GCompound;
import acm.graphics.GImage;
import acm.graphics.GLine;
import acm.graphics.GObject;
import acm.graphics.GOval;
import acm.graphics.GRect;

import entities.DefenderType;
import entities.EnemyType;

import java.awt.Color;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Loads character art from {@code res/sprites} and scales it once per size.
 * If a file is missing it returns a plain colored placeholder, so the game
 * never crashes over art. Run the game from the project root.
 */
public final class Assets {
    private static final Map<String, BufferedImage> RAW = new HashMap<>();
    private static final Map<String, Image> SCALED = new HashMap<>();

    private Assets() {
    }

    /**
     * Gets a defender's picture.
     *
     * @param type the defender
     * @param size width and height in pixels
     * @return a graphics object
     */
    public static GObject defender(DefenderType type, int size) {
        return sprite("res/sprites/neon/defenders/" + type.name().toLowerCase() + ".png", size,
                type.getDisplayName(), new Color(120, 170, 220));
    }

    /**
     * Gets an enemy's picture.
     *
     * @param type the enemy
     * @param size width and height in pixels
     * @return a graphics object
     */
    public static GObject enemy(EnemyType type, int size) {
        return sprite("res/sprites/neon/enemies/" + type.name().toLowerCase() + ".png", size,
                type.getDisplayName(), new Color(230, 120, 120));
    }

    /**
     * Gets a map's scene.
     *
     * @param map the map
     * @param w width in pixels
     * @param h height in pixels
     * @return the picture
     */
    public static GObject mapScene(game.GameMap map, double w, double h) {
        return picture("res/maps/" + map.getArtName() + ".png", w, h, new Color(90, 110, 130));
    }

    /**
     * Gets the long corridor for a floor of the Institute.
     *
     * @param map the floor
     * @param w width in pixels
     * @param h height in pixels
     * @return the picture
     */
    public static GObject corridor(game.GameMap map, double w, double h) {
        return picture("res/maps/corridor_" + map.getArtName() + ".png", w, h, new Color(20, 20, 50));
    }

    /**
     * Gets the title screen's painted scene.
     *
     * @param w width in pixels
     * @param h height in pixels
     * @return the picture
     */
    public static GObject titleScene(double w, double h) {
        return picture("res/ui/title.png", w, h, new Color(120, 190, 240));
    }

    /**
     * Gets the picture of a ladder joining two platforms.
     *
     * @return a 40 by 78 pixel picture
     */
    public static GObject ladder() {
        return picture("res/maps/ladder.png", 40, 78, new Color(112, 74, 44));
    }

    /**
     * Gets the picture for a special tile.
     *
     * @param terrain the tile
     * @return an 80-pixel picture
     */
    public static GObject terrain(game.Terrain terrain) {
        return picture("res/maps/tile_" + terrain.name().toLowerCase() + ".png", Layout.CELL, Layout.CELL,
                new Color(120, 120, 120, 120));
    }

    /**
     * Gets an item's icon.
     *
     * @param gear the item
     * @param size size in pixels
     * @return the icon
     */
    public static GObject gear(game.Gear gear, double size) {
        return picture("res/gear/" + gear.name().toLowerCase() + ".png", size, size, rarityColor(gear.getTier()));
    }

    /**
     * Gets the color for an item tier.
     *
     * @param tier 0 to 3
     * @return the rarity color
     */
    public static Color rarityColor(int tier) {
        Color[] colors = {new Color(190, 190, 205), new Color(90, 170, 255), new Color(200, 110, 255),
            new Color(255, 190, 60)};
        return colors[tier];
    }

    private static GObject picture(String path, double w, double h, Color fallback) {
        BufferedImage raw = rawImage(path);
        if (raw == null) {
            GRect r = new GRect(w, h);
            r.setFilled(true);
            r.setColor(fallback);
            return r;
        }
        String key = path + "@" + w + "x" + h;
        Image cached = SCALED.get(key);
        if (cached == null) {
            BufferedImage out = new BufferedImage(Math.max(1, (int) w), Math.max(1, (int) h), BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(raw, 0, 0, out.getWidth(), out.getHeight(), null);
            g.dispose();
            cached = out;
            SCALED.put(key, cached);
        }
        return new GImage(cached);
    }

    /**
     * Draws a coffee cup in code.
     *
     * @param size size in pixels
     * @return the cup
     */
    public static GObject coffee(double size) {
        double u = size / 40.0;
        GCompound c = new GCompound();
        GRect cup = new GRect(6 * u, 14 * u, 22 * u, 20 * u);
        cup.setFilled(true);
        cup.setFillColor(new Color(240, 240, 240));
        cup.setColor(Ui.INK);
        c.add(cup);
        GRect coffee = new GRect(8 * u, 16 * u, 18 * u, 6 * u);
        coffee.setFilled(true);
        coffee.setFillColor(new Color(110, 66, 36));
        coffee.setColor(new Color(110, 66, 36));
        c.add(coffee);
        GOval handle = new GOval(24 * u, 18 * u, 11 * u, 12 * u);
        handle.setColor(Ui.INK);
        c.add(handle);
        for (int i = 0; i < 3; i++) {
            GLine steam = new GLine((11 + i * 6) * u, 12 * u, (13 + i * 6) * u, 3 * u);
            steam.setColor(new Color(200, 200, 205));
            c.add(steam);
        }
        return c;
    }

    /**
     * Everything a view needs to animate one character: a set of squashed and
     * stretched copies of its picture on a shared canvas, or a plain
     * placeholder if the art file is missing.
     */
    public static final class SpriteSet {
        /** One picture per {@link Pose}, or null when the art is missing. */
        final Image[] frames;
        /** Width and height of each frame's canvas. */
        final int canvas;
        /** Width and height of the character at normal size. */
        final int size;
        private final String label;
        private final Color fallbackColor;

        SpriteSet(Image[] frames, int canvas, int size, String label, Color fallbackColor) {
            this.frames = frames;
            this.canvas = canvas;
            this.size = size;
            this.label = label;
            this.fallbackColor = fallbackColor;
        }

        /**
         * Builds the plain placeholder used when the art file is missing.
         *
         * @return a placeholder graphics object
         */
        GObject fallback() {
            return sprite("", size, label, fallbackColor);
        }
    }

    /**
     * Gets the animation frames for a defender.
     *
     * @param type the defender
     * @param size normal size in pixels
     * @return the sprite set
     */
    public static SpriteSet defenderSet(DefenderType type, int size) {
        return set("res/sprites/neon/defenders/" + type.name().toLowerCase() + ".png", size,
                type.getDisplayName(), new Color(120, 170, 220));
    }

    /**
     * Gets the animation frames for an enemy.
     *
     * @param type the enemy
     * @param size normal size in pixels
     * @return the sprite set
     */
    public static SpriteSet enemySet(EnemyType type, int size) {
        return set("res/sprites/neon/enemies/" + type.name().toLowerCase() + ".png", size,
                type.getDisplayName(), new Color(230, 120, 120));
    }

    private static final Map<String, SpriteSet> SETS = new HashMap<>();

    private static SpriteSet set(String path, int size, String label, Color fallback) {
        String key = path + "@" + size;
        SpriteSet cached = SETS.get(key);
        if (cached != null) {
            return cached;
        }
        int canvas = (int) Math.ceil(size * 1.45);
        BufferedImage raw = rawImage(path);
        Image[] frames = null;
        if (raw != null) {
            Pose[] poses = Pose.values();
            frames = new Image[poses.length];
            for (Pose p : poses) {
                int w = Math.max(1, (int) Math.round(size * p.sx));
                int h = Math.max(1, (int) Math.round(size * p.sy));
                BufferedImage out = new BufferedImage(canvas, canvas, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = out.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(raw, (canvas - w) / 2, canvas - h, w, h, null);
                g.dispose();
                frames[p.ordinal()] = out;
            }
        }
        SpriteSet result = new SpriteSet(frames, canvas, size, label, fallback);
        SETS.put(key, result);
        return result;
    }

    private static BufferedImage rawImage(String path) {
        if (!RAW.containsKey(path)) {
            File f = new File(path);
            BufferedImage img = null;
            try {
                img = f.exists() ? ImageIO.read(f) : null;
            } catch (IOException e) {
                img = null;
            }
            RAW.put(path, img);
        }
        return RAW.get(path);
    }

    private static GObject sprite(String path, int size, String label, Color fallback) {
        Image img = scaled(path, size);
        if (img != null) {
            return new GImage(img);
        }
        GCompound c = new GCompound();
        GRect box = new GRect(2, 2, size - 4, size - 4);
        box.setFilled(true);
        box.setFillColor(fallback);
        box.setColor(Ui.INK);
        c.add(box);
        acm.graphics.GLabel l = new acm.graphics.GLabel(label.substring(0, Math.min(4, label.length())),
                size * 0.15, size * 0.55);
        l.setFont("SansSerif-bold-" + Math.max(9, size / 5));
        c.add(l);
        return c;
    }

    private static Image scaled(String path, int size) {
        String key = path + "@" + size;
        if (SCALED.containsKey(key)) {
            return SCALED.get(key);
        }
        BufferedImage raw = RAW.get(path);
        if (raw == null && !RAW.containsKey(path)) {
            File f = new File(path);
            try {
                raw = f.exists() ? ImageIO.read(f) : null;
            } catch (IOException e) {
                raw = null;
            }
            RAW.put(path, raw);
        }
        Image result = null;
        if (raw != null) {
            BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(raw, 0, 0, size, size, null);
            g.dispose();
            result = out;
        }
        SCALED.put(key, result);
        return result;
    }
}
