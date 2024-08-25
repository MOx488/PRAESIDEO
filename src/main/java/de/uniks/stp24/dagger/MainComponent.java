package de.uniks.stp24.dagger;

import dagger.BindsInstance;
import dagger.Component;
import de.uniks.stp24.App;
import de.uniks.stp24.Routes;
import de.uniks.stp24.rest.GameMembersApiService;
import de.uniks.stp24.service.DiscordActivityService;
import de.uniks.stp24.service.ImageCache;
import okhttp3.OkHttpClient;

import javax.inject.Singleton;

@Component(modules = {
        MainModule.class,
        HttpModule.class,
})
@Singleton
public interface MainComponent {

    Routes routes();

    GameMembersApiService gameMembersApiService();

    ImageCache imageCache();

    OkHttpClient okHttpClient();

    DiscordActivityService discordActivityService();

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder mainApp(App app);

        MainComponent build();
    }

}
