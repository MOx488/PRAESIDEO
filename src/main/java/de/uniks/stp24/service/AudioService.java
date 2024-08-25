package de.uniks.stp24.service;

import de.uniks.stp24.Main;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.Objects;

@Singleton
public class AudioService {
    private MediaPlayer mediaPlayer;
    public boolean isPlayed;
    private double soundVolume;

    @Inject
    public AudioService() {
    }

    public void init(double volume) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        playSound("medieval.mp3");
        setVolume(volume);
    }

    public void playSound(String soundPath) {
        if (this.isPlayed) {
            return;
        }

        final Media sound = new Media((Objects.requireNonNull(Main.class.getResource("sounds/" + soundPath))).toString());

        this.mediaPlayer = new MediaPlayer(sound);
        this.mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        this.mediaPlayer.play();
        this.isPlayed = true;
    }

    public void muteSound() {
        this.mediaPlayer.setMute(true);
    }

    public void unmuteSound() {
        this.mediaPlayer.setMute(false);
    }

    public double getVolume() {
        return this.soundVolume;
    }

    public void setVolume(double volume) {
        this.soundVolume = volume;
        this.mediaPlayer.setVolume(volume);
    }

    public void stopSound() {
        this.mediaPlayer.stop();
    }

}
