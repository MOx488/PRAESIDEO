package de.uniks.stp24;

import de.uniks.stp24.service.DiscordActivityService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.PrefService;
import javafx.stage.Stage;
import org.fulib.fx.controller.Subscriber;
import org.junit.jupiter.api.AfterAll;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;

import javax.inject.Provider;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;

public class ControllerTest extends ApplicationTest {

    @Spy
    public App app = new App();
    @Spy
    final public ResourceBundle bundle = ResourceBundle.getBundle("de/uniks/stp24/lang/lang", Locale.ENGLISH);
    @Spy
    public PrefService prefService;
    @Spy
    public final Subscriber subscriber = new Subscriber();
    @Spy
    public final ImageCache imageCache = new ImageCache();
    @Mock
    public DiscordActivityService discordActivityService;

    protected Stage stage;
    protected TestComponent testComponent;

    protected static <T> Provider<T> spyProvider(Provider<T> base) {
        //noinspection Anonymous2MethodRef,Convert2Lambda
        return new Provider<>() {
            @Override
            public T get() {
                return base.get();
            }
        };
    }

    @Override
    public void init() throws TimeoutException {
        FxToolkit.registerStage(Stage::new);
    }

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        this.stage = stage;
        stage.setX(0);
        stage.setY(0);
        testComponent = (TestComponent) DaggerTestComponent.builder().mainApp(app).build();
        Mockito.doReturn(null).when(app).show("/gameLaunch", Map.of());
        Mockito.doNothing().when(subscriber).dispose();
        app.setComponent(testComponent);
        app.start(stage);
        stage.requestFocus();
        prefService.setLocale(Locale.ENGLISH);
    }


    @Override
    public void stop() throws Exception {
        super.stop();
        app.stop();
        app = null;
        stage = null;
        testComponent = null;
    }


    @AfterAll
    public static void afterClass() {
        Mockito.framework().clearInlineMocks();
    }
}