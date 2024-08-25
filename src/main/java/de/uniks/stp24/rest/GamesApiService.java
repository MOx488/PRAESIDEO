package de.uniks.stp24.rest;

import de.uniks.stp24.dto.CreateGameDto;
import de.uniks.stp24.dto.UpdateGameDto;
import de.uniks.stp24.model.Game;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface GamesApiService {
    @POST("games")
    Observable<Game> createGame(@Body CreateGameDto dto);

    @GET("games")
    Observable<List<Game>> getGames();

    @GET("games/{id}")
    Observable<Game> getGameById(@Path("id") String id);

    @PATCH("games/{id}")
    Observable<Game> updateGame(@Path("id") String id, @Body UpdateGameDto dto);

    @PATCH("games/{id}")
    Observable<Game> updateSpeed(@Path("id") String id, @Body Map<String, Integer> speed);

    @PATCH("games/{id}")
    Observable<Game> gameTick(@Path("id") String id, @Query("tick") boolean tick);

    @DELETE("games/{id}")
    Observable<Game> deleteGame(@Path("id") String id);

}
