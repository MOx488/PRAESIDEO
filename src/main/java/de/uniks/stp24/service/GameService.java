package de.uniks.stp24.service;

import de.uniks.stp24.dto.UpdateGameDto;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.rest.GamesApiService;
import io.reactivex.rxjava3.core.Observable;

import javax.inject.Inject;
import java.util.Map;

public class GameService {
    @Inject
    GamesApiService gamesApiService;

    @Inject
    public GameService() {

    }

    public Observable<Game> startGame(Game game, String password) {
        return this.updateGame(game._id(), new UpdateGameDto(game.name(), game.maxMembers(), true, game.speed(), game.settings(), password));
    }

    public Observable<Game> updateGame(String gameId, UpdateGameDto updateGameDto) {
        return gamesApiService.updateGame(gameId, updateGameDto);
    }

    public Observable<Game> updateSpeed(String gameId, int speed) {
        return gamesApiService.updateSpeed(gameId, Map.of("speed", speed));
    }

    public Observable<Game> gameTick(String gameId) {
        return gamesApiService.gameTick(gameId, true);
    }

    public Observable<Game> deleteGame(String gameId) {
        return gamesApiService.deleteGame(gameId);
    }

}
