package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.rest.GameLogicApiService;
import de.uniks.stp24.rest.GameSystemsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.PresetsService;
import de.uniks.stp24.ws.EventListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "Statistics.fxml")
public class StatisticsComponent extends AnchorPane {
    @Inject
    public App app;
    @Inject
    public GameSystemsApiService gameSystemsApiService;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;
    @Inject
    public Subscriber subscriber;
    @Inject
    public GameLogicApiService gameLogicApiService;
    @Inject
    public EventListener eventListener;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;
    @Inject
    public PresetsService presetsService;

    @FXML
    Text type;
    @FXML
    Text level;
    @FXML
    Text capacity;
    @FXML
    Text population;

    @FXML
    ImageView energyImage;
    @FXML
    ImageView foodImage;
    @FXML
    ImageView mineralsImage;
    @FXML
    ImageView researchImage;
    @FXML
    ImageView creditsImage;
    @FXML
    ImageView fuelImage;
    @FXML
    ImageView alloysImage;
    @FXML
    ImageView consumerGoodsImage;
    @FXML
    Text health;
    @FXML
    ProgressBar healthBar;
    @FXML
    Text defense;
    @FXML
    Label healthToolTopLabel;
    @FXML
    Label defenseToolTipLabel;
    @FXML
    Text defenseToolTipText1;
    @FXML
    Text defenseToolTipText2;
    @FXML
    ImageView defenseToolTipImage;
    @FXML
    Text healthToolTipText1;
    @FXML
    Text healthToolTipText2;
    @FXML
    ImageView healthToolTipImage;


    @FXML
    Text energy;
    @FXML
    Text food;
    @FXML
    Text minerals;
    @FXML
    Text research;
    @FXML
    Text credits;
    @FXML
    Text fuel;
    @FXML
    Text alloys;
    @FXML
    Text consumer_goods;

    @Param("game")
    Game game;
    @Param("system")
    GameSystem system;
    @Param("empire")
    Empire empire;

    private Map<String, Text> resourceToText;

    @Inject
    public StatisticsComponent() {
    }

    @OnInit
    public void init() {
        AnchorPane.setTopAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 0.0);
        AnchorPane.setRightAnchor(this, 0.0);
        AnchorPane.setBottomAnchor(this, 0.0);

        this.initializeWebSockets();
    }

    private void initializeData() {
        this.resourceToText = Map.of(
                "credits", credits,
                "energy", energy,
                "minerals", minerals,
                "food", food,
                "fuel", fuel,
                "research", research,
                "alloys", alloys,
                "consumer_goods", consumer_goods);
    }

    private void initializeWebSockets() {
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems." + system._id() + ".updated", GameSystem.class),
                event -> {

                    GameSystem newSystem = event.data();
                    if (this.system.upgrade().equals(newSystem.upgrade()) && this.system.buildings().equals(newSystem.buildings()) && this.system.districts().equals(newSystem.districts()) && (this.system.health() == newSystem.health())) {
                        return;
                    }

                    this.system = newSystem;
                    this.updateStatistics();
                    this.updateResources();
                    this.setHealthAndDefense();
                }
        );
    }

    @OnRender
    public void render() {
        this.healthToolTopLabel.getTooltip().setShowDelay(new Duration(0));
        this.defenseToolTipLabel.getTooltip().setShowDelay(new Duration(0));
        this.initializeData();
        this.initializeImages();
        this.updateStatistics();
        this.updateResources();
        this.setHealthAndDefense();
    }

    private void initializeImages() {
        energyImage.setImage(imageCache.get("image/game_resources/energy.png"));
        foodImage.setImage(imageCache.get("image/game_resources/food.png"));
        mineralsImage.setImage(imageCache.get("image/game_resources/minerals.png"));
        researchImage.setImage(imageCache.get("image/game_resources/research.png"));
        creditsImage.setImage(imageCache.get("image/game_resources/credits.png"));
        fuelImage.setImage(imageCache.get("image/game_resources/fuel.png"));
        alloysImage.setImage(imageCache.get("image/game_resources/alloys.png"));
        consumerGoodsImage.setImage(imageCache.get("image/game_resources/consumer_goods.png"));
    }

    private void updateStatistics() {
        final int currentCapacity = system.buildings().size() + calculateDistricts();

        type.setText(bundle.getString("type").toUpperCase() + ": " + bundle.getString(system.type()));
        level.setText(bundle.getString("level").toUpperCase() + ": " + bundle.getString(system.upgrade()));
        capacity.setText(bundle.getString("capacity").toUpperCase() + ": " + currentCapacity + "/" + system.capacity());
        population.setText(bundle.getString("population").toUpperCase() + ": " + system.population());

    }

    private void updateResources() {
        subscriber.subscribe(gameLogicApiService.getAggregate(game._id(), empire._id(), "resources.periodic",
                        Map.of("system", system._id())), aggregateResult -> aggregateResult.items().forEach(item -> {
                            final Text text = resourceToText.get(item.variable().split("\\.")[1]); // resources.consumer_goods.periodic -> consumer_goods
                            //system cant produce a population
                            if (text == null) {
                                return;
                            }

                            final int subTotal = item.subtotal();
                            String subTotalPrefix = " ";
                            if (subTotal >= 0) {
                                subTotalPrefix = " +";
                            }

                            text.setText(subTotalPrefix + subTotal);
                        }
                )
        );
    }

    private int calculateDistricts() {
        int districtSum = 0;
        for (Map.Entry<String, Integer> stringIntegerEntry : system.districts().entrySet()) {
            districtSum += stringIntegerEntry.getValue();
        }
        return districtSum;
    }


    private void setHealthAndDefense() {
        subscriber.subscribe(gameLogicApiService.getAggregateSystem(game._id(), empire._id(), "system.max_health", system._id()), aggregateResult -> {
            health.setText(bundle.getString("health").toUpperCase() + ": " + this.system.health() + "/" + aggregateResult.total());
            createHealthToolTip(aggregateResult);
            updateHealthBar(aggregateResult.total());
        });
        subscriber.subscribe(gameLogicApiService.getAggregateSystem(game._id(), empire._id(), "system.defense", system._id()), aggregateResult -> {
            defense.setText(bundle.getString("defense").toUpperCase() + ": " + aggregateResult.total());
            createDefenseToolTip(aggregateResult);
        });
    }

    private void createHealthToolTip(@NotNull AggregateResult aggregateResult) {
        StringBuilder healthText = new StringBuilder();
        for (AggregateItem item : aggregateResult.items()) {
            healthText.append(bundle.getString(item.variable())).append(": +").append(item.subtotal()).append("\n");
        }
        healthText.append("-------");
        this.healthToolTipText1.setText(healthText.toString());
        healthText.append(bundle.getString("system.defense.total")).append(": ").append(aggregateResult.total());
        this.healthToolTipText2.setText((bundle.getString("system.health.total")) + (": ") + (aggregateResult.total()));
        this.healthToolTipImage.setImage(imageCache.get("image/game_resources/health.png"));
    }

    private void createDefenseToolTip(@NotNull AggregateResult aggregateResult) {
        StringBuilder defenseText = new StringBuilder();
        for (AggregateItem item : aggregateResult.items()) {
            defenseText.append(bundle.getString(item.variable())).append(": +").append(item.subtotal()).append("\n");
        }
        defenseText.append("-------");
        this.defenseToolTipText1.setText(defenseText.toString());
        defenseText.append(bundle.getString("system.defense.total")).append(": ").append(aggregateResult.total());
        this.defenseToolTipText2.setText((bundle.getString("system.defense.total")) + (": ") + (aggregateResult.total()));
        this.defenseToolTipImage.setImage(imageCache.get("image/game_resources/defense.png"));
    }


    // set progressbar value of ratio of health to max health
    private void updateHealthBar(int systemHealth) {
        double healthRatio = system.health() / systemHealth;
        if (systemHealth > 0) {
            healthBar.setProgress(healthRatio);
        } else {
            healthBar.setProgress(1);
        }
    }

    @OnDestroy
    void destroy() {
        subscriber.dispose();
    }


}
