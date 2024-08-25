package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.component.popups.PauseMenuPopUpComponent;
import de.uniks.stp24.model.AggregateItem;
import de.uniks.stp24.model.AggregateResult;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.rest.GameLogicApiService;
import de.uniks.stp24.service.GameService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.WindowEvent;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.Modals;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "ResourceBar.fxml")
public class ResourceBarComponent extends HBox {
    @FXML
    Label dateLabel;
    @FXML
    ToggleButton slowToggleButton;
    @FXML
    ToggleGroup gameSpeedControls;
    @FXML
    ImageView slowImage;
    @FXML
    ToggleButton mediumToggleButton;
    @FXML
    ImageView mediumImage;
    @FXML
    ToggleButton fastToggleButton;
    @FXML
    ImageView fastImage;
    @FXML
    ToggleButton pauseToggleButton;
    @FXML
    ImageView pauseImage;
    @FXML
    ImageView alloysImage;
    @FXML
    ImageView consumerGoodsImage;
    @FXML
    ImageView populationImage;
    @FXML
    ImageView creditsImage;
    @FXML
    ImageView fuelImage;
    @FXML
    ImageView researchImage;
    @FXML
    ImageView foodImage;
    @FXML
    ImageView mineralsImage;
    @FXML
    ImageView energyImage;
    @FXML
    HBox resourceBarRoot;
    @FXML
    Button escButton;
    @FXML
    Label alloys;
    @FXML
    Label consumer_goods;
    @FXML
    Label population;
    @FXML
    Label credits;
    @FXML
    Label fuel;
    @FXML
    Label research;
    @FXML
    Label food;
    @FXML
    Label minerals;
    @FXML
    Label energy;

    @Inject
    public Provider<PauseMenuPopUpComponent> pauseMenuComponentProvider;
    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public GameEmpiresApiService gameEmpireApiService;
    @Inject
    public GameLogicApiService gameLogicApiService;
    @Inject
    public GameService gameService;
    @Inject
    public ImageCache imageCache;
    @Inject
    public TokenStorage tokenStorage;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("game")
    public Game game;

    @Param("empire")
    Empire empire;

    @Param("windowCloseHandler")
    EventHandler<WindowEvent> windowCloseHandler;

    private AggregateResult aggregates;

    private List<ToggleButton> speedButtons;

    private Map<String, Label> resourceToLabel;

    private boolean isOwner;

    private final Calendar currentCalendar = Calendar.getInstance();

    private ChangeListener<Toggle> gameSpeedControlsListener;

    @Inject
    public ResourceBarComponent() {
    }


    @OnInit
    void init() {
        this.isOwner = tokenStorage.getUserId().equals(game.owner());

        this.currentCalendar.set(500, Calendar.JANUARY, 1);
        this.currentCalendar.add(Calendar.DAY_OF_MONTH, game.period());

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".updated", Game.class),
                event -> {
                    final Game newGame = event.data();
                    final Game oldGame = this.game;

                    this.game = event.data();

                    if (newGame.period() != oldGame.period()) {
                        this.currentCalendar.add(Calendar.DAY_OF_MONTH, 1);
                        this.applyCalenderToLabel();
                    }

                    if (newGame.speed() == oldGame.speed()) {
                        return;
                    }

                    this.setSelectedToggleBasedOnGameSpeed();
                }
        );

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class),
                event -> {
                    if (this.empire.resources().equals(event.data().resources())) {
                        return;
                    }

                    this.empire = event.data();
                    this.fetchAndUpdateAggregates();
                }
        );

        //always take up whole hbox width
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    @OnRender
    void render() {
        this.applyCalenderToLabel();
        this.initializeGameSpeedButtons();
        this.initializeResources();
        this.fetchAndUpdateAggregates();
    }

    private void applyCalenderToLabel() {
        this.dateLabel.setText(this.currentCalendar.get(Calendar.DATE) + "." + (this.currentCalendar.get(Calendar.MONTH) + 1) + "." + this.currentCalendar.get(Calendar.YEAR));
    }

    private void fetchAndUpdateAggregates() {
        subscriber.subscribe(gameLogicApiService.getAggregate(game._id(), empire._id(), "resources.periodic", Map.of()),
                aggregateResult -> {
                    this.aggregates = aggregateResult;
                    this.updateResources();
                }
        );
    }

    private void initializeResources() {
        this.energyImage.setImage(imageCache.get("image/game_resources/energy.png"));
        this.mineralsImage.setImage(imageCache.get("image/game_resources/minerals.png"));
        this.foodImage.setImage(imageCache.get("image/game_resources/food.png"));
        this.fuelImage.setImage(imageCache.get("image/game_resources/fuel.png"));
        this.creditsImage.setImage(imageCache.get("image/game_resources/credits.png"));
        this.populationImage.setImage(imageCache.get("image/game_resources/population.png"));
        this.researchImage.setImage(imageCache.get("image/game_resources/research.png"));
        this.alloysImage.setImage(imageCache.get("image/game_resources/alloys.png"));
        this.consumerGoodsImage.setImage(imageCache.get("image/game_resources/consumer_goods.png"));

        this.resourceToLabel = Map.of(
                "credits", credits,
                "population", population,
                "energy", energy,
                "minerals", minerals,
                "food", food,
                "fuel", fuel,
                "research", research,
                "alloys", alloys,
                "consumer_goods", consumer_goods);
    }

    private void initializeGameSpeedButtons() {
        this.slowImage.setImage(imageCache.get("image/game_speeds/slow.png"));
        this.mediumImage.setImage(imageCache.get("image/game_speeds/medium.png"));
        this.fastImage.setImage(imageCache.get("image/game_speeds/fast.png"));
        this.pauseImage.setImage(imageCache.get("image/game_speeds/pause.png"));
        this.speedButtons = List.of(pauseToggleButton, slowToggleButton, mediumToggleButton, fastToggleButton);
        this.setSelectedToggleBasedOnGameSpeed();

        if (!this.isOwner) {
            for (ToggleButton button : speedButtons) {
                button.setDisable(true);
            }
        }

        this.gameSpeedControls.selectedToggleProperty().addListener(gameSpeedControlsListener = (observable, oldValue, newValue) -> {
            // dont do anything if user presses the same button again
            if (oldValue == null || oldValue.equals(newValue)) {
                return;
            }

            // dont let owner deselect
            if (newValue == null) {
                this.gameSpeedControls.selectToggle(oldValue);
                return;
            }

            //dont send request as non-owner
            if (!this.isOwner) {
                return;
            }

            //set patch request
            subscriber.subscribe(gameService.updateSpeed(game._id(), speedButtons.indexOf((ToggleButton) newValue)), game -> this.game = game);
        });
    }

    private void updateResources() {
        final List<AggregateItem> items = aggregates.items();
        for (final AggregateItem aggregateItem : items) {
            final Label label = resourceToLabel.get(aggregateItem.variable().split("\\.")[1]); // resources.consumer_goods.periodic -> consumer_goods
            final int subTotal = aggregateItem.subtotal();

            String subTotalPrefix = " ";
            if (subTotal >= 0) {
                subTotalPrefix = " +";
            }

            label.setText(empire.resources().get(label.getId()) + subTotalPrefix + subTotal);
        }
    }

    private void setSelectedToggleBasedOnGameSpeed() {
        final int clampedSpeed = Math.clamp(game.speed(), 0, 3);
        final ToggleButton toggleButton = speedButtons.get(clampedSpeed);
        if (toggleButton == null || toggleButton.isSelected()) {
            return;
        }

        this.gameSpeedControls.selectToggle(toggleButton);
    }

    @OnDestroy
    void destroy() {
        subscriber.dispose();
        if (gameSpeedControlsListener != null) {
            gameSpeedControls.selectedToggleProperty().removeListener(gameSpeedControlsListener);
        }
    }

    //esc button press, not key press
    public void onEsc() {
        new Modals(app)
                .modal(pauseMenuComponentProvider.get())
                .params(Map.of("windowCloseHandler", windowCloseHandler))
                .dialog(true)
                .show();
    }
}
