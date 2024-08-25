package de.uniks.stp24;

import dagger.Module;
import dagger.Provides;
import de.uniks.stp24.rest.*;
import de.uniks.stp24.service.*;
import de.uniks.stp24.ws.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

import javax.inject.Singleton;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@Module
public class TestModule {

    @Provides
    @Singleton
    ImageCache imageCache() {
        return spy(ImageCache.class);
    }

    @Provides
    @Singleton
    AuthApiService authApiService() {
        return mock(AuthApiService.class);
    }

    @Provides
    @Singleton
    UsersApiService usersApiService() {
        return mock(UsersApiService.class);
    }

    @Provides
    @Singleton
    GamesApiService gamesApiService() {
        return mock(GamesApiService.class);
    }

    @Provides
    @Singleton
    GameMembersApiService gameMembersApiService() {
        return mock(GameMembersApiService.class);
    }

    @Provides
    @Singleton
    GameLogicApiService gameLogicApiService() {
        return mock(GameLogicApiService.class);
    }

    @Provides
    @Singleton
    PresetsApiService presetsApiService() {
        return mock(PresetsApiService.class);
    }

    @Provides
    @Singleton
    GameSystemsApiService gameSystemsApiService() {
        return mock(GameSystemsApiService.class);
    }

    @Provides
    @Singleton
    GameEmpiresApiService gameEmpiresApiService() {
        return mock(GameEmpiresApiService.class);
    }

    @Provides
    @Singleton
    FriendsApiService friendsApiService() {
        return mock(FriendsApiService.class);
    }

    @Provides
    @Singleton
    EventListener eventListener() {
        return mock(EventListener.class);
    }

    @Provides
    @Singleton
    Retrofit retrofit() {
        return mock(Retrofit.class);
    }

    @Provides
    @Singleton
    DiscordActivityService discordActivityService() {
        return mock(DiscordActivityService.class);
    }

    @Provides
    @Singleton
    JobsApiService jobsApiService() {
        return mock(JobsApiService.class);
    }

    @Provides
    @Singleton
    GameTicksService gameTicksService() {
        return mock(GameTicksService.class);
    }

    @Provides
    @Singleton
    JobService jobService() {
        return mock(JobService.class);
    }

    @Provides
    @Singleton
    WarsApiService warsApiService() {
        return mock(WarsApiService.class);
    }

    @Provides
    @Singleton
    ShipsApiService shipsApiService() {
        return mock(ShipsApiService.class);
    }

    @Provides
    @Singleton
    FleetsApiService fleetsApiService() {
        return mock(FleetsApiService.class);
    }

    @Provides
    @Singleton
    static OkHttpClient client(TokenStorage tokenStorage, NotificationService notificationService) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    final String token = tokenStorage.getToken();
                    if (token == null) {
                        return chain.proceed(chain.request());
                    }
                    final Request newRequest = chain
                            .request()
                            .newBuilder()
                            .addHeader("Authorization", "Bearer " + token)
                            .build();
                    return chain.proceed(newRequest);
                }).addInterceptor(chain -> {
                    final Response response = chain.proceed(chain.request());

                    notificationService.handleServerResponse(response);


                    return response;
                }).build();
    }
}
