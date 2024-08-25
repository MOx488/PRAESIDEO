package de.uniks.stp24.service;

import de.uniks.stp24.App;
import de.uniks.stp24.component.PauseTextComponent;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.ws.EventListener;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import javafx.scene.layout.Pane;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GameTicksService {
    @Inject
    public GameService gameService;
    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public EventService eventService;
    @Inject
    public Provider<PauseTextComponent> pauseTextComponentProvider;

    public BehaviorSubject<Long> periodSubject;
    private Game game;
    private Pane pauseTextContainer;

    @Inject
    public GameTicksService() {
    }

    public void init(boolean isHost, Game game, Pane pauseTextContainer) {
        if (isHost) {
            startGameTicks(game);
        }

        this.pauseTextContainer = pauseTextContainer;
        this.game = game;
        handlePausedTextComponent();

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".updated", Game.class), event -> {
            final Game newGame = event.data();
            if (newGame.speed() == this.game.speed()) {
                return;
            }

            this.game = newGame;

            if (isHost) {
                changeTickSpeed(this.game.speed());
            }

            this.handlePausedTextComponent();
        });
    }

    private void handlePausedTextComponent() {
        if (game.speed() != 0) {
            pauseTextContainer.getChildren().clear();
            return;
        }

        //game speed is 0 -> pause text should be visible
        if (!pauseTextContainer.getChildren().isEmpty()) {
            //pause text is already visible
            return;
        }

        //pause text is not visible and game speed is 0 -> show it
        pauseTextContainer.getChildren().add(app.initAndRender(pauseTextComponentProvider.get(), Map.of(), subscriber));
    }

    private void startGameTicks() {
        // Use switchMap to switch to a new interval observable whenever the interval duration changes
        Observable<Long> intervalObservable = getIntervalObservable();

        subscriber.subscribe(intervalObservable.subscribe(value -> {
                            Runnable onFinish = () -> subscriber.subscribe(this.gameService.gameTick(this.game._id()));
                            eventService.onTick(onFinish);
                        }
                )
        );
    }

    public @NotNull Observable<Long> getIntervalObservable() {
        return periodSubject.switchMap(speed -> {
            if (speed == 0) {
                return Observable.never();
            }

            return Observable.interval(speed, TimeUnit.SECONDS);
        });
    }

    public void startGameTicks(Game game) {
        this.game = game;
        periodSubject = BehaviorSubject.createDefault(calculatePeriod(game.speed()));
        startGameTicks();
    }

    private long calculatePeriod(int gameSpeed) {
        if (gameSpeed <= 0) return 0;

        return 30 / gameSpeed;
    }

    public void changeTickSpeed(int newSpeed) {
        periodSubject.onNext(calculatePeriod(newSpeed));
    }

    public void stopGameTicks() {
        subscriber.dispose();
    }
}
