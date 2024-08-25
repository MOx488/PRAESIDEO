package de.uniks.stp24.component.popups;

import de.uniks.stp24.App;
import de.uniks.stp24.service.AudioService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.PrefService;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;
import java.awt.*;
import java.util.ResourceBundle;

@Component(view = "PauseMenuPopUp.fxml")
public class PauseMenuPopUpComponent extends AnchorPane {
    @FXML
    Button highVolumeButton;
    @FXML
    Button muteButton;
    @FXML
    AnchorPane pauseMenuBox;
    @FXML
    Slider audioSlider;
    @FXML
    public ImageView muteIconImageView;
    @FXML
    public ImageView highVolumeIconImageView;

    @Inject
    public App app;

    @Inject
    public ImageCache imageCache;
    @Inject
    public AudioService audioService;
    @Inject
    public PrefService prefService;

    @Param("modalStage")
    public Stage modal;
    @Param("windowCloseHandler")
    EventHandler<WindowEvent> windowCloseHandler;

    @Inject
    @Resource
    public ResourceBundle bundle;

    private ChangeListener<Number> audioSliderListener;

    @Inject
    public PauseMenuPopUpComponent() {
    }

    @OnRender
    public void onRender() {
        muteIconImageView.setImage(imageCache.get("image/sound/mute.png"));
        highVolumeIconImageView.setImage(imageCache.get("image/sound/unmute.png"));

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        if (prefService.isMuted()) {
            audioSlider.setValue(0);
        } else {
            audioSlider.setValue(prefService.getVolume() * 100);
        }

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        audioSlider.valueProperty().addListener(audioSliderListener = (observable, oldValue, newValue) -> {
            if (newValue.doubleValue() == 0) {
                prefService.setMute(true);
                audioService.muteSound();
            } else {
                prefService.setMute(false);
                audioService.unmuteSound();
            }

            prefService.setVolume(newValue.doubleValue() / 100);
            audioService.setVolume(newValue.doubleValue() / 100);
        });
    }

    public void resume() {
        modal.close();
        modal.setScene(null);
    }

    public void quit() {
        modal.close();
        modal.setScene(null);
        windowCloseHandler.handle(null);
        app.show("/lobby");
    }

    public void mute() {
        audioSlider.setValue(0);
    }

    public void fullVolume() {
        audioSlider.setValue(100);
    }

    public void exitGame() {
        System.exit(0);
    }

    @OnDestroy
    void onDestroy() {
        if (audioSliderListener != null) {
            audioSlider.valueProperty().removeListener(audioSliderListener);
        }
    }
}
