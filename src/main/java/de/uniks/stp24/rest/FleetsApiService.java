package de.uniks.stp24.rest;

import de.uniks.stp24.dto.CreateFleetDto;
import de.uniks.stp24.dto.UpdateFleetDto;
import de.uniks.stp24.model.Fleet;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;

public interface FleetsApiService {
    @POST("games/{gameId}/fleets")
    Observable<Fleet> createFleet(@Path("gameId") String gameId, @Body CreateFleetDto createFleetDto);

    @GET("games/{gameId}/fleets")
    Observable<List<Fleet>> getFleets(@Path("gameId") String gameId, @Query("empire") String empire);

    @GET("games/{gameId}/fleets")
    Observable<List<Fleet>> getFleets(@Path("gameId") String gameId);

    @GET("games/{gameId}/fleets/{fleetId}")
    Observable<Fleet> getFleet(@Path("gameId") String gameId, @Path("fleetId") String fleetId);

    @PATCH("games/{gameId}/fleets/{fleetId}")
    Observable<Fleet> updateFleet(@Path("gameId") String gameId, @Path("fleetId") String fleetId, @Body UpdateFleetDto updateFleetDto);

    @DELETE("games/{gameId}/fleets/{fleetId}")
    Observable<Fleet> deleteFleet(@Path("gameId") String gameId, @Path("fleetId") String fleetId);
}
