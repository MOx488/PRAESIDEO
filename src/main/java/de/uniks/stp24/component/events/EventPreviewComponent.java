package de.uniks.stp24.component.events;

import de.uniks.stp24.App;
import de.uniks.stp24.model.ChoiceType;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.service.EventService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.PrefService;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.EventListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.WindowEvent;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@Component(view = "EventPreview.fxml")
public class EventPreviewComponent extends HBox {

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public ImageCache imageCache;

    @Inject
    public EventListener eventListener;
    @Inject
    public TokenStorage tokenStorage;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public EventService eventService;
    @Inject
    public PrefService prefService;

    @Inject
    public Provider<EventComponent> eventComponentProvider;

    @FXML
    Label activeEventLabel;
    @FXML
    ImageView eventImagePreview;
    @FXML
    Button viewEventInformationButton;

    @Param("game")
    Game game;

    @Param("empire")
    Empire empire;

    @Param("parent")
    AnchorPane parentContainer;

    @Param("eventId")
    String eventId;

    @Param("overlapContainer")
    AnchorPane overlapContainer;

    private int effectIndex;

    private final Consumer<ChoiceType> closedComponentNotifier = this::eventComponentClosed;
    private final Consumer<Integer> setEffectIndex = this::setEffectIndex;

    private ChoiceType previousChoice;

    private final List<String> specialEventIds = List.of("marriage", "mysterious_liquid", "group");

    public final EventHandler<WindowEvent> windowCloseEventHandler = window -> {
        this.eventService.saveEventData();
        this.eventService.removeEvent();
    };

    @Inject
    public EventPreviewComponent() {

    }

    @OnInit
    public void init() {
        AnchorPane.setTopAnchor(this, 105d);
        AnchorPane.setLeftAnchor(this, 130d);

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".updated", Game.class), event -> {
            final Game newGame = event.data();
            if (newGame.period() == this.game.period()) {
                return;
            }

            this.game = newGame;
        });

        this.eventService.setConsumers(this::OnNewEvent, this::OnEndEvent);
        this.effectIndex = -1;
    }

    private void OnNewEvent(String eventId) {
        this.eventId = eventId;
        this.previousChoice = ChoiceType.NO_CHOICE;
        this.effectIndex = -1;
        this.viewEventInformation();
    }

    private void OnEndEvent(String eventId) {
        this.eventId = null;
        this.previousChoice = ChoiceType.NO_CHOICE;
        this.effectIndex = -1;
        this.setVisible(false);
    }

    private void setEffectIndex(Integer integer) {
        this.effectIndex = integer;
    }

    @OnRender
    public void onRender() {
        if (this.eventId == null) {
            //if we don't have an eventId, we need to check if we left the game with an active event
            if (empire._private() != null && empire._private().containsKey("activeEventId") && empire._private().containsKey("effectIndex") && empire._private().containsKey("previousChoice")) {
                this.eventId = (String) empire._private().get("activeEventId");
                this.effectIndex = (int) empire._private().get("effectIndex");
                this.previousChoice = ChoiceType.values()[(int) empire._private().get("previousChoice")];
            }
        }

        this.app.stage().getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseEventHandler);

        //still null after checking pref storage -> no active event -> hide component
        if (this.eventId == null) {
            this.setVisible(false);
            return;
        }

        this.activeEventLabel.setText(bundle.getString("event.active"));
        this.viewEventInformationButton.setText(bundle.getString("event.view.information"));
        if (!this.eventId.contains("resignation")) {
            this.eventImagePreview.setImage(imageCache.get("image/events/" + eventId + ".jpg"));
        }
        this.setVisible(true);
    }

    public void viewEventInformation() {
        final EventComponent eventComponent = app.initAndRender(eventComponentProvider.get(), Map.of("game", game, "empire", empire, "eventId", eventId, "isSpecialEvent", specialEventIds.contains(eventId), "isResignation", eventId.contains("resignation"), "closedComponentRunnable", closedComponentNotifier, "previousChoice", previousChoice, "effectIndex", effectIndex, "setEffectIndexConsumer", setEffectIndex, "overlapContainer", overlapContainer));
        this.parentContainer.getChildren().removeIf(child -> child instanceof EventComponent);
        this.parentContainer.getChildren().add(eventComponent);
        this.setVisible(false);
    }

    private void eventComponentClosed(ChoiceType choice) {
        this.parentContainer.getChildren().removeIf(child -> child instanceof EventComponent);
        this.onRender();
        this.previousChoice = choice;
    }

    @OnDestroy
    public void destroy() {
        this.subscriber.dispose();
        this.eventService.saveEventData();
        this.app.stage().getScene().getWindow().removeEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseEventHandler);
    }
}
