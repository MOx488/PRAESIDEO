package de.uniks.stp24.service;

import de.uniks.stp24.Main;
import javafx.scene.image.Image;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static de.uniks.stp24.util.Constants.IMAGE_DEFAULT_REQUESTED_HEIGHT;
import static de.uniks.stp24.util.Constants.IMAGE_DEFAULT_REQUESTED_WIDTH;

@Singleton
public class ImageCache {
    private final Map<String, Image> images = new HashMap<>();

    @Inject
    public ImageCache() {

    }

    public Image get(String path) {
        return get(path, true, IMAGE_DEFAULT_REQUESTED_WIDTH, IMAGE_DEFAULT_REQUESTED_HEIGHT);
    }

    public Image get(String path, boolean background) {
        return get(path, background, IMAGE_DEFAULT_REQUESTED_WIDTH, IMAGE_DEFAULT_REQUESTED_HEIGHT);
    }

    public Image get(String path, double requestedWidth, double requestedHeight) {
        return get(path, true, requestedWidth, requestedHeight);
    }

    public Image get(String path, boolean background, double requestedWidth, double requestedHeight) {
        return images.computeIfAbsent(path, p -> load(p, background, requestedWidth, requestedHeight));
    }

    private Image load(String path, boolean background, double requestedWidth, double requestedHeight) {
        if (GraphicsEnvironment.isHeadless()) {
            requestedWidth = 1;
            requestedHeight = 1;
        }

        if (!path.startsWith("http://") && !path.startsWith("https://") && !path.startsWith("file://") && !path.startsWith("data:")) {
            final URL url = Main.class.getResource(path);
            if (url != null) {
                path = url.toExternalForm();
            } else {
                System.err.println("Failed to load image: " + path);
                return new Image("https://via.placeholder.com/150?text=Image+not+found", true);
            }
        }

        final String finalPath = path;
        Image image = new Image(path, requestedWidth, requestedHeight, true, false, background);
        image.errorProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                System.err.println("Failed to load image: " + finalPath);
                images.remove(finalPath);
            }
        });

        return image;
    }
}
