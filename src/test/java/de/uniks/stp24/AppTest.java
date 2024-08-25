package de.uniks.stp24;

import de.uniks.stp24.component.ZoomDragComponent;
import de.uniks.stp24.component.enhancements.EnhancementItemComponent;
import de.uniks.stp24.dto.*;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.*;
import de.uniks.stp24.service.*;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.fulib.fx.controller.Subscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.service.query.EmptyNodeQueryException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public class AppTest extends ApplicationTest {

    @Spy
    public App app = new App();

    @Spy
    public ResourceBundle bundle = ResourceBundle.getBundle("de/uniks/stp24/lang/lang", Locale.ENGLISH);

    protected Stage stage;
    protected TestComponent testComponent;
    private GameMembersApiService gameMembersApiService;
    private GameEmpiresApiService gameEmpiresApiService;
    private FleetsApiService fleetsApiService;
    private ShipsApiService shipsApiService;
    private JobsApiService jobsApiService;
    private WarsApiService warsApiService;
    private Subject<Event<Member>> subjectMember;
    private Subject<Event<GameSystem>> subjectSystem;
    private Subject<Event<Empire>> subjectEmpire;
    private Subject<Event<Job>> subjectJob;
    private Subject<Event<Fleet>> subjectFleet;
    private Subject<Event<Ship>> subjectShip;
    private Subject<Event<War>> subjectWar;
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
    private SystemUpgradesResult systemUpgrades;
    private final List<Job> jobs = createJobs();
    final Map<String, Object> _private = new HashMap<>();
    private EventService eventService = new EventService();

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        this.stage = stage;
        testComponent = (TestComponent) DaggerTestComponent.builder().mainApp(app).build();
        app.setComponent(testComponent);
        stage.setX(0);
        stage.setY(0);
        stage.requestFocus();
        testComponent.prefService().setLocale(Locale.ENGLISH);
        app.start(stage);
    }

    @BeforeEach
    public void setup() {
        final AuthApiService authApiService = testComponent.authApiService();
        final UsersApiService usersApiService = testComponent.usersApiService();
        final GamesApiService gamesApiService = testComponent.gamesApiService();
        final EventListener eventListener = testComponent.eventListener();
        final GameSystemsApiService gameSystemsApiService = testComponent.gameSystemsApiService();
        final FriendsApiService friendsApiService = testComponent.friendsApiService();
        final GameLogicApiService gameLogicApiService = testComponent.gameLogicApiService();
        final PresetsApiService presetsApiService = testComponent.presetsApiService();
        final GameTicksService gameTicksService = testComponent.gameTicksService();
        final JobService jobService = testComponent.jobService();
        final MapService mapService = testComponent.mapService();
        eventService = testComponent.eventService();

        final TokenStorage tokenStorage = Mockito.mock(TokenStorage.class);
        final Subscriber subscriber = Mockito.mock(Subscriber.class);
        final DiscordActivityService discordActivityService = Mockito.mock(DiscordActivityService.class);

        this.warsApiService = testComponent.warsApiService();
        this.gameMembersApiService = testComponent.gameMembersApiService();
        this.gameEmpiresApiService = testComponent.gameEmpiresApiService();
        this.fleetsApiService = testComponent.fleetsApiService();
        this.shipsApiService = testComponent.shipsApiService();
        this.jobsApiService = testComponent.jobsApiService();


        // Mock Discord away
        doNothing().when(discordActivityService).runCallbacks();

        // Login TestUser
        doReturn(Observable.just(new LoginResult("", "", "TestUser", "TestUser", null, "a", "r")))
                .when(authApiService).login(new LoginDto("TestUser", "Password"));
        doReturn(Observable.just(new LoginResult("", "", "TestUser", "TestUser", null, "a", "r")))
                .when(authApiService).refresh(new RefreshDto("r"));


        // Signup TestUser
        doReturn(Observable.just(new User("", "", "TestUser", "TestUser", "")))
                .when(usersApiService).createUser(new CreateUserDto("TestUser", "Password"));

        // Members
        doReturn(Observable.just(new Member("", "", "2", "TestUser", false, null))).when(gameMembersApiService).updateMember(anyString(), anyString(), any());

        doReturn(Observable.just(List.of(
                new Member("", "", "2", "User 2", true, null),
                new Member("", "", "2", "User 3", true, null)
        ))).when(gameMembersApiService).getMembersOfGame("2");

        doReturn(Observable.just(List.of(new User("", "", "TestUser", "TestUser", null))))
                .when(usersApiService).getUsers();

        when(usersApiService.getUser(Mockito.anyString())).thenAnswer(invocation -> {
            final String userId = invocation.getArgument(0);
            return Observable.just(new User("", "", userId, userId, null));
        });

        // Game List
        doReturn(Observable.just(List.of(
                new Game("", "", "1", "Game 1", "1", 1, 5, false, 1, 33043, "", new GameSettings(50)),
                new Game("", "", "2", "Game 2", "User 2", 1, 5, false, 1, 33043, "", new GameSettings(50)),
                new Game("", "", "3", "Game 3", "3", 1, 5, false, 1, 33043, "", new GameSettings(50))
        ))).when(gamesApiService).getGames();
        doReturn(Observable.just(List.of())).when(friendsApiService).getFriends("TestUser");
        doReturn(Observable.just(List.of())).when(friendsApiService).getFriendsByStatus("TestUser", "requested");


        // Join Game
        List<String> traits = Arrays.asList("prepared", "strong", "intelligent");

        EmpireTemplate empireTemplate = new EmpireTemplate("New Empire", "", "FFFFFF", 0, 0, List.of(), List.of(), null, null, "regular");

        ExplainedVariable explainedVariable = new ExplainedVariable(
                "empire.market.fee",
                0.30d,
                List.of(),
                0.30d
        );
        doReturn(Observable.just(explainedVariable)).when(gameLogicApiService).getExplainedVariable(any(), any(), eq("empire.market.fee"));
        ExplainedVariableWithMapValues explainedVariableWithMapValues = new ExplainedVariableWithMapValues(
                null,
                new TreeMap<>(Map.of()),
                List.of(),
                new TreeMap<>(Map.of("energy", 50d))
        );
        doReturn(Observable.just(explainedVariableWithMapValues)).when(gameLogicApiService).getExplainedVariableWithMapValues(any(), any(), any());

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
        doReturn(Observable.just(createAvailableTraits())).when(presetsApiService).getTraits();
        doReturn(Observable.just(createSystemTypes())).when(presetsApiService).getSystemTypes();
        doReturn(Observable.just(new Member("", "", "Game 2", "TestUser", false, empireTemplate))).when(gameMembersApiService).joinGame("2", new CreateMemberDto(false, empireTemplate, "GamePassword"));
        doReturn(Observable.empty()).when(gameEmpiresApiService).updateEmpire(any(), any(), any());

        // dispose Subscriber
        doNothing().when(subscriber).dispose();
        doReturn("-> 20.07.2024").when(jobService).getJobEndDate(any());
        doNothing().when(gameTicksService).startGameTicks(any());

        // Members
        EmpireTemplate testEmpire = new EmpireTemplate("", "", "#000000", 0, 0, null, List.of(), null, null, null);
        EmpireTemplate empire4 = new EmpireTemplate("", "", "#888888", 1, 1, null, List.of(), null, null, null);
        EmpireTemplate empire5 = new EmpireTemplate("", "", "#FFFFFF", 2, 2, null, List.of(), null, null, null);
        Mockito.doReturn(Observable.just(List.of(
                new Member("", "", "OwnGameId", "TestUser", true, testEmpire),
                new Member("", "", "OwnGameId", "TestUser2", true, empire4),
                new Member("", "", "OwnGameId", "TestUser3", true, empire5)
        ))).when(gameMembersApiService).getMembersOfGame("OwnGameId");

        // Contacts
        doReturn(Observable.just(new AggregateResultCompare(0, List.of()))).when(gameLogicApiService).getAggregateCompare("OwnGameId", "4444", "empire.compare.military", "4444");
        doReturn(Observable.just(new AggregateResultCompare(0, List.of()))).when(gameLogicApiService).getAggregateCompare("OwnGameId", "4444", "empire.compare.economy", "4444");
        doReturn(Observable.just(new AggregateResultCompare(0, List.of()))).when(gameLogicApiService).getAggregateCompare("OwnGameId", "4444", "empire.compare.technology", "4444");

        doReturn(Observable.just(new AggregateResultCompare(-0.3, List.of()))).when(gameLogicApiService).getAggregateCompare("OwnGameId", "4444", "empire.compare.military", "5555");
        doReturn(Observable.just(new AggregateResultCompare(-5, List.of()))).when(gameLogicApiService).getAggregateCompare("OwnGameId", "4444", "empire.compare.economy", "5555");
        doReturn(Observable.just(new AggregateResultCompare(5, List.of()))).when(gameLogicApiService).getAggregateCompare("OwnGameId", "4444", "empire.compare.technology", "5555");

        doReturn(Observable.just(new AggregateResultCompare(0.3, List.of()))).when(gameLogicApiService).getAggregateCompare("OwnGameId", "4444", "empire.compare.military", "6666");
        doReturn(Observable.just(new AggregateResultCompare(0.3, List.of()))).when(gameLogicApiService).getAggregateCompare("OwnGameId", "4444", "empire.compare.economy", "6666");
        doReturn(Observable.just(new AggregateResultCompare(5, List.of()))).when(gameLogicApiService).getAggregateCompare("OwnGameId", "4444", "empire.compare.technology", "6666");


        // EventListener
        subjectMember = BehaviorSubject.create();
        subjectSystem = BehaviorSubject.create();
        subjectEmpire = BehaviorSubject.create();
        subjectJob = BehaviorSubject.create();
        subjectWar = BehaviorSubject.create();
        subjectFleet = BehaviorSubject.create();
        subjectShip = BehaviorSubject.create();

        final Subject<Event<Game>> subjectGame = BehaviorSubject.create();
        final Subject<Event<Empire>> subjectEmpire = BehaviorSubject.create();
        final Subject<Event<Friend>> friendSubject = BehaviorSubject.create();
        final Subject<Event<Friend>> friendRequestSubject = BehaviorSubject.create();


        doReturn(subjectGame).when(eventListener).listen("games.*.*", Game.class);
        doReturn(subjectGame).when(eventListener).listen("games.2.*", Game.class);
        doReturn(subjectMember).when(eventListener).listen("games.2.members.*.*", Member.class);
        doReturn(subjectGame).when(eventListener).listen("games.2.*", Game.class);
        doReturn(subjectGame).when(eventListener).listen("games.OwnGameId.*", Game.class);
        doReturn(subjectMember).when(eventListener).listen("games.OwnGameId.members.*.*", Member.class);
        doReturn(subjectGame).when(eventListener).listen("games.OwnGameId.*", Game.class);
        doReturn(subjectGame).when(eventListener).listen("games.OwnGameId.updated", Game.class);
        doReturn(subjectSystem).when(eventListener).listen("games.OwnGameId.systems.*.*", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.OwnGameId.systems._idDummyOne.updated", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.OwnGameId.systems._idDummyTwo.updated", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.OwnGameId.systems._idDummyThree.updated", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.OwnGameId.systems._idDummyFour.updated", GameSystem.class);
        doReturn(subjectSystem).when(eventListener).listen("games.OwnGameId.systems.*.updated", GameSystem.class);
        doReturn(friendSubject).when(eventListener).listen("users.TestUser.friends.*.*", Friend.class);
        doReturn(friendRequestSubject).when(eventListener).listen("users.*.friends.TestUser.*", Friend.class);
        doReturn(subjectEmpire).when(eventListener).listen("games.OwnGameId.empires.4444.updated", Empire.class);
        doReturn(subjectJob).when(eventListener).listen("games.OwnGameId.empires.4444.jobs.*.*", Job.class);
        doReturn(subjectJob).when(jobService).listenForJobEvent("games.OwnGameId.empires.4444.jobs.*.*");
        doReturn(this.subjectEmpire).when(eventListener).listen("games.OwnGameId.empires.4444.updated", Empire.class);
        doReturn(this.subjectEmpire).when(eventListener).listen("games.OwnGameId.empires.*.updated", Empire.class);
        doReturn(subjectWar).when(eventListener).listen("games.OwnGameId.wars.*.*", War.class);
        doReturn(subjectFleet).when(eventListener).listen("games.OwnGameId.fleets.*.*", Fleet.class);
        doReturn(subjectFleet).when(eventListener).listen("games.OwnGameId.fleets.testTroopId.*", Fleet.class);
        doReturn(subjectFleet).when(eventListener).listen("games.OwnGameId.fleets.testTroopId.updated", Fleet.class);
        doReturn(subjectShip).when(eventListener).listen("games.OwnGameId.fleets.testTroopId.ships.*.*", Ship.class);
        doReturn(subjectShip).when(eventListener).listen("games.OwnGameId.fleets.*.ships.*.*", Ship.class);

        // create
        EmpireTemplate ownEmpireTemp = new EmpireTemplate("Own Empire", "Test creating empire", "ffffffff", 1, 0, traits, List.of(), null, null, "regular");
        doReturn(Observable.just(new Member("", "", "Game 2", "", false, ownEmpireTemp)))
                .when(gameMembersApiService).updateMember(eq("2"), eq("TestUser"), any());

        // set Ready
        doReturn(Observable.just(new Member("", "", "Game 2", "TestUser", true, ownEmpireTemp)))
                .when(gameMembersApiService).getMember("2", "TestUser");

        // leave Game as Member
        Mockito.doReturn(Observable.just(new Member("", "", "testGameId", "1", false, null)))
                .when(gameMembersApiService).leaveGame("2", "TestUser");

        // Create Own Game
        CreateGameDto createGameDto = new CreateGameDto("OwnGame", 100, new GameSettings(50), "OwnGamePassword");
        Game game = new Game("", "", "OwnGameId", "OwnGame", "TestUser", 1, 5, false, 1, 33043, "", new GameSettings(50));
        doReturn(Observable.just(game)).when(gamesApiService).createGame(createGameDto);

        // start Game
        doReturn(Observable.just(new Game("", "", "OwnGameId", "OwnGame", "TestUser", 1, 5, false, 1, 33043, "", new GameSettings(50))))
                .when(gamesApiService).updateGame(eq("OwnGameId"), any());

        // Ingame
        doReturn(Observable.just(List.of(
                new User("", "", "TestUser", "Jan", ""),
                new User("", "", "TestUser2", "Peter", ""),
                new User("", "", "TestUser3", "Hans", "")
        ))).when(usersApiService).getUsersByIDs(anyList());

        GameSystem firstSystem = new GameSystem(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "_idDummyOne",
                "OwnGameId",
                "regular",
                "Castle 1",
                100.0,
                new TreeMap<>(),
                new TreeMap<>(),
                100,
                List.of("shipyard"),
                "unexplored",
                1000,
                Map.of(
                        "_idDummyTwo", 6.5d,
                        "_idDummyThree", 8.5d,
                        "_idDummyFour", 8.5d
                ),
                10,
                10,
                "4444",
                new HashMap<>()
        );
        doReturn(Observable.just(
                List.of(
                        firstSystem,
                        new GameSystem(
                                "createdAtDummyValue",
                                "updatedAtDummyValue",
                                "_idDummyTwo",
                                "OwnGameId",
                                "uninhabitable_0",
                                "Castle 2",
                                100.0,
                                new TreeMap<>(),
                                new TreeMap<>(),
                                100,
                                Collections.emptyList(),
                                "unexplored",
                                1000,
                                Map.of(
                                        "_idDummyOne", 6.5d
                                ),
                                -5,
                                25,
                                null,
                                new HashMap<>()
                        ),
                        new GameSystem(
                                "createdAtDummyValue",
                                "updatedAtDummyValue",
                                "_idDummyThree",
                                "OwnGameId",
                                "uninhabitable_0",
                                "Castle 3",
                                100.0,
                                new TreeMap<>(),
                                new TreeMap<>(),
                                100,
                                Collections.emptyList(),
                                "unexplored",
                                1000,
                                Map.of(
                                        "_idDummyOne", 6.5d
                                ),
                                -10,
                                20,
                                null,
                                new HashMap<>()
                        ),
                        new GameSystem(
                                "createdAtDummyValue",
                                "updatedAtDummyValue",
                                "_idDummyFour",
                                "OwnGameId",
                                "uninhabitable_0",
                                "Castle 4",
                                100.0,
                                new TreeMap<>(),
                                new TreeMap<>(),
                                100,
                                Collections.emptyList(),
                                "unexplored",
                                1000,
                                Map.of(
                                        "_idDummyOne", 6.5d
                                ),
                                20,
                                10,
                                null,
                                new HashMap<>()
                        )
                ))).when(gameSystemsApiService).getSystems("OwnGameId", null);

        doReturn(Observable.just(List.of())).when(gameSystemsApiService).getSystems("OwnGameId", "TestUser");

        doReturn(Observable.just(new Member("", "", "Game 2", "TestUser", true, ownEmpireTemp)))
                .when(gameMembersApiService).getMember("OwnGameId", "TestUser");

        String userId = "TestUser";
        doReturn(userId).when(tokenStorage).getUserId();
        doReturn(Observable.just(

                new ArrayList<>(List.of(new ReadEmpireDto(null, null, "4444", null, userId, null, null, "#000000", 1, 1, null, null, null),
                        new ReadEmpireDto(null, null, "5555", null, "TestUser2", null, null, "#888888", 1, 1, null, null, null),
                        new ReadEmpireDto(null, null, "6666", null, "TestUser3", null, null, "#FFFFFF", 1, 1, null, null, null)))))
                .when(gameEmpiresApiService).getEmpires("OwnGameId");
        TreeMap<String, Integer> resources = new TreeMap<>(Map.of("energy", 1000, "minerals", 2000, "alloys", 30, "food", 4000, "research", 5000, "fuel", 6000, "credits", 7000, "population", 8000, "consumer_goods", 9000));
        doReturn(Observable.just(new Empire("", "", "4444", null, userId, null, null, "#0080ff", 1, 1, null, List.of(), resources, List.of("society"), List.of(), _private, null)))
                .when(gameEmpiresApiService).getEmpire("OwnGameId", "4444");
        doReturn(subjectEmpire).when(eventListener).listen("games.OwnGameId.empires.4444", Empire.class);
        doReturn(Observable.just(new AggregateResult(153, List.of(
                new AggregateItem("resources.energy.periodic", 1, 153),
                new AggregateItem("resources.minerals.periodic", 1, 131),
                new AggregateItem("resources.food.periodic", 1, 34),
                new AggregateItem("resources.fuel.periodic", 1, 55),
                new AggregateItem("resources.research.periodic", 1, 12),
                new AggregateItem("resources.credits.periodic", 1, 10),
                new AggregateItem("resources.alloys.periodic", 1, 38),
                new AggregateItem("resources.population.periodic", 1, 119),
                new AggregateItem("resources.consumer_goods.periodic", 1, 119)
        )))).when(gameLogicApiService).getAggregate(eq("OwnGameId"), eq("4444"), eq("resources.periodic"), Mockito.anyMap());

        Mockito.doReturn(Observable.just(List.of(new ExplainedVariable(
                "districts.city.upkeep.energy",
                50,
                List.of(new EffectSource("mineral_production_1", List.of(new Effect("", 50, 1.1d, 20)))),
                75
        )))).when(gameLogicApiService).getExplainedVariables(any(), any(), any());

        doReturn(Observable.just(
                new Game("", "", "OwnGameId", "OwnGame", "TestUser", 1, 5, false, 0, 33043, "", new GameSettings(50))
        )).when(gamesApiService).updateSpeed("OwnGameId", Map.of("speed", 0));
        doReturn(Observable.just(
                new Game("", "", "OwnGameId", "OwnGame", "TestUser", 1, 5, false, 1, 33043, "", new GameSettings(50))
        )).when(gamesApiService).updateSpeed("OwnGameId", Map.of("speed", 1));
        doReturn(Observable.just(
                new Game("", "", "OwnGameId", "OwnGame", "TestUser", 1, 5, false, 2, 33043, "", new GameSettings(50))
        )).when(gamesApiService).updateSpeed("OwnGameId", Map.of("speed", 2));
        doReturn(Observable.just(
                new Game("", "", "OwnGameId", "OwnGame", "TestUser", 1, 5, false, 3, 33043, "", new GameSettings(50))
        )).when(gamesApiService).updateSpeed("OwnGameId", Map.of("speed", 3));

        // Troops list

        doReturn(subjectFleet).when(eventListener).listen("games.null.fleets.*.*", Fleet.class);
        doReturn(Observable.empty()).when(fleetsApiService).getFleets("OwnGameId", "4444");

        doReturn(Observable.empty()).when(fleetsApiService).getFleets("OwnGameId");

        // Castle view
        doReturn(Observable.just(List.of())).when(presetsApiService).getDistricts();
        initSystemUpgrades();
        doReturn(Observable.just(systemUpgrades)).when(presetsApiService).getSystemUpgrades();
        doReturn(Observable.just(firstSystem)).when(gameSystemsApiService).getSystem("OwnGameId", "_idDummyOne");
        initAllBuildings();
        doReturn(Observable.just(List.of(exchange, power_plant, mine, farm, research_lab, foundry, factory, refinery, shipyard, fortress))).when(presetsApiService).getBuildings();
        doReturn(Observable.just(getUpdatedSystem("explored"))).when(gameSystemsApiService).updateSystem("OwnGameId", "_idDummyOne", new UpdateSystemDto(
                null,
                null,
                null,
                "explored",
                "4444",
                null
        ));
        doReturn(Observable.just(getUpdatedSystem("colonized"))).when(gameSystemsApiService).updateSystem("OwnGameId", "_idDummyOne", new UpdateSystemDto(
                null,
                null,
                null,
                "colonized",
                "4444",
                null
        ));
        doReturn(Observable.just(getUpdatedSystem("upgraded"))).when(gameSystemsApiService).updateSystem("OwnGameId", "_idDummyOne", new UpdateSystemDto(
                null,
                null,
                null,
                "upgraded",
                "4444",
                null
        ));
        doReturn(Observable.just(getUpdatedSystem("developed"))).when(gameSystemsApiService).updateSystem("OwnGameId", "_idDummyOne", new UpdateSystemDto(
                null,
                null,
                null,
                "developed",
                "4444",
                null
        ));
        doReturn(Observable.just(getUpgradeJob()))
                .when(jobsApiService).createJob("OwnGameId", "4444", new CreateJobDto(
                        "_idDummyOne",
                        0,
                        "upgrade",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));

        doReturn(Observable.just(createJobsNoType())).when(jobsApiService).getFilteredJobs(any(), any(), eq("technology"), any(), any());
        doReturn(Observable.just(createJobsNoType())).when(jobsApiService).getFilteredJobs("OwnGameId", "4444", null, null, "_idDummyOne");
        doReturn(Observable.just(List.of())).when(jobsApiService).getFilteredJobs("OwnGameId", "4444", "ship", "testTroopId", "_idDummyOne");
        doReturn(Observable.just(List.of())).when(jobsApiService).getFilteredJobs("OwnGameId", "4444", "travel", "testTroopId", "_idDummyOne");

        doReturn(Observable.just(new AggregateResult(100, List.of()))).when(gameLogicApiService).getAggregateSystem(any(), any(), any(), any());
        doReturn(Observable.just(new AggregateResult(100, List.of()))).when(gameLogicApiService).getAggregateSystem(any(), any(), any(), any());


        // Districts
        doReturn(Observable.just(getDistrictJob()))
                .when(jobsApiService).createJob("OwnGameId", "4444", new CreateJobDto(
                        "_idDummyOne",
                        0,
                        "district",
                        null,
                        "energy",
                        null,
                        null,
                        null,
                        null
                ));

        doReturn(Observable.just(new ArrayList<>() {{
            add(new District(
                    "energy",
                    new TreeMap<>(Map.of("energy", 30)), null, new TreeMap<>(Map.of("minerals", 75)), new TreeMap<>(Map.of("minerals", 2)), 6
            ));
        }})).when(presetsApiService).getDistricts();

        doReturn(Observable.just(districtAndBuilding(""))).when(gameSystemsApiService).updateSystem(
                "OwnGameId", "_idDummyOne", new UpdateSystemDto(
                        null, new TreeMap<>(Map.of("energy", -1)),
                        null, null, "4444", null)
        );

        // Buildings
        doReturn(Observable.just(getDistrictJob()))
                .when(jobsApiService).createJob("OwnGameId", "4444", new CreateJobDto(
                        "_idDummyOne",
                        0,
                        "building",
                        "farm",
                        null,
                        null,
                        null,
                        null,
                        null
                ));
        doReturn(Observable.just(districtAndBuilding(""))).when(gameSystemsApiService).updateSystem(
                "OwnGameId", "_idDummyOne", new UpdateSystemDto(
                        null, null, Collections.emptyList(), null, "4444", null
                )
        );

        // Jobs
        doReturn(Observable.just(jobs)).when(jobsApiService).getJobs("OwnGameId", "4444");
        doReturn(FXCollections.observableArrayList(jobs)).when(jobService).init(any(), any());


        // logout
        doReturn(Completable.complete()).when(authApiService).logout();

        // SideButtons
        doReturn(Observable.just(createTechnologies())).when(presetsApiService).getTechnologies();

        // Technologies/Enhancements
        doReturn(Observable.just(jobs)).when(jobsApiService).deleteJob("OwnGameId", "4444", "testJobId4");

        doReturn(Observable.just(new AggregateResult(100, List.of(new AggregateItem("empire.technologies.difficulty", 1, 100))))).when(gameLogicApiService).getAggregateTech("4444", "technology.cost", "society");
        doReturn(Observable.just(new AggregateResult(90, List.of(new AggregateItem("empire.technologies.difficulty", 1, 100), new AggregateItem("technologies.society.cost_multiplier", 1, -10))))).when(gameLogicApiService).getAggregateTech("4444", "technology.cost", "demographic");
        doReturn(Observable.just(new AggregateResult(90, List.of(new AggregateItem("empire.technologies.difficulty", 1, 100), new AggregateItem("technologies.society.cost_multiplier", 1, -10))))).when(gameLogicApiService).getAggregateTech("4444", "technology.cost", "more_colonists_1");
        doReturn(Observable.just(new AggregateResult(100, List.of(new AggregateItem("empire.technologies.difficulty", 1, 100), new AggregateItem("technologies.computing.cost_multiplier", 1, 0))))).when(gameLogicApiService).getAggregateTech("4444", "technology.cost", "computing");

        doReturn(Observable.just(new ExplainedVariable(
                "technologies.society.cost_multiplier",
                1.0,
                List.of(new EffectSource("society", List.of(new Effect("technologies.society.cost_multiplier", 0, 0.95, 0)))),
                0.95
        ))).when(gameLogicApiService).getExplainedVariable("OwnGameId", "4444", "technologies.society.cost_multiplier");

        doReturn(Observable.just(getUpgradeJob()))
                .when(jobsApiService).createJob("OwnGameId", "4444", new CreateJobDto(
                        null,
                        0,
                        "technology",
                        null,
                        null,
                        "demographic",
                        null,
                        null,
                        null
                ));

        // Market
        ExplainedVariable explainedMarktVariable = new ExplainedVariable(
                "empire.market.fee",
                0.30d,
                List.of(),
                0.30d
        );
        doReturn(Observable.just(explainedMarktVariable)).when(gameLogicApiService).getExplainedVariable(any(), any(), eq("empire.market.fee"));

        doReturn(Observable.just(resourcesResult)).when(presetsApiService).getResources();

        Empire empire = new Empire(
                "",
                "",
                "4444",
                "OwnGameId",
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
        doReturn(Observable.just(empire)).when(gameEmpiresApiService).updateResources(any(), any(), any());

        // Tasks
        List<Job> newJobsTwo = getJobsTwo();
        doReturn(Observable.just(newJobsTwo)).when(jobsApiService).deleteJob("OwnGameId", "4444", "testJobId2");

        // Fleet
        TreeMap<String, Integer> shipAmounts = new TreeMap<>();
        Fleet fleet = new Fleet("123", "123", "testFleetId", "testGameId", "testEmpireId", "fleet1", "Bellevue", shipAmounts, null, null, null);
        List<ShipType> shipTypes = createShipsTypes();

        // Player List
        doReturn(Observable.just(getWars())).when(warsApiService).getWars(any(), any());

        // Build Fleet
        doReturn(Observable.just(shipTypes)).when(presetsApiService).getShips();
        doReturn(Observable.just(fleet)).when(fleetsApiService).createFleet(any(), any());

        doReturn(Observable.just(fleet)).when(fleetsApiService).deleteFleet("OwnGameId", "testTroopId");

        // Explore Castle
        TreeMap<String, Integer> ships = new TreeMap<>();
        ships.put("explorer", 1);
        ships.put("colonizer", 1);
        doReturn(Observable.just(List.of(new Fleet("", "", "fleetOne", "OwnGameId", "4444", "Troop1", "_idDummyOne", ships, null, null, null)))).when(fleetsApiService).getFleets(any(), any());

        doReturn(Observable.just(List.of(new ReadShipDto("", "", "explorer", "OwnGameId", "4444", "fleetOne", "explorer", 10, 0, null),
                new ReadShipDto("", "", "colonizer", "OwnGameId", "4444", "fleetOne", "colonizer", 10, 0, null))))
                .when(shipsApiService).getShips(any(), any());
    }

    private List<Job> getJobsTwo() {
        TreeMap<String, Integer> cost = new TreeMap<>(Map.of("energy", 200));
        Job dummyJobThree = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId3",
                0,
                6,
                "OwnGameId",
                "4444",
                "testSystemId3",
                0,
                "building",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                cost,
                Map.of("minerals", 100, "energy", 25)
        );
        return List.of(dummyJobThree);

    }

    @Test
    public void v1() {
        // Launch Game
        waitForFxEvents();
        assertEquals("PRAESIDEO - Launching", stage.getTitle());

        clickOn(app.stage());
        press(KeyCode.ENTER);

        waitForFxEvents();

        // Change to Login View after Launching
        assertEquals("PRAESIDEO - Login", stage.getTitle());

        // open Signup
        clickOn("#signupButton");

        waitForFxEvents();

        assertEquals("PRAESIDEO - Signup", stage.getTitle());

        // Enter Username and Password
        clickOn("#usernameInput").write("TestUser");
        clickOn("#passwordInput").write("Password");
        clickOn("#confirmPasswordInput").write("Password");

        // signup user and check login input
        clickOn("#signupButton");

        waitForFxEvents();

        assertEquals("PRAESIDEO - Login", stage.getTitle());

        assertEquals("TestUser", ((TextField) lookup("#usernameField").query()).getText());
        waitForFxEvents();

        assertEquals("Password", ((TextField) lookup("#passwordField").query()).getText());


        // Login and change to Lobby
        clickOn("#loginButton");

        waitForFxEvents();

        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Join a game
        clickOn("Game 2");
        waitForFxEvents();

        clickOn("#joinButton");
        waitForFxEvents();

        clickOn("#passwordField");
        waitForFxEvents();

        write("GamePassword");

        doReturn(Observable.just(List.of(
                new Member("", "", "2", "TestUser", false, null),
                new Member("", "", "2", "User 2", true, null),
                new Member("", "", "2", "User 3", true, null)
        ))).when(gameMembersApiService).getMembersOfGame("2");
        clickOn("#confirmButton");
        waitForFxEvents();

        assertEquals("PRAESIDEO - Game Lobby", stage.getTitle());

        // create Empire
        clickOn("#btnBuildEmpire");
        waitForFxEvents();

        clickOn("#txtInputName");
        eraseText(((TextField) lookup("#txtInputName").query()).getLength());
        clickOn("#txtInputName").write("Own Empire");
        clickOn("#txtInputDescription").write("Test creating empire");

        clickOn((Node) lookup(".tab-pane > .tab-header-area > .headers-region > .tab").nth(1).query());
        waitForFxEvents();
        clickOn("#btnFlagIncrease");
        waitForFxEvents();
        clickOn("#btnSave");

        // set Ready
        clickOn("Ready");
        subjectMember.onNext(new Event<>("games.2.members.2.TestUser.updated", new Member("", "", "2", "TestUser", true, null)));
        waitForFxEvents();

        assertEquals("Not Ready", ((Button) lookup("#btnGameAction").query()).getText());

        // leave Game
        clickOn("#btnLeaveGame");
        waitForFxEvents();

        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // create Own Game
        clickOn("#newGameButton");
        waitForFxEvents();

        clickOn("#nameField").write("OwnGame");
        clickOn("#passwordField").write("OwnGamePassword");
        clickOn("#createButton");
        waitForFxEvents();

        // start Game
        clickOn("#btnGameAction");
        waitForFxEvents();

        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Quit Game
        press(KeyCode.ESCAPE);
        waitForFxEvents();

        clickOn("Return to Lobby");
        waitForFxEvents();

        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Logout
        clickOn("#logoutButton");
        waitForFxEvents();

        assertEquals("PRAESIDEO - Login", stage.getTitle());
    }

    @Test
    public void v2() {
        // Launch Game
        waitForFxEvents();
        assertEquals("PRAESIDEO - Launching", stage.getTitle());

        clickOn(app.stage());
        press(KeyCode.ENTER);

        waitForFxEvents();

        // Change to Login View after Launching
        assertEquals("PRAESIDEO - Login", stage.getTitle());

        // Enter Username and Password
        clickOn("#usernameField").write("TestUser");
        clickOn("#passwordField").write("Password");

        waitForFxEvents();

        // Login and change to Lobby
        clickOn("#loginButton");

        waitForFxEvents();

        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // create Own Game
        clickOn("#newGameButton");
        waitForFxEvents();

        clickOn("#nameField").write("OwnGame");
        clickOn("#passwordField").write("OwnGamePassword");
        clickOn("#createButton");
        waitForFxEvents();

        // start Game
        clickOn("#btnGameAction");
        waitForFxEvents();

        assertEquals("PRAESIDEO - Ingame", stage.getTitle());

        // Change music volume
        press(KeyCode.ESCAPE).release(KeyCode.ESCAPE);
        waitForFxEvents();
        drag("#audioSlider", MouseButton.PRIMARY)
                .moveBy(-100, 0)
                .moveBy(200, 0)
                .release(MouseButton.PRIMARY);
        press(KeyCode.ESCAPE).release(KeyCode.ESCAPE);

        // Change game speed
        clickOn("#slowToggleButton");
        waitForFxEvents();
        clickOn("#mediumToggleButton");
        waitForFxEvents();
        clickOn("#fastToggleButton");
        waitForFxEvents();

        // Pause game
        clickOn("#pauseToggleButton");
        waitForFxEvents();

        // Unpause game
        clickOn("#fastToggleButton");
        waitForFxEvents();

        // Click on a castle
        ZoomDragComponent zoomDragComponent = (ZoomDragComponent) lookup("#vBoxRoot").queryAs(VBox.class).getChildren().getFirst();
        clickOn(zoomDragComponent.getCastleContainer().getChildren().getFirst());
        waitForFxEvents();

        // Explore castle
        clickOn("#exploreButton");
        subjectSystem.onNext(new Event<>("games.OwnGameId.systems._idDummyOne.updated", getUpdatedSystem("explored")));
        waitForFxEvents();

        // Colonize castle
        clickOn("#exploreButton");
        subjectSystem.onNext(new Event<>("games.OwnGameId.systems._idDummyOne.updated", getUpdatedSystem("colonized")));
        waitForFxEvents();

        // Upgrade castle
        clickOn("#exploreButton");
        subjectSystem.onNext(new Event<>("games.OwnGameId.systems._idDummyOne.updated", getUpdatedSystem("upgraded")));
        waitForFxEvents();

        // Develop castle
        clickOn("#exploreButton");
        subjectSystem.onNext(new Event<>("games.OwnGameId.systems._idDummyOne.updated", getUpdatedSystem("developed")));
        waitForFxEvents();

        // Build district
        GridPane districtGridPane = lookup("#districtGridPane").query();
        clickOn(districtGridPane.getChildren().get(2));
        clickOn("#districtBuildButton");
        subjectSystem.onNext(new Event<>("games.OwnGameId.systems._idDummyOne.updated", districtAndBuilding("buildDistrict")));
        waitForFxEvents();

        // Destroy district
        districtGridPane = lookup("#districtGridPane").query();
        clickOn(districtGridPane.getChildren().getFirst());
        clickOn("#districtDestroyButton");
        subjectSystem.onNext(new Event<>("games.OwnGameId.systems._idDummyOne.updated", districtAndBuilding("")));
        waitForFxEvents();

        // Build building
        clickOn("#farm");
        waitForFxEvents();
        clickOn("#buildButton");
        subjectSystem.onNext(new Event<>("games.OwnGameId.systems._idDummyOne.updated", districtAndBuilding("buildBuilding")));
        waitForFxEvents();

        // Destroy building
        moveBy(-250, -250);
        clickOn();
        clickOn("#destroyButton");
        subjectSystem.onNext(new Event<>("games.OwnGameId.systems._idDummyOne.updated", districtAndBuilding("")));
        waitForFxEvents();
    }

    @Test
    public void v3() {
        // Launch Game
        waitForFxEvents();
        assertEquals("PRAESIDEO - Launching", stage.getTitle());

        clickOn(app.stage());
        press(KeyCode.ENTER);

        waitForFxEvents();

        // Change to Login View after Launching
        assertEquals("PRAESIDEO - Login", stage.getTitle());

        // Enter Username and Password
        clickOn("#usernameField").write("TestUser");
        clickOn("#passwordField").write("Password");

        waitForFxEvents();

        // Login and change to Lobby
        clickOn("#loginButton");

        waitForFxEvents();

        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Filter for Game Name
        clickOn("#searchField").write("2");

        ListView<Game> gameList = lookup("#gameList").query();
        Game gameFiltered = gameList.getItems().getFirst();
        assertEquals("Game 2", gameFiltered.name());

        clickOn("#searchField").eraseText(((TextField) lookup("#searchField").query()).getLength());

        // Sort Games by Name
        waitForFxEvents();

        clickOn("#gameName");

        Game gameSortedDown = gameList.getItems().getFirst();
        assertEquals("Game 1", gameSortedDown.name());

        clickOn("#gameName");

        Game gameSortedUp = gameList.getItems().getFirst();
        assertEquals("Game 3", gameSortedUp.name());

        waitForFxEvents();

        // Traits
        clickOn("#newGameButton");
        waitForFxEvents();

        clickOn("#nameField").write("OwnGame");
        clickOn("#passwordField").write("OwnGamePassword");

        clickOn("#createButton");
        waitForFxEvents();

        clickOn("#btnBuildEmpire");
        waitForFxEvents();

        clickOn("#tabTraits");
        waitForFxEvents();

        ListView<Trait> availableTraits = lookup("#availableTraitsList").query();
        Label traitPointsLabel = lookup("#traitPointsLabel").query();
        Label stillSelectLabel = lookup("#stillSelectLabel").query();
        Label buttonLabel = lookup("#traitButtonLabel").query();
        Label buttonCostLabel = lookup("#traitButtonCostLabel").query();

        assertEquals("5", traitPointsLabel.getText());
        assertEquals(4, availableTraits.getItems().size());
        assertEquals("You can still choose up to " + 5 + " Traits.", stillSelectLabel.getText());
        assertEquals("Select", buttonLabel.getText());
        assertEquals("- " + 1, buttonCostLabel.getText());

        // Add Prepared Trait
        Button traitButton = lookup("#traitButton").query();
        assertFalse(traitButton.isDisabled());
        clickOn(traitButton);
        waitForFxEvents();

        ListView<Trait> yourTraits = lookup("#ownedTraitsList").query();
        assertEquals(1, yourTraits.getItems().size());
        assertEquals(3, availableTraits.getItems().size());
        assertEquals("4", traitPointsLabel.getText());
        assertEquals("You can still choose up to " + 4 + " Traits.", stillSelectLabel.getText());
        assertEquals("Deselect", buttonLabel.getText());
        assertEquals("+ " + 1, buttonCostLabel.getText());

        // Delete Prepared Trait
        assertFalse(traitButton.isDisabled());
        clickOn(traitButton);
        waitForFxEvents();

        assertEquals(0, yourTraits.getItems().size());
        assertEquals(4, availableTraits.getItems().size());
        assertEquals("5", traitPointsLabel.getText());
        assertEquals(4, availableTraits.getItems().size());
        assertEquals("You can still choose up to " + 5 + " Traits.", stillSelectLabel.getText());
        assertEquals("Select", buttonLabel.getText());
        assertEquals("- " + 1, buttonCostLabel.getText());

        // Open Ingame Screen with Side Buttons
        clickOn("#btnBack");
        waitForFxEvents();

        clickOn("#btnGameAction");
        waitForFxEvents();

        // Test Enhancements
        clickOn("#enhancementsButton");
        waitForFxEvents();

        // cancel Enhancement
        ListView<EnhancementItemComponent> enhancementItemList = lookup("#itemListView").queryListView();
        EnhancementItemComponent societyEnhancement = enhancementItemList.getItems().get(1);
        assertFalse(societyEnhancement.itemNoJob.isVisible());
        assertTrue(societyEnhancement.itemCross.isVisible());
        assertTrue(societyEnhancement.itemDate.isVisible());
        assertTrue(societyEnhancement.itemProgressBar.isVisible());
        assertTrue(societyEnhancement.itemTechnologie.isVisible());

        clickOn(societyEnhancement.itemCross);
        waitForFxEvents();

        subjectJob.onNext(new Event<>("games.OwnGameId.empires.4444.jobs.dummyJobFour.deleted", createJobsNoType().get(3)));
        waitForFxEvents();

        assertTrue(societyEnhancement.itemNoJob.isVisible());
        assertFalse(societyEnhancement.itemCross.isVisible());
        assertFalse(societyEnhancement.itemDate.isVisible());
        assertFalse(societyEnhancement.itemProgressBar.isVisible());
        assertFalse(societyEnhancement.itemTechnologie.isVisible());

        // start Enhancement
        final JobsApiService jobsApiService = testComponent.jobsApiService();
        doReturn(Observable.just(createJobs())).when(jobsApiService).getFilteredJobs(any(), any(), any(), any(), any());

        assertEquals(1, lookup("#enhancementBox").queryAs(HBox.class).getChildren().size());

        clickOn(societyEnhancement.itemViewEnhancementsButton);
        waitForFxEvents();

        Label enhancementsLabel = lookup("#enhancementsLabel").query();
        assertEquals(enhancementsLabel.getText(), "Society enhancements");

        ListView<Technology> enhancementsList = lookup("#enhancementsList").query();
        moveTo(enhancementsList.getChildrenUnmodifiable().getFirst());
        moveBy(0, -180);
        clickOn();
        waitForFxEvents();

        clickOn("#unlockButton");
        waitForFxEvents();

        doReturn(Observable.just(createJobsNoType())).when(jobsApiService).getFilteredJobs(any(), any(), any(), any(), any());

        TreeMap<String, Integer> cost = new TreeMap<>(Map.of("research", 200));
        subjectJob.onNext(new Event<>("games.OwnGameId.empires.4444.jobs.dummyJobFour.created", new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId4",
                0,
                6,
                "OwnGameId",
                "4444",
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
        )));
        waitForFxEvents();

        assertFalse(societyEnhancement.itemNoJob.isVisible());
        assertTrue(societyEnhancement.itemCross.isVisible());
        assertTrue(societyEnhancement.itemDate.isVisible());
        assertTrue(societyEnhancement.itemProgressBar.isVisible());
        assertTrue(societyEnhancement.itemTechnologie.isVisible());

        // complete enhancement
        for (int i = 1; i <= 6; i++) {
            subjectJob.onNext(new Event<>("games.OwnGameId.empires.4444.jobs.dummyJobFour.updated", new Job(
                    "createdAtDummyValue",
                    "updatedAtDummyValue",
                    "testJobId4",
                    i,
                    6,
                    "OwnGameId",
                    "4444",
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
            )));
            waitForFxEvents();
            assertEquals("-> 20.07.2024", societyEnhancement.itemDate.getText());
        }

        subjectJob.onNext(new Event<>("games.OwnGameId.empires.4444.jobs.dummyJobFour.deleted", new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId4",
                6,
                6,
                "OwnGameId",
                "4444",
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
        )));
        waitForFxEvents();

        assertTrue(societyEnhancement.itemNoJob.isVisible());
        assertFalse(societyEnhancement.itemCross.isVisible());
        assertFalse(societyEnhancement.itemDate.isVisible());
        assertFalse(societyEnhancement.itemProgressBar.isVisible());
        assertFalse(societyEnhancement.itemTechnologie.isVisible());

        // Market
        clickOn("#marketButton");
        waitForFxEvents();

        // Buy Steel
        Text buyAddNum = lookup("#buyAddNum").query();
        Text sellAddNum = lookup("#sellAddNum").query();
        Text buySubNum = lookup("#buySubNum").query();
        Text sellSubNum = lookup("#sellSubNum").query();
        Button buyButton = lookup("#buyButton").query();

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

        TreeMap<String, Integer> resources = new TreeMap<>(Map.of("energy", 10, "minerals", 20, "alloys", 40, "food", 40, "research", 100, "fuel", 60, "credits", 44, "population", 80, "consumer_goods", 90));
        subjectEmpire.onNext(new Event<>("games.OwnGameId.empires.4444.updated",
                        new Empire(
                                "",
                                "",
                                "4444",
                                "OwnGameId",
                                "",
                                "",
                                "",
                                "",
                                0,
                                0,
                                "",
                                List.of(),
                                resources,
                                List.of("society, demographic"),
                                new ArrayList<>(),
                                _private,
                                null)
                )
        );
        waitForFxEvents();

        // Sell Steel
        clickOn("#sellButton");
        waitForFxEvents();

        resources = new TreeMap<>(Map.of("energy", 10, "minerals", 20, "alloys", 30, "food", 40, "research", 100, "fuel", 60, "credits", 58, "population", 80, "consumer_goods", 90));
        subjectEmpire.onNext(new Event<>("games.OwnGameId.empires.4444.updated",
                        new Empire(
                                "",
                                "",
                                "4444",
                                "OwnGameId",
                                "",
                                "",
                                "",
                                "",
                                0,
                                0,
                                "",
                                List.of(),
                                resources,
                                List.of("society, demographic"),
                                new ArrayList<>(),
                                _private,
                                null)
                )
        );
        waitForFxEvents();

        // Task
        clickOn("#taskButton");
        waitForFxEvents();

        ListView<Job> taskList = lookup("#taskList").query();
        assertNotNull(taskList);

        ObservableList<Job> items = taskList.getItems();

        assertEquals(3, items.size());

        for (int i = 4; i <= 6; i++) {
            subjectJob.onNext(new Event<>("games.OwnGameId.empires.4444.jobs.testJobId1.updated", new Job(
                    "createdAtDummyValue",
                    "updatedAtDummyValue",
                    "testJobId1",
                    i,
                    6,
                    "OwnGameId",
                    "4444",
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
            )));
            waitForFxEvents();
        }

        subjectJob.onNext(new Event<>("games.OwnGameId.empires.4444.jobs.testJobId1.deleted", new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId1",
                6,
                6,
                "OwnGameId",
                "4444",
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
        )));
        waitForFxEvents();

        items = taskList.getItems();

        assertEquals(2, items.size());

        subjectJob.onNext(new Event<>("games.OwnGameId.empires.4444.jobs.testJobId2.updated",
                new Job(
                        "createdAtDummyValue",
                        "updatedAtDummyValue",
                        "testJobId2",
                        1,
                        6,
                        "OwnGameId",
                        "4444",
                        "_idDummyTwo",
                        0,
                        "building",
                        "mine",
                        "energy",
                        null,
                        null,
                        null,
                        null,
                        cost,
                        Map.of("minerals", 100, "energy", 25)
                )
        ));

        clickOn("#cancelTooltip");
        waitForFxEvents();

        subjectJob.onNext(new Event<>("games.OwnGameId.empires.4444.jobs.testJobId2.deleted",
                new Job(
                        "createdAtDummyValue",
                        "updatedAtDummyValue",
                        "testJobId2",
                        1,
                        6,
                        "OwnGameId",
                        "4444",
                        "_idDummyTwo",
                        0,
                        "building",
                        "mine",
                        "energy",
                        null,
                        null,
                        null,
                        null,
                        cost,
                        Map.of("minerals", 100, "energy", 25)
                )
        ));
        resources = new TreeMap<>(Map.of("energy", 210, "minerals", 20, "alloys", 30, "food", 40, "research", 100, "fuel", 60, "credits", 58, "population", 80, "consumer_goods", 90));
        subjectEmpire.onNext(new Event<>("games.OwnGameId.empires.4444.updated",
                        new Empire(
                                "",
                                "",
                                "4444",
                                "OwnGameId",
                                "",
                                "",
                                "",
                                "",
                                0,
                                0,
                                "",
                                List.of(),
                                resources,
                                List.of("society, demographic"),
                                new ArrayList<>(),
                                _private,
                                null)
                )
        );
        waitForFxEvents();

        clickOn("#taskButton");
        waitForFxEvents();

        // Events
        Platform.runLater(() -> this.eventService.setEvent("blizzard"));
        waitForFxEvents();

        clickOn("OKAY");
        waitForFxEvents();

        assertThrows(EmptyNodeQueryException.class, () -> lookup("OKAY").query());

        Platform.runLater(() -> this.eventService.setEvent("marriage"));
        waitForFxEvents();

        clickOn("Yes");
        waitForFxEvents();

        clickOn("OKAY");
        waitForFxEvents();

        assertThrows(EmptyNodeQueryException.class, () -> lookup("OKAY").query());
    }


    @Test
    public void v4() {
        loginAndCreateGame();

        testHomeSystems();

        startGame();

        testVolumeButtons();

        testContacts();

        testPlanningTroop();

        testTroopView();

        testDiplomacy();
    }

    private void testDiplomacy() {
        // declare War against Peter
        clickOn("#diplomacyButton");

        ChoiceBox<String> diplomacyFilter = lookup("#diplomacyFilter").query();
        clickOn(diplomacyFilter);
        clickOn("Peace");

        assertEquals("Peace", diplomacyFilter.getValue());


        clickOn("#declareWarButton");
        clickOn("#funTab");

        assertTrue(lookup("#declareWarButton1").query().isDisabled());
        clickOn("#warNameTextField").write("Hello War");
        assertFalse(lookup("#declareWarButton1").query().isDisabled());

        doReturn(Observable.just(new War("", "", "warID2", "OwnGameId", "4444", "5555", "Hello War", Map.of("reason", "fun")))).when(warsApiService).createWar(any(), any());
        subjectWar.onNext(new Event<>("games.OwnGameId.wars.*.created", new War("", "", "warID2", "OwnGameId", "4444", "5555", "Hello War", Map.of("reason", "fun"))));
        subjectWar.onNext(new Event<>("games.OwnGameId.wars.*.updated", new War("", "0", "warID2", "OwnGameId", "4444", "5555", "Hello War", Map.of("reason", "fun"))));
        clickOn("#declareWarButton1");

        clickOn(diplomacyFilter);
        clickOn("Attacker");

        // show the reason for the war
        waitForFxEvents();
        VBox reasonBox = lookup("#reasonBox").query();
        assertFalse(reasonBox.isVisible());

        clickOn("#reasonButton");

        assertTrue(reasonBox.isVisible());

        Image reasonImage = new Image(Objects.requireNonNull(App.class.getResource("image/warreasons/fun.png")).toString());
        ImageView reasonImageView = lookup("#reasonImage").query();
        assertEquals(reasonImage.getUrl(), reasonImageView.getImage().getUrl());

        Label reasonName = lookup("#reasonName").query();
        assertEquals("Fun", reasonName.getText());

        clickOn("#unSeeReasonImage");

        waitForFxEvents();

        assertFalse(reasonBox.isVisible());

        // stop the war
        doReturn(Observable.just(new War("", "", "warID2", "OwnGameId", "4444", "5555", "Hello War", Map.of("reason", "fun")))).when(warsApiService).deleteWar(any(), any());
        clickOn("#stopWarButton");
        subjectWar.onNext(new Event<>("games.OwnGameId.wars.*.deleted", new War("", "", "warID2", "OwnGameId", "4444", "5555", "Hello War", Map.of("reason", "fun"))));
        waitForFxEvents();

        clickOn("#diplomacyButton");
        waitForFxEvents();

        // War Notification
        doReturn(Observable.just(new War("", "", "warID1", "OwnGameId", "5555", "4444", "Krieg", Map.of("reason", "fun")))).when(warsApiService).getWars(any(), any());
        subjectWar.onNext(new Event<>("games.OwnGameId.wars.*.created", new War("", "0", "warID1", "OwnGameId", "5555", "4444", "Krieg", Map.of("reason", "fun"))));
        waitForFxEvents();

        clickOn("#okButton");

        clickOn("#diplomacyButton");
        assertEquals("Attacker", diplomacyFilter.getValue());

        clickOn(diplomacyFilter);
        clickOn("Defender");

        assertEquals("Defender", diplomacyFilter.getValue());
    }

    private void loginAndCreateGame() {
        // Launch Game
        waitForFxEvents();
        assertEquals("PRAESIDEO - Launching", stage.getTitle());

        clickOn(app.stage());
        press(KeyCode.ENTER);

        waitForFxEvents();

        // Change to Login View after Launching
        assertEquals("PRAESIDEO - Login", stage.getTitle());

        // Enter Username and Password
        clickOn("#usernameField").write("TestUser");
        clickOn("#passwordField").write("Password");

        waitForFxEvents();

        // Login and change to Lobby
        clickOn("#loginButton");

        waitForFxEvents();

        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Create Own Game
        clickOn("#newGameButton");
        waitForFxEvents();

        clickOn("#nameField").write("OwnGame");
        clickOn("#passwordField").write("OwnGamePassword");
        clickOn("#createButton");
        waitForFxEvents();
    }

    private void testHomeSystems() {
        // HomeSystem
        clickOn("#btnBuildEmpire");
        waitForFxEvents();

        clickOn("#tabHomeSystems");
        waitForFxEvents();

        HBox firstSystemRow = lookup("#firstSystemRow").query();
        HBox secondSystemRow = lookup("#secondSystemRow").query();

        assertEquals(2, firstSystemRow.getChildren().size());
        assertEquals(2, secondSystemRow.getChildren().size());

        Label typeLabel = lookup("#typeLabel").query();
        Label capacityLabel = lookup("#capacityLabel").query();

        assertEquals("Agriculture", typeLabel.getText());
        assertEquals("Capacity: 15 - 30", capacityLabel.getText());

        clickOn(firstSystemRow.getChildren().get(0));
        clickOn(firstSystemRow.getChildren().get(1));

        clickOn("#btnBack");
        waitForFxEvents();
    }

    private void startGame() {
        // Start Game
        clickOn("#btnGameAction");
        waitForFxEvents();

        assertEquals("PRAESIDEO - Ingame", stage.getTitle());
    }

    private void testVolumeButtons() {
        // Change music volume
        press(KeyCode.ESCAPE).release(KeyCode.ESCAPE);
        waitForFxEvents();

        Button highVolumeButton = lookup("#highVolumeButton").query();
        Button muteButton = lookup("#muteButton").query();
        Slider audioSlider = lookup("#audioSlider").query();

        clickOn(highVolumeButton);
        waitForFxEvents();

        assertEquals(100,audioSlider.getValue() );

        clickOn(muteButton);
        waitForFxEvents();
        assertEquals(0.0, audioSlider.getValue());
        press(KeyCode.ESCAPE).release(KeyCode.ESCAPE);
    }

    private void testContacts() {
        // Contacts
        HBox contactsButton = lookup("#contactsIcon").query();
        clickOn(contactsButton);

        // A big contacts pop up window appears with all the information about the other players.
        waitForFxEvents();

        AnchorPane contactsBox = lookup("#contactsRoot").query();
        assertNotNull(contactsBox);

        assertTrue(lookup("#sendButton").query().isDisabled());

        ListView<Player> contactsList = lookup("#contactsList").query();
        waitForFxEvents();
        assertEquals(3, contactsList.getItems().size());

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

        // End:
        // Jan can now press the send button to send the emoji.
        waitForFxEvents();

        assertFalse(lookup("#sendButton").query().isDisabled());

        doReturn(Observable.just(new Empire("", "", "4444", "OwnGameId", "", "", "", "", 0, 0, "", List.of(), null, List.of("society"), new ArrayList<>(), _private, null))).when(gameEmpiresApiService).updateEmpire(any(), any(), any());

        clickOn("#sendButton");
        waitForFxEvents();

        assertDoesNotThrow(() -> lookup("#notificationImage").query());

        clickOn("#contactsViewBackButton");
        waitForFxEvents();
    }

    private void testPlanningTroop() {
        // Plan Troop
        clickOn("#buildFleetsButton");

        // Tim selects a castle.
        // Open the ChoiceBox
        ChoiceBox<String> castleList = lookup("#systemsChoiceBox").query();
        clickOn(castleList);
        interact(() -> castleList.getSelectionModel().select(1));

        // Tim writes a name for the new troop.
        clickOn("#fleetName").write("My Troop");

        // Tim selects the amount of ships and clicks on the build fleet button.
        clickOn("#imgViewIncrease");

        // Action:
        // Tim clicks on the build fleet button.
        clickOn("#buildFleetButton");

        final TreeMap<String, Integer> size = new TreeMap<>(Map.of("explorer", 1));
        subjectFleet.onNext(new Event<>("games.OwnGameId.fleets.testTroopId.created", new Fleet(null, null, "testTroopId", "OwnGameId", "4444", "My Troop", "_idDummyOne", size, null, null, null)));
        waitForFxEvents();

        // Close view
        clickOn("#buildFleetsButton");
        waitForFxEvents();
    }

    private void testTroopView() {
        final TreeMap<String, Integer> size = new TreeMap<>(Map.of("explorer", 1));
        Fleet troop = new Fleet(null, null, "testTroopId", "OwnGameId", "4444", "My Troop", "_idDummyOne", size, null, null, null);
        TreeMap<String, Integer> ships = new TreeMap<>();
        ships.put("explorer", 1);
        ships.put("colonizer", 1);
        Fleet otherTroop = new Fleet("", "", "fleetOne", "OwnGameId", "4444", "Troop1", "_idDummyOne", ships, null, null, null);
        doReturn(Observable.just(List.of(troop, otherTroop))).when(fleetsApiService).getFleets(any(), any());
        doReturn(Observable.just(List.of())).when(shipsApiService).getShips(any(), any());
        CreateJobDto createJobDto = new CreateJobDto("_idDummyOne", 0, "ship", null, null, null, "testTroopId", "explorer", null);
        Job job = new Job(null, null, "jobId", 0, 100, "OwnGameId", "4444", "_idDummyOne", 0, "ship", null, null, null, "testTroopId", "explorer", null, new TreeMap<>(Map.of("energy", 50)), null);
        doReturn(Observable.just(job)).when(jobsApiService).createJob("OwnGameId", "4444", createJobDto);
        Observable<List<Job>> jobs = Observable.just(List.of());
        doReturn(jobs).when(jobsApiService).getFilteredJobs(any(), any(), eq("travel"), any(), any());

        // Open troop view
        clickOn("My Troop").clickOn(MouseButton.PRIMARY);

        // Start training job ("Explorer")
        clickOn("#trainUnitsTab");
        waitForFxEvents();
        clickOn(lookup("#chooseUnitsListView .list-cell").queryAs(ListCell.class));
        waitForFxEvents();
        clickOn("+ Train Unit");
        waitForFxEvents();
        subjectJob.onNext(new Event<>("games.OwnGameID.empires.testEmpireId.jobs.*.created", job));
        waitForFxEvents();

        // Switch to update troop tab to see when the job finishes
        clickOn("#updateTroopTab");
        waitForFxEvents();

        // Training job is finished
        job = new Job(null, "1", "jobId", 0, 100, "OwnGameId", "4444", "_idDummyOne", 0, "ship", null, null, null, "testTroopId", "explorer", null, new TreeMap<>(Map.of("energy", 50)), null);
        subjectJob.onNext(new Event<>("games.OwnGameID.empires.testEmpireId.jobs.*.deleted", job));
        subjectShip.onNext(new Event<>("games.OwnGameID.fleets.testTroopId.ships.testUnitId.created", new Ship(null, null, "testUnitId", "OwnGameID", "4444", "testTroopId", "explorer", 100, 0, null, null)));
        waitForFxEvents();

        // Update troop name
        Fleet newTroop = new Fleet(troop.createdAt(), troop.updatedAt(), troop._id(), troop.game(), troop.empire(), "New Troop Name", troop.location(), troop.size(), troop._private(), troop._public(), troop.effects());
        doReturn(Observable.just(newTroop)).when(fleetsApiService).updateFleet(eq(troop.game()), eq(troop._id()), any());
        clickOn("#troopNameTextField").write("New Troop Name");
        clickOn("#updateTroopButton");
        subjectFleet.onNext(new Event<>("games.OwnGameId.fleets.*.updated", newTroop));
        waitForFxEvents();

        // Update troop size
        newTroop = new Fleet(troop.createdAt(), troop.updatedAt(), troop._id(), troop.game(), troop.empire(), "New Troop Name", troop.location(), new TreeMap<>(Map.of("explorer", 0, "interceptor", 2)), troop._private(), troop._public(), troop.effects());
        doReturn(Observable.just(newTroop)).when(fleetsApiService).updateFleet(eq(troop.game()), eq(troop._id()), any());
        clickOn(lookup("#decreaseButton").nth(0).queryAs(Button.class));
        clickOn(lookup("#increaseButton").nth(2).queryAs(Button.class));
        clickOn(lookup("#increaseButton").nth(2).queryAs(Button.class));
        waitForFxEvents();
        clickOn("#updateTroopButton");
        waitForFxEvents();
        subjectFleet.onNext(new Event<>("games.testGameId.fleets.*.updated", newTroop));
        waitForFxEvents();

        // Transfer unit
        Ship updatedExplorer = new Ship(null, null, "testUnitId", "OwnGameId", "4444", "testTroopId2", "explorer", 100, 0, null, null);
        doReturn(Observable.just(updatedExplorer)).when(shipsApiService).updateShip(any(), any(), any(), any());
        clickOn("#transferUnitsTab");
        waitForFxEvents();
        clickOn(lookup("#yourUnitsListView .list-cell").queryAs(ListCell.class));
        clickOn(lookup("#troopsListView .list-cell").queryAs(ListCell.class));
        clickOn("#transferUnitButton");
        subjectShip.onNext(new Event<>("games.testGameId.fleets.*.ships.*.updated", updatedExplorer));

        // Start another training job ("Explorer")
        job = new Job(null, "1", "jobId", 0, 100, "OwnGameId", "4444", "_idDummyOne", 0, "ship", null, null, null, "testTroopId", "fighter", null, new TreeMap<>(Map.of("energy", 50)), null);
        doReturn(Observable.just(job)).when(jobsApiService).createJob(eq("OwnGameId"), eq("4444"), any());
        clickOn("#trainUnitsTab");
        waitForFxEvents();
        clickOn("+ Train Unit");
        waitForFxEvents();
        subjectJob.onNext(new Event<>("games.OwnGameID.empires.testEmpireId.jobs.*.created", job));
        waitForFxEvents();

        // Training job is finished
        job = new Job(null, "1", "jobId", 0, 100, "OwnGameId", "4444", "_idDummyOne", 0, "ship", null, null, null, "testTroopId", "explorer", null, new TreeMap<>(Map.of("energy", 50)), null);
        Ship newUnit = new Ship(null, null, "testUnitId", "OwnGameID", "4444", "testTroopId", "fighter", 100, 0, null, null);
        subjectJob.onNext(new Event<>("games.OwnGameID.empires.testEmpireId.jobs.*.deleted", job));
        subjectShip.onNext(new Event<>("games.OwnGameID.fleets.testTroopId.ships.testUnitId.created", newUnit));
        waitForFxEvents();

        // Close troop
        clickOn("#closeTroopViewImage");
        waitForFxEvents();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        app.stop();
        app = null;
    }

    private void initSystemUpgrades() {
        SystemUpgrade unexplored = new SystemUpgrade(
                "unexplored",
                "",
                1, 0, new TreeMap<>(Map.of()), new TreeMap<>(Map.of()), 0
        );
        SystemUpgrade explored = new SystemUpgrade(
                "explored",
                "",
                1, 0, new TreeMap<>(Map.of()), new TreeMap<>(Map.of()), 0
        );
        SystemUpgrade colonized = new SystemUpgrade(
                "colonized",
                "",
                1, 0.05, new TreeMap<>(Map.of("minerals", 100, "energy", 100)), new TreeMap<>(Map.of("energy", 1, "minerals", 1, "fuel", 1)), 0
        );
        SystemUpgrade upgraded = new SystemUpgrade(
                "upgraded",
                "",
                1.25, 0.02, new TreeMap<>(Map.of("minerals", 100, "alloys", 100)), new TreeMap<>(Map.of("energy", 2, "minerals", 2, "fuel", 2, "alloys", 1)), 0
        );
        SystemUpgrade developed = new SystemUpgrade(
                "developed",
                "",
                1.25, 0.01, new TreeMap<>(Map.of("alloys", 200, "fuel", 100)), new TreeMap<>(Map.of("energy", 4, "minerals", 4, "fuel", 4, "alloys", 3)), 0
        );
        systemUpgrades = new SystemUpgradesResult(
                unexplored, explored, colonized, upgraded, developed
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

    private GameSystem getUpdatedSystem(String upgrade) {
        TreeMap<String, Integer> districtSlots = new TreeMap<>(Map.of("energy", 5));
        TreeMap<String, Integer> districts = new TreeMap<>(Map.of("energy", 2));
        return switch (upgrade) {
            case "explored" -> new GameSystem(
                    "createdAtDummyValue", "updatedAtDummyValue", "_idDummyOne",
                    "OwnGameId", "regular", "Castle 1", 100.0, new TreeMap<>(), new TreeMap<>(),
                    100, Collections.emptyList(), "explored", 1000, Map.of("_idDummyTwo",
                    6.5d, "_idDummyThree", 8.5d, "_idDummyFour", 8.5d), 10, 10,
                    "4444", new HashMap<>()
            );
            case "colonized" -> new GameSystem(
                    "createdAtDummyValue", "updatedAtDummyValue", "_idDummyOne", "OwnGameId",
                    "regular", "Castle 1", 100.0, districtSlots, districts, 100, Collections.emptyList()
                    , "colonized", 1050, Map.of("_idDummyTwo", 6.5d,
                    "_idDummyThree", 8.5d, "_idDummyFour", 8.5d), 10, 10, "4444", new HashMap<>()
            );
            case "upgraded" -> new GameSystem(
                    "createdAtDummyValue", "updatedAtDummyValue", "_idDummyOne", "OwnGameId",
                    "regular", "Castle 1", 100.0, districtSlots, districts, 125, Collections.emptyList(),
                    "upgraded", 1071, Map.of("_idDummyTwo", 6.5d, "_idDummyThree", 8.5d,
                    "_idDummyFour", 8.5d), 10, 10, "4444", new HashMap<>()
            );
            case "developed" -> new GameSystem(
                    "createdAtDummyValue", "updatedAtDummyValue", "_idDummyOne", "OwnGameId",
                    "regular", "Castle 1", 100.0, districtSlots, districts, 156, Collections.emptyList(),
                    "developed", 1082, Map.of("_idDummyTwo", 6.5d, "_idDummyThree", 8.5d, "_idDummyFour",
                    8.5d), 10, 10, "4444", new HashMap<>()
            );
            default -> new GameSystem(
                    "createdAtDummyValue", "updatedAtDummyValue", "_idDummyOne", "OwnGameId",
                    "regular", "Castle 1", 100.0, districtSlots, districts, 100, Collections.emptyList(),
                    "unexplored", 1000, Map.of("_idDummyTwo", 6.5d, "_idDummyThree", 8.5d,
                    "_idDummyFour", 8.5d), 10, 10, "4444", new HashMap<>());
        };
    }

    private GameSystem districtAndBuilding(String action) {
        TreeMap<String, Integer> districtSlots = new TreeMap<>(Map.of("energy", 5));
        TreeMap<String, Integer> districts = new TreeMap<>(Map.of("energy", 2));
        TreeMap<String, Integer> districtsBuild = new TreeMap<>(Map.of("energy", 3));
        List<String> buildings = new ArrayList<>() {{
            add("farm");
        }};
        return switch (action) {
            case "buildDistrict" -> new GameSystem(
                    "createdAtDummyValue", "updatedAtDummyValue", "_idDummyOne", "OwnGameId",
                    "regular", "Castle 1", 100.0, districtSlots, districtsBuild, 100, Collections.emptyList(),
                    "developed", 1000, Map.of("_idDummyTwo", 6.5d, "_idDummyThree", 8.5d,
                    "_idDummyFour", 8.5d), 10, 10, "4444", new HashMap<>()
            );
            case "buildBuilding" -> new GameSystem(
                    "createdAtDummyValue", "updatedAtDummyValue", "_idDummyOne", "OwnGameId",
                    "regular", "Castle 1", 100.0, districtSlots, districts, 100, buildings,
                    "developed", 1000, Map.of("_idDummyTwo", 6.5d, "_idDummyThree", 8.5d,
                    "_idDummyFour", 8.5d), 10, 10, "4444", new HashMap<>()
            );
            default -> new GameSystem(
                    "createdAtDummyValue", "updatedAtDummyValue", "_idDummyOne", "OwnGameId",
                    "regular", "Castle 1", 100.0, districtSlots, districts, 100, Collections.emptyList(),
                    "developed", 1000, Map.of("_idDummyTwo", 6.5d, "_idDummyThree", 8.5d,
                    "_idDummyFour", 8.5d), 10, 10, "4444", new HashMap<>());
        };
    }

    private Job getUpgradeJob() {
        return new Job("",
                "",
                "testJobId",
                0,
                0,
                "OwnGameId",
                "4444",
                "_idDummyOne",
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

    private Job getDistrictJob() {
        return new Job("",
                "",
                "testJobId",
                0,
                0,
                "OwnGameId",
                "4444",
                "_idDummyOne",
                0,
                "district",
                null,
                "energy",
                null,
                null,
                null,
                null,
                new TreeMap<>(Map.of("minerals", 75)),
                null
        );
    }

    private List<Trait> createAvailableTraits() {
        Trait prepared = new Trait(
                "prepared",
                List.of(
                        new Effect("resources.credits.starting", 0, 0, 200),
                        new Effect("resources.energy.starting", 0, 0, 20),
                        new Effect("resources.minerals.starting", 0, 0, 20),
                        new Effect("resources.food.starting", 0, 0, 20),
                        new Effect("resources.research.starting", 0, 0, 20),
                        new Effect("resources.fuel.starting", 0, 0, 20)
                ),
                1,
                List.of("unprepared")
        );

        Trait unprepared = new Trait(
                "unprepared",
                List.of(
                        new Effect("resources.credits.starting", 0, 0, -200),
                        new Effect("resources.energy.starting", 0, 0, -20),
                        new Effect("resources.minerals.starting", 0, 0, -20),
                        new Effect("resources.food.starting", 0, 0, -20),
                        new Effect("resources.research.starting", 0, 0, -20),
                        new Effect("resources.fuel.starting", 0, 0, -20)
                ),
                -1,
                List.of("prepared")
        );

        Trait strong = new Trait(
                "strong",
                List.of(
                        new Effect("buildings.mine.production.minerals", 0, 1.05, 0)
                ),
                1,
                List.of("weak")
        );

        Trait intelligent = new Trait(
                "intelligent",
                List.of(
                        new Effect("buildings.research_lab.production.research", 0, 1.1, 0)
                ),
                3,
                List.of("dumb")
        );

        return List.of(prepared, unprepared, strong, intelligent);
    }

    private Map<String, SystemType> createSystemTypes() {
        return new HashMap<>(Map.of(
                "regular", new SystemType("regular", 10, List.of(10, 25), 0.9),
                "energy", new SystemType("energy", 8, List.of(12, 25), 1),
                "mining", new SystemType("mining", 8, List.of(13, 23), 1),
                "agriculture", new SystemType("agriculture", 8, List.of(15, 30), 1)
        ));
    }

    private List<Job> createJobs() {
        TreeMap<String, Integer> cost = new TreeMap<>(Map.of("energy", 200));
        Job dummyJobOne = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId1",
                3,
                6,
                "OwnGameId",
                "4444",
                "_idDummyOne",
                0,
                "building",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                cost,
                Map.of("minerals", 100, "energy", 25)
        );
        Job dummyJobTwo = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId2",
                0,
                6,
                "OwnGameId",
                "4444",
                "_idDummyTwo",
                0,
                "building",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                cost,
                Map.of("minerals", 100, "energy", 25)
        );
        List<Job> jobList = getJobs(cost, dummyJobOne, dummyJobTwo);
        jobList.addAll(createEnhancementsJobs());
        return jobList;
    }

    private List<Job> getJobs(TreeMap<String, Integer> cost, Job dummyJobOne, Job dummyJobTwo) {
        Job dummyJobThree = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId3",
                0,
                6,
                "OwnGameId",
                "4444",
                "testSystemId3",
                0,
                "building",
                "mine",
                "energy",
                null,
                null,
                null,
                null,
                cost,
                Map.of("minerals", 100, "energy", 25)
        );

        List<Job> jobList = new ArrayList<>();
        jobList.add(dummyJobOne);
        jobList.add(dummyJobTwo);
        jobList.add(dummyJobThree);
        return jobList;
    }

    private ObservableList<Job> createEnhancementsJobs() {
        TreeMap<String, Integer> cost = new TreeMap<>(Map.of("research", 100));
        Job dummyJobOne = new Job(
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
        ObservableList<Job> jobList = FXCollections.observableArrayList();
        jobList.add(dummyJobOne);
        return jobList;
    }

    private List<Job> createJobsNoType() {
        Job dummyJobOne = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId1",
                3,
                6,
                "OwnGameId",
                "4444",
                "_idDummyOne",
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
        Job dummyJobTwo = new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId2",
                0,
                6,
                "OwnGameId",
                "4444",
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
                "OwnGameId",
                "4444",
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
        Job dummyJobFour = getJob();
        return List.of(
                dummyJobOne,
                dummyJobTwo,
                dummyJobThree,
                dummyJobFour
        );
    }

    private Job getJob() {
        TreeMap<String, Integer> cost = new TreeMap<>(Map.of("research", 200));
        return new Job(
                "createdAtDummyValue",
                "updatedAtDummyValue",
                "testJobId4",
                3,
                6,
                "OwnGameId",
                "4444",
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
                "computing",
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

    private List<War> getWars() {
        return List.of();
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
}
