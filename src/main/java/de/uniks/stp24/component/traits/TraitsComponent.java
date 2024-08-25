package de.uniks.stp24.component.traits;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Effect;
import de.uniks.stp24.model.EmpireTemplate;
import de.uniks.stp24.model.Trait;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.PresetsService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.controller.SubComponent;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

import static de.uniks.stp24.util.Constants.*;
import static de.uniks.stp24.util.Methods.initListView;

@Component(view = "Traits.fxml")
public class TraitsComponent extends VBox {
    @FXML
    Label errorLabel;
    @FXML
    ListView<Trait> availableTraitsList;
    @FXML
    ListView<Trait> ownedTraitsList;
    @FXML
    Button traitButton;
    @FXML
    Label stillSelectLabel;
    @FXML
    Label traitPointsLabel;
    @FXML
    ImageView traitPointsIcon;
    @FXML
    ImageView traitPointsButtonIcon;
    @FXML
    Label traitNameLabel;
    @FXML
    Label traitButtonLabel;
    @FXML
    Label traitButtonCostLabel;
    @FXML
    VBox traitInformationVBox;
    @FXML
    ScrollPane scrollPaneTraitInfo;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public PresetsService presetsService;
    @Inject
    public Provider<TraitComponent> traitComponentProvider;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;

    @SubComponent
    @Inject
    public TraitInfoComponent traitStartingComponent;
    @SubComponent
    @Inject
    public TraitInfoComponent traitProductionComponent;
    @SubComponent
    @Inject
    public TraitInfoComponent traitCostComponent;
    @SubComponent
    @Inject
    public TraitInfoComponent traitConflictsComponent;

    @Param("empireTemplate")
    EmpireTemplate empireTemplate;

    private int traitPoints = MAX_TRAIT_AMOUNT;

    private final ObservableList<Trait> availableTraits = FXCollections.observableArrayList();
    private final ObservableList<Trait> ownedTraits = FXCollections.observableArrayList();
    private final Map<String, Trait> storeTraits = new HashMap<>();
    private final Map<Trait, Integer> availableTraitsListPosition = new HashMap<>();

    private final List<Effect> startingEffects = new ArrayList<>();
    private final List<Effect> costEffects = new ArrayList<>();
    private final List<Effect> productionEffects = new ArrayList<>();

    private final SimpleBooleanProperty maxTraitsSelected = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty notEnoughTraitPoints = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty hasConflict = new SimpleBooleanProperty(false);

    private ChangeListener<Trait> availableTraitsListListener;
    private ChangeListener<Trait> ownedTraitsListListener;

    @Inject
    public TraitsComponent() {
    }

    public List<Trait> getOwnedTraits() {
        return ownedTraits;
    }

    @OnInit
    public void onInit() {
        subscriber.subscribe(presetsService.getCachedPreset("getTraits"), traits -> {
            List<Trait> traitsList = (List<Trait>) traits;
            initTraits(traitsList);
            renderTraits();
        });
    }

    private void initTraits(List<Trait> traitsList) {
        int position = 0;
        for (Trait trait : traitsList) {
            storeTraits.put(trait.id(), trait);
            if (!"__dev__".equals(trait.id())) {
                availableTraits.add(trait);
                availableTraitsListPosition.put(trait, position++);
            }
        }

        empireTemplate.traits().forEach(traitId -> ownedTraits.add(storeTraits.get(traitId)));
        availableTraits.removeAll(ownedTraits);

        // init trait points
        for (Trait trait : ownedTraits) {
            if (trait.cost() < 0) {
                traitPoints += Math.abs(trait.cost());
            } else {
                traitPoints -= trait.cost();
            }
        }
    }

    void renderTraits() {
        updateStillSelectText();
        updateTraitPointsLabel();
        setTraitIcons();

        initListView(ownedTraitsList, ownedTraits, app, traitComponentProvider, Map.of());
        initListView(availableTraitsList, availableTraits, app, traitComponentProvider, Map.of());

        setupListeners();
        availableTraitsList.getSelectionModel().selectFirst();

        traitButton.disableProperty().bind(maxTraitsSelected.or(notEnoughTraitPoints).or(hasConflict));
        checkCanSelect();

        // Show fitting error message
        errorLabel.textProperty().bind(
                Bindings.when(maxTraitsSelected).then(bundle.getString("traits.max.selected"))
                        .otherwise(Bindings.when(notEnoughTraitPoints).then(bundle.getString("traits.not.enough.points"))
                                .otherwise(Bindings.when(hasConflict).then(bundle.getString("traits.conflict"))
                                        .otherwise("")))
        );
    }

    private void setTraitIcons() {
        traitPointsIcon.setImage(imageCache.get("image/icons/trait_points.png"));
        traitPointsButtonIcon.setImage(imageCache.get("image/icons/trait_points.png"));
    }

    private void setupListeners() {
        availableTraitsList.getSelectionModel().selectedItemProperty().addListener(availableTraitsListListener = (observable, oldValue, newValue) -> {
            if (newValue == null || Objects.equals(oldValue, newValue)) {
                return;
            }
            handleTraitSelection(newValue);
        });

        ownedTraitsList.getSelectionModel().selectedItemProperty().addListener(ownedTraitsListListener = (observable, oldValue, newValue) -> {
            if (newValue == null || Objects.equals(oldValue, newValue)) {
                return;
            }
            handleTraitDeselection(newValue);
        });
    }

    private void handleTraitSelection(Trait newValue) {
        fillInformationTrait(newValue);
        updateToSelectButtonUI(newValue);
        checkCanSelect();
        ownedTraitsList.getSelectionModel().clearSelection();
    }

    private void handleTraitDeselection(Trait newValue) {
        updateToDeselectButtonUI(newValue);
        fillInformationTrait(newValue);
        maxTraitsSelected.set(false);
        notEnoughTraitPoints.set(false);
        hasConflict.set(false);
        availableTraitsList.getSelectionModel().clearSelection();
    }

    private void updateToSelectButtonUI(Trait trait) {
        traitButton.setOnAction(event -> selectTrait());
        traitButtonLabel.setText(bundle.getString("select.button"));
        traitButton.getStyleClass().remove("back-button");
        setTraitButtonCostLabel(trait.cost(), false);
    }

    private void updateToDeselectButtonUI(Trait trait) {
        traitButton.setOnAction(event -> deselectTrait());
        traitButtonLabel.setText(bundle.getString("deselect.button"));
        traitButton.getStyleClass().add("back-button");
        setTraitButtonCostLabel(trait.cost(), true);
    }

    private void setTraitButtonCostLabel(int cost, boolean isDeselect) {
        if (cost == 0) {
            traitButtonCostLabel.setText(String.valueOf(cost));
        } else if (isDeselect) {
            traitButtonCostLabel.setText((cost > 0 ? "+ " : "- ") + Math.abs(cost));
        } else {
            traitButtonCostLabel.setText((cost > 0 ? "- " : "+ ") + Math.abs(cost));
        }
    }

    private void fillInformationTrait(Trait newValue) {
        traitInformationVBox.getChildren().clear();
        traitNameLabel.setText(bundle.getString("traits." + newValue.id()));
        traitInformationVBox.getChildren().add(traitNameLabel);

        fillEffectLists(newValue);

        addTraitInfoComponent(traitStartingComponent, TRAIT_INFO_TYPE_STARTING, startingEffects, newValue);
        addTraitInfoComponent(traitCostComponent, TRAIT_INFO_TYPE_COST, costEffects, newValue);
        addTraitInfoComponent(traitProductionComponent, TRAIT_INFO_TYPE_PRODUCTION, productionEffects, newValue);
        addTraitInfoComponent(traitConflictsComponent, TRAIT_INFO_TYPE_CONFLICTS, null, newValue);
    }

    private void fillEffectLists(Trait trait) {
        startingEffects.clear();
        costEffects.clear();
        productionEffects.clear();

        for (Effect effect : trait.effects()) {
            String variable = effect.variable();
            if (variable.contains("starting")) {
                startingEffects.add(effect);
            } else if (variable.contains("cost")) {
                costEffects.add(effect);
            } else if (variable.contains("production")) {
                productionEffects.add(effect);
            }
        }
    }

    private void addTraitInfoComponent(TraitInfoComponent component, String type, List<Effect> effects, Trait trait) {
        if ((effects != null && !effects.isEmpty()) || (type.equals(TRAIT_INFO_TYPE_CONFLICTS) && !trait.conflicts().isEmpty())) {
            component.setTraitInfoComponent(type, effects, trait);
            traitInformationVBox.getChildren().add(component);
        }
    }

    private void updateStillSelectText() {
        int stillSelectTraits = MAX_TRAIT_AMOUNT - ownedTraits.size();
        if (stillSelectTraits == 0) {
            stillSelectLabel.setText(bundle.getString("traits.cannot.choose"));
            return;
        }
        String stillSelectText = String.format(bundle.getString("traits.stillSelect") + " %d %s%s.",
                stillSelectTraits,
                bundle.getString("traits"),
                bundle.getLocale().equals(Locale.ENGLISH) ? "" : " " + bundle.getString("select"));

        if (stillSelectTraits == 1) {
            stillSelectText = stillSelectText.replace(bundle.getString("traits"), bundle.getString("trait"));
        }

        stillSelectLabel.setText(stillSelectText);
    }

    private void updateTraitPointsLabel() {
        traitPointsLabel.setText(String.valueOf(traitPoints));
    }

    private void checkCanSelect() {
        Trait selectedTrait = availableTraitsList.getSelectionModel().getSelectedItem();

        maxTraitsSelected.set(ownedTraits.size() == MAX_TRAIT_AMOUNT);
        notEnoughTraitPoints.set(traitPoints < selectedTrait.cost());
        hasConflict.set(ownedTraits.stream().anyMatch(trait -> trait.conflicts().contains(selectedTrait.id())));
    }

    public void selectTrait() {
        Trait selectedTrait = availableTraitsList.getSelectionModel().getSelectedItem();

        // change the trait lists
        ownedTraits.add(selectedTrait);
        availableTraits.remove(selectedTrait);

        // change the trait points
        calculateNewTraitPoints(selectedTrait, false);
        updateTraitPointsAndStillSelectUI();

        availableTraitsList.getSelectionModel().clearSelection();
        ownedTraitsList.getSelectionModel().selectLast();
    }

    public void deselectTrait() {
        Trait deselectedTrait = ownedTraitsList.getSelectionModel().getSelectedItem();

        addSelectedTraitToAvailableList(deselectedTrait);
        ownedTraits.remove(deselectedTrait);

        // change the trait points
        calculateNewTraitPoints(deselectedTrait, true);
        updateTraitPointsAndStillSelectUI();

        ownedTraitsList.getSelectionModel().clearSelection();
        availableTraitsList.getSelectionModel().select(deselectedTrait);
    }

    private void addSelectedTraitToAvailableList(Trait deselectedTrait) {
        for (int i = 0; i < availableTraits.size(); i++) {
            Trait trait = availableTraits.get(i);
            if (availableTraitsListPosition.get(trait) > availableTraitsListPosition.get(deselectedTrait)) {
                availableTraits.add(i, deselectedTrait);
                return;
            }
        }
    }

    private void updateTraitPointsAndStillSelectUI() {
        updateTraitPointsLabel();
        updateStillSelectText();
    }

    private void calculateNewTraitPoints(Trait trait, boolean isDeselect) {
        int cost = trait.cost();

        if (isDeselect) {
            if (trait.cost() < 0) {
                this.traitPoints -= Math.abs(cost);
            } else {
                this.traitPoints += cost;
            }
        } else { // select trait
            if (trait.cost() < 0) {
                this.traitPoints += Math.abs(cost);
            } else {
                this.traitPoints -= cost;
            }
        }
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
        if (availableTraitsListListener != null) {
            availableTraitsList.getSelectionModel().selectedItemProperty().removeListener(availableTraitsListListener);
        }
        if (ownedTraitsListListener != null) {
            ownedTraitsList.getSelectionModel().selectedItemProperty().removeListener(ownedTraitsListListener);
        }
    }
}
