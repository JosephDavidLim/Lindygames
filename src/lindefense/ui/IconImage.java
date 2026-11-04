package lindefense.ui;

import acm.graphics.GObject;
import acm.graphics.GRectangle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import lindefense.tower.TowerType;
import lindefense.ui.sprites.towers.TowerIcons;

/**
 * A tower icon pre-rendered once into a bitmap and cached per (type, game tier, size), centred on its location. The
 * live art is a compound of dozens of shapes; dozens of ability-bar slots painting those every frame cost milliseconds,
 * a cached bitmap costs almost nothing.
 */
final class IconImage extends GObject {
    private static final long serialVersionUID = 1L;
    private static final int SS = 2;   // supersampling: bitmap is SS x the logical size
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private final BufferedImage img;
    private final double size;

    IconImage(TowerType type, int tier, double size) {
        this.size = size;
        String key = type + ":" + tier + ":" + size;
        BufferedImage b = CACHE.get(key);
        if (b == null) {
            int px = (int) Math.ceil(size * SS);
            b = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = b.createGraphics();
            try {
                Cyber.quality(g);
                g.translate(px / 2.0, px / 2.0);
                g.scale(SS, SS);
                TowerIcons.create(type, tier, size).paint(g);
            } finally {
                g.dispose();
            }
            CACHE.put(key, b);
        }
        img = b;
    }

    @Override public GRectangle getBounds() { return new GRectangle(getX() - size / 2, getY() - size / 2, size, size); }

    @Override public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, (int) Math.round(getX() - size / 2), (int) Math.round(getY() - size / 2), (int) Math.round(size), (int) Math.round(size), null);
        } finally {
            g2.dispose();
        }
    }
}
