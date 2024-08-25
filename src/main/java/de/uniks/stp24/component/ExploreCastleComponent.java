package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.CreateJobDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.*;
import de.uniks.stp24.service.*;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.Subscription;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static de.uniks.stp24.util.Methods.createLabel;

@Component(view = "ExploreCastle.fxml")
public class ExploreCastleComponent extends HBox {
    @FXML
    SplitPane costBoxToolTipOwner;
    @FXML
    SplitPane exploreTooltipOwner;
    @FXML
    HBox exploreCastleRoot;
    @FXML
    Label errorLabel;
    @FXML
    Label exploreLabel;
    @FXML
    HBox costBox;
    @FXML
    Button exploreButton;

    @Inject
    public App app;
    @Inject
    public Provider<ResourceAmountComponent> costProvider;
    @Inject
    public PresetsService presetsService;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    public GameSystemsApiService gameSystemsApiService;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public NotificationService notificationService;
    @Inject
    public ImageCache imageCache;
    @Inject
    public TokenStorage tokenStorage;
    @Inject
    public PresetsApiService presetsApiService;
    @Inject
    public GameLogicApiService gameLogicApiService;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ExplainedVariableService explainedVariableService;
    @Inject
    public FleetsApiService fleetsApiService;
    @Inject
    public ShipsApiService shipsApiService;

    private final Map<String, String> nextSystemUpgrade = Map.of(
            "unexplored", "explored",
            "explored", "colonized",
            "colonized", "upgraded",
            "upgraded", "developed",
            "developed", "no upgrade left"
    );

    private final Map<String, String> upgradeStateToBundleKey = Map.of(
            "explored", "explore",
            "colonized", "colonize",
            "upgraded", "upgrade",
            "developed", "develop"
    );
    private String currentUpgradeState;
    private String nextUpgradeState;
    private Map<String, Integer> currentResources;
    private SystemUpgrade nextUpgrade;
    private Subscription jobsSubscription;
    private boolean foundShip = false;
    private Fleet lastFleet;
    private final SimpleBooleanProperty hasEnoughResources = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty hasJobInProgress = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty notMine = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty shipInCity = new SimpleBooleanProperty(false);
    private final SimpleStringProperty errorText = new SimpleStringProperty();

    @Param("game")
    Game game;
    @Param("empire")
    Empire empire;
    @Param("system")
    GameSystem system;
    @Param("jobs")
    ObservableList<Job> jobs;

    @Inject
    public ExploreCastleComponent() {
    }

    @OnInit
    public void init() {
        currentUpgradeState = system.upgrade();
        nextUpgradeState = nextSystemUpgrade.get(currentUpgradeState);
        currentResources = empire.resources();
        notMine.set(checkMine());

        this.hasJobInProgress.set(jobs.stream().anyMatch(job -> job.system() != null && job.system().equals(system._id()) && job.type().equals("upgrade") && job.progress() != job.total()));
        this.jobsSubscription = jobs.subscribe(() -> this.hasJobInProgress.set(jobs.stream().anyMatch(job -> job.system() != null && job.system().equals(system._id()) && job.type().equals("upgrade") && job.progress() != job.total())));

        // Event listener to handle system and resource updates
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems." + system._id() + ".updated", GameSystem.class), event -> {
            // Ignore same server responses
            final GameSystem newSystem = event.data();
            if (newSystem.upgrade().equals(this.system.upgrade())) {
                return;
            }

            this.displayNotification();
            this.system = event.data();
            this.currentUpgradeState = system.upgrade();
            this.nextUpgradeState = nextSystemUpgrade.get(currentUpgradeState);
            this.notMine.set(checkMine());
            this.updateUi();
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), event -> {
            // Ignore same server responses
            final Empire newEmpire = event.data();
            if (newEmpire.resources().equals(this.currentResources)) {
                return;
            }

            this.updateOnVariablesEffectingChange(event);
            this.empire = event.data();
            this.currentResources = event.data().resources();
            this.hasEnoughResources.set(true);
            this.checkAffordability();
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".fleets.*.*", Fleet.class), event -> {
            if (event.data().empire().equals(empire._id()) && event.data().location().equals(system._id())) {
                // ignore same server responses
                if (event.data().equals(lastFleet)) {
                    return;
                }
                lastFleet = event.data();
                updateUi();
            }
        });
    }

    private void updateOnVariablesEffectingChange(Event<Empire> event) {
        Empire newEmpire = event.data();
        if (newEmpire.technologies().equals(this.empire.technologies()) && newEmpire.effects().equals(this.empire.effects())) {
            return;
        }

        this.updateUi();
    }

    private void displayNotification() {
        final String successKey = "success.castle." + upgradeStateToBundleKey.get(nextUpgradeState);
        notificationService.displayNotification(bundle.getString(successKey), true);
    }

    @OnRender
    public void render() {
        final BooleanBinding notEnoughResources = hasEnoughResources.not();
        final BooleanBinding waiting = notWaiting.not();
        final BooleanBinding ship = shipInCity.not();

        // Do not allow upgrades if the user can not pay or if they are waiting for a server response
        exploreButton.disableProperty().bind(notEnoughResources.or(waiting).or(notMine).or(hasJobInProgress).or(ship));
        errorLabel.visibleProperty().bind(notEnoughResources.or(ship));
        errorLabel.textProperty().bind(errorText);

        updateUi();
    }

    public void onButtonClick() {
        // Disable button as long as the server is processing the request
        notWaiting.set(false);

        // Update the system to the next state
        CreateJobDto createJobDto = new CreateJobDto(
                this.system._id(), 0, "upgrade", null, null, null, null, null, null
        );

        subscriber.subscribe(jobsApiService.createJob(game._id(), empire._id(), createJobDto),
                Job -> {

                },
                error -> {
                    if (!error.getMessage().equals("closed")) {
                        notWaiting.set(true);
                    }
                }
        );
    }

    private void updateUi() {
        // Clear old information in case this is not the first time the user is looking at the upgrade
        costBox.getChildren().subList(1, costBox.getChildren().size()).clear();
        hasEnoughResources.set(true);
        notWaiting.set(false);

        final String bundleKey = upgradeStateToBundleKey.get(nextUpgradeState);
        if (bundleKey == null) {
            exploreCastleRoot.setVisible(false);
            return;
        }

        checkForFleets();

        updateErrorText();

        exploreCastleRoot.setVisible(true);
        this.showBonuses(this.upgradeStateToBundleKey.get(nextUpgradeState), nextUpgradeState);
    }

    private void checkForFleets() {
        // if unexplored or explored, check if there is the right ship in the city
        if (!currentUpgradeState.equals("unexplored") && !currentUpgradeState.equals("explored")) {
            shipInCity.set(true);
            return;
        }

        String shipType = currentUpgradeState.equals("unexplored") ? "explorer" : "colonizer";

        subscriber.subscribe(fleetsApiService.getFleets(game._id(), empire._id()), fleets -> {
            List<Fleet> possibleFleets = fleets.stream()
                    .filter(fleet -> fleet.location().equals(system._id()) && fleet.size().containsKey(shipType))
                    .toList();

            if (possibleFleets.isEmpty()) {
                shipInCity.set(false);
                return;
            }

            foundShip = false;
            for (Fleet fleet : possibleFleets) {
                subscriber.subscribe(shipsApiService.getShips(game._id(), fleet._id()), ships -> {
                    if (ships.stream().anyMatch(ship -> ship.type().equals(shipType))) {
                        foundShip = true;
                        shipInCity.set(true);
                        updateErrorText();
                    }
                });
                if (foundShip) break;
            }
            shipInCity.set(foundShip);
        });
    }

    private void updateErrorText() {
        if (!shipInCity.get()) {
            if (currentUpgradeState.equals("unexplored")) errorText.set(bundle.getString("error.no.explorer"));
            else errorText.set(bundle.getString("error.no.colonizer"));
        } else errorText.set(bundle.getString("error.not.enough.resources"));
    }

    private void showBonuses(String bundleKey, String nextUpgradeState) {
        exploreLabel.setText(bundle.getString("castle." + bundleKey));
        exploreButton.setText(bundle.getString(bundleKey));
        subscriber.subscribe(presetsService.getCachedPreset("getSystemUpgrades"), upgrades -> {
            final Field nextUpgradeField = upgrades.getClass().getDeclaredField(nextUpgradeState);
            nextUpgradeField.setAccessible(true);
            this.nextUpgrade = (SystemUpgrade) nextUpgradeField.get(upgrades);
            this.fillInformation();
        });
    }

    private void fillInformation() {
        // If the costs were free, show that to the user
        if (nextUpgrade.cost().isEmpty()) {
            costBox.getChildren().subList(1, costBox.getChildren().size()).clear();
            Label freeLabel = new Label(" " + bundle.getString("free"));
            freeLabel.getStyleClass().add("medium");
            costBox.getChildren().add(freeLabel);
        } else {
            this.checkAffordability();
            this.populateCostBox();
        }

        //fill explore button tooltip
        //consumer as request is async -> signalize when the request is done
        final Consumer<VBox> vBoxConsumer = this::onPopGrowthToolTipFinish;
        this.explainedVariableService.buildExplainedVariableToolTip(bundle.getString("castle.population.growth"), empire._id(), "systems", nextSystemUpgrade.get(currentUpgradeState), nextUpgrade, List.of("pop_growth"), vBoxConsumer, false);

        // Now that the costs and bonuses are displayed, the user might be allowed to pay
        notWaiting.set(true);
    }

    private void onPopGrowthToolTipFinish(VBox tooltipRoot) {
        //separator
        tooltipRoot.getChildren().add(createLabel("------", "small-medium"));

        //capacity bonus
        tooltipRoot.getChildren().add(
                createLabel(
                        bundle.getString("castle.capacity.bonus") + ": " + getPercentageBonus(nextUpgrade.capacity_multiplier()),
                        "small-medium"
                )
        );

        //upkeep
        if (!nextUpgrade.upkeep().isEmpty()) {
            tooltipRoot.getChildren().add(createLabel("------", "small-medium"));
            tooltipRoot.getChildren().add(this.explainedVariableService.buildExplainedVariableToolTip(bundle.getString("upkeep"), empire._id(), "systems", nextSystemUpgrade.get(currentUpgradeState), nextUpgrade, List.of("upkeep"), null, false));
        }

        // upgrade time
        tooltipRoot.getChildren().add(createLabel("------", "small-medium"));
        tooltipRoot.getChildren().add(this.explainedVariableService.buildExplainedVariableToolTip(bundle.getString("upgrade_time"), empire._id(), "systems", nextSystemUpgrade.get(currentUpgradeState), nextUpgrade, List.of("upgrade_time"), null, false));

        //job
        tooltipRoot.getChildren().add(createLabel("------", "small-medium"));
        tooltipRoot.getChildren().add(createLabel(bundle.getString("jobs.action.tooltip"), "small-medium"));

        final Tooltip exploreTooltip = new Tooltip();
        exploreTooltip.setGraphic(tooltipRoot);
        exploreTooltip.setShowDelay(Duration.ONE);

        this.exploreTooltipOwner.setTooltip(exploreTooltip);
    }

    private void checkAffordability() {
        if (nextUpgrade == null) {
            return;
        }

        // Check if the user has enough resources to pay for the upgrade
        for (Node child : costBox.getChildren()) {
            if (!(child instanceof ResourceAmountComponent resourceAmountComponent)) {
                continue;
            }

            final String resource = resourceAmountComponent.getResource();
            final int amount = resourceAmountComponent.getAmount();

            if (currentResources.get(resource) == null || currentResources.get(resource) < amount) {
                hasEnoughResources.set(false);
            }
        }
    }

    private void populateCostBox() {
        costBox.getChildren().subList(1, costBox.getChildren().size()).clear();


        final Consumer<List<ExplainedVariable>> explainedVariablesConsumer = (explainedVariables) -> {
            explainedVariables.forEach(explainedVariable -> {
                final String[] variableParts = explainedVariable.variable().split("\\.");
                costBox.getChildren().add(getResourceAmountComponent(variableParts[variableParts.length - 1], (int) explainedVariable.end()));
            });

            this.checkAffordability();
        };

        explainedVariableService.setExplainedVariableToolTip(costBoxToolTipOwner, null, empire._id(), "systems", nextSystemUpgrade.get(currentUpgradeState), nextUpgrade, List.of("cost"), explainedVariablesConsumer);
    }

    private ResourceAmountComponent getResourceAmountComponent(String resource, int amount) {
        return app.initAndRender(costProvider.get(), Map.of("amount", amount, "resource", resource), subscriber);
    }

    private boolean checkMine() {
        return (system.owner() != null) && (!system.owner().equals(empire._id()));
    }

    private String getPercentageBonus(double bonus) {
        String percentage = Math.round((bonus - 1) * 100) + "%";
        return bonus > 0 ? "+" + percentage : percentage;
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
        this.jobsSubscription.unsubscribe();
    }
}
