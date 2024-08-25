package de.uniks.stp24.service;

import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.rest.GameEmpiresApiService;
import io.reactivex.rxjava3.core.Observable;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import java.util.List;

public class EmpireService {
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;

    @Inject
    Subscriber subscriber;

    @Inject
    public EmpireService() {

    }

    public Observable<List<ReadEmpireDto>> getReadEmpires(String gameId) {
        return gameEmpiresApiService.getEmpires(gameId);
    }
}
