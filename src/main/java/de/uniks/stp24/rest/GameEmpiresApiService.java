package de.uniks.stp24.rest;

import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.dto.UpdateEmpireDto;
import de.uniks.stp24.model.ChangeRessource;
import de.uniks.stp24.model.Empire;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;

public interface GameEmpiresApiService {
    @GET("games/{gameId}/empires")
    Observable<List<ReadEmpireDto>> getEmpires(@Path("gameId") String gameId);

    @GET("games/{gameId}/empires/{id}")
    Observable<Empire> getEmpire(@Path("gameId") String gameId, @Path("id") String empireId);

    @PATCH("games/{gameId}/empires/{id}")
    Observable<Empire> updateEmpire(@Path("gameId") String gameId, @Path("id") String empireId, @Body UpdateEmpireDto UpdateEmpireDto);

    @PATCH("games/{gameId}/empires/{id}")
    Observable<Empire> updateResources(@Path("gameId") String gameId, @Path("id") String empireId, @Body ChangeRessource changeRessource);

    @PATCH("games/{gameId}/empires/{id}")
    Observable<Empire> updateResources(@Path("gameId") String gameId, @Path("id") String empireId, @Body ChangeRessource changeRessource, @Query("free") boolean isFree);
}