package de.uniks.stp24.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.stp24.App;
import de.uniks.stp24.model.Job;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.controlsfx.control.Notifications;
import org.fulib.fx.annotation.controller.Resource;
import retrofit2.Invocation;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.function.Function;

@Singleton
public class NotificationService {

    @Inject
    public App app;

    @Inject
    public ImageCache imageCache;

    @Inject
    public ErrorService errorService;

    @Inject
    public ObjectMapper objectMapper;

    @Inject
    @Resource
    public ResourceBundle bundle;

    // retrofit method name maps to error message key
    private final HashMap<String, Function<Response, String>> localizedMessages = new HashMap<>();

    @Inject
    public NotificationService() {
        localizedMessages.put("createJob", response -> {
            try {
                final ResponseBody body = response.peekBody(1024);
                final Job job = objectMapper.readValue(body.string(), Job.class);
                final String daysMessage = bundle.getString("days");
                final String correctDays = job.total() == 1 ? daysMessage.substring(0, daysMessage.length() - 1) : daysMessage;
                return bundle.getString("jobs.started.success") + " (" + job.total() + " " + correctDays + ")";
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

            return bundle.getString("error.unknown");
        });
    }

    public NotificationService setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        return this;
    }

    public void handleServerResponse(Response response) {
        // handle error response
        String message;
        boolean isSuccess;

        if (!response.isSuccessful()) {
            message = errorService.getLocalizedErrorMessage(response);
            isSuccess = false;
        } else {
            // handle successful response
            final Request req = response.request();
            final Invocation tag = req.tag(Invocation.class);
            assert tag != null;
            final String methodName = tag.method().getName();
            final Function<Response, String> responseFunction = localizedMessages.get(methodName);
            if (responseFunction == null) {
                return;
            }

            message = responseFunction.apply(response);
            isSuccess = true;
        }

        Platform.runLater(() -> this.displayNotification(message, isSuccess));
    }

    public void displayNotification(String message, boolean isSuccess) {
        displayNotification(message, isSuccess, null);
    }

    public void displayNotification(String message, boolean isSuccess, String customImagePath) {
        final String imageUrl = (customImagePath != null && !customImagePath.isEmpty())
                ? customImagePath
                : (isSuccess ? "image/green_checkmark.png" : "image/cross_red.png");

        ImageView imageView = new ImageView();
        imageView.setImage(imageCache.get(imageUrl));
        imageView.setFitHeight(25);
        imageView.setFitWidth(25);
        imageView.setId("notificationImage");

        Notifications.create()
                .text(message)
                .hideAfter(javafx.util.Duration.seconds(2))
                .owner(app.stage())
                .graphic(imageView)
                .show();
    }
}