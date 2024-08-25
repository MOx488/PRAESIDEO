package de.uniks.stp24.component.enhancements;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.CreateJobDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameLogicApiService;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.onTaskEvent;

@Component(view = "EnhancementSelected.fxml")
public class EnhancementSelectedComponent extends VBox {
    @FXML
    SplitPane tooltipBackground;
    @FXML
    Button unlockButton;
    @FXML
    ImageView costImage;
    @FXML
    Label costLabel;
    @FXML
    ImageView selectedCross;
    @FXML
    VBox selectedEnhancementText;
    @FXML
    VBox unlockTooltip;

    @Param("enhancement")
    Technology enhancement;
    @Param("allEnhancements")
    List<Technology> allEnhancements;
    @Param("empire")
    Empire empire;
    @Param("game")
    Game game;
    @Param("jobIdToStartPeriod")
    HashMap<String, Integer> jobIdToStartPeriod;
    @Param("enhancementComponent")
    EnhancementComponent enhancementComponent;

    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    public Subscriber subscriber;
    @Inject
    public GameLogicApiService gameLogicApiService;
    @Inject
    public App app;
    @Inject
    public EventListener eventListener;

    private HBox parentContainer;
    private ListView<Technology> parentList;
    private int costTotal = 0;
    private final ObservableList<Job> jobs = FXCollections.observableArrayList();
    private final ResourceBundle englishBundle = ResourceBundle.getBundle("de/uniks/stp24/lang/lang", Locale.ENGLISH);
    private final List<String> technologyTags = List.of("physics", "society", "engineering", "military", "economy", "state", "biology", "energy", "computing", "propulsion", "materials", "construction", "production", "rare", "weaponry", "shipmaking");
    private final SimpleBooleanProperty completedEnhancement = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty progressEnhancement = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty cantAfford = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty requireEnhancement = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty anotherInProgress = new SimpleBooleanProperty(false);

    @Inject
    public EnhancementSelectedComponent() {
    }

    @OnInit
    public void init() {
        subscriber.subscribe(jobsApiService.getFilteredJobs(game._id(), empire._id(), "technology", null, null), jobsList -> {
            jobs.addAll(jobsList);
            checkJobs();
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), empireEvent -> {
            this.empire = empireEvent.data();
            cantAfford.set(costTotal > empire.resources().get("research"));
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".jobs.*.*", Job.class), jobsEvent -> {
            onTaskEvent(jobsEvent, jobs);
            checkJobs();
        });
    }

    private void checkJobs() {
        // check if enhancement is in progress
        boolean inProgress = false;

        // check if any enhancement in this category is in progress
        String tag = "";
        if (enhancement.tags().contains("physics")) {
            tag = "physics";
        } else if (enhancement.tags().contains("society")) {
            tag = "society";
        } else if (enhancement.tags().contains("engineering")) {
            tag = "engineering";
        }

        boolean anyInProgress = false;
        for (Job job : jobs) {
            if (job.technology() != null) {
                Technology tech = getTech(job);
                if (tech.tags().contains(tag)) anyInProgress = true;

                if (job.technology().equals(enhancement.id())) inProgress = true;
            }
        }
        anotherInProgress.set(anyInProgress);
        progressEnhancement.set(inProgress);
    }

    private Technology getTech(Job job) {
        return allEnhancements.stream().filter(tech -> tech.id().equals(job.technology())).findFirst().orElse(null);
    }

    @OnRender(1)
    public void render() {
        this.selectedCross.setImage(imageCache.get("image/circle-xmark-regular.png"));
        renderUnlockButton();
        renderText();
    }

    @OnRender(2)
    public void buttonBinding() {
        // check if enhancement is already completed
        completedEnhancement.set(empire.technologies().contains(enhancement.id()));

        // Bind Button visibility
        unlockButton.visibleProperty().bind(completedEnhancement.not().and(progressEnhancement.not()));

        // check if enough resources and requires any other Enhancement
        if (enhancement.requires() != null) {
            for (String requires : enhancement.requires()) {
                if (!empire.technologies().contains(requires)) {
                    requireEnhancement.set(true);
                }
            }
        }

        unlockButton.disableProperty().bind(cantAfford.or(requireEnhancement).or(anotherInProgress));

    }

    // Adding Labels and Images
    private void addLabelToVBox(VBox vBox, String text, boolean wrapText) {
        Label label = new Label(text);
        label.getStyleClass().add("medium");
        label.setWrapText(wrapText);
        vBox.getChildren().add(label);
    }

    private void addLinesToVBox(VBox vBox) {
        Label lines = new Label("----------------");
        lines.getStyleClass().add("medium");
        vBox.getChildren().add(lines);
    }

    private void addLabelToHBox(HBox hBox, String text, boolean wrapText) {
        Label label = new Label(text);
        label.getStyleClass().add("medium");
        label.setWrapText(wrapText);
        hBox.getChildren().add(label);
    }

    private void addResourceImageToHBox(HBox hBox, Effect effect, String resourceName) {
        String res = resourceName != null ? resourceName : effect.variable().substring(effect.variable().lastIndexOf(".") + 1);
        ImageView imageView = new ImageView(imageCache.get("image/game_resources/" + res + ".png"));
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        hBox.getChildren().add(imageView);
    }

    private boolean hasLabel(VBox vBox, String labelText) {
        return vBox.getChildren().stream()
                .filter(node -> node instanceof Label)
                .map(node -> ((Label) node).getText())
                .anyMatch(text -> text.equals(labelText));
    }

    private void renderText() {
        // show information about the selected enhancement
        addLabelToVBox(selectedEnhancementText, bundle.getString(enhancement.id()), true);

        // show requirements if existing
        if (enhancement.requires() != null) {
            addLinesToVBox(selectedEnhancementText);
            addLabelToVBox(selectedEnhancementText, bundle.getString("selected.requires"), false);
            enhancement.requires().forEach(requirements -> addLabelToVBox(selectedEnhancementText, bundle.getString(requirements), true));
        }

        // show effects
        if (enhancement.effects().isEmpty()) {
            addLinesToVBox(selectedEnhancementText);
            addLabelToVBox(selectedEnhancementText, bundle.getString("info.text"), false);
            addLabelToVBox(selectedEnhancementText, bundle.getString("technologies." + enhancement.id() + ".info"), true);
        } else {
            enhancement.effects().forEach(this::handleEffect);
        }
    }

    private void handleEffect(Effect effect) {
        // filter for different effects
        // type = first word (districts, buildings, ...)
        // Reduction headline -> effect
        String type = effect.variable().substring(0, effect.variable().indexOf("."));
        if (effect.variable().endsWith(".cost_multiplier") || effect.variable().contains(".cost.")) {
            writeReduction(effect, "cost.reduction", type);
        } else if (effect.variable().contains(".upkeep.")) {
            writeReduction(effect, "upkeep.reduction", type);
        } else if (effect.variable().contains(".build_time") || effect.variable().contains(".time_multiplier")
                || effect.variable().contains(".upgrade_time") || effect.variable().contains(".research_time")) {
            writeReduction(effect, "time.reduction", type);
        } else if (effect.variable().contains(".production.")) {
            writeReduction(effect, "production.increase", type);
        } else if (effect.variable().contains(".chance.")) {
            writeReduction(effect, "chance.increase", type);
        } else if (effect.variable().contains(".pop.")) {
            writeReduction(effect, "pop.reductions", type);
        } else if (effect.variable().contains(".pop_growth")) {
            writeReduction(effect, "pop.growth", type);
        } else if (effect.variable().contains(".capacity_multiplier")) {
            writeReduction(effect, "capacity.increase", type);
        } else if (effect.variable().contains(".market.")) {
            writeReduction(effect, "market.reduction", "market");
        } else if (effect.variable().contains(".difficulty")) {
            writeReduction(effect, "difficulty.reduction", type);
        } else if (effect.variable().contains(".speed")) {
            writeReduction(effect, "speed.increase", type);
        } else if (effect.variable().contains(".health")) {
            writeReduction(effect, "health.increase", type);
        } else if (effect.variable().contains(".defense.")) {
            writeReduction(effect, "defense.increase", type);
        } else if (effect.variable().contains(".attack.")) {
            writeReduction(effect, "attack.increase", type);
        }
    }

    private void writeReduction(Effect effect, String reductionText, String type) {
        // check if reduction text for effect is already there
        if (!hasLabel(selectedEnhancementText, bundle.getString(reductionText))) {
            addLinesToVBox(selectedEnhancementText);
            addLabelToVBox(selectedEnhancementText, bundle.getString(reductionText), false);
        }

        // write effect information
        HBox hBox = new HBox();
        hBox.setSpacing(5);
        if (!effect.variable().contains(".colonists")) {
            // write enhancement tag if it effects technologies
            if (type.equals("technologies")) {
                Label tag = new Label();
                tag.getStyleClass().add("enhancementLabel");
                tag.setPadding(new Insets(0, 5, 0, 5));
                setTag(effect.variable(), tag);
                hBox.getChildren().add(tag);
            }
            // write percentage
            double percentage = (effect.multiplier() * 100) - 100;
            Label percentageLabel = new Label((percentage > 0 ? "+" : "") + (int) percentage + "%");
            percentageLabel.getStyleClass().add("medium");
            if (effect.variable().contains(".chance.")) {
                percentageLabel.setMinWidth(35);
            }
            hBox.getChildren().add(percentageLabel);
        }

        // change different effects depending on their type
        switch (type) {
            case "districts":
                handleDistrictsEffect(effect, hBox);
                break;
            case "buildings":
                handleBuildingsEffect(effect, hBox);
                break;
            case "systems":
                handleSystemsEffect(effect, hBox);
                break;
            case "market":
                handleMarketEffect(hBox);
                break;
            case "empire":
                handleEmpireEffect(effect, hBox);
                break;
            case "ships":
                handleShipsEffect(effect, hBox);
                break;
        }

        selectedEnhancementText.getChildren().add(hBox);
    }

    private void handleShipsEffect(Effect effect, HBox hBox) {
        handleSystemsEffect(effect, hBox);
        if (effect.variable().contains("attack") || effect.variable().contains("defense")) {
            int lastIndex = effect.variable().lastIndexOf(".");
            String ship = effect.variable().substring(lastIndex + 1);
            addLabelToHBox(hBox, "against " + bundle.getString(ship), false);
        }
    }

    private void handleDistrictsEffect(Effect effect, HBox hBox) {
        if (effect.variable().contains(".chance.")) {
            int firstIndex = effect.variable().indexOf(".");
            int secondIndex = effect.variable().indexOf(".", firstIndex + 1);
            String district1 = effect.variable().substring(effect.variable().lastIndexOf(".") + 1);
            String district2 = effect.variable().substring(firstIndex + 1, secondIndex);
            addLabelToHBox(hBox, bundle.getString("chance.district.begin") + " " + bundle.getString(district1) + " " +
                    bundle.getString("chance.district.end") + " " + bundle.getString("district.name." + district2), true);
        } else {
            if (!effect.variable().endsWith(".build_time")) {
                addResourceImageToHBox(hBox, effect, null);
            }
            int firstIndex = effect.variable().indexOf(".");
            int secondIndex = effect.variable().indexOf(".", firstIndex + 1);
            addLabelToHBox(hBox, bundle.getString("district.name." + effect.variable().substring(firstIndex + 1, secondIndex)), false);
        }
    }

    private void handleBuildingsEffect(Effect effect, HBox hBox) {
        if (!effect.variable().endsWith(".build_time")) {
            addResourceImageToHBox(hBox, effect, null);
        }
        int firstIndex = effect.variable().indexOf(".");
        int secondIndex = effect.variable().indexOf(".", firstIndex + 1);
        addLabelToHBox(hBox, bundle.getString("building.name." + effect.variable().substring(firstIndex + 1, secondIndex)), false);
    }

    private void handleSystemsEffect(Effect effect, HBox hBox) {
        if (effect.variable().contains(".upkeep.") || effect.variable().contains(".cost.")) {
            addResourceImageToHBox(hBox, effect, null);
        }
        int firstIndex = effect.variable().indexOf(".");
        int secondIndex = effect.variable().indexOf(".", firstIndex + 1);
        addLabelToHBox(hBox, bundle.getString(effect.variable().substring(firstIndex + 1, secondIndex)), false);
    }

    private void handleMarketEffect(HBox hBox) {
        addLabelToHBox(hBox, bundle.getString("selected.market.fee"), false);
    }

    private void handleEmpireEffect(Effect effect, HBox hBox) {
        if (effect.variable().contains("technologies")) {
            addLabelToHBox(hBox, bundle.getString("enhancements"), false);
        } else if (effect.variable().contains(".colonists")) {
            addLabelToHBox(hBox, bundle.getString("colonistText"), false);
        } else {
            addResourceImageToHBox(hBox, effect, null);
            if (effect.variable().contains("consumption")) {
                addLabelToHBox(hBox, bundle.getString("consumption"), false);
            } else if (effect.variable().contains("unemployed_upkeep")) {
                addLabelToHBox(hBox, bundle.getString("unemployed.upkeep"), false);
            }
        }
    }

    private void setTag(String tag, Label label) {
        //again, you see the redundancy
        //these 40 lines can be reduced to this:
        int firstIndex = tag.indexOf(".");
        int secondIndex = tag.indexOf(".", firstIndex + 1);
        String tagName = tag.substring(firstIndex + 1, secondIndex);
        if (!technologyTags.contains(tagName)) {
            return;
        }

        label.setText(bundle.getString("tag." + tagName));
        label.getStyleClass().add(englishBundle.getString("tag." + tagName).toLowerCase().replace(" ", ""));
    }

    private void renderUnlockButton() {
        // load information into the Unlock Button
        subscriber.subscribe(gameLogicApiService.getAggregateTech(empire._id(), "technology.cost", enhancement.id()),
                cost -> {
                    costTotal = cost.total();
                    costLabel.setText("-" + cost.total());
                    cantAfford.set(cost.total() > empire.resources().get("research"));
                    renderTooltip(cost);
                }
        );

        costImage.setImage(imageCache.get("image/game_resources/research.png"));
    }

    public void renderTooltip(@NotNull AggregateResult cost) {
        // load tooltip for the Unlock Button
        addLabelToVBox(unlockTooltip, bundle.getString("cost"), false);
        // write base Cost
        HBox baseBox = new HBox(5);
        int base = cost.items().stream().filter(item -> item.variable().equals("empire.technologies.difficulty")).mapToInt(AggregateItem::subtotal).sum();
        addLabelToHBox(baseBox, bundle.getString("variable.initial") + ": " + base, false);
        addResourceImageToHBox(baseBox, null, "research");
        unlockTooltip.getChildren().add(baseBox);

        cost.items().stream().filter(item -> !item.variable().equals("empire.technologies.difficulty") && item.subtotal() != 0).forEach(item -> {
            double percentage = ((double) item.subtotal() / base) * 100;
            subscriber.subscribe(gameLogicApiService.getExplainedVariable(game._id(), empire._id(), item.variable()), result -> {
                result.sources().forEach(effectSource -> addLabelToVBox(unlockTooltip, (int) percentage + "% " + bundle.getString(effectSource.id()), false));
                if (unlockTooltip.getChildren().size() == cost.items().size() + 1) loadTooltipEnd(cost, base);
            });
        });

        if (unlockTooltip.getChildren().size() == cost.items().size() + 1) loadTooltipEnd(cost, base);
    }

    public void loadTooltipEnd(@NotNull AggregateResult cost, int base) {
        // write the end of the Tooltip
        if (base != cost.total()) {
            HBox totalBox = new HBox(5);
            addLabelToHBox(totalBox, bundle.getString("variable.final") + ": " + cost.total(), false);
            addResourceImageToHBox(totalBox, null, "research");
            unlockTooltip.getChildren().add(totalBox);
        }
        addLinesToVBox(unlockTooltip);
        addLabelToVBox(unlockTooltip, bundle.getString("jobs.action.tooltip"), true);
    }

    public void unlockEnhancement() {
        CreateJobDto newEnhancementJobDto = new CreateJobDto(null, 0, "technology", null, null, enhancement.id(), null, null, null);
        subscriber.subscribe(jobsApiService.createJob(game._id(), empire._id(), newEnhancementJobDto));
        onCrossClicked();
    }

    public void onCrossClicked() {
        if (parentList != null) parentList.getSelectionModel().clearSelection();

        if (parentContainer != null) {
            parentContainer.getChildren().remove(this);
        }
        if (enhancementComponent != null) enhancementComponent.deselectBoxes();
    }

    public void setParentContainer(HBox parentContainer) {
        this.parentContainer = parentContainer;
    }

    public void setParentList(ListView<Technology> parentList) {
        this.parentList = parentList;
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
    }
}