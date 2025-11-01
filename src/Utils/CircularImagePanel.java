package Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws a circular (masked) profile picture that automatically
 * scales smoothly and fits inside its assigned diameter.
 * The panel is fully theme-neutral (no hard‑coded colors).
 */
public class CircularImagePanel extends JPanel {

    private BufferedImage image;
    private final int diameter;

    public CircularImagePanel(String path, int diameter) {
        this.diameter = diameter;
        loadImage(path);
        setPreferredSize(new Dimension(diameter, diameter));
        setOpaque(false); // transparent background so FlatLaf shows panel bg underneath
    }

    private void loadImage(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                image = ImageIO.read(file);
            } else {
                // Optional fallback: use a simple colored placeholder
                image = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                g2.setColor(UIManager.getColor("Component.borderColor")); // subtle theme color
                g2.fillOval(0, 0, diameter, diameter);
                g2.dispose();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Shape clip = new Ellipse2D.Float(0, 0, diameter, diameter);
        g2.setClip(clip);

        // use high‑quality interpolation for smooth resize
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, diameter, diameter, this);
        g2.dispose();
    }
}