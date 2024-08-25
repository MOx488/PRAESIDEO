package de.uniks.stp24;

import de.uniks.stp24.dagger.DaggerMainComponent;
import de.uniks.stp24.dagger.MainComponent;
import de.uniks.stp24.util.Constants;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.fulib.fx.FulibFxApp;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import static de.uniks.stp24.util.Constants.ORIGINAL_WINDOW_HEIGHT;
import static de.uniks.stp24.util.Constants.ORIGINAL_WINDOW_WIDTH;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;

public class App extends FulibFxApp {
    private MainComponent component;
    private Runnable cssFxStop;

    public App() {
        super();
        this.component = DaggerMainComponent.builder().mainApp(this).build();
    }

    // package-private - only for testing
    public void setComponent(MainComponent component) {
        this.component = component;
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            super.start(primaryStage);
            registerRoutes(component.routes());

            stage().addEventHandler(KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F5) {
                    this.refresh();
                }
            });

            primaryStage.getScene().getStylesheets().add(
                    Objects.requireNonNull(App.class.getResource("styles.css")).toExternalForm()
            );

            if (System.getenv("DEV") != null) {
                cssFxStop = CSSFX.start(primaryStage);
            }

            this.setStageDimension(primaryStage);
            this.setAppIcon(primaryStage);
            this.setTaskbarIcon();
            this.setTitlePattern("PRAESIDEO - %s");

            if (System.getenv("DEV") != null) {
                autoRefresher().setup(Path.of("src/main/resources/de/uniks/stp24"));
            }

            // Start the application and check it is a client change
            Parameters params = getParameters();
            List<String> args = (params != null) ? params.getRaw() : List.of();
            show("/gameLaunch", (args.size() == 2) ? Map.of("RefreshToken", args.get(0),
                    "GameID", args.get(1)) : Map.of());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while starting the application: " + e.getMessage(), e);
        }
    }

    private void setStageDimension(Stage primaryStage) {
        double height = ORIGINAL_WINDOW_HEIGHT;
        if (GraphicsEnvironment.isHeadless()) {
            height = Math.min(ORIGINAL_WINDOW_HEIGHT, Constants.MAX_WINDOW_HEIGHT_HEADLESS);
        }

        primaryStage.setWidth(ORIGINAL_WINDOW_WIDTH);
        primaryStage.setHeight(height);

        primaryStage.setMinWidth(ORIGINAL_WINDOW_WIDTH);
        primaryStage.setMinHeight(height);
    }

    private void closeDiscordActivityService() {
        component.discordActivityService().stopActivity();
    }

    private void closeOkHttpClient() {
        OkHttpClient client = component.okHttpClient();
        try (ExecutorService executorService = client.dispatcher().executorService()) {
            executorService.shutdown();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while closing the OkHttpClient executor service: " + e.getMessage(), e);
        }
        client.connectionPool().evictAll();

        Cache cache = client.cache();
        if (cache == null) {
            return;
        }

        try {
            cache.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while closing the OkHttpClient cache: " + e.getMessage(), e);
        }
    }

    private void setAppIcon(Stage stage) {
        final Image image = component.imageCache().get("image/icon.png", false);
        stage.getIcons().add(image);
    }

    private void setTaskbarIcon() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        try {
            final Taskbar taskbar = Taskbar.getTaskbar();
            final java.awt.Image image = ImageIO.read(Objects.requireNonNull(App.class.getResource("image/icon.png")));
            taskbar.setIconImage(image);
        } catch (Exception ignored) {
        }
    }

    public MainComponent component() {
        return component;
    }

    @Override
    public void stop() {
        super.stop();
        autoRefresher().close();
        closeOkHttpClient();
        closeDiscordActivityService();

        if (cssFxStop != null) {
            cssFxStop.run();
        }
    }
}
