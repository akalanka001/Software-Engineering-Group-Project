package Utils;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

public final class FxHost {
    private static volatile boolean started = false;
    private static JFXPanel anchor;

    private FxHost() {}

    public static void ensureStarted() {
        if (started) return;
        synchronized (FxHost.class) {
            if (started) return;
            anchor = new JFXPanel();          // starts the toolkit
            started = true;
            System.out.println("[FxHost] JavaFX runtime initialized.");
        }
    }

    public static void shutdown() {
        if (!started) return;
        Platform.exit();
        started = false;
        anchor = null;
    }
}