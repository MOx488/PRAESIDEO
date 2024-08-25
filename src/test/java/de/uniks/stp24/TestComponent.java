package de.uniks.stp24;

import dagger.Component;
import de.uniks.stp24.dagger.MainComponent;
import de.uniks.stp24.dagger.MainModule;
import de.uniks.stp24.rest.*;
import de.uniks.stp24.service.*;
import de.uniks.stp24.ws.EventListener;
import okhttp3.OkHttpClient;

import javax.inject.Singleton;

@Component(modules = {
        MainModule.class,
        TestModule.class,
})
@Singleton
public interface TestComponent extends MainComponent {

    OkHttpClient okHttpClient();

    PrefService prefService();

    AuthApiService authApiService();

    UsersApiService usersApiService();

    GamesApiService gamesApiService();

    GameMembersApiService gameMembersApiService();

    GameSystemsApiService gameSystemsApiService();

    GameEmpiresApiService gameEmpiresApiService();

    FriendsApiService friendsApiService();

    GameLogicApiService gameLogicApiService();

    PresetsApiService presetsApiService();

    DiscordActivityService discordActivityService();

    EventListener eventListener();

    ImageCache imageCache();

    JobsApiService jobsApiService();

    FleetsApiService fleetsApiService();

    GameTicksService gameTicksService();

    PresetsService presetsService();

    EventService eventService();

    EmojiService emojiService();

    JobService jobService();

    WarsApiService warsApiService();

    ShipsApiService shipsApiService();

    ClientChangeService clientChangeService();

    MapService mapService();

    @Component.Builder
    interface Builder extends MainComponent.Builder {
        @Override
        TestComponent build();
    }
}
