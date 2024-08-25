package de.uniks.stp24.controller;

import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.component.HomeSystemComponent;
import de.uniks.stp24.component.traits.TraitComponent;
import de.uniks.stp24.component.traits.TraitEffectComponent;
import de.uniks.stp24.component.traits.TraitInfoComponent;
import de.uniks.stp24.component.traits.TraitsComponent;
import de.uniks.stp24.dto.CreateMemberDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.PresetsApiService;
import de.uniks.stp24.service.MembersService;
import de.uniks.stp24.service.NotificationService;
import de.uniks.stp24.service.PresetsService;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.util.WaitForAsyncUtils;

import javax.inject.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmpireControllerTest extends ControllerTest {
    @Mock
    EventListener eventListener;
    @Mock
    MembersService membersService;
    @Mock
    TokenStorage tokenStorage;
    @Spy
    NotificationService notificationService;
    @Mock
    PresetsApiService presetsApiService;

    @Spy
    PresetsService presetsService;

    final Subject<Event<Game>> subjectGame = BehaviorSubject.create();

    @Spy
    final
    Provider<TraitEffectComponent> traitEffectComponentProvider = spyProvider(() -> {
        final TraitEffectComponent traitEffectComponent = new TraitEffectComponent();
        traitEffectComponent.bundle = bundle;
        traitEffectComponent.imageCache = imageCache;
        return traitEffectComponent;
    });

    @Spy
    final
    Provider<TraitInfoComponent> traitInfoComponentProvider = spyProvider(() -> {
        final TraitInfoComponent traitInfoComponent = new TraitInfoComponent();
        traitInfoComponent.bundle = bundle;
        traitInfoComponent.imageCache = imageCache;
        traitInfoComponent.app = app;
        traitInfoComponent.subscriber = subscriber;
        traitInfoComponent.traitEffectComponentProvider = traitEffectComponentProvider;
        return traitInfoComponent;
    });

    @Spy
    final
    Provider<TraitComponent> traitComponentProvider = spyProvider(() -> {
        final TraitComponent traitComponent = new TraitComponent();
        traitComponent.bundle = bundle;
        traitComponent.imageCache = imageCache;
        return traitComponent;
    });

    @Spy
    final
    Provider<TraitsComponent> traitsComponentProvider = spyProvider(() -> {
        final TraitsComponent traitsComponent = new TraitsComponent();
        traitsComponent.app = app;
        traitsComponent.subscriber = subscriber;
        traitsComponent.presetsService = presetsService;
        traitsComponent.bundle = bundle;
        traitsComponent.imageCache = imageCache;
        traitsComponent.traitComponentProvider = traitComponentProvider;
        traitsComponent.traitStartingComponent = traitInfoComponentProvider.get();
        traitsComponent.traitCostComponent = traitInfoComponentProvider.get();
        traitsComponent.traitProductionComponent = traitInfoComponentProvider.get();
        traitsComponent.traitConflictsComponent = traitInfoComponentProvider.get();
        return traitsComponent;
    });

    @Spy
    final
    Provider<HomeSystemComponent> homeSystemComponentProvider = spyProvider(() -> {
        final HomeSystemComponent homeSystemComponent = new HomeSystemComponent();
        homeSystemComponent.bundle = bundle;
        return homeSystemComponent;
    });

    @InjectMocks
    EmpireController empireController;

    @Override
    public void start(Stage stage) throws Exception {
        Mockito.doReturn(subjectGame).when(eventListener).listen("games.testGameId.*", Game.class);

        notificationService.app = app;
        notificationService.imageCache = imageCache;
        super.start(stage);
        presetsService.presetsApiService = presetsApiService;
        empireController.traitsComponentProvider = traitsComponentProvider;
        empireController.homeSystemComponentProvider = homeSystemComponentProvider;
        Mockito.doReturn(Observable.just(createAvailableTraits())).when(presetsApiService).getTraits();
        doReturn(Observable.just(createSystemTypes())).when(presetsApiService).getSystemTypes();
    }

    @Test
    void testApplyInfoFromPassedEmpireTemplate() {
        WaitForAsyncUtils.waitForFxEvents();

        // Start:
        // Jan already configured his empire
        EmpireTemplate empireTemplate = new EmpireTemplate("testEmpire", "testDescription", "FFFFFF", 0, 0, List.of(), List.of(), null, null, "testId");

        // Action:
        // Jan goes back to the build empire screen
        Platform.runLater(() -> app.show(empireController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, empireTemplate, "string"),
                        "empireTemplate", empireTemplate)));

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // Jan sees the empire template he configured before already filled in
        assertEquals(empireTemplate.name(), empireController.txtInputName.getText());
    }

    @Test
    void testSave() {
        EmpireTemplate empireTemplate = new EmpireTemplate("testEmpire", "testDescription", "FFFFFF", 0, 0, List.of(), List.of(), null, null, "testId");

        Mockito.doReturn("").when(tokenStorage).getUserId();
        Mockito.doReturn(Observable.just(new Member("", "", "", "", false, empireTemplate))).when(membersService).updateMember(any(), any(), any());

        // Start:
        // Jan finished configuring his empire and wants to save it

        Platform.runLater(() -> app.show(empireController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, empireTemplate, "string"),
                        "empireTemplate", empireTemplate)));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // Jan presses save

        clickOn("Save");

        // Result:
        // Jan's empire is saved
        verify(membersService, times(1)).updateMember(any(), any(), any());
    }

    @Test
    void testBack() {
        // Start:
        // Jan finished configuring his empire and wants to return to the lobby
        Mockito.doReturn(null).when(app).show(eq("/members"), any());

        EmpireTemplate empireTemplate = new EmpireTemplate("testEmpire", "testDescription", "FFFFFF", 0, 0, List.of(), List.of(), null, null, "testId");

        Platform.runLater(() -> app.show(empireController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, empireTemplate, "string"),
                        "empireTemplate", empireTemplate)));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // Jan presses back

        clickOn("Back");

        // Result:
        // Jan is now back in the lobby
        verify(app, times(1)).show(eq("/members"), any());
    }

    @Test
    void testFlagChoosability() {

        // Start:
        // Jan wants to select a flag for his empire
        EmpireTemplate empireTemplate = new EmpireTemplate("testEmpire", "testDescription", "FFFFFF", 0, 0, List.of(), List.of(), null, null, "testId");

        Platform.runLater(() -> app.show(empireController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, empireTemplate, "string"),
                        "empireTemplate", empireTemplate)));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // Jan clicks the decrease and increase button

        clickOn((Node) lookup(".tab-pane > .tab-header-area > .headers-region > .tab").nth(1).query());

        assertEquals("1", empireController.txtFlagID.getText().split("/")[0]);

        clickOn(empireController.btnFlagDecrease);

        assertEquals(String.valueOf(empireController.MAX_FLAG_ID), empireController.txtFlagID.getText().split("/")[0]);

        clickOn(empireController.btnFlagIncrease);

        assertEquals("1", empireController.txtFlagID.getText().split("/")[0]);

        // Result:
        // He sees a new image when cycling through the portraits and sees that the text below is changing
        // to the corresponding portrait id that he is currently seeing
    }

    @Test
    void testPortraitChoosability() {
        // Start:
        // Jan wants to select a portrait for his empire
        EmpireTemplate empireTemplate = new EmpireTemplate("testEmpire", "testDescription", "FFFFFF", 0, 0, List.of(), List.of(), null, null, "testId");

        Platform.runLater(() -> app.show(empireController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, empireTemplate, "string"),
                        "empireTemplate", empireTemplate)));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // Jan clicks the decrease and increase button

        clickOn((Node) lookup(".tab-pane > .tab-header-area > .headers-region > .tab").nth(1).query());

        assertEquals("1", empireController.txtPortraitID.getText().split("/")[0]);

        clickOn(empireController.btnPortraitDecrease);

        assertEquals(String.valueOf(empireController.MAX_PORTRAIT_ID), empireController.txtPortraitID.getText().split("/")[0]);

        clickOn(empireController.btnPortraitIncrease);

        assertEquals("1", empireController.txtPortraitID.getText().split("/")[0]);

        // Result:
        // He sees a new image when cycling through the portraits and sees that the text below is changing
        // to the corresponding portrait id that he is currently seeing
    }

    @Test
    void testGameDeletionDuringEmpireConfiguration() {
        Mockito.doReturn(null).when(app).show(eq("/lobby"));

        // Start:
        // Jan wants to configure his empire
        EmpireTemplate empireTemplate = new EmpireTemplate("testEmpire", "testDescription", "FFFFFF", 0, 0, List.of(), List.of(), null, null, "testId");
        Game game = new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100));
        Platform.runLater(() -> app.show(empireController,
                Map.of("game", game,
                        "createMemberDto", new CreateMemberDto(true, empireTemplate, "string"),
                        "empireTemplate", empireTemplate)));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // During the empire configuration, the owner of the game deletes the game

        subjectGame.onNext(new Event<>("games.testGameId.deleted", game));

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // Jan will be sent back to the lobby

        verify(app, times(1)).show(eq("/lobby"));
    }

    @Test
    void selectAndDeselectPositiveTraits() {
        // Start:
        // Jan wants to select traits for his empire. He sees a list of traits he can choose from. He wants to select the trait the "Prepared" trait.
        // He can select still 5 traits and
        EmpireTemplate empireTemplate = new EmpireTemplate("testEmpire", "testDescription", "FFFFFF", 0, 0, List.of(), List.of(), null, null, "testId");

        Platform.runLater(() -> app.show(empireController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, empireTemplate, "string"),
                        "empireTemplate", empireTemplate)));

        WaitForAsyncUtils.waitForFxEvents();

        clickOn("Traits");

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

        // Action:
        // Jan selects the trait prepared
        Button traitButton = lookup("#traitButton").query();
        assertFalse(traitButton.isDisabled());
        clickOn(traitButton);

        // Result:
        // In the trait list yours appears the "Prepared" trait prepared. Jan has now 4 trait points.
        ListView<Trait> yourTraits = lookup("#ownedTraitsList").query();
        assertEquals(1, yourTraits.getItems().size());
        assertEquals(3, availableTraits.getItems().size());
        assertEquals("4", traitPointsLabel.getText());
        assertEquals("You can still choose up to " + 4 + " Traits.", stillSelectLabel.getText());
        assertEquals("Deselect", buttonLabel.getText());
        assertEquals("+ " + 1, buttonCostLabel.getText());


        // Start:
        // Jan owns now the "Prepared" trait. However, he decided to deselect that trait.

        // Action:
        // Jan deselects the "Prepared" trait.

        assertFalse(traitButton.isDisabled());
        clickOn(traitButton);

        // End:
        // Jan has no owned traits anymore and the deselected trait is now in available traits.
        assertEquals(0, yourTraits.getItems().size());
        assertEquals(4, availableTraits.getItems().size());
        assertEquals("5", traitPointsLabel.getText());
        assertEquals(4, availableTraits.getItems().size());
        assertEquals("You can still choose up to " + 5 + " Traits.", stillSelectLabel.getText());
        assertEquals("Select", buttonLabel.getText());
        assertEquals("- " + 1, buttonCostLabel.getText());
    }

    @Test
    void selectAndDeselectNegativeTraits() {
        // Start:
        // Jan wants to select traits for his empire. He sees a list of traits he can choose from. He wants to select the "Unprepared" trait
        EmpireTemplate empireTemplate = new EmpireTemplate("testEmpire", "testDescription", "FFFFFF", 0, 0, List.of(), List.of(), null, null, "testId");

        Platform.runLater(() -> app.show(empireController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, empireTemplate, "string"),
                        "empireTemplate", empireTemplate)));

        WaitForAsyncUtils.waitForFxEvents();

        clickOn("Traits");

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

        // Action:
        // Jan clicks the "Unprepared" trait and selects it.
        clickOn("Unprepared");
        assertEquals("Select", buttonLabel.getText());
        assertEquals("+ " + 1, buttonCostLabel.getText());

        Button traitButton = lookup("#traitButton").query();
        assertFalse(traitButton.isDisabled());
        clickOn(traitButton);

        // End:
        // Jan owns the "Unprepared" trait which appears in your traits. Jan has now 6 trait points.
        ListView<Trait> yourTraits = lookup("#ownedTraitsList").query();
        assertEquals(1, yourTraits.getItems().size());
        assertEquals(3, availableTraits.getItems().size());
        assertEquals("6", traitPointsLabel.getText());
        assertEquals("You can still choose up to " + 4 + " Traits.", stillSelectLabel.getText());
        assertEquals("Deselect", buttonLabel.getText());
        assertEquals("- " + 1, buttonCostLabel.getText());
        assertFalse(traitButton.isDisabled());

        // Start: Jan owns the "Unprepared" trait. Now he wants to deselect it.

        // Action:
        // Jan deselects the "Unprepared" trait.
        clickOn(traitButton);

        // End:
        // Jan has now no owned traits and has again 5 trait points. The "Unprepared" trait is in the available traits.
        assertEquals("5", traitPointsLabel.getText());
        assertEquals(4, availableTraits.getItems().size());
        assertEquals("You can still choose up to " + 5 + " Traits.", stillSelectLabel.getText());
        assertEquals("Select", buttonLabel.getText());
        assertEquals("+ " + 1, buttonCostLabel.getText());

    }

    @Test
    void canNotSelectTraitConflict() {
        // Start: Jan wants to select traits for his empire. He sees a list of traits he can choose from. He wants to select the "Prepared" trait.
        // Currently he owns the "Unprepared" trait
        EmpireTemplate empireTemplate = new EmpireTemplate("testEmpire", "testDescription", "FFFFFF", 0, 0, List.of("unprepared"), List.of(), null, null, "testId");

        Platform.runLater(() -> app.show(empireController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, empireTemplate, "string"),
                        "empireTemplate", empireTemplate)));

        WaitForAsyncUtils.waitForFxEvents();

        clickOn("Traits");

        ListView<Trait> availableTraits = lookup("#availableTraitsList").query();
        Label traitPointsLabel = lookup("#traitPointsLabel").query();
        Label stillSelectLabel = lookup("#stillSelectLabel").query();
        Label buttonLabel = lookup("#traitButtonLabel").query();
        Label buttonCostLabel = lookup("#traitButtonCostLabel").query();

        assertEquals("6", traitPointsLabel.getText());
        assertEquals(3, availableTraits.getItems().size());
        assertEquals("You can still choose up to " + 4 + " Traits.", stillSelectLabel.getText());
        assertEquals("Select", buttonLabel.getText());
        assertEquals("- " + 1, buttonCostLabel.getText());

        // Action:
        // Jan selects the "Prepared" trait

        clickOn("Prepared");

        // Result: Jan cannot select that trait because the button is disabled and the trait "Prepared" is in conflict with the "Unprepared" trait.
        Button traitButton = lookup("#traitButton").query();
        assertTrue(traitButton.isDisabled());
    }

    public List<Trait> createAvailableTraits() {
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
                "energy", new SystemType("regular", 8, List.of(12, 25), 1),
                "mining", new SystemType("regular", 8, List.of(13, 23), 1),
                "agriculture", new SystemType("regular", 8, List.of(15, 30), 1)
        ));
    }
}
