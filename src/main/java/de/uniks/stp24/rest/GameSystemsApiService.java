package de.uniks.stp24.rest;

import de.uniks.stp24.dto.UpdateSystemDto;
import de.uniks.stp24.model.GameSystem;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;

public interface GameSystemsApiService {
    @GET("games/{gameId}/systems")
    Observable<List<GameSystem>> getSystems(@Path("gameId") String gameId, @Query("owner") String ownerId);

    @GET("games/{gameId}/systems/{id}")
    Observable<GameSystem> getSystem(@Path("gameId") String gameId, @Path("id") String id);

    @PATCH("games/{gameId}/systems/{id}")
    Observable<GameSystem> updateSystem(@Path("gameId") String gameId, @Path("id") String id, @Body UpdateSystemDto updateSystemDto);
}
