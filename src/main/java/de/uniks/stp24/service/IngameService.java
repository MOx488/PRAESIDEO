package de.uniks.stp24.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.stp24.dto.CastleNamesDto;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.rest.GameSystemsApiService;
import io.reactivex.rxjava3.core.Observable;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class IngameService {
    @Inject
    public GameSystemsApiService gameSystemsApiService;
    @Inject
    public ObjectMapper objectMapper;

    @Inject
    public IngameService() {

    }

    public Observable<List<GameSystem>> getSystems(String gameId) {
        return gameSystemsApiService.getSystems(gameId, null);
    }

    public List<String> readCastleNames() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("de/uniks/stp24/constants/castle_name.json");
        if (inputStream == null) {
            System.err.println("Couldn't find castle_name.json");
            return null;
        }

        try {
            return objectMapper.readValue(inputStream, CastleNamesDto.class).castles();
        } catch (IOException e) {
            System.err.println("Couldn't parse castle_name.json");
        }

        return null;
    }
}
