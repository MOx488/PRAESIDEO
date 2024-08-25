package de.uniks.stp24.service;

import de.uniks.stp24.Main;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.prefs.Preferences;

@Singleton
public class PrefService {
    private final Preferences preferences = Preferences.userNodeForPackage(Main.class);

    @Inject
    public AudioService audioService;

    @Inject
    public PrefService() {
    }

    //Auth

    public String getRefreshToken() {
        return preferences.get("refreshToken", null);
    }

    public void setRefreshToken(String refreshToken) {
        preferences.put("refreshToken", refreshToken);
    }

    public void removeRefreshToken() {
        preferences.remove("refreshToken");
    }

    //Sound
    public void setVolume(Double volume) {
        preferences.putDouble("volume", volume);
    }

    public Double getVolume() {
        return preferences.getDouble("volume", audioService.getVolume());
    }

    public boolean isMuted() {
        return preferences.getBoolean("mute", false);
    }

    public void setMute(boolean mute) {
        preferences.putBoolean("mute", mute);
    }

    //Language
    public Locale getLocale() {
        return Locale.forLanguageTag(preferences.get("locale", Locale.getDefault().toLanguageTag()));
    }

    public void setLocale(Locale locale) {
        preferences.put("locale", locale.toLanguageTag());
    }

}
