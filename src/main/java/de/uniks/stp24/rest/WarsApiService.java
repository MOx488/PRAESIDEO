package de.uniks.stp24.rest;

import de.uniks.stp24.dto.CreateWarDto;
import de.uniks.stp24.dto.UpdateWarDto;
import de.uniks.stp24.model.War;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;

public interface WarsApiService {
    @POST("games/{gameId}/wars")
    Observable<War> createWar(@Path("gameId") String gameId, @Body CreateWarDto createWarDto);

    @GET("games/{gameId}/wars")
    Observable<List<War>> getWars(@Path("gameId") String gameId, @Query("empire") String empire);

    @GET("games/{gameId}/wars/{warId}")
    Observable<War> getWar(@Path("gameId") String gameId, @Path("warId") String warId);

    @PATCH("games/{gameId}/wars/{warId}")
    Observable<War> updateWar(@Path("gameId") String gameId, @Path("warId") String warId, @Body UpdateWarDto updateWarDto);

    @DELETE("games/{gameId}/wars/{warId}")
    Observable<War> deleteWar(@Path("gameId") String gameId, @Path("warId") String warId);
}
