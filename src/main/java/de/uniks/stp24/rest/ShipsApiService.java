package de.uniks.stp24.rest;

import de.uniks.stp24.dto.ReadShipDto;
import de.uniks.stp24.dto.UpdateShipDto;
import de.uniks.stp24.model.Ship;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;

public interface ShipsApiService {
    @GET("games/{gameId}/fleets/{fleetId}/ships")
    Observable<List<ReadShipDto>> getShips(@Path("gameId") String gameId, @Path("fleetId") String fleetId);

    @GET("games/{gameId}/fleets/{fleetId}/ships/{shipId}")
    Observable<Ship> getShip(@Path("gameId") String gameId, @Path("fleetId") String fleetId, @Path("shipId") String shipId);

    @PATCH("games/{gameId}/fleets/{fleetId}/ships/{shipId}")
    Observable<Ship> updateShip(@Path("gameId") String gameId, @Path("fleetId") String fleetId, @Path("shipId") String shipId, @Body UpdateShipDto updateShipDto);

    @DELETE("games/{gameId}/fleets/{fleetId}/ships/{shipId}")
    Observable<Ship> deleteShip(@Path("gameId") String gameId, @Path("fleetId") String fleetId, @Path("shipId") String shipId);
}
