package de.uniks.stp24.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.stp24.App;
import de.uniks.stp24.component.*;
import de.uniks.stp24.component.buildings.BuildingsViewComponent;
import de.uniks.stp24.component.districts.DistrictComponent;
import de.uniks.stp24.component.enhancements.EnhancementComponent;
import de.uniks.stp24.component.events.EventComponent;
import de.uniks.stp24.component.events.EventPreviewComponent;
import de.uniks.stp24.component.players.PlayerListComponent;
import de.uniks.stp24.component.popups.DeleteAccPopUpComponent;
import de.uniks.stp24.component.popups.DeleteGamePopUpComponent;
import de.uniks.stp24.component.popups.JoinGamePopupComponent;
import de.uniks.stp24.component.popups.PauseMenuPopUpComponent;
import de.uniks.stp24.component.traits.TraitsComponent;
import de.uniks.stp24.dto.CreateMemberDto;
import de.uniks.stp24.model.EmpireTemplate;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.rest.*;
import de.uniks.stp24.service.*;
import de.uniks.stp24.ws.EventListener;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.controller.SubComponent;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.forloop.FxFor;
import org.fulib.fx.controller.Subscriber;
import retrofit2.Retrofit;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ResourceBundle;

public class BaseController {
    // Basics
    @Inject
    App app;
    @Inject
    @Resource
    ResourceBundle bundle;
    @Inject
    Subscriber subscriber;

    // Components
    @Inject
    Provider<SideButtonsComponent> sideButtonsComponentProvider;
    @Inject
    Provider<BattleResult> battleResultComponentProvider;
    @Inject
    Provider<MarketComponent> marketComponentProvider;
    @Inject
    Provider<DeleteAccPopUpComponent> deleteAccPopUpComponent;
    @Inject
    Provider<DeleteGamePopUpComponent> deleteGamePopUpComponent;
    @Inject
    Provider<ExploreCastleComponent> exploreCastleComponentProvider;
    @Inject
    Provider<BuildingsViewComponent> buildingsViewComponentProvider;
    @Inject
    Provider<CastleViewComponent> castleViewComponentProvider;
    @Inject
    Provider<DistrictComponent> districtComponentProvider;
    @Inject
    Provider<GameComponent> gameComponentProvider;
    @Inject
    Provider<JoinGamePopupComponent> joinGamePopupComponentProvider;
    @Inject
    Provider<StatisticsComponent> statisticsComponentProvider;
    @Inject
    Provider<MemberComponent> memberComponentProvider;
    @Inject
    Provider<PauseMenuPopUpComponent> pauseMenuComponentProvider;
    @Inject
    Provider<ResourceBarComponent> resourceBarComponent;
    @Inject
    Provider<PlayerListComponent> playerListComponentProvider;
    @Inject
    Provider<CastleListComponent> castleListComponentProvider;
    @Inject
    Provider<TroopsListComponent> troopsListComponentProvider;
    @Inject
    Provider<PauseTextComponent> pauseTextComponentProvider;
    @Inject
    Provider<TraitsComponent> traitsComponentProvider;
    @Inject
    Provider<EventPreviewComponent> eventPreviewComponentProvider;
    @Inject
    Provider<EventComponent> eventComponentProvider;
    @Inject
    Provider<EnhancementComponent> enhancementComponentProvider;
    @Inject
    @SubComponent
    Provider<TasksViewComponent> tasksViewComponentProvider;
    @Inject
    Provider<CastleComponent> castleComponentProvider;
    @Inject
    Provider<HomeSystemComponent> homeSystemComponentProvider;
    @Inject
    public ZoomDragComponent zoomDragComponent;

    // REST
    @Inject
    Retrofit retrofit;
    @Inject
    AuthApiService authApiService;
    @Inject
    GameMembersApiService gameMembersApiService;
    @Inject
    GameEmpiresApiService gameEmpiresApiService;
    @Inject
    GamesApiService gamesApiService;
    @Inject
    UsersApiService usersApiService;
    @Inject
    GameSystemsApiService gameSystemsApiService;
    @Inject
    FriendsApiService friendsApiService;
    @Inject
    JobsApiService jobsApiService;
    @Inject
    WarsApiService warsApiService;
    @Inject
    FleetsApiService fleetsApiService;

    // Services
    @Inject
    DiscordActivityService discordActivityService;
    @Inject
    BattleResultService battleResultService;
    @Inject
    ErrorService errorService;
    @Inject
    GameService gameService;
    @Inject
    ImageCache imageCache;
    @Inject
    LicensesAndCreditsService licensesAndCreditsService;
    @Inject
    LoginService loginService;
    @Inject
    MembersService membersService;
    @Inject
    PrefService prefService;
    @Inject
    TokenStorage tokenStorage;
    @Inject
    IngameService ingameService;
    @Inject
    EmojiService emojiService;
    @Inject
    EmpireService empireService;
    @Inject
    AudioService audioService;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    NotificationService notificationService;
    @Inject
    PresetsService presetsService;
    @Inject
    GameTicksService gameTicksService;
    @Inject
    JobService jobService;
    @Inject
    EventService eventService;
    @Inject
    ClientChangeService clientChangeService;
    @Inject
    MapService mapService;

    // WebSocket
    @Inject
    EventListener eventListener;

    // Parameters
    @Param("username")
    String username;
    @Param("password")
    String password;
    @Param("accDeleted")
    Boolean accDeleted;
    @Param("empireTemplate")
    EmpireTemplate empireTemplate;
    @Param("system")
    GameSystem system;
    @Param("game")
    Game game;
    @Param("createMemberDto")
    CreateMemberDto createMemberDto;

    // Other
    @Inject
    Provider<ResourceBundle> bundleProvider;
    @Inject
    public FxFor fxFor;

    @OnDestroy
    public void destroy() {
        subscriber.dispose();
    }
}
