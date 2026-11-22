package ui;

/**
 * Standard JavaFX Launcher entry point that delegates to FixTrackApp.main(args).
 * Bypasses JavaFX runtime component checks when executed via standard classpath
 * or IDE run configurations.
 */
public class Launcher {
    public static void main(String[] args) {
        FixTrackApp.main(args);
    }
}

