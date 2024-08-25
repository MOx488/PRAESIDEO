package de.uniks.stp24.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.stp24.App;
import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.component.*;
import de.uniks.stp24.component.buildings.*;
import de.uniks.stp24.component.districts.DistrictBarComponent;
import de.uniks.stp24.component.districts.DistrictComponent;
import de.uniks.stp24.component.districts.DistrictSquareComponent;
import de.uniks.stp24.component.enhancements.*;
import de.uniks.stp24.component.events.EventComponent;
import de.uniks.stp24.component.events.EventEffectComponent;
import de.uniks.stp24.component.events.EventPreviewComponent;
import de.uniks.stp24.component.players.ContactsListComponent;
import de.uniks.stp24.component.players.ContactsViewComponent;
import de.uniks.stp24.component.players.PlayerComponent;
import de.uniks.stp24.component.players.PlayerListComponent;
import de.uniks.stp24.component.popups.PauseMenuPopUpComponent;
import de.uniks.stp24.component.troopview.*;
import de.uniks.stp24.component.war.*;
import de.uniks.stp24.dto.*;
import de.uniks.stp24.model.*;
import de.uniks.stp24.model.troopview.TroopSizeItem;
import de.uniks.stp24.rest.*;
import de.uniks.stp24.service.*;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import mockit.MockUp;
import org.fulib.fx.constructs.forloop.FxFor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.service.query.EmptyNodeQueryException;

import javax.inject.Provider;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.hasText;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IngameControllerTest extends ControllerTest {
    @InjectMocks
    IngameController ingameController;
    @InjectMocks
    ZoomDragComponent zoomDragComponent;
    @Mock
    UsersApiService usersApiService;
    @Mock
    GameMembersApiService gameMembersApiService;
    @Mock
    GameSystemsApiService gameSystemsApiService;
    @Mock
    EventListener eventListener;
    @Spy
    JobService jobService;
    @Mock
    JobsApiService jobsApiService;
    @Mock
    GameEmpiresApiService gameEmpiresApiService;
    @Mock
    GameLogicApiService gameLogicApiService;
    @Mock
    GameService gameService;
    @Mock
    GamesApiService gamesApiService;
    @Spy
    GameTicksService gameTicksService;
    @Mock
    TokenStorage tokenStorage;
    @Mock
    AudioService audioService;
    @Mock
    MapService mapService;
    @Mock
    WarsApiService warsApiService;
    @Spy
    ZoomDragService zoomDragService;
    @Spy
    IngameService ingameService;
    @Spy
    ObjectMapper objectMapper;
    @Spy
    EmpireService empireService;
    @Spy
    BattleResultService battleResultService;
    @Spy
    EmojiService emojiService;
    @Spy
    PresetsService presetsService;
    @Spy
    ExplainedVariableService explainedVariableService;
    @Mock
    PresetsApiService presetsApiService;
    @Spy
    EnhancementService enhancementService;
    @Spy
    EventService eventService;
    @Spy
    NotificationService notificationService;
    @Spy
    EnhancementComponent enhancementComponent;
    @Mock
    ShipsApiService shipsApiService;
    @Mock
    FleetsApiService fleetsApiService;
    @Spy
    ClientChangeService clientChangeService;

    @Spy
    final
    Provider<BattleResult> battleResultProvider = spyProvider(() -> {
        final BattleResult battleResult = new BattleResult();
        battleResult.app = app;
        battleResult.imageCache = imageCache;
        battleResult.bundle = bundle;
        return battleResult;
    });

    @Spy
    final
    Provider<ShipComponent> shipComponentProvider = spyProvider(() -> {
        final ShipComponent shipComponent = new ShipComponent();
        shipComponent.imageCache = imageCache;
        shipComponent.explainedVariableService = explainedVariableService;
        shipComponent.bundle = bundle;
        return shipComponent;
    });
    @Spy
    final
    Provider<BuildFleetComponent> buildFleetComponentProvider = spyProvider(() -> {
        final BuildFleetComponent buildFleetComponent = new BuildFleetComponent();
        buildFleetComponent.app = app;
        buildFleetComponent.subscriber = subscriber;
        buildFleetComponent.shipsApiService = shipsApiService;
        buildFleetComponent.shipComponentProvider = shipComponentProvider;
        buildFleetComponent.fleetsApiService = fleetsApiService;
        buildFleetComponent.imageCache = imageCache;
        buildFleetComponent.presetsService = presetsService;
        buildFleetComponent.eventListener = eventListener;
        buildFleetComponent.bundle = bundle;
        buildFleetComponent.notificationService = notificationService;
        return buildFleetComponent;
    });
    @Spy
    final
    Provider<TaskComponent> taskComponentProvider = spyProvider(() -> {
        final TaskComponent taskComponent = new TaskComponent();
        taskComponent.app = app;
        taskComponent.imageCache = imageCache;
        taskComponent.subscriber = subscriber;
        taskComponent.jobsApiService = jobsApiService;
        taskComponent.jobService = jobService;
        taskComponent.bundle = bundle;
        return taskComponent;
    });

    @Spy
    final
    Provider<TasksViewComponent> tasksViewComponentProvider = spyProvider(() -> {
        final TasksViewComponent tasksViewComponent = new TasksViewComponent();
        tasksViewComponent.app = app;
        tasksViewComponent.subscriber = subscriber;
        tasksViewComponent.imageCache = imageCache;
        tasksViewComponent.eventListener = eventListener;
        tasksViewComponent.jobService = jobService;
        tasksViewComponent.bundle = bundle;
        tasksViewComponent.taskComponentProvider = taskComponentProvider;
        return tasksViewComponent;
    });

    @Spy
    final
    Provider<MarketComponent> marketComponentProvider = spyProvider(() -> {
        final MarketComponent marketComponent = new MarketComponent();
        marketComponent.app = app;
        marketComponent.imageCache = imageCache;
        marketComponent.bundle = bundle;
        marketComponent.subscriber = subscriber;
        marketComponent.eventListener = eventListener;
        marketComponent.presetsService = presetsService;
        marketComponent.gameLogicApiService = gameLogicApiService;
        marketComponent.gameEmpiresApiService = gameEmpiresApiService;
        marketComponent.notificationService = notificationService;
        marketComponent.prefService = prefService;
        return marketComponent;
    });

    @Spy
    final
    Provider<EnhancementBoxComponent> enhancementBoxComponentProvider = spyProvider(() -> {
        final EnhancementBoxComponent enhancementBoxComponentProvider = new EnhancementBoxComponent();
        enhancementBoxComponentProvider.bundle = bundle;

        return enhancementBoxComponentProvider;
    });

    @Spy
    final
    Provider<EnhancementSelectedComponent> enhancementSelectedComponentProvider = spyProvider(() -> {
        final EnhancementSelectedComponent enhancementSelectedComponentProvider = new EnhancementSelectedComponent();
        enhancementSelectedComponentProvider.bundle = bundle;
        enhancementSelectedComponentProvider.imageCache = imageCache;
        enhancementSelectedComponentProvider.jobsApiService = jobsApiService;
        enhancementSelectedComponentProvider.subscriber = subscriber;
        enhancementSelectedComponentProvider.gameLogicApiService = gameLogicApiService;
        enhancementSelectedComponentProvider.app = app;
        enhancementSelectedComponentProvider.eventListener = eventListener;

        return enhancementSelectedComponentProvider;
    });

    @Spy
    final
    Provider<EnhancementListComponent> enhancementListComponentProvider = spyProvider(() -> {
        final EnhancementListComponent enhancementListComponentProvider = new EnhancementListComponent();
        enhancementListComponentProvider.bundle = bundle;
        enhancementListComponentProvider.imageCache = imageCache;
        enhancementListComponentProvider.app = app;
        enhancementListComponentProvider.jobsApiService = jobsApiService;
        enhancementListComponentProvider.subscriber = subscriber;
        enhancementListComponentProvider.eventListener = eventListener;
        enhancementListComponentProvider.enhancementBoxComponentProvider = enhancementBoxComponentProvider;
        enhancementListComponentProvider.enhancementSelectedComponentProvider = enhancementSelectedComponentProvider;

        return enhancementListComponentProvider;
    });

    @Spy
    final
    Provider<EnhancementItemComponent> enhancementItemComponentProvider = spyProvider(() -> {
        final EnhancementItemComponent enhancementItemComponent = new EnhancementItemComponent();
        enhancementItemComponent.app = app;
        enhancementItemComponent.bundle = bundle;
        enhancementItemComponent.imageCache = imageCache;
        enhancementItemComponent.subscriber = subscriber;
        enhancementItemComponent.jobService = jobService;
        enhancementItemComponent.jobsApiService = jobsApiService;
        enhancementItemComponent.eventListener = eventListener;
        enhancementItemComponent.enhancementListComponentProvider = enhancementListComponentProvider;
        enhancementItemComponent.enhancementSelectedComponentProvider = enhancementSelectedComponentProvider;
        return enhancementItemComponent;
    });

    @Spy
    final
    Provider<EnhancementComponent> enhancementComponentProvider = spyProvider(() -> {
        final EnhancementComponent enhancementComponent = new EnhancementComponent();
        enhancementComponent.app = app;
        enhancementComponent.bundle = bundle;
        enhancementComponent.subscriber = subscriber;
        enhancementComponent.jobService = jobService;
        enhancementComponent.eventListener = eventListener;
        enhancementComponent.enhancementService = enhancementService;
        enhancementComponent.enhancementItemComponentProvider = enhancementItemComponentProvider;
        return enhancementComponent;
    });

    @Spy
    final
    Provider<DeclareWarComponent> declareWarComponentProvider = spyProvider(() -> {
        final DeclareWarComponent declareWarComponent = new DeclareWarComponent();
        declareWarComponent.app = app;
        declareWarComponent.subscriber = subscriber;
        declareWarComponent.imageCache = imageCache;
        declareWarComponent.bundle = bundle;
        declareWarComponent.warsApiService = warsApiService;
        return declareWarComponent;
    });

    @Spy
    final
    Provider<PeaceComponent> peaceComponentProvider = spyProvider(() -> {
        final PeaceComponent peaceComponent = new PeaceComponent();
        peaceComponent.app = app;
        peaceComponent.subscriber = subscriber;
        peaceComponent.imageCache = imageCache;
        peaceComponent.declareWarComponentProvider = declareWarComponentProvider;
        return peaceComponent;
    });

    @Spy
    final
    Provider<WarAttacksComponent> warAttacksComponentProvider = spyProvider(() -> {
        final WarAttacksComponent warAttacksComponent = new WarAttacksComponent();
        warAttacksComponent.warsApiService = warsApiService;
        warAttacksComponent.imageCache = imageCache;
        warAttacksComponent.subscriber = subscriber;
        warAttacksComponent.bundle = bundle;
        return warAttacksComponent;
    });

    @Spy
    final
    Provider<WarDefendsComponent> warDefendsComponentProvider = spyProvider(() -> {
        final WarDefendsComponent warDefendsComponent = new WarDefendsComponent();
        warDefendsComponent.imageCache = imageCache;
        warDefendsComponent.bundle = bundle;
        return warDefendsComponent;
    });

    @Spy
    final
    Provider<WarNotificationComponent> warNotificationComponentProvider = spyProvider(() -> {
        final WarNotificationComponent warNotificationComponent = new WarNotificationComponent();
        warNotificationComponent.imageCache = imageCache;
        warNotificationComponent.bundle = bundle;
        return warNotificationComponent;
    });

    @Spy
    final
    Provider<DiplomacyComponent> diplomacyComponentProvider = spyProvider(() -> {
        final DiplomacyComponent diplomacyComponent = new DiplomacyComponent();
        diplomacyComponent.app = app;
        diplomacyComponent.subscriber = subscriber;
        diplomacyComponent.imageCache = imageCache;
        diplomacyComponent.bundle = bundle;
        diplomacyComponent.eventListener = eventListener;
        diplomacyComponent.warsApiService = warsApiService;
        diplomacyComponent.gameEmpiresApiService = gameEmpiresApiService;
        diplomacyComponent.peaceComponentProvider = peaceComponentProvider;
        diplomacyComponent.warAttacksComponentProvider = warAttacksComponentProvider;
        diplomacyComponent.warDefendsComponentProvider = warDefendsComponentProvider;
        diplomacyComponent.warNotificationComponentProvider = warNotificationComponentProvider;
        return diplomacyComponent;
    });

    @Spy
    Provider<SideButtonsComponent> sideButtonsComponentProvider = spyProvider(() -> {
        final SideButtonsComponent sideButtonsComponent = new SideButtonsComponent();
        sideButtonsComponent.app = app;
        sideButtonsComponent.subscriber = subscriber;
        sideButtonsComponent.imageCache = imageCache;
        sideButtonsComponent.gameEmpiresApiService = gameEmpiresApiService;
        sideButtonsComponent.eventListener = eventListener;
        sideButtonsComponent.jobsApiService = jobsApiService;
        sideButtonsComponent.presetsService = presetsService;
        sideButtonsComponent.marketComponentProvider = marketComponentProvider;
        sideButtonsComponent.enhancementComponentProvider = enhancementComponentProvider;
        sideButtonsComponent.tasksViewComponentProvider = tasksViewComponentProvider;
        sideButtonsComponent.buildFleetComponentProvider = buildFleetComponentProvider;
        sideButtonsComponent.diplomacyComponentProvider = diplomacyComponentProvider;
        sideButtonsComponent.bundle = bundle;

        return sideButtonsComponent;
    });
    @Spy
    final
    Provider<CastleNameComponent> castleNameComponentProvider = spyProvider(CastleNameComponent::new);

    @Spy
    Provider<CastleListComponent> castleListComponentProvider = spyProvider(() -> {
        final CastleListComponent castleListComponent = new CastleListComponent();
        castleListComponent.app = app;
        castleListComponent.imageCache = imageCache;
        castleListComponent.bundle = bundle;
        castleListComponent.subscriber = subscriber;
        castleListComponent.eventListener = eventListener;
        castleListComponent.gameMembersApiService = gameMembersApiService;
        castleListComponent.gameSystemsApiService = gameSystemsApiService;
        castleListComponent.tokenStorage = tokenStorage;
        castleListComponent.castleNameComponentProvider = castleNameComponentProvider;
        castleListComponent.castleViewComponentProvider = this.castleViewComponentProvider;
        return castleListComponent;
    });

    @Spy
    final
    Provider<TroopsNameComponent> fleetsNameComponentProvider = spyProvider(TroopsNameComponent::new);
    @Spy
    Provider<TroopsListComponent> troopsListComponentProvider = spyProvider(() -> {
        final TroopsListComponent troopsListComponent = new TroopsListComponent();
        troopsListComponent.app = app;
        troopsListComponent.subscriber = subscriber;
        troopsListComponent.bundle = bundle;
        troopsListComponent.eventListener = eventListener;
        troopsListComponent.fleetsApiService = fleetsApiService;
        troopsListComponent.fleetsNameComponentProvider = this.fleetsNameComponentProvider;
        troopsListComponent.troopViewComponentProvider = this.troopViewComponentProvider;
        return troopsListComponent;
    });

    @Spy
    final
    Provider<PauseMenuPopUpComponent> pauseMenuComponentProvider = spyProvider(() -> {
        final PauseMenuPopUpComponent pauseMenuComponent = new PauseMenuPopUpComponent();
        pauseMenuComponent.modal = new Stage();
        pauseMenuComponent.app = app;
        pauseMenuComponent.bundle = bundle;
        pauseMenuComponent.imageCache = imageCache;
        pauseMenuComponent.audioService = audioService;
        pauseMenuComponent.prefService = prefService;
        return pauseMenuComponent;
    });
    @Spy
    final
    Provider<EventComponent> eventComponentProvidor = spyProvider(() -> {
        final EventComponent eventComponent = new EventComponent();
        eventComponent.app = app;
        eventComponent.subscriber = subscriber;
        eventComponent.imageCache = imageCache;
        eventComponent.eventListener = eventListener;
        eventComponent.bundle = bundle;
        eventComponent.eventService = eventService;
        eventComponent.prefService = prefService;
        eventComponent.enhancementService = enhancementService;
        eventComponent.eventEffectComponentProvider = this.eventEffectComponentProvider;
        return eventComponent;
    });
    @Spy
    final
    Provider<EventEffectComponent> eventEffectComponentProvider = spyProvider(() -> {
        final EventEffectComponent eventEffectComponent = new EventEffectComponent();
        eventEffectComponent.app = app;
        eventEffectComponent.subscriber = subscriber;
        eventEffectComponent.imageCache = imageCache;
        eventEffectComponent.eventListener = eventListener;
        eventEffectComponent.bundle = bundle;
        return eventEffectComponent;
    });
    @Spy
    Provider<EventPreviewComponent> eventPreviewComponentProvider = spyProvider(() -> {
        final EventPreviewComponent eventPreviewComponent = new EventPreviewComponent();
        eventPreviewComponent.app = app;
        eventPreviewComponent.subscriber = subscriber;
        eventPreviewComponent.imageCache = imageCache;
        eventPreviewComponent.eventListener = eventListener;
        eventPreviewComponent.tokenStorage = tokenStorage;
        eventPreviewComponent.bundle = bundle;
        eventPreviewComponent.eventService = eventService;
        eventPreviewComponent.eventComponentProvider = this.eventComponentProvidor;
        eventPreviewComponent.prefService = prefService;
        return eventPreviewComponent;
    });
    @Spy
    Provider<ResourceBarComponent> resourceBarComponentProvider = spyProvider(() -> {
        final ResourceBarComponent resourceBarComponent = new ResourceBarComponent();
        resourceBarComponent.app = app;
        resourceBarComponent.imageCache = imageCache;
        resourceBarComponent.bundle = bundle;
        resourceBarComponent.subscriber = subscriber;
        resourceBarComponent.eventListener = eventListener;
        resourceBarComponent.gameEmpireApiService = gameEmpiresApiService;
        resourceBarComponent.pauseMenuComponentProvider = pauseMenuComponentProvider;
        resourceBarComponent.gameLogicApiService = gameLogicApiService;
        resourceBarComponent.tokenStorage = tokenStorage;
        resourceBarComponent.gameService = gameService;
        return resourceBarComponent;
    });

    @Spy
    final
    Provider<PauseTextComponent> pauseTextComponentProvider = spyProvider(() -> {
        final PauseTextComponent pauseTextComponent = new PauseTextComponent();
        pauseTextComponent.bundle = bundle;
        return pauseTextComponent;
    });

    @Spy
    final
    Provider<PlayerComponent> playerComponentProvider = spyProvider(() -> {
        final PlayerComponent playerComponent = new PlayerComponent();
        playerComponent.imageCache = imageCache;
        return playerComponent;
    });

    @Spy
    final
    Provider<CastleViewComponent> castleViewComponentProvider = spyProvider(() -> {
        final CastleViewComponent castleViewComponent = new CastleViewComponent();
        castleViewComponent.app = app;
        castleViewComponent.subscriber = subscriber;
        castleViewComponent.bundle = bundle;
        castleViewComponent.eventListener = eventListener;
        castleViewComponent.gameSystemsApiService = gameSystemsApiService;
        castleViewComponent.imageCache = imageCache;
        castleViewComponent.discordActivityService = discordActivityService;
        castleViewComponent.districtComponentProvider = this.districtComponentProvider;
        castleViewComponent.buildingsViewComponentProvider = this.buildingsViewComponentProvider;
        castleViewComponent.exploreCastleComponentProvider = this.exploreCastleComponentProvider;
        castleViewComponent.statisticsComponentProvider = this.statsComponentProvider;
        castleViewComponent.jobsApiService = this.jobsApiService;
        return castleViewComponent;
    });

    @Spy
    final
    Provider<TroopSizeComponent> troopSizeComponentProvider = spyProvider(() -> {
        final TroopSizeComponent troopSizeComponent = new TroopSizeComponent();
        troopSizeComponent.imageCache = imageCache;
        troopSizeComponent.bundle = bundle;
        return troopSizeComponent;
    });

    @Spy
    final
    Provider<UnitComponent> unitComponentProvider = spyProvider(() -> {
        final UnitComponent unitComponent = new UnitComponent();
        unitComponent.subscriber = subscriber;
        unitComponent.bundle = bundle;
        return unitComponent;
    });

    @Spy
    final
    Provider<ViewTabComponent> viewTabComponentProvider = spyProvider(() -> {
        final ViewTabComponent viewTabComponent = new ViewTabComponent();
        viewTabComponent.app = app;
        viewTabComponent.troopSizeComponentProvider = troopSizeComponentProvider;
        viewTabComponent.unitComponentProvider = unitComponentProvider;
        viewTabComponent.subscriber = subscriber;
        viewTabComponent.eventListener = eventListener;
        viewTabComponent.shipsApiService = shipsApiService;
        viewTabComponent.explainedVariableService = explainedVariableService;
        viewTabComponent.notificationService = notificationService;
        viewTabComponent.bundle = bundle;
        return viewTabComponent;
    });

    @Spy
    final
    Provider<SizeUpdateComponent> sizeUpdateComponentProvider = spyProvider(() -> {
        final SizeUpdateComponent sizeUpdateComponent = new SizeUpdateComponent();
        sizeUpdateComponent.imageCache = imageCache;
        sizeUpdateComponent.bundle = bundle;
        return sizeUpdateComponent;
    });

    @Spy
    final
    Provider<UpdateTroopTabComponent> updateTroopTabComponentProvider = spyProvider(() -> {
        final UpdateTroopTabComponent updateTroopTabComponent = new UpdateTroopTabComponent();
        updateTroopTabComponent.app = app;
        updateTroopTabComponent.subscriber = subscriber;
        updateTroopTabComponent.eventListener = eventListener;
        updateTroopTabComponent.fleetsApiService = fleetsApiService;
        updateTroopTabComponent.explainedVariableService = explainedVariableService;
        updateTroopTabComponent.notificationService = notificationService;
        updateTroopTabComponent.troopSizeComponentProvider = troopSizeComponentProvider;
        updateTroopTabComponent.sizeUpdateComponentProvider = sizeUpdateComponentProvider;
        updateTroopTabComponent.bundle = bundle;
        return updateTroopTabComponent;
    });

    @Spy
    final
    Provider<TrainUnitsTabComponent> trainUnitsTabComponentProvider = spyProvider(() -> {
        final TrainUnitsTabComponent trainUnitsTabComponent = new TrainUnitsTabComponent();
        trainUnitsTabComponent.imageCache = imageCache;
        trainUnitsTabComponent.app = app;
        trainUnitsTabComponent.subscriber = subscriber;
        trainUnitsTabComponent.eventListener = eventListener;
        trainUnitsTabComponent.troopSizeComponentProvider = troopSizeComponentProvider;
        trainUnitsTabComponent.taskComponentProvider = taskComponentProvider;
        trainUnitsTabComponent.gameSystemsApiService = gameSystemsApiService;
        trainUnitsTabComponent.explainedVariableService = explainedVariableService;
        trainUnitsTabComponent.jobsApiService = jobsApiService;
        trainUnitsTabComponent.gameLogicApiService = gameLogicApiService;
        trainUnitsTabComponent.bundle = bundle;
        return trainUnitsTabComponent;
    });

    @Spy
    final
    Provider<TravelTabComponent> travelTabComponentProvider = spyProvider(() -> {
        final TravelTabComponent travelTabComponent = new TravelTabComponent();
        travelTabComponent.app = app;
        travelTabComponent.jobsApiService = jobsApiService;
        travelTabComponent.subscriber = subscriber;
        travelTabComponent.eventListener = eventListener;
        travelTabComponent.taskComponentProvider = taskComponentProvider;
        travelTabComponent.bundle = bundle;
        return travelTabComponent;
    });

    @Spy
    final
    Provider<TroopComponent> troopComponentProvider = spyProvider(TroopComponent::new);

    @Spy
    final
    Provider<TransferUnitsTabComponent> transferUnitsTabComponentProvider = spyProvider(() -> {
        final TransferUnitsTabComponent transferUnitsTabComponent = new TransferUnitsTabComponent();
        transferUnitsTabComponent.app = app;
        transferUnitsTabComponent.subscriber = subscriber;
        transferUnitsTabComponent.eventListener = eventListener;
        transferUnitsTabComponent.fleetsApiService = fleetsApiService;
        transferUnitsTabComponent.shipsApiService = shipsApiService;
        transferUnitsTabComponent.notificationService = notificationService;
        transferUnitsTabComponent.unitComponentProvider = unitComponentProvider;
        transferUnitsTabComponent.troopComponentProvider = troopComponentProvider;
        transferUnitsTabComponent.troopSizeComponentProvider = troopSizeComponentProvider;
        transferUnitsTabComponent.bundle = bundle;
        return transferUnitsTabComponent;
    });

    @Spy
    final
    Provider<TroopViewComponent> troopViewComponentProvider = spyProvider(() -> {
        final TroopViewComponent troopViewComponent = new TroopViewComponent();
        troopViewComponent.imageCache = imageCache;
        troopViewComponent.subscriber = subscriber;
        troopViewComponent.eventListener = eventListener;
        troopViewComponent.presetsService = presetsService;
        troopViewComponent.fleetsApiService = fleetsApiService;
        troopViewComponent.shipsApiService = shipsApiService;
        troopViewComponent.notificationService = notificationService;
        troopViewComponent.app = app;
        troopViewComponent.viewTabComponentProvider = viewTabComponentProvider;
        troopViewComponent.updateTroopTabComponentProvider = updateTroopTabComponentProvider;
        troopViewComponent.trainUnitsTabComponentProvider = trainUnitsTabComponentProvider;
        troopViewComponent.travelTabComponentProvider = travelTabComponentProvider;
        troopViewComponent.transferUnitsTabComponentProvider = transferUnitsTabComponentProvider;
        troopViewComponent.bundle = bundle;
        return troopViewComponent;
    });

    @Spy
    final
    Provider<FleetMapComponent> fleetMapComponentProvider = spyProvider(() -> {
        final FleetMapComponent fleetMapComponent = new FleetMapComponent();
        fleetMapComponent.app = app;
        fleetMapComponent.subscriber = subscriber;
        fleetMapComponent.imageCache = imageCache;
        fleetMapComponent.bundle = bundle;
        fleetMapComponent.troopViewComponentProvider = troopViewComponentProvider;
        fleetMapComponent.jobsApiService = jobsApiService;
        return fleetMapComponent;
    });

    @Spy
    final
    Provider<CastleComponent> castleComponentProvider = spyProvider(() -> {
        final CastleComponent castleComponent = new CastleComponent();
        castleComponent.imageCache = imageCache;
        castleComponent.app = app;
        castleComponent.bundle = bundle;
        castleComponent.subscriber = subscriber;
        castleComponent.eventListener = eventListener;
        castleComponent.castleViewComponent = castleViewComponentProvider;
        castleComponent.gameEmpiresApiService = gameEmpiresApiService;
        castleComponent.empireService = empireService;
        castleComponent.tokenStorage = tokenStorage;
        castleComponent.fleetMapComponentProvider = fleetMapComponentProvider;
        return castleComponent;
    });

    @Spy
    final
    ResourceBundle buildingBundle = ResourceBundle.getBundle("de/uniks/stp24/lang/building_icons", Locale.ENGLISH);

    @Spy
    final
    Provider<StatisticsComponent> statsComponentProvider = spyProvider(() -> {
        final StatisticsComponent statsComponent = new StatisticsComponent();
        statsComponent.app = app;
        statsComponent.gameEmpiresApiService = gameEmpiresApiService;
        statsComponent.gameSystemsApiService = gameSystemsApiService;
        statsComponent.gameLogicApiService = gameLogicApiService;
        statsComponent.eventListener = eventListener;
        statsComponent.bundle = bundle;
        statsComponent.imageCache = imageCache;
        statsComponent.subscriber = subscriber;
        return statsComponent;
    });

    @Spy
    final
    Provider<ResourceAmountComponent> costProvider = spyProvider(() -> {
        final ResourceAmountComponent resourceAmountComponent = new ResourceAmountComponent();
        resourceAmountComponent.imageCache = imageCache;
        return resourceAmountComponent;
    });

    @Spy
    final
    Provider<ExploreCastleComponent> exploreCastleComponentProvider = spyProvider(() -> {
        final ExploreCastleComponent exploreCastleComponent = new ExploreCastleComponent();
        exploreCastleComponent.app = app;
        exploreCastleComponent.costProvider = costProvider;
        exploreCastleComponent.gameSystemsApiService = gameSystemsApiService;
        exploreCastleComponent.subscriber = subscriber;
        exploreCastleComponent.eventListener = eventListener;
        exploreCastleComponent.imageCache = imageCache;
        exploreCastleComponent.tokenStorage = tokenStorage;
        exploreCastleComponent.bundle = bundle;
        exploreCastleComponent.presetsService = presetsService;
        exploreCastleComponent.jobsApiService = jobsApiService;
        exploreCastleComponent.explainedVariableService = explainedVariableService;
        exploreCastleComponent.notificationService = notificationService;
        exploreCastleComponent.fleetsApiService = fleetsApiService;
        exploreCastleComponent.shipsApiService = shipsApiService;
        return exploreCastleComponent;
    });

    @Spy
    final
    Provider<BuildingPopUpStatComponent> buildingPopUpStatComponentProvider = spyProvider(() -> {
        final BuildingPopUpStatComponent buildingPopUpStatComponent = new BuildingPopUpStatComponent();
        buildingPopUpStatComponent.imageCache = imageCache;

        return buildingPopUpStatComponent;
    });

    @Spy
    final
    Provider<DistrictSquareComponent> districtSquareComponentProvider = spyProvider(DistrictSquareComponent::new);

    @Spy
    final
    Provider<DistrictBarComponent> districtBarComponentProvider = spyProvider(() -> {
        final DistrictBarComponent districtBarComponent = new DistrictBarComponent();
        districtBarComponent.app = app;
        districtBarComponent.districtSquareComponentProvider = districtSquareComponentProvider;
        districtBarComponent.imageCache = imageCache;
        districtBarComponent.bundle = bundle;
        return districtBarComponent;
    });

    @Spy
    final
    Provider<DistrictComponent> districtComponentProvider = spyProvider(() -> {
        final DistrictComponent districtComponent = new DistrictComponent();
        districtComponent.app = app;
        districtComponent.gameEmpiresApiService = gameEmpiresApiService;
        districtComponent.gameSystemsApiService = gameSystemsApiService;
        districtComponent.presetsService = presetsService;
        districtComponent.eventListener = eventListener;
        districtComponent.subscriber = subscriber;
        districtComponent.bundle = bundle;
        districtComponent.buildingPopUpStatComponentProvider = buildingPopUpStatComponentProvider;
        districtComponent.districBarComponentProvider = districtBarComponentProvider;
        districtComponent.explainedVariableService = explainedVariableService;
        return districtComponent;
    });

    @Spy
    final
    Provider<BuildingStatComponent> buildingStatComponentProvider = spyProvider(() -> {
        final BuildingStatComponent buildingStatComponent = new BuildingStatComponent();
        buildingStatComponent.imageCache = imageCache;
        return buildingStatComponent;
    });

    @Spy
    final
    Provider<BuildingStatsViewComponent> buildingStatsViewComponentProvider = spyProvider(() -> {
        final BuildingStatsViewComponent buildingStatsViewComponent = new BuildingStatsViewComponent();
        buildingStatsViewComponent.imageCache = imageCache;
        buildingStatsViewComponent.app = app;
        buildingStatsViewComponent.bundle = bundle;
        buildingStatsViewComponent.buildingBundle = buildingBundle;
        buildingStatsViewComponent.subscriber = subscriber;
        buildingStatsViewComponent.buildingStatComponentProvider = buildingStatComponentProvider;
        return buildingStatsViewComponent;
    });

    @Spy
    final
    Provider<BuildingComponent> buildingComponentProvider = spyProvider(() -> {
        final BuildingComponent buildingComponent = new BuildingComponent();
        buildingComponent.imageCache = imageCache;
        buildingComponent.bundle = bundle;
        buildingComponent.buildingIcons = buildingBundle;
        return buildingComponent;
    });

    @Spy
    final
    Provider<BuildingsViewComponent> buildingsViewComponentProvider = spyProvider(() -> {
        final BuildingsViewComponent buildingsViewComponent = new BuildingsViewComponent();
        buildingsViewComponent.app = app;
        buildingsViewComponent.gameSystemsApiService = gameSystemsApiService;
        buildingsViewComponent.subscriber = subscriber;
        buildingsViewComponent.buildingComponentProvider = buildingComponentProvider;
        buildingsViewComponent.buildingStatsComponent = buildingStatsViewComponentProvider.get();
        buildingsViewComponent.buildingPopUpStatComponentProvider = buildingPopUpStatComponentProvider;
        buildingsViewComponent.imageCache = imageCache;
        buildingsViewComponent.bundle = bundle;
        buildingsViewComponent.presetsService = presetsService;
        buildingsViewComponent.buildingBundle = buildingBundle;
        buildingsViewComponent.eventListener = eventListener;
        buildingsViewComponent.jobsApiService = jobsApiService;
        buildingsViewComponent.explainedVariableService = explainedVariableService;
        return buildingsViewComponent;
    });

    @Spy
    final
    Provider<ContactsListComponent> contactsListComponentProvider = spyProvider(() -> {
        final ContactsListComponent contactsListComponent = new ContactsListComponent();
        contactsListComponent.imageCache = imageCache;
        contactsListComponent.bundle = bundle;
        contactsListComponent.subscriber = subscriber;
        contactsListComponent.app = app;
        contactsListComponent.gameLogicApiService = gameLogicApiService;
        return contactsListComponent;
    });

    @Spy
    final
    Provider<ContactsViewComponent> contactsViewComponentProvider = spyProvider(() -> {
        final ContactsViewComponent contactsViewComponent = new ContactsViewComponent();
        contactsViewComponent.bundle = bundle;
        contactsViewComponent.imageCache = imageCache;
        contactsViewComponent.app = app;
        contactsViewComponent.subscriber = subscriber;
        contactsViewComponent.contactsListComponentProvider = contactsListComponentProvider;
        contactsViewComponent.gameEmpiresApiService = gameEmpiresApiService;
        contactsViewComponent.eventListener = eventListener;
        contactsViewComponent.gameLogicApiService = gameLogicApiService;
        contactsViewComponent.notificationService = notificationService;
        return contactsViewComponent;
    });

    @Spy
    final
    Provider<PlayerListComponent> playerListComponentProvider = spyProvider(() -> {
        final PlayerListComponent playerListComponent = new PlayerListComponent();
        playerListComponent.app = app;
        playerListComponent.subscriber = subscriber;
        playerListComponent.usersApiService = usersApiService;
        playerListComponent.playerComponentProvider = playerComponentProvider;
        playerListComponent.bundle = bundle;
        playerListComponent.imageCache = imageCache;
        playerListComponent.contactsViewComponentProvider = contactsViewComponentProvider;
        playerListComponent.warsApiService = warsApiService;
        playerListComponent.eventListener = eventListener;
        return playerListComponent;
    });

    private Game game;
    private Empire empire;
    private GameSystem system;
    private Fleet troop;
    private final List<GameSystem> systems = createSystems();
    private SystemUpgradesResult systemUpgrades;

    private Building exchange;
    private Building power_plant;
    private Building mine;
    private Building farm;
    private Building research_lab;
    private Building foundry;
    private Building factory;
    private Building refinery;
    private Building shipyard;
    private Building fortress;
    private GameSystem updatedDeleteBuildingSystem;
    private GameSystem updatedBuildBuildingSystem;
    private GameSystem updatedOwnerSystem;
    private GameSystem updatedOwnerSystem2;
    private GameSystem updatedOwnerSystem3;

    private AggregateResult aggregateResult;

    private final Subject<Event<GameSystem>> subjectSystem = BehaviorSubject.create();
    private final Subject<Event<Empire>> subjectEmpire = BehaviorSubject.create();
    private final Subject<Event<Game>> subjectGame = BehaviorSubject.create();
    private final Subject<Event<Job>> subjectJob = BehaviorSubject.create();
    private final Subject<Event<Fleet>> subjectFleet = BehaviorSubject.create();
    private final Subject<Event<War>> subjectWar = BehaviorSubject.create();
    private final Subject<Event<Fleet>> subjectTroop = BehaviorSubject.create();
    private final Subject<Event<Ship>> subjectUnit = BehaviorSubject.create();

    private final Map<String, Object> _private = new HashMap<>();
    private final TreeMap<String, Integer> shipAmounts = new TreeMap<>();

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        List<Job> jobs = createJobs();
        List<Job> jobsDelete = createNonTechJobs();
        this.initParams();
        this.initAllBuildings();
        this.initAllAggregates();
        this.updatedDeleteBuildingSystem = getDeleteBuildingUpdatedSystem();
        this.updatedBuildBuildingSystem = getBuildBuildingUpdateSystem();
        this.updatedOwnerSystem = getUpdatedSystemOwner();
        this.updatedOwnerSystem2 = getUpdatedSystemOwner2();
        this.updatedOwnerSystem3 = getUpdatedSystemOwner3();

        // Declare Data
        Game game = new Game(null, null, "testGameId", "name", "123", 1, 5, false, 1, 1, "", new GameSettings(50));
        EmpireTemplate empire = new EmpireTemplate(null, null, "#0080ff", 1, 1, List.of(), List.of(), _private, null, null);
        Member member = new Member(null, null, game.name(), "123", true, empire);
        CreateJobDto createJobDto = new CreateJobDto(this.system._id(), 0, "building", "farm", null, null, null, null, null);
        TreeMap<String, Integer> resources = new TreeMap<>(Map.of("energy", 10, "minerals", 20, "alloys", 30, "food", 40, "research", 50, "fuel", 60, "credits", 70, "population", 80, "consumer_goods", 90));
        String userId = "user0";
        Fleet fleet = new Fleet("123", "123", "testFleetId", "testGameId", "testEmpireId", "fleet1", "_idDummyOne", shipAmounts, null, null, null);
        List<ShipType> shipTypes = createShipsTypes();
        Fleet secondTroop = new Fleet(null, null, "testTroopId2", "testGameId", "testEmpireId", "2nd Troop", "testSystemId", new TreeMap<>(Map.of("explorer", 1, "colonizer", 1)), null, null, null);
        ExplainedVariableWithMapValues unitCostExplanation = new ExplainedVariableWithMapValues("ships.fighter.cost", new TreeMap<>(Map.of("energy", 10d)), List.of(), new TreeMap<>(Map.of()));

        // Services
        doReturn("-> 20.07.2024").when(jobService).getJobEndDate(any());

        doReturn(userId).when(tokenStorage).getUserId();
        doReturn(Observable.just(system)).when(gameSystemsApiService).getSystem(eq("testGameId"), any());
        doReturn(Observable.just(systems)).when(gameSystemsApiService).getSystems(any(), any());
        doReturn(Observable.just(createMembers())).when(gameMembersApiService).getMembersOfGame("testGameId");
        doReturn(Observable.just(createUsers())).when(usersApiService).getUsersByIDs(anyList());
        doReturn(Observable.just(createUsers())).when(usersApiService).getUsers();
        doReturn(Observable.just(aggregateResult)).when(gameLogicApiService).getAggregate(eq("testGameId"), any(), eq("resources.periodic"), anyMap());
        doReturn(Observable.just(unitCostExplanation)).when(gameLogicApiService).getExplainedVariableWithMapValues(troop.game(), troop.empire(), "ships.fighter.cost");
        doReturn(Observable.just(List.of(exchange, power_plant, mine, farm, research_lab, foundry, factory, refinery, shipyard, fortress))).when(presetsApiService).getBuildings();
        doReturn(Observable.just(createDistricts())).when(presetsApiService).getDistricts();
        doReturn(Observable.just(systemUpgrades)).when(presetsApiService).getSystemUpgrades();
        doReturn(Observable.just(member)).when(gameMembersApiService).getMember(game._id(), userId);
        doReturn(Observable.just(new Empire("123", "123", "testEmpireId", "testGameId", "user0", null, null, "#0080ff", 1, 1, null, List.of(), new TreeMap<>(Map.of("energy", 10, "minerals", 20, "alloys", 30, "food", 40, "research", 50, "fuel", 60, "credits", 70, "population", 80, "consumer_goods", 90)), List.of("society"), new ArrayList<>(), _private, null))).when(gameEmpiresApiService).updateEmpire(any(), any(), any());
        doReturn(Observable.just(jobs)).when(jobsApiService).getJobs(any(), any());
        doReturn(FXCollections.observableArrayList(jobs)).when(jobService).init(any(), any());

        doReturn(Observable.just(createTechnologies())).when(presetsService).getCachedPreset("getTechnologies");
        doReturn(Observable.just(createUnitTypes())).when(presetsService).getCachedPreset("getShips");
        doReturn(Observable.just(createTechJobs())).when(jobsApiService).getFilteredJobs(any(), any(), eq("technology"), any(), any());
        doReturn(Observable.just(createTechJobs())).when(jobsApiService).getFilteredJobs("testGameId", "testEmpireId", null, null, "testSystemId");
        doReturn(Observable.just(List.of())).when(jobsApiService).getFilteredJobs(troop.game(), troop.empire(), "ship", troop._id(), troop.location());
        doReturn(Observable.just(List.of())).when(jobsApiService).getFilteredJobs(troop.game(), troop.empire(), "travel", troop._id(), null);
        doReturn(Observable.just(jobsDelete)).when(jobsApiService).deleteJob(any(), any(), any());
        doReturn(Observable.just(getBuildBuildingJob())).when(jobsApiService).createJob(this.game._id(), this.empire._id(), createJobDto);

        doReturn(Observable.just(shipTypes)).when(presetsApiService).getShips();
        doReturn(Observable.just(fleet)).when(fleetsApiService).createFleet(any(), any());

        doReturn(Observable.just(List.of(new ReadShipDto("", "", "explorer", "testGameId", "testEmpireId", "fleetOne", "explorer", 10, 0, null),
                new ReadShipDto("", "", "colonizer", "testGameId", "testEmpireId", "fleetOne", "colonizer", 10, 0, null))))
                .when(shipsApiService).getShips(any(), any());

        mockMarketApiCall();
        mockFleetsListApiCall();

        doReturn(0.0d).when(prefService).getVolume();
        when(gameService.updateSpeed(eq("testGameId"), anyInt())).thenAnswer(invocation -> {
            final int speed = invocation.getArgument(1);
            return Observable.just(new Game(null, null, "testGameId", "name", "123", 1, 5, true, speed, 1, null, new GameSettings(50)));
        });
        doReturn(Observable.just(mockEmpires())).when(empireService).getReadEmpires("testGameId");
        doReturn(Observable.just(mockEmpires())).when(gameEmpiresApiService).getEmpires("testGameId");
        doReturn(Observable.just(new Empire("123", "123", "testEmpireId", null, userId, null, null, "#0080ff", 1, 1, null, List.of(), resources, List.of("society"), new ArrayList<>(), _private, null))).when(gameEmpiresApiService).getEmpire("testGameId", "testEmpireId");
        doReturn(Observable.just(new Empire("123", "123", "testEmpireId", null, userId, null, null, "#0080ff", 1, 1, null, List.of(), resources, List.of("society"), new ArrayList<>(), _private, null))).when(gameEmpiresApiService).getEmpire(anyString(), anyString());
        doReturn(Observable.just(new AggregateResult(23, createList()))).when(gameLogicApiService).getAggregate("testGameId", "testEmpireId", "resources.periodic", Map.of());
        doReturn(Observable.just(List.of(new ExplainedVariable(
                "districts.city.upkeep.energy",
                50,
                List.of(new EffectSource("mineral_production_1", List.of(new Effect("", 50, 1.1d, 20)))),
                75
        )))).when(gameLogicApiService).getExplainedVariables(any(), any(), any());
        doReturn(Observable.just(new AggregateResult(90, List.of(new AggregateItem("empire.technologies.difficulty", 1, 100), new AggregateItem("technologies.society.cost_multiplier", 1, -10))))).when(gameLogicApiService).getAggregateTech("testEmpireId", "technology.cost", "demographic");
        doReturn(Observable.just(new AggregateResult(90, List.of(new AggregateItem("empire.technologies.difficulty", 1, 100), new AggregateItem("technologies.society.cost_multiplier", 1, -10))))).when(gameLogicApiService).getAggregateTech("testEmpireId", "technology.cost", "more_colonists_1");
        doReturn(Observable.just(new AggregateResult(100, List.of(new AggregateItem("empire.technologies.difficulty", 1, 100), new AggregateItem("technologies.research_site_production_1.cost_multiplier", 1, 0))))).when(gameLogicApiService).getAggregateTech("testEmpireId", "technology.cost", "research_site_production_1");
        doReturn(Observable.just(new ExplainedVariable(
                "technologies.society.cost_multiplier",
                1.0,
                List.of(new EffectSource("society", List.of(new Effect("technologies.society.cost_multiplier", 0, 0.95, 0)))),
                0.95
        ))).when(gameLogicApiService).getExplainedVariable("testGameId", "testEmpireId", "technologies.society.cost_multiplier");

        when(gameService.updateGame(eq("testGameId"), any(UpdateGameDto.class))).thenAnswer(invocation -> {
            final UpdateGameDto updateGameDto = invocation.getArgument(1);
            return Observable.just(new Game(null, null, "testGameId", "name", "123", 1, 5, true, updateGameDto.speed(), 1, null, new GameSettings(50)));
        });
        doReturn(Observable.just(new AggregateResult(100, List.of()))).when(gameLogicApiService).getAggregateSystem("testGameId", "testEmpireId", "system.max_health", "testSystemId");
        doReturn(Observable.just(new AggregateResult(100, List.of()))).when(gameLogicApiService).getAggregateSystem("testGameId", "testEmpireId", "system.defense", "testSystemId");


        doReturn(Observable.just(getWars())).when(warsApiService).getWars("testGameId", "testEmpireId");

        doReturn(Observable.just(new AggregateResultCompare(0, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.military", "testEmpireId");
        doReturn(Observable.just(new AggregateResultCompare(0, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.economy", "testEmpireId");
        doReturn(Observable.just(new AggregateResultCompare(0, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.technology", "testEmpireId");

        doReturn(Observable.just(new AggregateResultCompare(-0.3, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.military", "empire1");
        doReturn(Observable.just(new AggregateResultCompare(-5, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.economy", "empire1");
        doReturn(Observable.just(new AggregateResultCompare(5, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.technology", "empire1");

        doReturn(Observable.just(new AggregateResultCompare(0.3, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.military", "empire2");
        doReturn(Observable.just(new AggregateResultCompare(0.3, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.economy", "empire2");
        doReturn(Observable.just(new AggregateResultCompare(5, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.technology", "empire2");

        doReturn(Observable.just(new AggregateResultCompare(0.3, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.military", "testEmpireId2");
        doReturn(Observable.just(new AggregateResultCompare(0.3, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.economy", "testEmpireId2");
        doReturn(Observable.just(new AggregateResultCompare(5, List.of()))).when(gameLogicApiService).getAggregateCompare("testGameId", "testEmpireId", "empire.compare.technology", "testEmpireId2");

        //Fleets
        doReturn(Observable.just(shipTypes)).when(presetsApiService).getShips();
        doReturn(Observable.just(fleet)).when(fleetsApiService).createFleet(any(), any());


        doReturn(Observable.just(getUnits())).when(shipsApiService).getShips(troop.game(), troop._id());
        doReturn(Observable.just(List.of(new ReadShipDto("", "", "explorer", "testGameId", "testEmpireId", "testTroopId2", "explorer", 10, 0, null),
                new ReadShipDto("", "", "colonizer", "testGameId", "testEmpireId", "testTroopId2", "colonizer", 10, 0, null))))
                .when(shipsApiService).getShips(troop.game(), "testTroopId2");
        doReturn(Observable.just(getUnits().getFirst())).when(shipsApiService).deleteShip(troop.game(), troop._id(), "testUnitId");
        doReturn(Observable.just(new Ship(null, null, "testUnitId", "testGameId", "testEmpireId", "testTroopId2", "explorer", 100, 0, null, null))).when(shipsApiService).updateShip(eq(troop.game()), eq(troop._id()), eq("testUnitId"), any());
        doReturn(Observable.just(new Ship(null, null, "testUnitId2", "testGameId", "testEmpireId", "testTroopId2", "fighter", 100, 0, null, null))).when(shipsApiService).updateShip(eq(troop.game()), eq(troop._id()), eq("testUnitId2"), any());

        doReturn(Observable.just(troop)).when(fleetsApiService).deleteFleet(troop.game(), troop._id());
        doReturn(Observable.just(List.of(troop, secondTroop))).when(fleetsApiService).getFleets(troop.game(), troop.empire());

        //Web Sockets

        //Systems
        doReturn(subjectSystem).when(eventListener).listen("games.testGameId.systems.*.updated", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.testGameId.systems.*.*", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.testGameId.systems._idDummyOne.updated", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.testGameId.systems._idDummyTwo.updated", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.testGameId.systems._idDummyThree.updated", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.testGameId.systems.testSystemId.updated", GameSystem.class);

        //Empire
        doReturn(subjectEmpire).when(eventListener).listen("games.testGameId.empires.testEmpireId.updated", Empire.class);
        doReturn(subjectEmpire).when(eventListener).listen("games.testGameId.empires.testEmpireId.updated", Empire.class);
        doReturn(subjectEmpire).when(eventListener).listen("games.testGameId.empires.*.updated", Empire.class);

        //Game
        doReturn(subjectGame).when(eventListener).listen("games.testGameId.updated", Game.class);
        doReturn(subjectGame).when(eventListener).listen("games.testGameId.*", Game.class);

        //Jobs
        doReturn(subjectJob).when(eventListener).listen("games.testGameId.empires.testEmpireId.jobs.*.*", Job.class);
        doReturn(subjectJob).when(jobService).listenForJobEvent(any());

        //Wars
        doReturn(this.subjectWar).when(eventListener).listen("games.testGameId.wars.*.*", War.class);

        //Fleet
        doReturn(this.subjectFleet).when(eventListener).listen("games.testGameId.fleets.*.*", Fleet.class);

        //Troops
        doReturn(subjectTroop).when(eventListener).listen("games.testGameId.fleets.testTroopId.*", Fleet.class);
        doReturn(subjectTroop).when(eventListener).listen("games.testGameId.fleets.*.*", Fleet.class);
        doReturn(subjectTroop).when(eventListener).listen("games.testGameId.fleets.testTroopId.updated", Fleet.class);

        //Units
        doReturn(subjectUnit).when(eventListener).listen("games.testGameId.fleets.testTroopId.ships.*.*", Ship.class);
        doReturn(subjectUnit).when(eventListener).listen("games.testGameId.fleets.*.ships.*.*", Ship.class);

        //Fleet Map
        Fleet fleet1 = new Fleet("", "", "fleet1ID", "OwnGameId", "TestUser", "X Wing", "_idDummyOne", new TreeMap<>(), new HashMap<>(), new HashMap<>(), new ArrayList<>());
        Fleet fleet2 = new Fleet("", "", "fleet2ID", "OwnGameId", "TestUser", "T-Fighter", "_idDummyOne", new TreeMap<>(), new HashMap<>(), new HashMap<>(), new ArrayList<>());
        List<Fleet> fleets = List.of(fleet1, fleet2);

        doReturn(Observable.just(fleets)).when(fleetsApiService).getFleets(any());
        doReturn(subjectFleet).when(eventListener).listen("games.OwnGameId.fleets.*.*", Fleet.class);

        jobService.gameEmpiresApiService = gameEmpiresApiService;
        jobService.subscriber = subscriber;
        jobService.eventListener = eventListener;
        jobService.jobsApiService = jobsApiService;

        zoomDragComponent.imageCache = imageCache;
        zoomDragComponent.zoomDragService = zoomDragService;

        eventService.objectMapper = objectMapper;
        eventService.gameEmpiresApiService = gameEmpiresApiService;
        eventService.subscriber = subscriber;
        eventService.eventListener = eventListener;
        eventService.prefService = prefService;

        emojiService.subscriber = subscriber;
        emojiService.eventListener = eventListener;
        emojiService.imageCache = imageCache;
        emojiService.bundle = bundle;
        emojiService.usersApiService = usersApiService;
        emojiService.gameEmpiresApiService = gameEmpiresApiService;
        emojiService.app = app;
        emojiService.notificationService = notificationService;

        clientChangeService.subscriber = subscriber;
        clientChangeService.gameEmpiresApiService = gameEmpiresApiService;
        clientChangeService.eventListener = eventListener;
        clientChangeService.prefService = prefService;
        clientChangeService.objectMapper = objectMapper;

        enhancementService.subscriber = subscriber;
        enhancementService.gameEmpiresApiService = gameEmpiresApiService;

        enhancementComponent.jobService = jobService;

        ingameService.objectMapper = objectMapper;
        ingameService.gameSystemsApiService = gameSystemsApiService;

        empireService.gameEmpiresApiService = gameEmpiresApiService;

        presetsService.presetsApiService = presetsApiService;

        notificationService.imageCache = imageCache;
        notificationService.app = app;

        explainedVariableService.subscriber = subscriber;
        explainedVariableService.bundle = bundle;
        explainedVariableService.gameLogicApiService = gameLogicApiService;
        explainedVariableService.buildingPopUpStatComponentProvider = buildingPopUpStatComponentProvider;
        explainedVariableService.app = app;

        gameTicksService.gameService = gameService;
        gameTicksService.app = app;
        gameTicksService.subscriber = subscriber;
        gameTicksService.eventListener = eventListener;
        gameTicksService.eventService = eventService;
        gameTicksService.pauseTextComponentProvider = pauseTextComponentProvider;

        ingameController.playerListComponentProvider = playerListComponentProvider;
        ingameController.ingameService = ingameService;
        ingameController.empireService = empireService;
        ingameController.eventListener = eventListener;
        ingameController.emojiService = emojiService;
        ingameController.castleComponentProvider = castleComponentProvider;
        ingameController.pauseMenuComponentProvider = pauseMenuComponentProvider;
        ingameController.battleResultComponentProvider = battleResultProvider;
        ingameController.zoomDragComponent = zoomDragComponent;
        ingameController.fxFor = new FxFor(app);

        app.show(ingameController, Map.of("game", game));
    }

    // ----- Post Battle tests -----------------------------------------------------------------------------------------

    @Test
    void lostBattle() {

        waitForFxEvents();
        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "ingameView". His troop is in a battle.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        StackPane battleResult = lookup("#battleResult").query();
        assertFalse(battleResult.isVisible());

        waitForFxEvents();
        // Action:
        // Jan's troop lost the battle and he has no troops left. His castle has a new owner now.
        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyOne.updated", updatedOwnerSystem));

        waitForFxEvents();
        // Result:
        // A notification with the text "You lost the battle." appears. The "battleResult" is visible now.
        assertTrue(battleResult.isVisible());
        Text battleText = lookup("#battleText").query();
        assertEquals("You lost a battle against", battleText.getText());

    }

    @Test
    void lostBattleClickOk() {
        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyOne.updated", updatedOwnerSystem));
        waitForFxEvents();
        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "ingameView". His troop lost in a battle.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        StackPane battleResult = lookup("#battleResult").query();
        Button lobbyButton = lookup("#lobbyButton").query();
        assertEquals("OK", lobbyButton.getText());

        waitForFxEvents();
        // Action:
        // Jan's troop lost the battle and he has no troops left. His castle has a new owner now.
        // Jan clicks on Ok
        clickOn("#lobbyButton");

        waitForFxEvents();
        // Result:
        // A notification with the text "You lost the battle." appears. The "battleResult" is visible now.
        assertFalse(battleResult.isVisible());
    }

    @Test
    void wonBattle() {
        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyOne.updated", updatedOwnerSystem));
        waitForFxEvents();
        clickOn("#lobbyButton");
        waitForFxEvents();

        GameSystem newSystem = new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyOne",
                "testGameId",
                "typeDummyValue",
                "Bellevue",
                100.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                List.of("shipyard"),
                "upgradeDummyValue",
                1000,
                Map.of(
                        "_idDummyTwo", 6.5d,
                        "_idDummyThree", 8.5d
                ),
                10,
                10,
                "testEmpireId",
                new HashMap<>()
        );
        waitForFxEvents();
        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "ingameView". His troop is in a battle.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        StackPane battleResult = lookup("#battleResult").query();
        assertFalse(battleResult.isVisible());

        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyTwo.updated", updatedOwnerSystem2));
        waitForFxEvents();
        clickOn("#lobbyButton");


        waitForFxEvents();
        //Action
        //Jan's troop won the battle, and he is the new owner of the castle.
        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyOne.updated", newSystem));
        waitForFxEvents();

        //Result
        //A notification with the text "You won the battle." appears. The "battleResult" is visible now.
        assertTrue(battleResult.isVisible());
        Text battleText = lookup("#battleText").query();
        assertEquals("You won a battle against", battleText.getText());

    }

    @Test
    void lostGame() {
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        StackPane battleResult = lookup("#battleResult").query();
        assertFalse(battleResult.isVisible());
        //Start
        //Jan loses his first battle and his castle is taken over by another player.
        waitForFxEvents();
        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyOne.updated", updatedOwnerSystem));
        waitForFxEvents();
        clickOn("#lobbyButton");
        //Jan loses his second battle and his castle is taken over by another player.
        waitForFxEvents();
        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyTwo.updated", updatedOwnerSystem2));
        waitForFxEvents();
        clickOn("#lobbyButton");
        assertFalse(battleResult.isVisible());
        //Action
        //Jan loses his third battle and his castle is taken over by another player. He loses the game.
        waitForFxEvents();
        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyThree.updated", updatedOwnerSystem3));
        waitForFxEvents();

        waitForFxEvents();
        // Result:
        // A notification with the text "You won the battle." appears. The "battleResult" is visible now.
        assertTrue(battleResult.isVisible());
        Text battleText = lookup("#battleText").query();
        assertEquals("You have lost your empire", battleText.getText());
    }

    @Test
    void wonGame() {
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        StackPane battleResult = lookup("#battleResult").query();
        assertFalse(battleResult.isVisible());
        //Start
        //Jan loses his first battle and his castle is taken over by another player.
        waitForFxEvents();
        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyOne.updated", updatedOwnerSystem));
        waitForFxEvents();
        clickOn("#lobbyButton");

        GameSystem newSystem = new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyOne",
                "testGameId",
                "typeDummyValue",
                "Bellevue",
                100.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                List.of("shipyard"),
                "upgradeDummyValue",
                1000,
                Map.of(
                        "_idDummyTwo", 6.5d,
                        "_idDummyThree", 8.5d
                ),
                10,
                10,
                "testEmpireId",
                new HashMap<>()
        );

        //Action
        //Jan wins his second battle and his castle is taken over by another player.
        subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyOne.updated", newSystem));
        waitForFxEvents();

        //Result
        //A notification with the text "You won the battle." appears. The "battleResult" is visible now.
        assertTrue(battleResult.isVisible());
        Text battleText = lookup("#battleText").query();
        assertEquals("You won the game", battleText.getText());
    }

    // ----- Troop view tests ------------------------------------------------------------------------------------------
    @Test
    void locationOwned() {
        showTroopView();

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "View" tab of the troop view.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());

        // Result:
        // Jan owns this location, so a happy face is displayed next to the name of the location.
        Image ownerImage = new Image(Objects.requireNonNull(App.class.getResource("image/icons/face-smile.png")).toString());
        assertEquals(ownerImage.getUrl(), ((ImageView) lookup("#ownerImage").query()).getImage().getUrl());
    }

    @Test
    void locationNotOwned() {
        // change owner of location
        GameSystem location = systems.getFirst();
        location.setOwner("otherEmpireId");
        showTroopView();

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "View" tab of the troop view.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());

        // Result:
        // Jan owns this location, so a happy face is displayed next to the name of the location.
        Image ownerImage = new Image(Objects.requireNonNull(App.class.getResource("image/icons/face-frown.png")).toString());
        assertEquals(ownerImage.getUrl(), ((ImageView) lookup("#ownerImage").query()).getImage().getUrl());
    }

    @Test
    void destroyTroop() {
        showTroopView();

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "View" tab of the troop view.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        verifyThat("#destroyTroopButton", NodeMatchers.isVisible());

        // Action:
        // Jan wants to destroy the troop, so he clicks on "Destroy Troop".
        clickOn("Destroy Troop");
        waitForFxEvents();

        // Result:
        // The troop view closes. A notification with the text "Troop was destroyed." appears.
        assertThrows(EmptyNodeQueryException.class, () -> lookup("#troopViewRoot").query());
        Label notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("Troop was destroyed."));
    }

    @Test
    void destroyUnit() {
        showTroopView();

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "View" tab of the troop view. His troop has two units in it.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        verifyThat("#destroyTroopButton", NodeMatchers.isVisible());
        assertEquals(2, lookup("#unitsListView").queryAs(ListView.class).getItems().size());
        ListView<ReadShipDto> listView = lookup("#unitsListView").query();
        ReadShipDto explorer = listView.getItems().getFirst();

        // Action:
        // Jan wants to destroy his "Explorer" unit, so he clicks on it and then clicks on "Destroy Unit".
        clickOn(lookup("#unitsListView .list-cell").queryAs(ListCell.class));
        waitForFxEvents();
        clickOn("- Destroy Unit");
        waitForFxEvents();
        subjectUnit.onNext(new Event<>("games.testGameId.fleets.*.ships.*.deleted", getUnits().getFirst().toShip()));
        waitForFxEvents();

        // Result:
        // The unit is removed from the list of units. The "actual" value of the "explorer" unit is now 0.
        assertEquals(1, lookup("#unitsListView").queryAs(ListView.class).getItems().size());
        TroopSizeItem explorerSize = (TroopSizeItem) lookup("#sizeListView").queryAs(ListView.class).getItems().getFirst();
        assertEquals(0, explorerSize.actual());
        assertFalse(listView.getItems().contains(explorer));
    }

    @Test
    void trainUnit() {
        CreateJobDto createJobDto = new CreateJobDto("_idDummyOne", 0, "ship", null, null, null, "testTroopId", "fighter", null);
        Job job = new Job(null, null, "", 0, 100, "testGameId", "testEmpireId", "_idDummyOne", 0, "ship", null, null, null, "testTroopId", "fighter", null, new TreeMap<>(Map.of("alloys", 75, "energy", 50)), null);
        doReturn(Observable.just(job)).when(jobsApiService).createJob(this.game._id(), this.empire._id(), createJobDto);

        showTroopView();
        clickOn("#trainUnitsTab");

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Train Units" tab of the troop view. He can train two types
        // of units. He is currently not training any units. The location of the troop has a barracks building.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        assertEquals(2, lookup("#chooseUnitsListView").queryAs(ListView.class).getItems().size());
        ListView<Job> tasksListView = lookup("#unitTasksListView").query();
        assertEquals(0, tasksListView.getItems().size());
        verifyThat("#shipyardAmountLabel", hasText("1"));

        // Action:
        // Jan wants to train a "Fighter" unit, so he clicks on it and then clicks on "Train Unit".
        clickOn(lookup("#chooseUnitsListView .list-cell").nth(1).queryAs(ListCell.class));
        waitForFxEvents();
        clickOn("+ Train Unit");
        waitForFxEvents();
        subjectJob.onNext(new Event<>("games.testGameId.empires.testEmpireId.jobs.*.created", job));
        waitForFxEvents();

        // Result:
        // A new "Fighter" unit task is created.
        assertEquals(1, tasksListView.getItems().size());
        assertEquals("ship", tasksListView.getItems().getFirst().type());
        assertEquals("fighter", tasksListView.getItems().getFirst().ship());
    }

    @Test
    void trainUnitWithoutBarracks() {
        // delete barracks
        GameSystem system = systems.getFirst();
        List<String> buildings = new ArrayList<>(system.buildings());
        buildings.removeIf(building -> building.equals("shipyard"));
        system.setBuildings(buildings);

        showTroopView();
        clickOn("#trainUnitsTab");

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Train Units" tab of the troop view. The location of his
        // troop has no barracks building. Jan is not training any units yet.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        verifyThat("#shipyardAmountLabel", hasText("0"));
        ListView<Job> tasksListView = lookup("#unitTasksListView").query();
        assertEquals(0, tasksListView.getItems().size());

        // Action:
        // Jan wants to train a "Fighter" unit, so he clicks on it and then clicks on "Train Unit".
        clickOn(lookup("#chooseUnitsListView .list-cell").nth(1).queryAs(ListCell.class));
        waitForFxEvents();
        clickOn("+ Train Unit");
        waitForFxEvents();

        // Result:
        // Nothing happens, because the "Train Unit" button is disabled, since the location does not have a barracks
        // building.
        assertTrue(lookup("#trainUnitButton").query().isDisabled());
        assertEquals(0, tasksListView.getItems().size());
    }

    @Test
    void trainUnitLocationNotOwned() {
        // change owner of location
        GameSystem location = systems.getFirst();
        location.setOwner("otherEmpireId");

        showTroopView();
        clickOn("#trainUnitsTab");

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Train Units" tab of the troop view. Jan does not own the
        // location of the troop. Jan is not training any units yet.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        Image ownerImage = new Image(Objects.requireNonNull(App.class.getResource("image/icons/face-frown.png")).toString());
        assertEquals(ownerImage.getUrl(), ((ImageView) lookup("#ownerImage").query()).getImage().getUrl());
        ListView<Job> tasksListView = lookup("#unitTasksListView").query();
        assertEquals(0, tasksListView.getItems().size());

        // Action:
        // Jan wants to train a "Fighter" unit, so he clicks on it and then clicks on "Train Unit".
        clickOn(lookup("#chooseUnitsListView .list-cell").nth(1).queryAs(ListCell.class));
        waitForFxEvents();
        clickOn("+ Train Unit");
        waitForFxEvents();

        // Result:
        // Nothing happens, because the "Train Unit" button is disabled, since Jan does not own the location of the
        // troop.
        assertTrue(lookup("#trainUnitButton").query().isDisabled());
        assertEquals(0, tasksListView.getItems().size());
    }

    @Test
    void updateTroop() {
        showTroopView();
        clickOn("#updateTroopTab");

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Update Troop" tab of the troop view. The name of his troop
        // is "My Troop".
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        verifyThat("#troopNameLabel", hasText("My Troop"));

        // Action:
        // Jan wants to update the name of his troop, so he changes the name and clicks on "Update Troop".
        Fleet newTroop = new Fleet(troop.createdAt(), troop.updatedAt(), troop._id(), troop.game(), troop.empire(), "New Troop Name", troop.location(), troop.size(), troop._private(), troop._public(), troop.effects());
        doReturn(Observable.just(newTroop)).when(fleetsApiService).updateFleet(eq(troop.game()), eq(troop._id()), any());

        clickOn("#troopNameTextField").write("New Troop Name");
        clickOn("#updateTroopButton");

        subjectTroop.onNext(new Event<>("games.testGameId.fleets.*.updated", newTroop));

        waitForFxEvents();

        // Result:
        // The name of the troop is updated to "New Troop Name". A success notification shows up.
        verifyThat("#troopNameLabel", hasText("New Troop Name"));
        assertDoesNotThrow(() -> lookup("#notificationImage").query());
        Label notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("Troop was updated."));

        // Action:
        // Jan wants to change the "planned" values of his troop. He wants to decrease the "planned" value of the
        // "explorer" unit by 2 and increase the "planned" value of the "interceptor" by 1.
        newTroop = new Fleet(troop.createdAt(), troop.updatedAt(), troop._id(), troop.game(), troop.empire(), "New Troop Name", troop.location(), new TreeMap<>(Map.of("explorer", 0, "fighter", 3, "interceptor", 1)), troop._private(), troop._public(), troop.effects());
        doReturn(Observable.just(newTroop)).when(fleetsApiService).updateFleet(eq(troop.game()), eq(troop._id()), any());

        assertEquals(2, lookup("#updateSizeListView").queryAs(ListView.class).getItems().size());
        TroopSizeItem explorerSize = (TroopSizeItem) lookup("#updateSizeListView").queryAs(ListView.class).getItems().getFirst();
        assertEquals(2, explorerSize.planned());
        clickOn(lookup("#decreaseButton").nth(0).queryAs(Button.class));
        clickOn(lookup("#decreaseButton").nth(0).queryAs(Button.class));
        clickOn(lookup("#increaseButton").nth(2).queryAs(Button.class));

        waitForFxEvents();

        clickOn("#updateTroopButton");

        waitForFxEvents();

        subjectTroop.onNext(new Event<>("games.testGameId.fleets.*.updated", newTroop));

        waitForFxEvents();

        // Result:
        // The "planned" value of the "explorer" unit is now 0 and the "interceptor" unit now appears in the list. Its
        // "planned" value is 1. A success notification shows up.
        assertEquals(3, lookup("#updateSizeListView").queryAs(ListView.class).getItems().size());
        explorerSize = (TroopSizeItem) lookup("#updateSizeListView").queryAs(ListView.class).getItems().get(0);
        TroopSizeItem interceptorSize = (TroopSizeItem) lookup("#updateSizeListView").queryAs(ListView.class).getItems().get(2);
        assertEquals(0, explorerSize.planned());
        assertEquals(1, interceptorSize.planned());
        assertEquals("interceptor", interceptorSize.type());

        assertDoesNotThrow(() -> lookup("#notificationImage").query());
        notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("Troop was updated."));
    }

    @Test
    void noTravelTask() {
        showTroopView();
        clickOn("#travelTab");

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Travel" tab of the troop view.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());

        // Result:
        // Jan has no travel task, so he sees a message that says "Troop is stationary".
        Label travelTaskLabel = (Label) lookup("#travelTaskBox").queryAs(VBox.class).getChildren().getFirst();
        verifyThat(travelTaskLabel, hasText("Troop is stationary"));
    }

    @Test
    void travelTask() {
        Job job = new Job(null, null, null, 0, 100, "testGameId", "testEmpireId", null, 0, "travel", null, null, null, "testTroopId", null, List.of("_idDummyOne", "_idDummyTwo"), new TreeMap<>(Map.of()), null);
        doReturn(Observable.just(List.of(job))).when(jobsApiService).getFilteredJobs(troop.game(), troop.empire(), "travel", troop._id(), null);

        showTroopView();
        clickOn("#travelTab");

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Travel" tab of the troop view.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());

        // Result:
        // Jan has a travel task, so he sees the details of the task.
        verifyThat("#travelTaskBox #taskName", hasText("Rotenburg"));
        verifyThat("#travelTaskBox #jobDetailsBox", NodeMatchers.isInvisible());
        verifyThat("#travelTaskBox #cancelTooltip", NodeMatchers.isInvisible());
    }

    @Test
    void transferUnit() {
        showTroopView();
        clickOn("#transferUnitsTab");

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Transfer Units" tab of the troop view. He currently has
        // two units in his troop.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        assertEquals(2, lookup("#yourUnitsListView").queryAs(ListView.class).getItems().size());

        // Action:
        // Jan wants to move his "Fighter" unit to his second troop, "2nd Troop". He clicks on the "Fighter" unit and
        // then on "2nd Troop". He then clicks on "Transfer Unit".
        ReadShipDto fighter = (ReadShipDto) lookup("#yourUnitsListView").queryAs(ListView.class).getItems().get(1);

        clickOn(lookup("#yourUnitsListView .list-cell").nth(1).queryAs(ListCell.class));
        clickOn(lookup("#troopsListView .list-cell").queryAs(ListCell.class));
        waitForFxEvents();
        assertEquals(1, lookup("#focusedTroopSizeListView").queryAs(ListView.class).getItems().size());
        clickOn("#transferUnitButton");

        Ship updatedFighter = new Ship(null, null, "testUnitId2", "testGameId", "testEmpireId", "testTroopId2", "fighter", 100, 0, null, null);
        subjectUnit.onNext(new Event<>("games.testGameId.fleets.*.ships.*.updated", updatedFighter));

        waitForFxEvents();

        // Result:
        // The fighter unit is removed from the list of units in the "Transfer Units" tab. The "2nd Troop" now has the
        // "Fighter" unit. A success notification shows up.
        assertFalse(lookup("#yourUnitsListView").queryAs(ListView.class).getItems().contains(fighter));
        assertEquals(1, lookup("#yourUnitsListView").queryAs(ListView.class).getItems().size());
        assertEquals(2, lookup("#focusedTroopSizeListView").queryAs(ListView.class).getItems().size());
        TroopSizeItem fighterSize = (TroopSizeItem) lookup("#focusedTroopSizeListView").queryAs(ListView.class).getItems().get(1);
        assertEquals("fighter", fighterSize.type());
        assertEquals(1, fighterSize.actual());
        assertEquals(0, fighterSize.planned());

        assertDoesNotThrow(() -> lookup("#notificationImage").query());
        Label notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("Unit was transferred."));

        // Action:
        // Jan wants to move his "Explorer" unit to his second troop as well. He clicks on the "Explorer" unit and
        // then on "2nd Troop". He then clicks on "Transfer Unit".
        clickOn(lookup("#yourUnitsListView .list-cell").queryAs(ListCell.class));
        clickOn(lookup("#troopsListView .list-cell").queryAs(ListCell.class));
        waitForFxEvents();
        TroopSizeItem explorerSize = (TroopSizeItem) lookup("#focusedTroopSizeListView").queryAs(ListView.class).getItems().getFirst();
        assertEquals(1, explorerSize.actual());
        clickOn("#transferUnitButton");

        Ship updatedExplorer = new Ship(null, null, "testUnitId", "testGameId", "testEmpireId", "testTroopId2", "explorer", 100, 0, null, null);
        subjectUnit.onNext(new Event<>("games.testGameId.fleets.*.ships.*.updated", updatedExplorer));

        waitForFxEvents();

        // Result:
        // The explorer unit is removed from the list of units in the "Transfer Units" tab. The "2nd Troop" now has an
        // actual "Explorer" unit. A success notification shows up.
        assertFalse(lookup("#yourUnitsListView").queryAs(ListView.class).getItems().contains(fighter));
        assertEquals(0, lookup("#yourUnitsListView").queryAs(ListView.class).getItems().size());
        explorerSize = (TroopSizeItem) lookup("#focusedTroopSizeListView").queryAs(ListView.class).getItems().getFirst();
        assertEquals(2, explorerSize.actual());

        assertDoesNotThrow(() -> lookup("#notificationImage").query());
        notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("Unit was transferred."));
    }

    @Test
    void viewTabWebSocketTest() {
        showTroopView();

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Transfer Units" tab of the troop view. He currently has
        // two different units in his troop.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        assertEquals(2, lookup("#sizeListView").queryAs(ListView.class).getItems().size());
        assertEquals(2, lookup("#unitsListView").queryAs(ListView.class).getItems().size());

        // Action:
        // Jan's "interceptor" unit has been transferred to this troop.
        subjectUnit.onNext(new Event<>("games.testGameId.fleets.testTroopId.ships.testUnitId3.updated", new Ship(null, null, "testUnitId3", "testGameId", "testEmpireId", "testTroopId", "interceptor", 100, 0, null, null)));
        waitForFxEvents();

        // Result:
        // The "interceptor" unit is now in the list of units. The "actual" value of the "interceptor" unit is 1, but
        // Jan did not plan this unit before, so the "planned" value is 0.
        assertEquals(3, lookup("#sizeListView").queryAs(ListView.class).getItems().size());
        assertEquals(3, lookup("#unitsListView").queryAs(ListView.class).getItems().size());
        TroopSizeItem interceptorSize = (TroopSizeItem) lookup("#sizeListView").queryAs(ListView.class).getItems().get(2);
        assertEquals("interceptor", interceptorSize.type());
        assertEquals(1, interceptorSize.actual());
        assertEquals(0, interceptorSize.planned());
        assertEquals("interceptor", ((ReadShipDto) lookup("#unitsListView").queryAs(ListView.class).getItems().get(2)).type());

        // Action:
        // Jan's "interceptor" unit has been transferred back to his other troop.
        subjectUnit.onNext(new Event<>("games.testGameId.fleets.testTroopId2.ships.testUnitId3.updated", new Ship(null, null, "testUnitId3", "testGameId", "testEmpireId", "testTroopId2", "interceptor", 100, 0, null, null)));
        waitForFxEvents();

        // Result:
        // The "interceptor" unit is not in the list of units anymore.
        assertEquals(2, lookup("#sizeListView").queryAs(ListView.class).getItems().size());
        assertEquals(2, lookup("#unitsListView").queryAs(ListView.class).getItems().size());

        // Action:
        // Jan's "train explorer" job has finished.
        TroopSizeItem explorerSize = (TroopSizeItem) lookup("#sizeListView").queryAs(ListView.class).getItems().getFirst();
        assertEquals("explorer", explorerSize.type());
        assertEquals(1, explorerSize.actual());
        assertEquals(2, explorerSize.planned());
        subjectUnit.onNext(new Event<>("games.testGameId.fleets.testTroopId.ships.testUnitId3.created", new Ship(null, null, "testUnitId3", "testGameId", "testEmpireId", "testTroopId", "explorer", 100, 0, null, null)));
        waitForFxEvents();

        // Result:
        // The "explorer" unit is now in the list of units. The "actual" value of the "explorer" unit is now 2.
        assertEquals(2, lookup("#sizeListView").queryAs(ListView.class).getItems().size());
        assertEquals(3, lookup("#unitsListView").queryAs(ListView.class).getItems().size());
        explorerSize = (TroopSizeItem) lookup("#sizeListView").queryAs(ListView.class).getItems().getFirst();
        assertEquals("explorer", explorerSize.type());
        assertEquals(2, explorerSize.actual());
        assertEquals(2, explorerSize.planned());
        assertEquals("explorer", ((ReadShipDto) lookup("#unitsListView").queryAs(ListView.class).getItems().get(2)).type());
    }

    @Test
    void trainUnitsTabWebSocketTest() {
        showTroopView();
        clickOn("#trainUnitsTab");

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Transfer Units" tab of the troop view. He currently has
        // two different unit types planned.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        assertEquals(2, lookup("#chooseUnitsListView").queryAs(ListView.class).getItems().size());

        // Action:
        // Jan has updated his troop to now plan "interceptor" units. He does not plan "explorer" or "fighter" units
        // anymore.
        subjectTroop.onNext(new Event<>("games.testGameId.fleets.testTroopId.updated", new Fleet(troop.createdAt(), troop.updatedAt(), troop._id(), troop.game(), troop.empire(), troop.name(), troop.location(), new TreeMap<>(Map.of("explorer", 0, "fighter", 0, "interceptor", 2)), troop._private(), troop._public(), troop.effects())));
        waitForFxEvents();

        // Result:
        // Jan can now only train "interceptor" units.
        assertEquals(1, lookup("#chooseUnitsListView").queryAs(ListView.class).getItems().size());
        assertEquals("interceptor", ((TroopSizeItem) lookup("#chooseUnitsListView").queryAs(ListView.class).getItems().getFirst()).type());
    }

    @Test
    void transferUnitsWebSocketTest() {
        showTroopView();
        clickOn("#transferUnitsTab");

        waitForFxEvents();

        clickOn(lookup("#troopsListView .list-cell").queryAs(ListCell.class));

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the "Transfer Units" tab of the troop view. He currently has
        // two units in his troop. He also has another troop which has one unit planned. He focuses on this troop.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#troopViewRoot").query());
        assertEquals(2, lookup("#yourUnitsListView").queryAs(ListView.class).getItems().size());
        assertEquals(1, lookup("#troopsListView").queryAs(ListView.class).getItems().size());
        assertEquals(1, lookup("#focusedTroopSizeListView").queryAs(ListView.class).getItems().size());

        // Action:
        // Jan's other troop gains a "fighter" unit.
        subjectUnit.onNext(new Event<>("games.testGameId.fleets.testTroopId2.ships.testUnitId3.created", new Ship(null, null, "testUnitId3", "testGameId", "testEmpireId", "testTroopId2", "fighter", 100, 0, null, null)));
        waitForFxEvents();

        // Result:
        // The focused troop now has a "fighter" unit. The "actual" value of the "fighter" unit is 1.
        assertEquals(2, lookup("#focusedTroopSizeListView").queryAs(ListView.class).getItems().size());
        TroopSizeItem fighter = (TroopSizeItem) lookup("#focusedTroopSizeListView").queryAs(ListView.class).getItems().get(1);
        assertEquals("fighter", fighter.type());
        assertEquals(1, fighter.actual());
        assertEquals(0, fighter.planned());

        // Action:
        // Jan's other troop loses the "fighter" unit.
        subjectUnit.onNext(new Event<>("games.testGameId.fleets.testTroopId2.ships.testUnitId3.deleted", new Ship(null, "1", "testUnitId3", "testGameId", "testEmpireId", "testTroopId2", "fighter", 100, 0, null, null)));
        waitForFxEvents();

        // Result:
        // The focused troop does not have a "fighter" unit anymore.
        assertEquals(1, lookup("#focusedTroopSizeListView").queryAs(ListView.class).getItems().size());
    }

    @Test
    void pauseAndResume() {
        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to pause the game, so he clicks on "Esc" in the top left corner.
        waitForFxEvents();

        clickOn(app.stage());
        press(KeyCode.ESCAPE);

        waitForFxEvents();

        // Result:
        // The pause menu opens.
        AnchorPane pauseMenuBox = lookup("#pauseMenuBox").query();
        assertNotNull(pauseMenuBox);

        // Action:
        // Jan wants to resume the game, so he clicks on "Resume".
        clickOn("Resume");

        waitForFxEvents();

        // Result:
        // The pause menu closes.
        assertThrows(EmptyNodeQueryException.class, () -> lookup("#pauseMenuBox").query());
    }

    @Test
    void pauseAndQuit() {
        waitForFxEvents();
        doReturn(null).when(app).show("/lobby");

        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to pause the game, so he hits the "Esc" key.
        waitForFxEvents();

        clickOn(app.stage());
        press(KeyCode.ESCAPE);

        waitForFxEvents();

        // Result:
        // The pause menu opens.
        AnchorPane pauseMenuBox = lookup("#pauseMenuBox").query();
        assertNotNull(pauseMenuBox);

        // Action:
        // Jan wants to quit his current game, so he clicks "Return to Lobby".
        clickOn("Return to Lobby");

        waitForFxEvents();

        // Result:
        // The pause menu closes and Jan is back in the lobby.
        assertThrows(EmptyNodeQueryException.class, () -> lookup("#pauseMenuBox").query());
        verify(app, times(1)).show("/lobby");
    }

    @Test
    void pauseAndExitGame() {
        // Mock System.exit(int value) to prevent the game from actually closing. Otherwise, the test would fail.
        final boolean[] gameClosed = {false};
        new MockUp<System>() {
            @mockit.Mock
            public void exit(int value) {
                gameClosed[0] = true;
            }
        };

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to pause the game, so he hits the "Esc" key.
        waitForFxEvents();

        clickOn(app.stage());
        press(KeyCode.ESCAPE);

        waitForFxEvents();

        // Result:
        // The pause menu opens.
        AnchorPane pauseMenuBox = lookup("#pauseMenuBox").query();
        assertNotNull(pauseMenuBox);

        // Action:
        // Jan wants to exit the game, so he clicks "Exit PRAESIDEO".
        assertFalse(gameClosed[0]);
        clickOn("Exit PRAESIDEO");

        // Result:
        // The game closes.
        assertTrue(gameClosed[0]);
    }

    @Test
    void testPauseButtonOwner() {
        waitForFxEvents();
        Pane pauseText = lookup("#pauseTextContainer").query();
        assertEquals(0, pauseText.getChildren().size());
        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        assertEquals(1, game.speed());

        // Action:
        // Jan wants to pause the game, so he clicks on the pause button.
        ToggleButton pauseButton = lookup("#pauseToggleButton").query();

        clickOn(pauseButton);
        subjectGame.onNext(new Event<>("games.testGameId.updated",
                game = new Game(null, null, "testGameId", "name", "123", 1,
                        5, true, 0, 1, null, new GameSettings(50))));
        waitForFxEvents();

        // Result:
        // The pause menu opens.
        assertEquals(1, pauseText.getChildren().size());
        assertEquals(0, game.speed());
    }

    @Test
    void playerList() {
        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to see the list of players, so he looks at the player list.
        ListView<Player> playerListView = lookup("#playerListView").query();
        assertNotNull(playerListView);

        ObservableList<Player> items = playerListView.getItems();

        // Result:
        // Jan sees the list of players.
        assertEquals(4, items.size());
        assertEquals("user0", items.getFirst()._id());
        assertEquals(1, items.getFirst().flag());
        assertEquals("#0080ff", items.get(0).color());
        assertEquals("Jan", items.get(0).name());
        assertEquals("user1", items.get(1)._id());
        assertEquals(0, items.get(1).flag());
        assertEquals("#000000", items.get(1).color());
        assertEquals("Peter", items.get(1).name());
        assertEquals("user2", items.get(2)._id());
        assertEquals(2, items.get(2).flag());
        assertEquals("#FFFFFF", items.get(2).color());
        assertEquals("Hans", items.get(2).name());
        assertEquals("testEmpireId2", items.get(3)._id());
        assertEquals(3, items.get(3).flag());
        assertEquals("#FF0000", items.get(3).color());
        assertEquals("Tim", items.get(3).name());
    }

    //____________________________Troops List Test Cases____________________________//
    @Test
    void troopsList() {
        waitForFxEvents();
        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to see the list of troops, so he looks at the troops list.
        ListView<Fleet> fleetsListView = lookup("#fleetsListView").query();
        assertNotNull(fleetsListView);
        ObservableList<Fleet> items = fleetsListView.getItems();

        // Result:
        // Jan sees the list of troops.
        assertEquals(2, items.size());
        assertEquals("X-Wing", items.get(0).name());
        assertEquals("testEmpireId", items.get(0).empire());
        assertEquals("T-Fighter", items.get(1).name());
        assertEquals("testEmpireId", items.get(1).empire());
    }

    @Test
    void troopsListAdd() {
        waitForFxEvents();
        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to see the list of troops, so he looks at the troops list.
        ListView<Fleet> fleetsListView = lookup("#fleetsListView").query();
        assertNotNull(fleetsListView);
        ObservableList<Fleet> items = fleetsListView.getItems();
        assertEquals(2, items.size());
        assertEquals("X-Wing", items.get(0).name());
        assertEquals("testEmpireId", items.get(0).empire());

        assertEquals("T-Fighter", items.get(1).name());
        assertEquals("testEmpireId", items.get(1).empire());

        subjectFleet.onNext(new Event<>("games.null.fleets.fleet3ID.created",
                new Fleet("", "", "fleet3ID", "OwnGameId", "testEmpireId", "Armada", "", new TreeMap<>(), new HashMap<>(), new HashMap<>(), new ArrayList<>())));
        waitForFxEvents();
        items = fleetsListView.getItems();
        // Result:
        // Jan sees the updated list of troops.
        assertEquals(3, items.size());
        assertEquals("X-Wing", items.get(0).name());
        assertEquals("testEmpireId", items.get(0).empire());
        assertEquals("T-Fighter", items.get(1).name());
        assertEquals("testEmpireId", items.get(1).empire());
        assertEquals("Armada", items.get(2).name());
        assertEquals("testEmpireId", items.get(2).empire());

    }

    @Test
    void troopsListUpdated() {
        waitForFxEvents();
        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to see the list of troops, so he looks at the troops list.
        ListView<Fleet> fleetsListView = lookup("#fleetsListView").query();
        assertNotNull(fleetsListView);
        ObservableList<Fleet> items = fleetsListView.getItems();
        assertEquals(2, items.size());
        assertEquals("X-Wing", items.get(0).name());
        assertEquals("testEmpireId", items.get(0).empire());

        assertEquals("T-Fighter", items.get(1).name());
        assertEquals("testEmpireId", items.get(1).empire());

        subjectFleet.onNext(new Event<>("games.null.fleets.fleet1ID.updated",
                new Fleet("", "", "fleet1ID", "OwnGameId", "testEmpireId", "Star Destroyer", "", new TreeMap<>(), new HashMap<>(), new HashMap<>(), new ArrayList<>())));
        waitForFxEvents();
        items = fleetsListView.getItems();

        // Result:
        // Jan sees the updated list of troops.
        assertEquals(2, items.size());
        assertEquals("Star Destroyer", items.get(0).name());
        assertEquals("testEmpireId", items.get(0).empire());
        assertEquals("T-Fighter", items.get(1).name());
        assertEquals("testEmpireId", items.get(1).empire());
    }

    @Test
    void troopsListDelete() {
        waitForFxEvents();
        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to see the list of troops, so he looks at the troops list.
        ListView<Fleet> fleetsListView = lookup("#fleetsListView").query();
        assertNotNull(fleetsListView);
        ObservableList<Fleet> items = fleetsListView.getItems();
        assertEquals(2, items.size());
        assertEquals("X-Wing", items.get(0).name());
        assertEquals("testEmpireId", items.get(0).empire());

        assertEquals("T-Fighter", items.get(1).name());
        assertEquals("testEmpireId", items.get(1).empire());

        subjectFleet.onNext(new Event<>("games.null.fleets.fleet1ID.deleted",
                new Fleet("", "", "fleet1ID", "OwnGameId", "testEmpireId", "X-Wing", "", new TreeMap<>(), new HashMap<>(), new HashMap<>(), new ArrayList<>())));
        waitForFxEvents();
        items = fleetsListView.getItems();

        // Result:
        // Jan sees the updated list of troops.
        assertEquals(1, items.size());
        assertEquals("T-Fighter", items.getFirst().name());
        assertEquals("testEmpireId", items.getFirst().empire());
    }

    @Test
    void checkResources() {
        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to see his resources, so he looks at the resource bar.
        HBox resourceBarContainer = lookup("#resourceBarContainer").query();
        assertNotNull(resourceBarContainer);

        waitForFxEvents();

        // Result:
        // Jan sees his resources.
        verifyThat("#energy", hasText("10 +5"));
        verifyThat("#minerals", hasText("20 +1"));
        verifyThat("#alloys", hasText("30 +1"));
        verifyThat("#food", hasText("40 +2"));
        verifyThat("#research", hasText("50 +2"));
        verifyThat("#fuel", hasText("60 +1"));
        verifyThat("#credits", hasText("70 +2"));
        verifyThat("#population", hasText("80 +3"));
        verifyThat("#consumer_goods", hasText("90 +6"));
    }

    @Test
    void updateResources() {
        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently ingame.
        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Action:
        // Jan wants to see his resources, so he looks at the resource bar.
        HBox resourceBarContainer = lookup("#resourceBarContainer").query();
        assertNotNull(resourceBarContainer);

        waitForFxEvents();

        // Result:
        // Jan sees his resources.
        verifyThat("#energy", hasText("10 +5"));
        verifyThat("#minerals", hasText("20 +1"));
        verifyThat("#alloys", hasText("30 +1"));
        verifyThat("#food", hasText("40 +2"));
        verifyThat("#research", hasText("50 +2"));
        verifyThat("#fuel", hasText("60 +1"));
        verifyThat("#credits", hasText("70 +2"));
        verifyThat("#population", hasText("80 +3"));
        verifyThat("#consumer_goods", hasText("90 +6"));

        // Action:
        // The server ticks and his resources are updated.
        TreeMap<String, Integer> resources = new TreeMap<>(Map.of("energy", 15, "minerals", 21, "alloys", 31, "food", 42, "research", 52, "fuel", 61, "credits", 72, "population", 83, "consumer_goods", 96));
        subjectEmpire.onNext(
                new Event<>(
                        "games.testGameId.empires.testEmpireId.updated",
                        new Empire(
                                "123", "123", "testEmpireId", "testGameId", "user0", null, null, "#0080ff", 1, 1, null, List.of(), resources, List.of("society"), new ArrayList<>(), _private, null
                        )
                )
        );

        waitForFxEvents();

        // Result:
        // Jan sees his updated resources.
        verifyThat("#energy", hasText("15 +5"));
        verifyThat("#minerals", hasText("21 +1"));
        verifyThat("#alloys", hasText("31 +1"));
        verifyThat("#food", hasText("42 +2"));
        verifyThat("#research", hasText("52 +2"));
        verifyThat("#fuel", hasText("61 +1"));
        verifyThat("#credits", hasText("72 +2"));
        verifyThat("#population", hasText("83 +3"));
        verifyThat("#consumer_goods", hasText("96 +6"));
    }

    @Test
    void testDisplayCastles() {
        // Start:
        // Jan is playing PRAESIDEO.
        waitForFxEvents();

        //Action: Jan starts the game
        assertEquals(3, zoomDragComponent.getCastleContainer().getChildren().size());

        GameSystem dummySystemFour = createSystemFour();
        this.subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyFour.created", dummySystemFour));

        waitForFxEvents();

        assertEquals(3, zoomDragComponent.getCastleContainer().getChildren().size());

        this.subjectSystem.onNext(new Event<>("games.testGameId.systems._idDummyFour.deleted", dummySystemFour));

        waitForFxEvents();

        //Result: Jan sees the castles of the game and also sees the change if a castles is added or removed
        assertEquals(3, zoomDragComponent.getCastleContainer().getChildren().size());
    }


    @Test
    void testZoom() {
        //Start: Jan is playing PRAESIDEO.
        //       He is ingame and sees the map.

        waitForFxEvents();

        final double scaleInitial = this.zoomDragComponent.scrollPane.getScaleX();

        //Action: Jan zooms in and out so he can see the map better.
        moveTo(this.zoomDragComponent.scrollPane.getWidth() / 2, this.zoomDragComponent.scrollPane.getHeight() / 2);
        scroll(50, VerticalDirection.UP);

        waitForFxEvents();

        final double scaleNew = this.zoomDragComponent.scrollPane.getScaleX();

        assertTrue(scaleNew >= scaleInitial);

        scroll(50, VerticalDirection.DOWN);

        waitForFxEvents();

        final double scaleEnd = this.zoomDragComponent.scrollPane.getScaleX();

        //End: Jan's view gets bigger and smaller depending on his current zoom level
        assertTrue(scaleEnd <= scaleInitial);
    }

    @Test
    void onBackToMap() {
        waitForFxEvents();

        this.showCastleView();

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the castle view.

        Button backButton = lookup("#castleViewBackButton").query();
        assertNotNull(backButton);

        // Action:
        // Jan wants to go back to the game so he closes the castle view
        clickOn(backButton);

        waitForFxEvents();

        // Result:
        // The castle view closes
        assertThrows(EmptyNodeQueryException.class, () -> lookup("#castleViewBackButton").query());
    }

    @Test
    void exploreSystemSuccess() {
        levelUpSuccess("unexplored");
    }

    @Test
    void exploreSystemFailure() {
        levelUpFailure("unexplored");
    }

    @Test
    void colonizeSystemSuccess() {
        levelUpSuccess("explored");
    }

    @Test
    void colonizeSystemFailure() {
        levelUpFailure("explored");
    }

    @Test
    void upgradeSystemSuccess() {
        levelUpSuccess("explored");
    }

    @Test
    void upgradeSystemFailure() {
        levelUpFailure("explored");
    }

    @Test
    void developSystemSuccess() {
        levelUpSuccess("upgraded");
    }

    @Test
    void developSystemFailure() {
        levelUpFailure("upgraded");
    }

    //---------------Test cases for the BuildingsViewComponent-------------------//
    @Test
    void testBuildingDetails() {
        // Title: Building details
        // Start:
        // Tim is in a game and is looking at the city view.
        // There, he sees all the details of the buildings he has constructed in a list with icons and building types.
        // He wants to take a closer look at one of the buildings.

        waitForFxEvents();

        this.showCastleView();

        waitForFxEvents();

        assertTrue(lookup("#destroyButton").query().isDisabled());

        // Action:
        // Tim moves his mouse over the building window and clicks on a building.
        ListView<String> buildingList = lookup("#buildingList").query();
        assertEquals(3, buildingList.getItems().size());
        clickOn("MANOR");

        // Result:
        // Tim sees a detailed overview of the building in a small window,
        // including the resources the building consumes and what it produces.

        Label buildingNameLabel = lookup("#buildingNameLabel").query();
        assertEquals("Information: MANOR", buildingNameLabel.getText());

        Label upKeepLabel = lookup("#upKeepLabel").query();
        assertEquals("Upkeep: ", upKeepLabel.getText());
        HBox upKeepHBox = lookup("#upKeepHBox").query();

        // two BuildingStatComponents
        assertEquals(2, upKeepHBox.getChildren().size());

        BuildingStatComponent buildingStatComponentUpKeep1 = (BuildingStatComponent) upKeepHBox.getChildren().getFirst();
        Label resourceLabel1 = (Label) buildingStatComponentUpKeep1.lookup("#resourceLabel");
        assertEquals("- 5", resourceLabel1.getText());
        BuildingStatComponent buildingStatComponentUpKeep2 = (BuildingStatComponent) upKeepHBox.getChildren().get(1);
        Label resourceLabel2 = (Label) buildingStatComponentUpKeep2.lookup("#resourceLabel");
        assertEquals("- 5", resourceLabel2.getText());

        Label productionLabel = lookup("#productionLabel").query();
        assertEquals("Production: ", productionLabel.getText());

        HBox productionHBox = lookup("#productionHBox").query();
        // one BuildingStatComponent
        assertEquals(1, productionHBox.getChildren().size());

        BuildingStatComponent buildingStatComponentProduction1 = (BuildingStatComponent) productionHBox.getChildren().getFirst();
        Image resourceImage = new Image(Objects.requireNonNull(App.class.getResource("image/game_resources/credits.png")).toString());
        ImageView resourceImageView = (ImageView) buildingStatComponentProduction1.lookup("#resourceImage");
        assertEquals(resourceImage.getUrl(), resourceImageView.getImage().getUrl());
        Label resourceAmountLabel = (Label) buildingStatComponentProduction1.lookup("#resourceLabel");
        assertEquals("+ 1", resourceAmountLabel.getText());
    }

    @Test
    void testSelectBuildIcon() {
        // Title: Select building construction window element
        // Start:
        // Tim is in a game and is in the city view. There, he sees all the details of the buildings he has constructed,
        // listed with icons and building types.
        // He wants to select a building that he would like to construct.

        waitForFxEvents();

        this.showCastleView();

        waitForFxEvents();

        Button buildButton = lookup("#buildButton").query();

        assertTrue(buildButton.isDisabled());

        // Action:
        // Tim moves his mouse to the icons of the resource for which he wants to build a building
        // and clicks on it. Then he hovers his mouse over the label under the "Build Building" button.
        clickOn("#farm");
        waitForFxEvents();

        // Result:
        // Tim sees on a pop-up window the details of the building he has selected by clicking the build icon before.
    }

    @Test
    void testDestroyBuilding() {
        // Title: Destroy building
        // Start:
        // Tim is in a game and is in the city view. There, he sees all the details of the three buildings he has constructed,
        // listed with icons and building types.
        // He wants to destroy a building that he no longer needs.

        waitForFxEvents();

        this.showCastleView();

        doReturn(Observable.just(updatedDeleteBuildingSystem))
                .when(gameSystemsApiService).updateSystem(this.game._id(), this.system._id(), getDeleteBuildingUpdateSystemDto());

        waitForFxEvents();
        Button destroyButton = lookup("#destroyButton").query();

        assertTrue(destroyButton.isDisabled());

        // Action:
        // Tim moves his mouse over the building window and clicks on a building and then clicks on the "Destroy Building" button.
        clickOn("MANOR");
        clickOn(destroyButton);
        subjectSystem.onNext(new Event<>("games.testGameId.systems.testSystemId.updated", this.updatedDeleteBuildingSystem));

        waitForFxEvents();

        // Result:
        // Tim sees that the building he has selected has been removed from the list of buildings.
        // He sees now two buildings in the list.
        ListView<String> buildingList = lookup("#buildingList").query();
        assertEquals(2, buildingList.getItems().size());
    }

    @Test
    void testBuildBuilding() {
        // Title: Build building
        // Start:
        // Tim is in a game and is in the city view. There, he sees all the details of the three buildings he has constructed,
        // listed with icons and building types.
        // He wants to build a new building.

        waitForFxEvents();

        this.showCastleView();

        waitForFxEvents();

        Button buildButton = lookup("#buildButton").query();

        assertTrue(buildButton.isDisabled());

        // Action:
        // Tim moves his mouse to the icons of the resource for which he wants to build a building
        // and clicks on it. Then he clicks on the "Build Building" button.
        clickOn("#farm");
        assertFalse(buildButton.isDisabled());
        clickOn(buildButton);
        subjectSystem.onNext(new Event<>("games.testGameId.systems.testSystemId.updated", this.updatedBuildBuildingSystem));

        waitForFxEvents();

        // Result:
        // Tim sees that the building he has selected has been added to the list of buildings.
        // He sees now four buildings in the list.
        ListView<String> buildingList = lookup("#buildingList").query();
        assertEquals(4, buildingList.getItems().size());
    }

    private Job getBuildBuildingJob() {
        return new Job(
                "",
                "",
                "testJobId",
                0,
                0,
                this.game._id(),
                this.empire._id(),
                this.system._id(),
                0,
                "building",
                "farm",
                null,
                null,
                null,
                null,
                null,
                farm.cost(),
                null
        );
    }

    @Test
    void cantAffordBuilding() {
        // Title: Can't afford building
        // Start:
        // Tim is in a game and is in the city view. There, he sees all the details of the three buildings he has constructed,
        // listed with icons and building types.
        // He wants to build a new building he can't afford.
        waitForFxEvents();

        this.showCastleView();

        waitForFxEvents();

        Button buildButton = lookup("#buildButton").query();

        assertTrue(buildButton.isDisabled());

        // Action:
        // Tim clicks the build icon of the resource for which he wants to build a building
        clickOn("#research_lab");
        waitForFxEvents();

        // Result:
        // Tim sees that the "Build Building" button is disabled because he can't afford to build the building.
        assertTrue(buildButton.isDisabled());
    }

    //-------------------------------------Test cases for the Tasks-------------------------------------------------//


    @Test
    void testTaskButtonHover() {
        // Title: Task button hover
        // Start:
        // Tim is in ingame view. He sees the task button.
        // He wants to see the tooltip of the task button.
        waitForFxEvents();

        Label taskButtonLabel = lookup("#taskToolTipLabel").query();
        ImageView taskButton = lookup("#taskButton").query();
        Tooltip tooltip = taskButtonLabel.getTooltip();
        tooltip.setShowDelay(new Duration(0));

        assertFalse(tooltip.isShowing());

        // Action:
        // Tim hovers over the task button.
        waitForFxEvents();
        moveTo(40, -45);
        moveTo(40, -40);

        waitForFxEvents();

        // Result:
        // Tim sees the tooltip of the task button.
        if (tooltip.isShowing()) {
            Text toolTipText = lookup("#taskText").query();
            assertTrue(tooltip.isShowing());
            assertEquals("Running tasks will unlock\nnew improvements.", toolTipText.getText());
        } else {
            moveTo(taskButton);
            moveTo(taskButtonLabel);
            Text toolTipText = lookup("#taskText").query();
            assertTrue(tooltip.isShowing());
            assertEquals("Running tasks will unlock\nnew improvements.", toolTipText.getText());
        }
    }


    @Test
    void testOpenTask() {

        // Title: Open task
        // Start:
        // Tim is in ingame view. He sees the task button.
        // He wants to open the task.
        waitForFxEvents();

        ImageView taskButton = lookup("#taskButton").query();

        // Action:
        // Tim clicks on the task button.
        clickOn(taskButton);

        waitForFxEvents();

        // Result:
        // Tim sees the task window.
        Pane taskRoot = lookup("#taskRoot").query();
        assertNotNull(taskRoot);
    }

    @Test
    void testCloseTask() {
        // Title: Close Task
        // Start:
        // Tim is in ingame view. He sees the task button.
        // He wants to Close the task.
        waitForFxEvents();

        ImageView taskButton = lookup("#taskButton").query();

        // Action:
        // Tim clicks on the task button.
        clickOn(taskButton);

        waitForFxEvents();

        Pane taskRoot = lookup("#taskRoot").query();
        assertNotNull(taskRoot);

        clickOn(taskButton);

        waitForFxEvents();
        assertNotNull(taskRoot);
        // Result:
        // Tim doesnt see the task window.
    }

    @Test
    void testTaskList() {
        // Title: Task list
        // Start:
        // Tim is in ingame view. He sees the task button.
        // He wants to see the list of tasks.
        waitForFxEvents();

        ImageView taskButton = lookup("#taskButton").query();

        // Action:
        // Tim clicks on the task button.
        clickOn(taskButton);

        waitForFxEvents();

        // Result:
        // Tim sees the list of tasks.
        ListView<Job> taskList = lookup("#taskList").query();
        assertNotNull(taskList);

        ObservableList<Job> items = taskList.getItems();

        assertEquals(4, items.size());
        assertEquals("testJobId1", items.get(0)._id());
        assertEquals("testJobId2", items.get(1)._id());
        assertEquals("testJobId3", items.get(2)._id());
    }

    @Test
    void testTaskCancelButtonTooltip() {
        // Title: Task cancel button tooltip
        // Start:
        // Tim is in ingame view. He sees the task button.
        // He wants to see the tooltip of the cancel button.
        waitForFxEvents();

        ImageView taskButton = lookup("#taskButton").query();

        // Action:
        // Tim clicks on the task button.
        clickOn(taskButton);

        waitForFxEvents();

        Label cancelButton = lookup("#cancelTooltip").query();
        Tooltip tooltip = cancelButton.getTooltip();
        tooltip.setShowDelay(new Duration(0));

        assertFalse(tooltip.isShowing());
        // Tim hovers over the cancel button.
        moveTo(cancelButton);

        waitForFxEvents();

        // Result:
        // Tim sees the tooltip of the cancel button.
        if (tooltip.isShowing()) {
            assertTrue(tooltip.isShowing());
            Text tooltipText = lookup("#tooltipText").query();
            assertEquals("Refund on cancellation:", tooltipText.getText());
        } else {
            moveTo(cancelButton);
            assertTrue(tooltip.isShowing());
            Text tooltipText = lookup("#tooltipText").query();
            assertEquals("Refund on cancellation:", tooltipText.getText());
        }
    }

    @Test
    void testTaskCancelButton() {
        // Title: Task cancel button
        // Start:
        // Tim is in ingame view. He sees the task button.
        // He wants to cancel a task.
        waitForFxEvents();

        ImageView taskButton = lookup("#taskButton").query();

        // Action:
        // Tim clicks on the task button.
        clickOn(taskButton);

        waitForFxEvents();

        ListView<Job> taskList = lookup("#taskList").query();
        assertNotNull(taskList);

        ObservableList<Job> items = taskList.getItems();

        assertEquals(4, items.size());

        Label cancelButton = lookup("#cancelTooltip").query();

        // Action:
        // Tim selects a task and clicks on the "Cancel" button.
        clickOn(cancelButton);
        //delete testJoId1
        subjectJob.onNext(new Event<>("games.testGameId.empires.testEmpireId.jobs.testJobId1.deleted", createJobs().getFirst()));

        waitForFxEvents();

        // Result:
        // Tim sees that the task he has selected has been removed from the list of task

        // He sees now two tasks in the list.
        assertEquals(3, taskList.getItems().size());

    }

    @Test
    void testTaskFilter() {
        // Title: Task filter
        // Start:
        // Tim is in ingame view. He sees the task button.
        // He wants to filter the list of tasks.
        waitForFxEvents();
        ImageView taskButton = lookup("#taskButton").query();

        // Action:
        // Tim clicks on the task button.
        clickOn(taskButton);

        waitForFxEvents();

        ListView<Job> taskList = lookup("#taskList").query();
        assertNotNull(taskList);

        ObservableList<Job> items = taskList.getItems();

        assertEquals(4, items.size());
        ChoiceBox<String> taskFilter = lookup("#taskFilter").query();

        assertEquals("All owned castles", taskFilter.getValue());
        assertEquals(4, taskFilter.getItems().size());


        // Action:
        // Tim clicks on the "Filter" button and selects a filter.
        clickOn(taskFilter);

        type(KeyCode.DOWN);
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);


        waitForFxEvents();

        // Result:
        // Tim sees the filtered list of tasks.
        assertEquals(2, taskList.getItems().size());
        assertEquals("Rotenburg", taskFilter.getValue());
        assertEquals("testJobId1", taskList.getItems().getFirst()._id());
    }

    @Test
    void testEvent() {
        // Start:
        // Tim is in ingame
        waitForFxEvents();

        // Action:
        // He randomly receives an event
        Platform.runLater(() -> this.eventService.setEvent("blizzard"));

        waitForFxEvents();

        // Result: Jan sees the events

        clickOn("OKAY");
        waitForFxEvents();

        assertThrows(EmptyNodeQueryException.class, () -> lookup("OKAY").query());
    }

    @Test
    void testEventSpecial() {
        // Start:
        // Tim is in ingame
        waitForFxEvents();

        // Action:
        // He randomly receives an event, this time its special, he can choose betwen Yes or no
        Platform.runLater(() -> this.eventService.setEvent("marriage"));

        waitForFxEvents();

        // Result: Jan sees the events
        clickOn("Yes");

        waitForFxEvents();
        clickOn("OKAY");

        assertThrows(EmptyNodeQueryException.class, () -> lookup("OKAY").query());
    }


    //____________________________Market Test Cases____________________________//
    @Test
    void testMarketButtonHover() {
        // Title: Market button hover
        // Start:
        // Tim is in ingame view. He sees the market button.
        // He wants to see the tooltip of the market button.
        waitForFxEvents();

        Label marketButtonLabel = lookup("#marketToolTipLabel").query();
        ImageView marketButton = lookup("#marketButton").query();
        Tooltip tooltip = marketButtonLabel.getTooltip();
        tooltip.setShowDelay(new Duration(0));

        assertFalse(tooltip.isShowing());

        // Action:
        // Tim hovers over the market button.
        waitForFxEvents();
        moveTo(40, -10);
        moveTo(40, -5);

        waitForFxEvents();

        // Result:
        // Tim sees the tooltip of the market button.
        if (tooltip.isShowing()) {
            Text toolTipText = lookup("#marketText").query();
            assertTrue(tooltip.isShowing());
            assertEquals("In the market, you can exchange\nresources for coins and vice versa.", toolTipText.getText());
        } else {
            moveTo(marketButton);
            moveTo(marketButtonLabel);
            Text toolTipText = lookup("#marketText").query();
            assertTrue(tooltip.isShowing());
            assertEquals("In the market, you can exchange\nresources for coins and vice versa.", toolTipText.getText());
        }
    }

    @Test
    void testOpenMarket() {
        // Title: Open Market
        // Start:
        // Tim is in ingame view. He sees the market button.
        // He wants to open the market.
        waitForFxEvents();

        ImageView marketButton = lookup("#marketButton").query();

        // Action:
        // Tim clicks on the market button.
        clickOn(marketButton);

        waitForFxEvents();

        // Result:
        // Tim sees the market window.
        VBox marketRoot = lookup("#marketRoot").query();
        assertNotNull(marketRoot);
    }

    @Test
    void testCloseMaret() {
        // Title: Close Market
        // Start:
        // Tim is in ingame view. He sees the market button.
        // He wants to Close the market.
        waitForFxEvents();

        ImageView marketButton = lookup("#marketButton").query();

        // Action:
        // Tim clicks on the market button.
        clickOn(marketButton);

        waitForFxEvents();

        VBox marketRoot = lookup("#marketRoot").query();
        assertNotNull(marketRoot);

        clickOn(marketButton);

        waitForFxEvents();
        assertNotNull(marketRoot);
        // Result:
        // Tim doesnt see the market window.
    }

    @Test
    void testMarketBuy() {
        // Title: Buy resource in market
        // Start:
        // Tim is in ingame view. He sees the market window.
        // He wants to buy steel in the market.
        waitForFxEvents();

        showMarketView();

        waitForFxEvents();
        Text buyAddNum = lookup("#buyAddNum").query();
        Text sellAddNum = lookup("#sellAddNum").query();
        Text buySubNum = lookup("#buySubNum").query();
        Text sellSubNum = lookup("#sellSubNum").query();


        doReturn(Observable.just(this.empire)).when(gameEmpiresApiService).updateResources(any(), any(), any());

        Button buyButton = lookup("#buyButton").query();
        assertTrue(buyButton.isDisabled());
        // Action:
        // Tim choose steel, choose amount of 10 and clicks on the buy button.
        clickOn("#resourceChoice").clickOn("Steel");
        waitForFxEvents();
        clickOn("#amountField").write("10");
        waitForFxEvents();

        assertEquals("  + 10", buyAddNum.getText());
        assertEquals("  - 10", sellAddNum.getText());
        assertEquals("  - 26", buySubNum.getText());
        assertEquals("  + 14", sellSubNum.getText());

        assertFalse(buyButton.isDisabled());

        clickOn("#buyButton");
        waitForFxEvents();

        // Result:
        // Tim sees the market window.
    }

    @Test
    void testMarketSell() {
        // Title: Sell resource in market
        // Start:
        // Tim is in ingame view. He sees the market window.
        // He wants to sell steel in the market.
        waitForFxEvents();

        showMarketView();

        waitForFxEvents();

        Text buyAddNum = lookup("#buyAddNum").query();
        Text sellAddNum = lookup("#sellAddNum").query();
        Text buySubNum = lookup("#buySubNum").query();
        Text sellSubNum = lookup("#sellSubNum").query();

        doReturn(Observable.just(this.empire)).when(gameEmpiresApiService).updateResources(any(), any(), any());

        Button buyButton = lookup("#sellButton").query();
        assertTrue(buyButton.isDisabled());
        // Action:
        // Tim choose steel, choose amount of 10 and clicks on the sell button.
        clickOn("#resourceChoice").clickOn("Steel");
        waitForFxEvents();
        clickOn("#amountField").write("10");
        waitForFxEvents();

        assertFalse(buyButton.isDisabled());

        assertEquals("  + 10", buyAddNum.getText());
        assertEquals("  - 10", sellAddNum.getText());
        assertEquals("  - 26", buySubNum.getText());
        assertEquals("  + 14", sellSubNum.getText());

        clickOn("#sellButton");
        waitForFxEvents();

        // Result:
        // Tim sees the market window.
    }

    @Test
    void testMarketFeeToolTip() {
        showMarketView();

        waitForFxEvents();

        Label marketFeeToolTiopLabel = lookup("#marketFeeToolTiopLabel").query();
        Tooltip tooltip = marketFeeToolTiopLabel.getTooltip();
        tooltip.setShowDelay(new Duration(0));

        Text marketFeeNum = lookup("#marketFeeNum").query();

        moveTo(marketFeeNum);
        String marketFee = "Base: +30%\nTotal Fee: +30%";

        if (tooltip.isShowing()) {
            Text toolTipText = lookup("#marketFeeExplain").query();
            assertEquals(marketFee, toolTipText.getText());
        } else {
            moveTo(marketFeeNum);
            Text toolTipText = lookup("#marketFeeExplain").query();
            assertEquals(marketFee, toolTipText.getText());
        }

    }

    //____________________________Enhancements Test Cases____________________________//

    @Test
    void testEnhancementButtonHover() {
        // Title: Enhancements Icon: Hover
        // Start:
        // Jan is on the in-game screen.
        // He sees an icon with three green arrows on the left edge and wonders what it means.

        waitForFxEvents();

        Label enhancementButtonLabel = lookup("#enhancementsToolTipLabel").query();
        ImageView enhancementButton = lookup("#enhancementsButton").query();
        Tooltip tooltip = enhancementButtonLabel.getTooltip();
        tooltip.setShowDelay(new Duration(0));

        assertFalse(tooltip.isShowing());

        // Action:
        // Tim hovers over the task button.
        waitForFxEvents();
        moveTo(40, -45);
        moveTo(40, -40);

        waitForFxEvents();

        // Result:
        // Tim sees the tooltip of the task button.
        if (tooltip.isShowing()) {
            Text toolTipText = lookup("#enhancementsText").query();
            assertTrue(tooltip.isShowing());
            assertEquals("Enhancements greatly\nimprove your empire.", toolTipText.getText());
        } else {
            moveTo(enhancementButton);
            moveTo(enhancementButtonLabel);
            Text toolTipText = lookup("#enhancementsText").query();
            assertTrue(tooltip.isShowing());
            assertEquals("Enhancements greatly\nimprove your empire.", toolTipText.getText());
        }
    }

    @Test
    void testOpenEnhancementComponent() {
        // Title: Opening the Enhancements Window
        // Start:
        // Jan is on the in-game screen. He wants to view the enhancements.

        // Action:
        // Jan clicks on the icon with the three green arrows.
        ImageView enhancementsButton = lookup("#enhancementsButton").query();
        clickOn(enhancementsButton);

        waitForFxEvents();

        // Result:
        // The enhancements window opens over the in-game screen.
        HBox enhancementBox = lookup("#enhancementBox").query();
        assertNotNull(enhancementBox);
    }

    @Test
    void testSelectEnhancementInProgress() {
        // Title: Select Enhancement in progress
        // Start:
        // Jan is on the in-game screen with an open enhancements window. His "Society" researcher,
        // is currently working on the enhancement called "Increased Well-being". Jan wants to review the details of this enhancement once again.
        waitForFxEvents();

        clickOn("#enhancementsBox");
        // Action:
        // Jan clicks on the enhancement.
        waitForFxEvents();

        assertEquals(1, lookup("#enhancementBox").queryAs(HBox.class).getChildren().size());

        ListView<EnhancementItemComponent> enhancementItemList = lookup("#itemListView").queryListView();
        clickOn(enhancementItemList.getItems().get(1).itemClickedBox);

        // Result:
        // A window opens on the right displaying all effects and dependencies of the "Increased Well-being" enhancement.
        waitForFxEvents();

        VBox selectedEnhancementText = lookup("#selectedEnhancementText").query();
        Label enhancement = new Label();
        Label requires = new Label();
        Label costReduction = new Label();
        Label timeReduction = new Label();
        for (Node node : selectedEnhancementText.getChildren()) {
            if (node instanceof Label) {
                if (((Label) node).getText().contains("Demographic")) {
                    enhancement = (Label) node;
                } else if (((Label) node).getText().contains("REQUIRES")) {
                    requires = (Label) node;
                } else if (((Label) node).getText().contains("COST REDUCTION")) {
                    costReduction = (Label) node;
                } else if (((Label) node).getText().contains("TIME REDUCTION")) {
                    timeReduction = (Label) node;
                }
            }
        }
        assertTrue(enhancement.getText().contains("Demographic"));
        assertTrue(requires.getText().contains("REQUIRES"));
        assertTrue(costReduction.getText().contains("COST REDUCTION"));
        assertTrue(timeReduction.getText().contains("TIME REDUCTION"));
    }

    @Test
    void testOpenEnhancementList() {
        // Title: View Enhancements of a Specific Category
        // Start:
        // Jan is on the in-game screen with the enhancements window open.
        // Jan wants to view all possible enhancements in the "Society" category.
        waitForFxEvents();

        clickOn("#enhancementsBox");

        // Action:
        // Jan clicks the "View Enhancements" button for the "Society" category.
        waitForFxEvents();

        assertEquals(1, lookup("#enhancementBox").queryAs(HBox.class).getChildren().size());

        ListView<EnhancementItemComponent> enhancementItemList = lookup("#itemListView").queryListView();
        clickOn(enhancementItemList.getItems().get(1).itemViewEnhancementsButton);

        // Result:
        // The "View Enhancements" buttons disappear. A window opens on the right side
        // showing all the enhancements in the "Society" category that Jan has not yet completed or assigned.
        waitForFxEvents();

        Label enhancementsLabel = lookup("#enhancementsLabel").query();
        assertEquals(enhancementsLabel.getText(), "Society enhancements");

        ListView<EnhancementBoxComponent> enhancementsList = lookup("#enhancementsList").query();
    }

    @Test
    void testShowCompletedEnhancements() {
        // Title: View Completed Enhancements of a Specific Category
        // Start:
        // Jan is on the in-game screen with the Enhancements window open. Jan can see all pending or in-progress enhancements in the "Society" category.
        // However, Jan wants to view all the completed enhancements in this category instead.
        waitForFxEvents();

        clickOn("#enhancementsBox");
        waitForFxEvents();
        ListView<EnhancementItemComponent> enhancementItemList = lookup("#itemListView").queryListView();
        clickOn(enhancementItemList.getItems().get(1).itemViewEnhancementsButton);

        // Action:
        // Jan clicks on "Show completed enhancements" at the bottom of the "Society enhancements" window.
        waitForFxEvents();

        CheckBox checkBoxCompletedEnhancements = lookup("#checkBoxCompletedEnhancements").query();
        clickOn(checkBoxCompletedEnhancements);

        // Result:
        // Only the enhancements in the "Society" category that Jan has actually completed are displayed.
        waitForFxEvents();

        ListView<EnhancementBoxComponent> enhancementsList = lookup("#enhancementsList").query();
    }

    @Test
    void testUnlockButtonToolTip() {
        // Title: Variable Explanation: Enhancement Costs
        // Start:
        // Jan is on the in-game screen with the enhancement window open. He also sees the details for the "Science" enhancement.
        // He wonders where the enhancement price comes from.
        waitForFxEvents();

        clickOn("#enhancementsBox");
        waitForFxEvents();
        ListView<EnhancementItemComponent> enhancementItemList = lookup("#itemListView").queryListView();
        clickOn(enhancementItemList.getItems().get(1).itemViewEnhancementsButton);
        waitForFxEvents();
        clickOn("Comfortable Colonized Castles I");

        // Action:
        // Jan moves his mouse over the "Unlock" button.
        waitForFxEvents();

        SplitPane tooltipBackground = lookup("#tooltipBackground").query();
        Tooltip tooltip = tooltipBackground.getTooltip();
        tooltip.setShowDelay(new Duration(0));

        assertFalse(tooltip.isShowing());

        waitForFxEvents();
        moveTo(1025, 675);
        moveTo(1050, 650);

        // Result:
        // A tooltip opens with information about the standard price of the enhancement, any effects that might reduce the price,
        // and a summary of the final price. Jan is also reminded that clicking the button will add a new task to the queue.
    }

    @Test
    void testStopEnhancementInProgress() {
        // Title: Cancel Enhancement in Progress
        // Start:
        // Jan is on the in-game screen with the enhancement window open.
        // He sees one Enhancement in Progress ann wants to cancel it.
        waitForFxEvents();

        clickOn("#enhancementsBox");

        // Action:
        // Jan clicks on the red Cross next to the enhancement.
        waitForFxEvents();

        ListView<EnhancementItemComponent> enhancementItemList = lookup("#itemListView").queryListView();
        EnhancementItemComponent societyEnhancement = enhancementItemList.getItems().get(1);
        assertFalse(societyEnhancement.itemNoJob.isVisible());
        assertTrue(societyEnhancement.itemCross.isVisible());
        assertTrue(societyEnhancement.itemDate.isVisible());
        assertTrue(societyEnhancement.itemProgressBar.isVisible());
        assertTrue(societyEnhancement.itemTechnologie.isVisible());

        clickOn(societyEnhancement.itemCross);

        waitForFxEvents();

        subjectJob.onNext(new Event<>("games.testGameId.empires.testEmpireId.jobs.testJobId1.deleted", createTechJobs().get(3)));

        // Result:
        // Jan gets Back what he paid for the enhancement and the scientist isn't working on any Task
        waitForFxEvents();

        assertTrue(societyEnhancement.itemNoJob.isVisible());
        assertFalse(societyEnhancement.itemCross.isVisible());
        assertFalse(societyEnhancement.itemDate.isVisible());
        assertFalse(societyEnhancement.itemProgressBar.isVisible());
        assertFalse(societyEnhancement.itemTechnologie.isVisible());
    }

    //____________________________Contacts Test Cases____________________________//

    @Test
    void testOpenContactsView() {
        // Start:
        // Jan is playing PRAESIDEO. He wants to see who is playing with him. Now he is in the Ingame menu.
        mockEmpires();

        // Action:
        // Jan is clicking the contacts icon on the top right.
        HBox contactsButton = lookup("#contactsIcon").query();
        clickOn(contactsButton);

        // End:
        // A big contacts pop up window appears with all the information about the other players.
        waitForFxEvents();

        AnchorPane contactsBox = lookup("#contactsRoot").query();
        assertNotNull(contactsBox);

        subjectWar.onNext(new Event<>("games.testGameId.wars.*.created", new War("", "", "", "testGameId", "testEmpireId", "empire2", "", null)));
        waitForFxEvents();

        clickOn(contactsButton);
    }

    @Test
    void testSendEmoji() {
        // Start:
        // Jan is playing PREASIDEO. He wants to send an Emoji to Peter. He is now in the Contacts View.
        clickOn("#contactsIcon");
        waitForFxEvents();

        assertTrue(lookup("#sendButton").query().isDisabled());

        ListView<Player> contactsList = lookup("#contactsList").query();
        waitForFxEvents();
        assertEquals(4, contactsList.getItems().size());

        interact(() -> contactsList.getSelectionModel().select(1));
        waitForFxEvents();
        assertEquals("Peter", contactsList.getSelectionModel().getSelectedItem().name());

        // Action:
        // Jan selects the Happy Emoji.
        waitForFxEvents();

        ListView<VBox> emojiList = lookup("#emojiList").query();
        assertEquals(5, emojiList.getItems().size());

        clickOn(emojiList.getItems().getFirst());
        waitForFxEvents();
        assertEquals(0, emojiList.getSelectionModel().getSelectedIndex());

        // End:
        // Jan can now press the send button to send the emoji.
        waitForFxEvents();

        assertFalse(lookup("#sendButton").query().isDisabled());

        clickOn("#sendButton");
        waitForFxEvents();

        assertTrue(lookup("#notificationImage").query().isVisible());
        assertTrue(lookup("Message was sent.").query().isVisible());

        clickOn("#contactsViewBackButton");
        waitForFxEvents();
    }

    // Build Fleet -------------------------------------------------------------------------------
    @Test
    // test open build fleet
    void testOpenBuildFleet() {
        // Title: Open Build Fleet
        // Start:
        // Tim is in ingame view. He sees the fleet button.
        // He wants to open the build fleet.
        waitForFxEvents();

        HBox buildFleetsBox = lookup("#buildFleetsBox").query();
        ImageView fleetButton = lookup("#buildFleetsButton").query();
        HBox fleetRoot = lookup("#buildFleetRoot").query();
        Text fleetTitle = lookup("#buildFleetTitle").query();

        assertTrue(buildFleetsBox.getStyleClass().contains("enhancement-not-selected"));
        assertFalse(fleetRoot.isVisible());


        // Action:
        // Tim clicks on the fleet button.
        clickOn(fleetButton);

        waitForFxEvents();

        // Result:
        // Tim sees the fleet window.
        assertTrue(buildFleetsBox.getStyleClass().contains("enhancement-selected"));
        assertTrue(fleetRoot.isVisible());
        assertEquals("Plan new Troop", fleetTitle.getText());
    }

    @Test
        // test close build fleet
    void testCloseBuildFleet() {
        // Title: Close Build Fleet
        // Start:
        // Tim is in ingame view. He sees the fleet button.
        // He wants to close the build fleet.
        waitForFxEvents();

        HBox buildFleetsBox = lookup("#buildFleetsBox").query();
        ImageView fleetButton = lookup("#buildFleetsButton").query();
        HBox fleetRoot = lookup("#buildFleetRoot").query();

        assertTrue(buildFleetsBox.getStyleClass().contains("enhancement-not-selected"));
        assertFalse(fleetRoot.isVisible());

        // Action:
        // Tim clicks on the fleet button.
        clickOn(fleetButton);

        waitForFxEvents();

        // Result:
        // Tim sees the fleet window.
        assertTrue(buildFleetsBox.getStyleClass().contains("enhancement-selected"));
        assertTrue(fleetRoot.isVisible());

        // Action:
        // Tim clicks on the fleet button again.
        clickOn(fleetButton);

        waitForFxEvents();

        // Result:
        // Tim doesn't see the fleet window.
        assertTrue(buildFleetsBox.getStyleClass().contains("enhancement-not-selected"));
        assertFalse(fleetRoot.isVisible());
    }

    @Test
        // test select Castle
    void testSelectCastle() {
        // Title: Select Castle
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to select a castle.
        waitForFxEvents();

        clickOn("#buildFleetsButton");

        HBox fleetRoot = lookup("#buildFleetRoot").query();
        ChoiceBox<String> castleList = lookup("#systemsChoiceBox").query();

        waitForFxEvents();
        assertTrue(fleetRoot.isVisible());
        assertEquals(4, castleList.getItems().size());
        assertEquals("Choose one of your castles", castleList.getValue());


        // Action:
        // Tim selects a castle.
        clickOn(castleList);
        type(KeyCode.DOWN);
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);


        waitForFxEvents();
        // Result:
        // Tim sees the selected castle.
        assertEquals("Rotenburg", castleList.getValue());
    }

    @Test
        // test write fleet name
    void testWriteFleetName() {
        // Title: Write Fleet Name
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to write a name for the new fleet.
        waitForFxEvents();

        clickOn("#buildFleetsButton");

        HBox fleetRoot = lookup("#buildFleetRoot").query();
        TextField fleetName = lookup("#fleetName").query();
        Button buildFleetButton = lookup("#buildFleetButton").query();

        assertTrue(fleetRoot.isVisible());
        assertNull(fleetName.getText());
        assertTrue(buildFleetButton.isDisabled());

        // Action:
        // Tim writes a name for the new fleet.
        clickOn(fleetName).write("Test Fleet");

        // Result:
        // Tim sees the name of the new fleet.
        assertEquals("Test Fleet", fleetName.getText());
        assertTrue(buildFleetButton.isDisabled());
    }

    @Test
        // test click buildFleetButton with no castle selected
    void testBuildFleetNoCastleSelected() {
        // Title: Build Fleet with no Castle selected
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to build a new fleet.
        waitForFxEvents();

        clickOn("#buildFleetsButton");

        HBox fleetRoot = lookup("#buildFleetRoot").query();
        TextField fleetName = lookup("#fleetName").query();
        Button buildFleetButton = lookup("#buildFleetButton").query();
        ImageView increaseButton = lookup("#imgViewIncrease").query();
        Label shipAmount = lookup("#shipAmount").query();

        clickOn(fleetName).write("Test Fleet");

        assertTrue(fleetRoot.isVisible());
        assertEquals("Test Fleet", fleetName.getText());
        // Action:
        // selects the amount of ships and clicks on the build fleet button.
        clickOn(increaseButton);
        clickOn(buildFleetButton);

        // Result:
        // Tim sees the fleet window.
        assertTrue(shipAmount.getText().contains("1"));
        assertTrue(fleetRoot.isVisible());
        assertTrue(buildFleetButton.isDisabled());
    }

    @Test
        // test click buildFleetButton with no shipAmount selected
    void testBuildFleetNoShipAmountSelected() {
        // Title: Build Fleet with no Ship Amount selected
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to build a new fleet.
        waitForFxEvents();

        clickOn("#buildFleetsButton");

        HBox fleetRoot = lookup("#buildFleetRoot").query();
        Button buildFleetButton = lookup("#buildFleetButton").query();
        TextField fleetName = lookup("#fleetName").query();

        ChoiceBox<String> castleList = lookup("#systemsChoiceBox").query();

        clickOn(fleetName).write("Test Fleet");

        waitForFxEvents();
        assertTrue(fleetRoot.isVisible());
        assertEquals(4, castleList.getItems().size());
        assertEquals("Choose one of your castles", castleList.getValue());
        assertEquals("Test Fleet", fleetName.getText());

        // Action:
        // Tim selects a castle.
        clickOn(castleList);
        type(KeyCode.DOWN);
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);

        // Tim clicks on the build fleet button.
        clickOn(buildFleetButton);

        // Result:
        // Tim sees the selected castle.
        assertEquals("Rotenburg", castleList.getValue());
        assertTrue(fleetRoot.isVisible());
        assertFalse(buildFleetButton.isDisabled());

    }

    @Test
        // test build fleet
    void testBuildFleet() {
        // Title: Build Fleet
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to build a new fleet.
        waitForFxEvents();

        clickOn("#buildFleetsButton");

        HBox fleetRoot = lookup("#buildFleetRoot").query();
        Button buildFleetButton = lookup("#buildFleetButton").query();
        TextField fleetName = lookup("#fleetName").query();
        Label shipAmount = lookup("#shipAmount").query();
        ChoiceBox<String> castleList = lookup("#systemsChoiceBox").query();

        assertTrue(fleetRoot.isVisible());
        assertEquals(4, castleList.getItems().size());

        // Tim selects a castle.
        // Open the ChoiceBox
        clickOn(castleList);
        type(KeyCode.DOWN);
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);


        // Tim writes a name for the new fleet.
        clickOn(fleetName).write("Test Fleet");

        // Tim selects the amount of ships and clicks on the build fleet button.
        clickOn("#imgViewIncrease");
        shipAmounts.put("explorer", 1);

        assertEquals("Rotenburg", castleList.getValue());
        assertFalse(buildFleetButton.isDisabled());
        assertEquals("Test Fleet", fleetName.getText());
        assertTrue(shipAmount.getText().contains("1"));


        // Action:

        // Tim clicks on the build fleet button.
        clickOn(buildFleetButton);

        waitForFxEvents();

        // Result:
        // Tim sees the fleet window.
        assertTrue(fleetRoot.isVisible());
        assertEquals("Choose one of your castles", castleList.getValue());
        assertTrue(buildFleetButton.isDisabled());
        assertEquals("", fleetName.getText());
    }

    @Test
        // test shipType in build fleet
    void testShipTypeInBuildFleet() {
        // Title: Ship Type in Build Fleet
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to build a new fleet.
        waitForFxEvents();


        Text shipType = lookup("#shipType").query();
        Text unitName = lookup("#unitName").query();

        // Action:
        clickOn("#buildFleetsButton");
        clickOn("#viewTroopButton");


        // Result:
        // Tim sees the ship type window.
        waitForFxEvents();
        assertEquals("Scout", unitName.getText());
        assertEquals("Scout", shipType.getText());


    }

    @Test
        // test shipAmountDecrease
    void testShipAmountDecrease() {
        // Title: Ship Amount Decrease
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to decrease the amount of ships.
        waitForFxEvents();

        clickOn("#buildFleetsButton");

        Label shipAmount = lookup("#shipAmount").query();
        ImageView increaseButton = lookup("#imgViewIncrease").query();
        clickOn(increaseButton);
        ImageView decreaseButton = lookup("#imgViewDecrease").query();

        assertEquals("1", shipAmount.getText());

        // Action:
        // Tim clicks on the decrease button.
        clickOn(decreaseButton);

        // Result:
        // Tim sees the amount of ships decrease by one.
        assertEquals("0", shipAmount.getText());
    }

    @Test
        // test close shipType view
    void testCloseShipTypeView() {
        // Title: Close Ship Type View
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to close the ship type view.
        waitForFxEvents();

        clickOn("#buildFleetsButton");

        VBox unitView = lookup("#unitView").query();
        Button viewTroopButton = lookup("#viewTroopButton").query();
        Point2D screenPosition = viewTroopButton.localToScreen(0, 0);
        double centerX = screenPosition.getX() + viewTroopButton.getWidth() / 2;
        double centerY = screenPosition.getY() + viewTroopButton.getHeight() / 2;
        assertFalse(unitView.isVisible());
        // Action:
        // Tim clicks on the ship type button.
        clickOn(viewTroopButton);


        // Result:
        // Tim sees the ship type window.
        waitForFxEvents();
        assertTrue(unitView.isVisible());

        // Action:
        // Tim clicks on the ship type button again.
        clickOn(centerX, centerY);

        // Result:
        // Tim doesn't see the ship type window.
        waitForFxEvents();
        assertFalse(unitView.isVisible());
    }

    @Test
        // test close shipType view with cancel button
    void testCloseShipTypeViewWithCancelButton() {
        // Title: Close Ship Type View with Cancel Button
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to close the ship type view with the cancel button.
        waitForFxEvents();

        clickOn("#buildFleetsButton");

        VBox unitView = lookup("#unitView").query();
        Button viewTroopButton = lookup("#viewTroopButton").query();
        ImageView cancelButton = lookup("#cancelImage").query();

        assertFalse(unitView.isVisible());

        // Action:
        // Tim clicks on the ship type button.
        clickOn(viewTroopButton);

        // Result:
        // Tim sees the ship type window.
        waitForFxEvents();
        assertTrue(unitView.isVisible());

        // Action:
        // Tim clicks on the cancel button.
        clickOn(cancelButton);

        // Result:
        // Tim doesn't see the ship type window.
        waitForFxEvents();
        assertFalse(unitView.isVisible());
    }

    @Test
        // test open and close sipTyp view with another shipType while open
    void testOpenAndCloseShipTypeViewWithAnotherShipType() {
        // Title: Open and Close Ship Type View with Another Ship Type
        // Start:
        // Tim is in ingame view. He sees the fleet window.
        // He wants to open and close the ship type view with another ship type.
        waitForFxEvents();

        clickOn("#buildFleetsButton");

        VBox unitView = lookup("#unitView").query();
        Button viewTroopButton = lookup("#viewTroopButton").query();
        Text unitName = lookup("#unitName").query();
        ImageView cancelButton = lookup("#cancelImage").query();

        assertFalse(unitView.isVisible());

        Point2D screenPosition = viewTroopButton.localToScreen(0, 0);
        double centerX = screenPosition.getX() + viewTroopButton.getWidth() / 2;
        double centerY = screenPosition.getY() + viewTroopButton.getHeight() / 2;

        // Action:
        // Tim clicks on the ship type button.
        clickOn(viewTroopButton);

        // Result:
        // Tim sees the ship type window.
        waitForFxEvents();
        assertTrue(unitView.isVisible());
        assertEquals("Scout", unitName.getText());

        // Action:
        // Tim clicks on the ship type button again.
        clickOn(centerX, centerY);

        // Result:
        // Tim doesn't see the ship type window.
        waitForFxEvents();
        assertFalse(unitView.isVisible());

        // Action:
        // Tim clicks on another ship type button.
        moveTo(500, 450);
        Node viewTroopButton1 = lookup("#viewTroopButton").nth(1).query();
        clickOn(viewTroopButton1);
        // Result:
        // Tim sees the ship type window.
        waitForFxEvents();
        assertTrue(unitView.isVisible());
        assertEquals("Swordsman", unitName.getText());

        // Action:
        // Tim clicks on the cancel button.
        clickOn(cancelButton);

        // Result:
        // Tim doesn't see the ship type window.
        waitForFxEvents();
        assertFalse(unitView.isVisible());
    }

    @Test
    void testDiplomacy() {
        clickOn("#diplomacyButton");

        waitForFxEvents();
        VBox reasonBox = lookup("#reasonBox").query();
        assertFalse(reasonBox.isVisible());

        clickOn("#reasonButton");

        assertTrue(reasonBox.isVisible());

        Image reasonImage = new Image(Objects.requireNonNull(App.class.getResource("image/warreasons/conquest.png")).toString());
        ImageView reasonImageView = lookup("#reasonImage").query();
        assertEquals(reasonImage.getUrl(), reasonImageView.getImage().getUrl());

        Label reasonName = lookup("#reasonName").query();
        assertEquals("Conquest", reasonName.getText());

        clickOn("#unSeeReasonImage");

        waitForFxEvents();

        assertFalse(reasonBox.isVisible());

        ChoiceBox<String> diplomacyFilter = lookup("#diplomacyFilter").query();
        assertEquals("Attacker", diplomacyFilter.getValue());

        clickOn(diplomacyFilter);
        type(KeyCode.DOWN);
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);

        assertEquals("Peace", diplomacyFilter.getValue());

        clickOn("#declareWarButton");
        clickOn("#funTab");

        assertTrue(lookup("#declareWarButton1").query().isDisabled());
        clickOn("#warNameTextField").write("Krieg");
        assertFalse(lookup("#declareWarButton1").query().isDisabled());

        doReturn(Observable.just(new War("", "", "warID2", "testGameId", "testEmpireId", "empire2", "Hello War", Map.of("reason", "fun")))).when(warsApiService).createWar(any(), any());
        subjectWar.onNext(new Event<>("games.testGameId.wars.*.created", new War("", "", "", "testGameId", "testEmpireId", "empire2", "Krieg", Map.of("reason", "fun"))));
        clickOn("#declareWarButton1");

        clickOn(diplomacyFilter);
        type(KeyCode.UP);
        type(KeyCode.UP);
        type(KeyCode.ENTER);

    }

    @Test
    void testStopWar() {
        clickOn("#diplomacyButton");
        waitForFxEvents();
        //doReturn(Observable.just(new War("", "", "warId1", "testGameId", "testEmpireId", "empire1", "Hello War", Map.of("reason", "conquest").when(warsApiService).deleteWar(any(), "warId1"));
        doReturn(Observable.just(new War("", "", "warID1", "testGameId", "testEmpireId", "empire2", "Hello", Map.of("reason", "fun")))).when(warsApiService).deleteWar(any(), any());
        subjectWar.onNext(new Event<>("games.testGameId.wars.*.deleted", new War("", "", "warID1", "testGameId", "testEmpireId", "empire1", "Hello", Map.of("reason", "fun"))));
        waitForFxEvents();
        clickOn("#stopWarButton");

        ChoiceBox<String> diplomacyFilter = lookup("#diplomacyFilter").query();

        clickOn(diplomacyFilter);
        type(KeyCode.DOWN);
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);
    }

    @Test
    void testWarNotification() {
        subjectWar.onNext(new Event<>("games.testGameId.wars.*.created", new War("", "", "warID1", "testGameId", "empire2", "testEmpireId", "Hello War", Map.of("reason", "fun"))));
        waitForFxEvents();
        clickOn("#okButton");

        clickOn("#diplomacyButton");
        ChoiceBox<String> diplomacyFilter = lookup("#diplomacyFilter").query();
        assertEquals("Attacker", diplomacyFilter.getValue());

        clickOn(diplomacyFilter);
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);

        assertEquals("Defender", diplomacyFilter.getValue());
    }

    private void levelUpSuccess(String currentUpgradeState) {
        String levelUpKeyword;
        String nextLevelUpKeyword;
        String successKeyword;
        switch (currentUpgradeState) {
            case "unexplored" -> {
                levelUpKeyword = "Explore";
                nextLevelUpKeyword = "Colonize";
                successKeyword = "Explored";
            }
            case "explored" -> {
                levelUpKeyword = "Colonize";
                nextLevelUpKeyword = "Upgrade";
                successKeyword = "Colonized";
            }
            case "colonized" -> {
                levelUpKeyword = "Upgrade";
                nextLevelUpKeyword = "Develop";
                successKeyword = "Upgraded";
            }
            case "upgraded" -> {
                levelUpKeyword = "Develop";
                nextLevelUpKeyword = "Unknown";
                successKeyword = "Developed";
            }
            default -> {
                levelUpKeyword = "Unknown";
                nextLevelUpKeyword = "Unknown";
                successKeyword = "Unknown";
            }
        }

        setSystemUpgrade(currentUpgradeState);
        GameSystem improvedSystem = new GameSystem(
                "",
                "new",
                "testSystemId",
                "testGameId",
                "regular",
                "Kassel",
                100.0,
                new TreeMap<>(Map.of("city", 5, "industry", 7, "mining", 3)),
                new TreeMap<>(Map.of("city", 3, "industry", 3, "mining", 3)),
                40,
                this.system.buildings(),
                successKeyword.toLowerCase(),
                80,
                this.system.links(),
                0,
                0,
                "testEmpireId",
                null
        );

        this.showCastleView();

        waitForFxEvents();

        doReturn(Observable.just(improvedSystem)).when(gameSystemsApiService).updateSystem(eq("testGameId"), eq("testSystemId"), Mockito.any());
        CreateJobDto createJobDto = new CreateJobDto(this.system._id(), 0, "upgrade", null, null, null, null, null, null);
        doReturn(Observable.just(getUpgradeJob())).when(jobsApiService).createJob(this.game._id(), this.empire._id(), createJobDto);

        doReturn("testUserId").when(tokenStorage).getUserId();

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the castle view.
        // Leveling up the castle costs 100 alloys and 200 consumer goods.
        // He has enough resources for the transaction.
        checkExploreCastleComponentInfo(levelUpKeyword);
        checkIfLevelUpAllowed(true);

        // Action:
        // Jan wants to level up the selected castle.
        // He hovers over the button to see more information about the transaction.
        // He then clicks the button.
        moveBy(1, 1);
        moveTo("#exploreButton");
        waitForFxEvents();

        clickOn(MouseButton.PRIMARY);
        waitForFxEvents();
        subjectSystem.onNext(new Event<>("games.testGameId.systems.testSystemId.updated", improvedSystem));
        Empire improvedEmpire = getImprovedEmpire();
        subjectEmpire.onNext(new Event<>("games.testGameId.empires.testEmpireId.updated", improvedEmpire));
        waitForFxEvents();

        // Result:
        // Jan has successfully leveled up the castle. He sees a success notification.
        // The resources have been deducted from his account.
        // He has received the bonuses for leveling up the castle.


        if (improvedSystem.upgrade().equals("developed")) {
            // If the castle has been developed, there are no more level ups left. The component should be gone
            assertThrows(EmptyNodeQueryException.class, () -> lookup("#exploreCastleRoot").query());
        } else {
            // Otherwise, the component should be updated to show the next possible level up
            checkExploreCastleComponentInfo(nextLevelUpKeyword);
        }
    }

    private Job getUpgradeJob() {
        return new Job(
                "",
                "",
                "testJobId",
                0,
                0,
                this.game._id(),
                this.empire._id(),
                this.system._id(),
                0,
                "upgrade",
                null,
                null,
                null,
                null,
                null,
                null,
                this.systemUpgrades.upgraded().cost(),
                null
        );
    }

    private void levelUpFailure(String currentUpgradeState) {
        String levelUpKeyword = switch (currentUpgradeState) {
            case "unexplored" -> "Explore";
            case "explored" -> "Colonize";
            case "colonized" -> "Upgrade";
            case "upgraded" -> "Develop";
            default -> "Unknown";
        };

        setSystemUpgrade(currentUpgradeState);
        deleteResources();

        this.showCastleView();

        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He is currently in the castle view.
        // Leveling up the castle costs 100 alloys and 200 consumer goods.
        // He does not have enough resources for the transaction.
        checkExploreCastleComponentInfo(levelUpKeyword);
        checkIfLevelUpAllowed(false);

        // Action:
        // Jan wants to level up the selected castle.
        // He hovers over the button to see more information on the transaction, but no information appears.
        // He then tries to click the button.
        moveTo("#exploreButton");
        waitForFxEvents();

        assertThrows(EmptyNodeQueryException.class, () -> lookup("#exploreTooltipBox").query());

        clickOn(MouseButton.PRIMARY);
        waitForFxEvents();

        // Result:
        // Jan has not successfully leveled up the castle. He sees no success notification.
        // He sees an error message.
        // The resources have not been deducted from his account.
        // Jan has not received any bonuses.
        // The explore castle component has not changed.
        assertThrows(EmptyNodeQueryException.class, () -> lookup("#successNotificationImage").query());
        verifyThat("#errorLabel", LabeledMatchers.hasText("Not enough resources."));
        checkExploreCastleComponentInfo(levelUpKeyword);
        checkIfLevelUpAllowed(false);
    }


    private void deleteResources() {
        TreeMap<String, Integer> resources = new TreeMap<>();
        this.empire = new Empire(
                "",
                "",
                "testEmpireId",
                "testGameId",
                "",
                "",
                "",
                "",
                0,
                0,
                "",
                List.of(),
                resources,
                List.of("society"),
                new ArrayList<>(),
                _private,
                null
        );
    }

    private void checkExploreCastleComponentInfo(String levelUpKeyword) {
        verifyThat("#exploreLabel", LabeledMatchers.hasText(levelUpKeyword + " Castle"));
        verifyThat("#exploreButton", LabeledMatchers.hasText(levelUpKeyword));

        waitForFxEvents();

        HBox costBox = lookup("#costBox").query();
        assertEquals(2, costBox.getChildren().size());

        HBox alloysBox = (HBox) costBox.getChildren().get(1);
        Image energyImage = new Image(Objects.requireNonNull(
                App.class.getResource("image/game_resources/energy.png")).toString());
        assertEquals(energyImage.getUrl(), ((ImageView) alloysBox.getChildren().get(1)).getImage().getUrl());
        assertEquals("75", ((Label) alloysBox.getChildren().getFirst()).getText());
    }

    private void checkIfLevelUpAllowed(boolean levelUpPossible) {
        if (levelUpPossible) {
            assertFalse(lookup("#exploreButton").query().isDisabled());
            assertFalse(lookup("#errorLabel").query().isVisible());
        } else {
            assertTrue(lookup("#exploreButton").query().isDisabled());
            assertTrue(lookup("#errorLabel").query().isVisible());
        }
    }

    private Empire getImprovedEmpire() {
        TreeMap<String, Integer> updatedResources = new TreeMap<>(Map.of(
                "alloys", 900,
                "consumer_goods", 800,
                "credits", 1000,
                "energy", 1000,
                "food", 1000,
                "fuel", 1000,
                "minerals", 1000,
                "population", 1000,
                "research", 1000));
        return new Empire(
                "",
                "",
                "testEmpireId",
                "testGameId",
                "",
                "",
                "",
                "",
                0,
                0,
                "",
                List.of(),
                updatedResources,
                List.of("society"),
                new ArrayList<>(),
                _private,
                null
        );
    }

    private void initAllBuildings() {
        exchange = new Building(
                "exchange",
                new TreeMap<>(Map.of("credits", 1)), 0, 0, 0, new TreeMap<>(Map.of("minerals", 5)), new TreeMap<>(Map.of("energy", 5,
                "consumer_goods", 5)), 0
        );

        power_plant = new Building(
                "power_plant",
                new TreeMap<>(Map.of("energy", 20)), 0, 0, 0, new TreeMap<>(Map.of("minerals", 75)), new TreeMap<>(Map.of("minerals", 2)), 0
        );

        mine = new Building(
                "mine",
                new TreeMap<>(Map.of("minerals", 24)), 0, 0, 0, new TreeMap<>(Map.of("minerals", 75,
                "energy", 25)), new TreeMap<>(Map.of("energy", 2,
                "fuel", 2)), 0
        );

        farm = new Building(
                "farm",
                new TreeMap<>(Map.of("food", 24)), 0, 0, 0, new TreeMap<>(Map.of("energy", 75)), new TreeMap<>(Map.of("energy", 2,
                "fuel", 2)), 0
        );

        research_lab = new Building(
                "research_lab",
                new TreeMap<>(Map.of("credits", 1)), 0, 0, 0, new TreeMap<>(Map.of("minerals", 2000)), new TreeMap<>(Map.of("research", 5)), 0
        );

        foundry = new Building(
                "foundry",
                new TreeMap<>(Map.of("alloys", 10)), 0, 0, 0, new TreeMap<>(Map.of("minerals", 100)), new TreeMap<>(Map.of("minerals", 15,
                "energy", 10)), 0
        );

        factory = new Building(
                "factory",
                new TreeMap<>(Map.of("consumer_goods", 10)), 0, 0, 0, new TreeMap<>(Map.of("minerals", 100)), new TreeMap<>(Map.of("minerals", 15,
                "energy", 10)), 0
        );

        refinery = new Building(
                "refinery",
                new TreeMap<>(Map.of("fuel", 10)), 0, 0, 0, new TreeMap<>(Map.of("minerals", 100)), new TreeMap<>(Map.of("minerals", 10,
                "energy", 15)), 0
        );

        shipyard = new Building(
                "shipyard",
                new TreeMap<>(), 0, 0, 0.1, new TreeMap<>(Map.of("minerals", 50, "alloys", 75)), new TreeMap<>(Map.of("minerals", 10, "energy", 5, "fuel", 5, "alloys", 10)), 0
        );

        fortress = new Building(
                "fortress",
                new TreeMap<>(), 100, 100, 0, new TreeMap<>(Map.of("minerals", 75, "alloys", 75)), new TreeMap<>(Map.of("minerals", 5, "energy", 8, "fuel", 8, "alloys", 5)), 0
        );
    }

    private void initAllAggregates() {
        aggregateResult = new AggregateResult(153, List.of(
                new AggregateItem("resources.energy.periodic", 1, 153),
                new AggregateItem("resources.minerals.periodic", 1, 131),
                new AggregateItem("resources.food.periodic", 1, 34),
                new AggregateItem("resources.fuel.periodic", 1, 55),
                new AggregateItem("resources.research.periodic", 1, 12),
                new AggregateItem("resources.credits.periodic", 1, 10),
                new AggregateItem("resources.alloys.periodic", 1, 38),
                new AggregateItem("resources.population.periodic", 1, 54),
                new AggregateItem("resources.consumer_goods.periodic", 1, 119)
        ));
    }

    private UpdateSystemDto getDeleteBuildingUpdateSystemDto() {
        return new UpdateSystemDto(
                null,
                null,
                List.of("power_plant", "mine"),
                null,
                this.updatedDeleteBuildingSystem.owner(),
                null
        );
    }

    private GameSystem getDeleteBuildingUpdatedSystem() {
        return new GameSystem(
                "",
                "",
                "testSystemId",
                "testGameId",
                "regular",
                "",
                0.0,
                new TreeMap<>(Map.of("city", 5, "industry", 7, "mining", 3)),
                new TreeMap<>(Map.of("city", 3, "industry", 3, "mining", 3)),
                40,
                List.of("power_plant", "mine"),
                this.system.upgrade(),
                80,
                null,
                0,
                0,
                "testEmpireId",
                null
        );
    }

    private GameSystem getBuildBuildingUpdateSystem() {
        return new GameSystem(
                "",
                "",
                "testSystemId",
                "testGameId",
                "regular",
                "testSystemId",
                0.0,
                new TreeMap<>(Map.of("city", 5, "industry", 7, "mining", 3)),
                new TreeMap<>(Map.of("city", 3, "industry", 3, "mining", 3)),
                40,
                List.of("exchange", "power_plant", "mine", "farm"),
                this.system.upgrade(),
                80,
                null,
                0,
                0,
                "testEmpireId",
                null
        );
    }

    private List<District> createDistricts() {
        List<District> districtList = new ArrayList<>();
        districtList.add(new District(
                "city",
                new TreeMap<>(Map.of("credits", 12)), null, new TreeMap<>(Map.of("minerals", 100)), new TreeMap<>(Map.of("energy", 5, "consumer_goods", 2)), 0
        ));
        districtList.add(new District(
                "mining",
                new TreeMap<>(Map.of("minerals", 24)), null, new TreeMap<>(Map.of("minerals", 50, "energy", 25)), new TreeMap<>(Map.of("energy", 4, "fuel", 2)), 0
        ));
        districtList.add(new District(
                "industry",
                new TreeMap<>(Map.of("alloys", 5, "consumer_goods", 5, "fuel", 5)), null, new TreeMap<>(Map.of("minerals", 100)), new TreeMap<>(Map.of("energy", 6, "minerals", 8)), 0
        ));
        return districtList;
    }


    private GameSystem createSystemFour() {

        return new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyFour",
                "testGameId",
                "typeDummyValue",
                "nameDummyValue",
                0.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                Collections.emptyList(),
                "upgradeDummyValue",
                1000,
                Map.of(
                        "_idDummyOne", 6.5d
                ),
                -25,
                -25,
                "testEmpireId",
                new HashMap<>()
        );
    }

    private void initParams() {
        this.game = new Game(
                null,
                null,
                "testGameId",
                "Neuschwanstein",
                null,
                1,
                5,
                false,
                1,
                1,
                null,
                new GameSettings(50));

        TreeMap<String, Integer> resources = new TreeMap<>(Map.of(
                "alloys", 1000,
                "consumer_goods", 1000,
                "credits", 1000,
                "energy", 1000,
                "food", 1000,
                "fuel", 1000,
                "minerals", 1000,
                "population", 1000,
                "research", 1000));

        this.empire = new Empire(
                "",
                "",
                "testEmpireId",
                "testGameId",
                "user0",
                "",
                "",
                "0080ff",
                1,
                1,
                "",
                List.of(),
                resources,
                List.of("society"),
                new ArrayList<>(),
                _private,
                null
        );
        this.system = new GameSystem(
                "",
                "",
                "testSystemId",
                "testGameId",
                "regular",
                "",
                0.0,
                new TreeMap<>(Map.of("city", 5, "industry", 7, "mining", 3)),
                new TreeMap<>(Map.of("city", 3, "industry", 3, "mining", 3)),
                40,
                List.of("exchange", "power_plant", "mine"),
                "",
                80,
                null,
                0,
                0,
                "testEmpireId",
                null
        );

        setSystemUpgrade("unexplored");
        TreeMap<String, Integer> cost = new TreeMap<>(Map.of("alloys", 100, "consumer_goods", 200));
        TreeMap<String, Integer> upkeep = new TreeMap<>(Map.of("fuel", 1, "research", 3));
        SystemUpgrade upgrade = new SystemUpgrade(
                "",
                "",
                2.0, 1.1, cost, upkeep, 0
        );
        this.systemUpgrades = new SystemUpgradesResult(
                upgrade, upgrade, upgrade, upgrade, upgrade
        );
        List<Technology> enhancements = createTechnologies();

        final TreeMap<String, Integer> size = new TreeMap<>(Map.of("explorer", 2, "fighter", 3));
        this.troop = new Fleet(
                null,
                null,
                "testTroopId",
                game._id(),
                empire._id(),
                "My Troop",
                "_idDummyOne",
                size,
                null,
                null,
                null
        );
    }

    private void setSystemUpgrade(String upgrade) {
        this.system = new GameSystem(
                "",
                "",
                "testSystemId",
                "testGameId",
                "regular",
                "Kassel",
                100.0,
                new TreeMap<>(Map.of("city", 5, "industry", 7, "mining", 3)),
                new TreeMap<>(Map.of("city", 3, "industry", 3, "mining", 3)),
                100,
                List.of("exchange", "power_plant", "mine"),
                upgrade,
                80,
                Map.of(
                        "_idDummyTwo", 6.5d,
                        "_idDummyThree", 8.5d
                ),
                0,
                0,
                "testEmpireId",
                null
        );
    }

    private List<Member> createMembers() {
        EmpireTemplate empire0 = new EmpireTemplate("", "", "#0080ff", 1, 1, List.of(), List.of(), null, _private, null);
        EmpireTemplate empire1 = new EmpireTemplate("", "", "#000000", 0, 0, List.of(), List.of(), null, _private, null);
        EmpireTemplate empire2 = new EmpireTemplate("", "", "#FFFFFF", 2, 2, List.of(), List.of(), null, _private, null);
        EmpireTemplate empire3 = new EmpireTemplate("", "", "#FF0000", 3, 3, List.of(), List.of(), null, _private, null);

        return List.of(
                new Member("", "", "", "user0", true, empire0),
                new Member("", "", "", "user1", true, empire1),
                new Member("", "", "", "user2", true, empire2),
                new Member("", "", "", "testEmpireId2", true, empire3)
        );
    }

    private List<User> createUsers() {
        return List.of(
                new User("", "", "user0", "Jan", "0"),
                new User("", "", "user1", "Peter", "1"),
                new User("", "", "user2", "Hans", "2"),
                new User("", "", "testEmpireId2", "Tim", "3")
        );
    }

    private List<AggregateItem> createList() {
        ArrayList<AggregateItem> aggregates = new ArrayList<>();
        aggregates.add(new AggregateItem("resources.credits.periodic", 1, 2));
        aggregates.add(new AggregateItem("resources.population.periodic", 1, 3));
        aggregates.add(new AggregateItem("resources.energy.periodic", 1, 5));
        aggregates.add(new AggregateItem("resources.minerals.periodic", 1, 1));
        aggregates.add(new AggregateItem("resources.food.periodic", 1, 2));
        aggregates.add(new AggregateItem("resources.fuel.periodic", 1, 1));
        aggregates.add(new AggregateItem("resources.research.periodic", 1, 2));
        aggregates.add(new AggregateItem("resources.alloys.periodic", 1, 1));
        aggregates.add(new AggregateItem("resources.consumer_goods.periodic", 1, 6));

        return aggregates;
    }

    private List<Technology> createTechnologies() {
        Effect effectOne = new Effect("technologies.society.cost_multiplier", 0, 0.95, 0);
        Effect effectTwo = new Effect("technologies.society.time_multiplier", 0, 0.9, 0);
        Effect effectThree = new Effect("empire.pop.colonists", 2, 0, 0);
        Effect effectFour = new Effect("technologies.physics.cost_multiplier", 0, 0.95, 0);
        Effect effectFive = new Effect("technologies.physics.time_multiplier", 0, 0.9, 0);
        Technology dummyTechnologyOne = new Technology(
                "society",
                List.of("society"),
                200,
                null,
                null,
                List.of(effectOne, effectTwo));
        Technology dummyTechnologyTwo = new Technology(
                "demographic",
                List.of("society"),
                100,
                List.of("society"),
                null,
                List.of(effectOne, effectTwo));
        Technology dummyTechnologyThree = new Technology(
                "more_colonists_1",
                List.of("society", "biology"),
                2,
                List.of("demographic"),
                List.of("more_colonists_2"),
                List.of(effectThree));
        Technology dummyTechnologyFour = new Technology(
                "research_site_production_1",
                List.of("physics", "computing"),
                2,
                null,
                null,
                List.of(effectFour, effectFive));
        return List.of(
                dummyTechnologyOne,
                dummyTechnologyTwo,
                dummyTechnologyThree,
                dummyTechnologyFour
        );
    }

    private List<ShipType> createUnitTypes() {
        return List.of(
                new ShipType("explorer", new TreeMap<>(Map.of("fuel", 2d, "energy", 0.4)), 100, 5, null, new TreeMap<>(Map.of("default", 10)), new TreeMap<>(Map.of("alloys", 75d, "energy", 20d)), 4),
                new ShipType("fighter", new TreeMap<>(Map.of("fuel", 2.5, "energy", 0.5)), 100, 10, new TreeMap<>(Map.of("default", 10, "interceptor", 30, "corvette", 50, "bomber", 50, "frigate", 50)), new TreeMap<>(Map.of("default", 10)), new TreeMap<>(Map.of("alloys", 75d, "energy", 50d)), 4),
                new ShipType("interceptor", new TreeMap<>(Map.of("fuel", 2d, "energy", 0.4)), 100, 10, new TreeMap<>(Map.of("default", 10, "fighter", 30, "corvette", 30, "bomber", 30, "frigate", 30)), new TreeMap<>(Map.of("default", 10)), new TreeMap<>(Map.of("alloys", 75d, "energy", 50d)), 4)
        );
    }

    private List<Job> createNonTechJobs() {
        Job dummyJobOne = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId1",
                3,
                6,
                "testGameId",
                "testEmpireId",
                "_idDummyOne",
                0,
                "building",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                new TreeMap<>(),
                Map.of("minerals", 100, "energy", 25)
        );
        Job dummyJobTwo = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId2",
                0,
                6,
                "testGameId",
                "testEmpireId",
                "_idDummyTwo",
                0,
                "building",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                new TreeMap<>(),
                Map.of("minerals", 100, "energy", 25)
        );
        return getJobs("_idDummyTwo", dummyJobOne, dummyJobTwo);

    }

    private List<Job> getJobs(String testSystemId3, Job dummyJobOne, Job dummyJobTwo) {
        Job dummyJobThree = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId3",
                0,
                6,
                "testGameId",
                "testEmpireId",
                testSystemId3,
                0,
                "building",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                new TreeMap<>(),
                Map.of("minerals", 100, "energy", 25)
        );

        List<Job> jobList = new ArrayList<>();
        jobList.add(dummyJobOne);
        jobList.add(dummyJobTwo);
        jobList.add(dummyJobThree);
        return jobList;
    }

    private List<Job> createJobs() {
        Job dummyJobOne = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId1",
                3,
                6,
                "testGameId",
                "testEmpireId",
                "_idDummyTwo",
                0,
                "building",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                new TreeMap<>(),
                Map.of("minerals", 100, "energy", 25)
        );
        Job dummyJobTwo = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId2",
                2,
                6,
                "testGameId",
                "testEmpireId",
                "_idDummyTwo",
                0,
                "building",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                new TreeMap<>(),
                Map.of("minerals", 100, "energy", 25)
        );
        List<Job> jobList = getJobs("Rotenburg", dummyJobOne, dummyJobTwo);
        jobList.addAll(createEnhancementsJobs());
        return jobList;
    }

    private ObservableList<Job> createEnhancementsJobs() {
        Job dummyJobOne = getJob(100);
        ObservableList<Job> jobList = FXCollections.observableArrayList();
        jobList.add(dummyJobOne);
        return jobList;
    }

    private List<Job> createTechJobs() {
        Job dummyJobOne = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId1",
                3,
                6,
                "testGameId",
                "testEmpireId",
                "_idDummyOne",
                0,
                "technology",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                new TreeMap<>(),
                Map.of("minerals", 100, "energy", 25)
        );
        Job dummyJobTwo = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId2",
                0,
                6,
                "testGameId",
                "testEmpireId",
                "_idDummyTwo",
                0,
                "",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                new TreeMap<>(),
                Map.of("minerals", 100, "energy", 25)
        );
        Job dummyJobThree = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId3",
                0,
                6,
                "testGameId",
                "testEmpireId",
                "testSystemId3",
                0,
                "",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                new TreeMap<>(),
                Map.of("minerals", 100, "energy", 25)
        );
        Job dummyJobFour = getJob(200);
        return List.of(
                dummyJobOne,
                dummyJobTwo,
                dummyJobThree,
                dummyJobFour
        );
    }

    private Job getJob(int v1) {
        TreeMap<String, Integer> cost = new TreeMap<>(Map.of("research", v1));
        return new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId4",
                3,
                6,
                "testGameId",
                "testEmpireId",
                "_idDummyOne",
                0,
                "technology",
                null,
                null,
                "demographic",
                null,
                null,
                null,
                cost,
                null
        );
    }

    private List<GameSystem> createSystems() {
        GameSystem dummySystemOne = new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyOne",
                "testGameId",
                "typeDummyValue",
                "Bellevue",
                100.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                List.of("shipyard"),
                "upgradeDummyValue",
                1000,
                Map.of(
                        "_idDummyTwo", 6.5d,
                        "_idDummyThree", 8.5d
                ),
                10,
                10,
                "testEmpireId",
                new HashMap<>()
        );

        GameSystem dummySystemTwo = new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyTwo",
                "testGameId",
                "typeDummyValue",
                "Rotenburg",
                100.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                Collections.emptyList(),
                "upgradeDummyValue",
                1000,
                Map.of(
                        "_idDummyOne", 6.5d
                ),
                10,
                10,
                "testEmpireId",
                new HashMap<>()
        );

        GameSystem dummySystemThree = new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyThree",
                "testGameId",
                "typeDummyValue",
                "Neuschwanstein",
                100.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                Collections.emptyList(),
                "upgradeDummyValue",
                1000,
                Map.of(
                        "_idDummyOne", 6.5d
                ),
                10,
                10,
                "testEmpireId",
                new HashMap<>()
        );

        return List.of(
                dummySystemOne,
                dummySystemTwo,
                dummySystemThree
        );
    }

    private void showCastleView() {
        Platform.runLater(() -> {
            final CastleViewComponent castleView = app.initAndRender(
                    castleViewComponentProvider.get(),
                    Map.of("game", game, "system", system, "empire", empire, "sideBar", ingameController.rightSideBarVbox, "sideButtons", ingameController.sideButtons, "troopsListContainer", ingameController.troopsListContainer),
                    subscriber
            );
            this.ingameController.ingameAnchorPane.getChildren().add(castleView);
        });
    }

    private void showTroopView() {
        interact(() -> {
            final TroopViewComponent troopView = app.initAndRender(
                    troopViewComponentProvider.get(),
                    Map.of(
                            "troop", troop, "parent", ingameController.ingameAnchorPane, "systems", FXCollections.observableArrayList(systems),
                            "sideBar", ingameController.rightSideBarVbox, "sideButtons", ingameController.sideButtons, "troopsList", ingameController.troopsListContainer,
                            "empire", empire
                    ),
                    subscriber
            );
            this.ingameController.ingameAnchorPane.getChildren().add(troopView);
        });
    }

    private void showMarketView() {
        Platform.runLater(() -> {
            final MarketComponent marketView = app.initAndRender(
                    marketComponentProvider.get(),
                    Map.of("game", game, "empire", empire), subscriber);
            this.ingameController.sideButtons.getChildren().removeLast();
            this.ingameController.sideButtons.getChildren().add(marketView);
        });
    }

    private void mockMarketApiCall() {
        ExplainedVariable explainedVariable = new ExplainedVariable(
                "empire.market.fee",
                0.30d,
                List.of(),
                0.30d
        );
        doReturn(Observable.just(explainedVariable)).when(gameLogicApiService).getExplainedVariable(any(), any(), eq("empire.market.fee"));

        Resource resource = new Resource(
                100,
                2
        );

        ResourcesResult resourcesResult = new ResourcesResult(
                resource,
                resource,
                resource,
                resource,
                resource,
                resource,
                resource,
                resource,
                resource
        );
        doReturn(Observable.just(resourcesResult)).when(presetsApiService).getResources();
    }

    private void mockFleetsListApiCall() {
        TreeMap<String, Integer> ships = new TreeMap<>();
        ships.put("explorer", 1);
        ships.put("colonizer", 1);

        Fleet fleet1 = new Fleet("", "", "fleet1ID", "testGameId", "testEmpireId", "X-Wing", "testSystemId", ships, new HashMap<>(), new HashMap<>(), new ArrayList<>());
        Fleet fleet2 = new Fleet("", "", "fleet2ID", "testGameId", "testEmpireId", "T-Fighter", "", new TreeMap<>(), new HashMap<>(), new HashMap<>(), new ArrayList<>());
        List<Fleet> fleets = List.of(fleet1, fleet2);
        doReturn(Observable.just(fleets)).when(fleetsApiService).getFleets(any(), any());
        doReturn(subjectFleet).when(eventListener).listen("games.null.fleets.*.*", Fleet.class);
    }

    private List<War> getWars() {
        War war = new War("", "", "warID1", "testGameId", "testEmpireId", "empire1", "Hello War", Map.of("reason", "conquest"));
        return List.of(
                war
        );
    }

    private List<ReadShipDto> getUnits() {
        return List.of(
                new ReadShipDto(null, null, "testUnitId", "testGameId", "testEmpireId", "testSystemId", "explorer", 20, 0, null),
                new ReadShipDto(null, null, "testUnitId2", "testGameId", "testEmpireId", "testSystemId2", "fighter", 30, 0, null)
        );
    }

    private List<ReadEmpireDto> mockEmpires() {
        return List.of(
                new ReadEmpireDto(null, null, "testEmpireId", "testGameId", "user0", null, null, "#0080ff", 1, 1, null, null, null),
                new ReadEmpireDto(null, null, "empire1", "testGameId", "user1", null, null, "#000000", 0, 0, null, null, null),
                new ReadEmpireDto(null, null, "empire2", "testGameId", "user2", null, null, "#FFFFFF", 2, 2, null, null, null),
                new ReadEmpireDto(null, null, "testEmpireId2", "testGameId", "testEmpireId2", null, null, "#008000", 0, 4, null, null, null)

        );
    }

    private List<ShipType> createShipsTypes() {
        ShipType explorer = new ShipType(
                "explorer",
                null, 2.0, 2.0, null, null, null, 6
        );
        ShipType colonizer = new ShipType(
                "colonizer",
                null, 2.0, 2.0, null, null, null, 6
        );
        ShipType interceptor = new ShipType(
                "interceptor",
                null, 2.0, 2.0, null, null, null, 6
        );
        ShipType fighter = new ShipType(
                "fighter",
                null, 2.0, 2.0, null, null, null, 6
        );
        ShipType corvette = new ShipType(
                "corvette",
                null, 2.0, 2.0, null, null, null, 6
        );
        return List.of(explorer, colonizer, interceptor, fighter, corvette);
    }

    private GameSystem getUpdatedSystemOwner() {
        return new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyOne",
                "gameDummyValue",
                "typeDummyValue",
                "Bellevue",
                100.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                List.of("shipyard"),
                "upgradeDummyValue",
                1000,
                Map.of(
                        "_idDummyTwo", 6.5d,
                        "_idDummyThree", 8.5d
                ),
                10,
                10,
                "testEmpireId2",
                new HashMap<>()
        );
    }

    private GameSystem getUpdatedSystemOwner2() {
        return new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyTwo",
                "testGameId",
                "typeDummyValue",
                "Rotenburg",
                100.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                Collections.emptyList(),
                "upgradeDummyValue",
                1000,
                Map.of(
                        "_idDummyOne", 6.5d
                ),
                10,
                10,
                "testEmpireId2",
                new HashMap<>()
        );
    }

    private GameSystem getUpdatedSystemOwner3() {
        return new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyThree",
                "testGameId",
                "typeDummyValue",
                "Neuschwanstein",
                100.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                Collections.emptyList(),
                "upgradeDummyValue",
                1000,
                Map.of(
                        "_idDummyOne", 6.5d
                ),
                10,
                10,
                "testEmpireId2",
                new HashMap<>()
        );
    }

}
