package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.ResourcesResult;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.rest.GameLogicApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.NotificationService;
import de.uniks.stp24.service.PrefService;
import de.uniks.stp24.service.PresetsService;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component(view = "Market.fxml")
public class MarketComponent extends VBox {

    @FXML
    public ChoiceBox<String> resourceChoice;
    @FXML
    public TextField amountField;
    @FXML
    public ImageView buyImage;
    @FXML
    public Text countBuy;
    @FXML
    public ImageView sellImage;
    @FXML
    public Text countSell;
    @FXML
    public Button buyButton;
    @FXML
    public Button sellButton;
    @FXML
    public ImageView buyAddImage;
    @FXML
    public Text buyAddNum;
    @FXML
    public ImageView buySubImage;
    @FXML
    public Text buySubNum;
    @FXML
    public ImageView sellAddImage;
    @FXML
    public Text sellAddNum;
    @FXML
    public ImageView sellSubImage;
    @FXML
    public Text sellSubNum;
    @FXML
    public Text marketName;
    @FXML
    public Text marketFee;
    @FXML
    public Text marketFeeNum;
    @FXML
    public VBox marketRoot;
    @FXML
    public VBox buyResult;
    @FXML
    public VBox sellResult;
    @FXML
    public HBox buySellContainer;
    @FXML
    public Text marketFeeExplain;
    @FXML
    public Label marketFeeToolTiopLabel;
    @FXML
    public HBox ratioSpacer;
    @FXML
    public Label errorMessage;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;
    @Inject
    public EventListener eventListener;
    @Inject
    public PresetsService presetsService;
    @Inject
    public GameLogicApiService gameLogicApiService;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;
    @Inject
    public PrefService prefService;
    @Inject
    public NotificationService notificationService;

    @Param("modalStage")
    public Stage modal;
    @Param("game")
    Game game;
    @Param("empire")
    Empire empire;

    private final List<String> resources = new ArrayList<>();
    private final Map<String, String> mapResources = new HashMap<>();
    private ResourcesResult resourcesResult;
    private double marketFeeValue = 0.0f;
    private double buyRessource;
    private double subCoins;
    private double addCoins;
    private int ratio = 0;
    private Map<String, Integer> currentResources;
    private final SimpleBooleanProperty waitForResponse = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty amountIsTooBig = new SimpleBooleanProperty(false);
    private ExplainedVariable marketFeeVariable;
    private boolean firstChange = true;
    private ChangeListener<String> resourceChoiceListener;
    private ChangeListener<String> amountFieldListener;

    @Inject
    public MarketComponent() {
    }

    @OnInit
    public void init() {
        fillArrayWithResources();
        createMap();
        getMarketRatio();
        getMarketFee();
        this.currentResources = this.empire.resources();

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), event -> {
                    this.checkResourceUpdate(event);
                    this.checkMarketFeeUpdate(event);
                }
        );
    }

    private void checkMarketFeeUpdate(Event<Empire> event) {
        Empire newEmpire = event.data();
        if (newEmpire.technologies().equals(this.empire.technologies()) && newEmpire.effects().equals(this.empire.effects())) {
            return;
        }

        this.empire = newEmpire;
        this.getMarketFee();
    }

    private void checkResourceUpdate(Event<Empire> event) {
        final TreeMap<String, Integer> resources = event.data().resources();
        if (resources.equals(this.currentResources)) {
            return;
        }
        this.currentResources = resources;
    }

    @OnRender
    public void choiceBoxAndImage() {
        resourceChoice.getItems().addAll(resources);
        resourceChoice.setValue(bundle.getString("market.choicePrompt"));

        // Add a listener if choice box value changes
        resourceChoice.getSelectionModel().selectedItemProperty().addListener(resourceChoiceListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (firstChange) {
                    firstChange = false;
                    resourceChoice.getStyleClass().remove("choiceBoxPrompt");
                    resourceChoice.getStyleClass().add("choiceBoxNormal");
                    ratioSpacer.setVisible(true);
                }
                imageLoaderAndRatio();
                buySellResult();
            }
        });
        // Add a listener if amount field value changes
        amountField.textProperty().addListener(amountFieldListener = (observable, oldValue, newValue) -> {
            try {
                double value = Double.parseDouble(newValue);
                if (value > 0) {
                    this.buyRessource = value;
                    buySellResult();
                }
            } catch (NumberFormatException ignored) {
            }
        });
        setMarketFee();
    }

    @OnRender
    public void bindButton() {
        // checks if text field positiv int
        BooleanBinding isPositiveInt = Bindings.createBooleanBinding(() -> {
            String text = amountField.getText();
            if (text.isEmpty() || text.isBlank()) {
                return false;
            }
            try {
                double value = Double.parseDouble(text);
                return value > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }, amountField.textProperty());

        // checks if choice box is not empty
        final BooleanBinding resourceChoiceEmpty = resourceChoice.valueProperty().
                isEqualTo(bundle.getString("market.choicePrompt"));

        // if ressource is not chosen or amount is not positiv int, disable buttons
        buyButton.disableProperty().bind(isPositiveInt.not().or(resourceChoiceEmpty).or(waitForResponse).or(amountIsTooBig));
        sellButton.disableProperty().bind(isPositiveInt.not().or(resourceChoiceEmpty).or(waitForResponse).or(amountIsTooBig));

        // if ressource is chosen and amount is positiv int, enable buy/sell container
        buySellContainer.visibleProperty().bind(isPositiveInt.and(resourceChoiceEmpty.not()).and(amountIsTooBig.not()));
    }

    public void buyResource() {
        if (this.currentResources.get("credits") < subCoins) {
            notificationService.displayNotification(bundle.getString("market.buy.failure"), false);
        } else {
            changeResources(buyRessource, -1 * subCoins);
        }
    }

    public void sellResource() {
        if (this.currentResources.get(mapResources.get(resourceChoice.getValue())) < buyRessource) {
            notificationService.displayNotification(bundle.getString("market.sell.failure"), false);
        } else {
            changeResources(-1 * buyRessource, addCoins);
        }
    }

    // make server request for resource change
    private void changeResources(double changingRessource, double changingCredits) {
        waitForResponse.set(true);
        ChangeRessource changeRessource = new ChangeRessource(
                Map.of(mapResources.get(resourceChoice.getValue()), changingRessource)
        );
        subscriber.subscribe(gameEmpiresApiService.updateResources(this.game._id(), this.empire._id(), changeRessource),
                result -> {
                    if (changingCredits < 0) {
                        notificationService.displayNotification(bundle.getString("market.buy.success"), true);
                    } else {
                        notificationService.displayNotification(bundle.getString("market.sell.success"), true);
                    }
                    waitForResponse.set(false);
                },
                error -> waitForResponse.set(false)
        );
    }

    // Sets the images and the numbers of the buy and sell result
    private void buySellResult() {
        if (handleBigNum()) {
            return;
        }
        this.buySubImage.setImage(imageCache.get("image/game_resources/credits.png"));
        this.sellSubImage.setImage(imageCache.get("image/game_resources/credits.png"));
        if (resourceChoice.getValue() != null &&
                !resourceChoice.getValue().equals(bundle.getString("market.choicePrompt"))) {
            String ressource = mapResources.get(resourceChoice.getValue());
            this.buyAddImage.setImage(imageCache.get("image/game_resources/" + ressource + ".png"));
            this.sellAddImage.setImage(imageCache.get("image/game_resources/" + ressource + ".png"));

            this.buyAddNum.setText("  + " + roundDoubleValue(buyRessource));
            this.sellAddNum.setText("  - " + roundDoubleValue(buyRessource));
            calcCredits();
        }
    }

    // Handle to big numbers
    private boolean handleBigNum() {
        if (this.buyRessource < 1e10) {
            this.errorMessage.setVisible(false);
            amountIsTooBig.set(false);
            return false;
        } else {
            this.errorMessage.setVisible(true);
            amountIsTooBig.set(true);
            return true;
        }
    }

    // Calculates the credits
    private void calcCredits() {
        double baseValue = this.buyRessource * this.ratio;
        this.subCoins = (baseValue * (this.marketFeeValue + 1));
        this.addCoins = (baseValue * (1 - this.marketFeeValue));

        this.buySubNum.setText("  - " + roundDoubleValue(this.subCoins));
        this.sellSubNum.setText("  + " + roundDoubleValue(this.addCoins));
    }

    // Sets the image and the ratio of the resources trade
    private void imageLoaderAndRatio() {
        this.sellImage.setImage(imageCache.get("image/game_resources/credits.png"));
        if (resourceChoice.getValue() != null) {
            String ressource = mapResources.get(resourceChoice.getValue());
            this.buyImage.setImage(imageCache.get("image/game_resources/" + ressource + ".png"));
            de.uniks.stp24.model.Resource resource = (de.uniks.stp24.model.Resource) getResourceResultValue(ressource);
            if (resource != null) {
                this.ratio = resource.credit_value();
                this.countSell.setText("  " + this.ratio);
                this.countBuy.setText("  1");
            }
        }
    }

    private void getMarketRatio() {
        subscriber.subscribe(presetsService.getCachedPreset("getResources"),
                result -> resourcesResult = (ResourcesResult) result
        );
    }

    private void getMarketFee() {
        subscriber.subscribe(gameLogicApiService
                        .getExplainedVariable(game._id(), empire._id(), "empire.market.fee"),
                result -> {
                    this.marketFeeVariable = result;
                    this.marketFeeValue = this.marketFeeVariable.end();
                    setMarketFee();
                    buildTooltip();
                });
    }

    // set market fee text
    private void setMarketFee() {
        double initialFee = this.marketFeeValue * 100;
        this.marketFeeNum.setText(" +" + formatNumber(initialFee));
    }

    private Object getResourceResultValue(String selectedResource) {
        return switch (selectedResource) {
            case "energy" -> resourcesResult.energy();
            case "minerals" -> resourcesResult.minerals();
            case "food" -> resourcesResult.food();
            case "fuel" -> resourcesResult.fuel();
            case "credits" -> resourcesResult.credits();
            case "consumer_goods" -> resourcesResult.consumer_goods();
            case "alloys" -> resourcesResult.alloys();
            default -> null;
        };
    }

    private void fillArrayWithResources() {
        if (prefService.getLocale() == Locale.GERMAN) {
            resources.add("Feuer");
            resources.add("Erz");
            resources.add("Essen");
            resources.add("Kohle");
            resources.add("Bier");
            resources.add("Stahl");
        } else {
            resources.add("Fire");
            resources.add("Ore");
            resources.add("Food");
            resources.add("Coal");
            resources.add("Beer");
            resources.add("Steel");
        }
    }

    private void createMap() {
        for (String key : bundle.keySet()) {
            if (key.startsWith("resources.")) {
                if (!key.endsWith(".credits") && !key.endsWith(".research") && !key.endsWith(".population")) {
                    mapResources.put(bundle.getString(key), key.substring("resources.".length()));
                }
            }
        }
    }

    private void buildTooltip() {
        final String[] marketToolTipText = new String[1];
        double initialFee = this.marketFeeVariable.initial() * 100;
        String formattedFee = formatNumber(initialFee);
        marketToolTipText[0] = bundle.getString("market.fee.base") + " +" + formattedFee + "\n";

        for (EffectSource effectSource : this.marketFeeVariable.sources()) {
            double multiplier = effectSource.effects().getFirst().multiplier();
            if (multiplier > 1) {
                marketToolTipText[0] += " +";
                multiplier = multiplier - 1;
            } else {
                marketToolTipText[0] += " -";
                multiplier = 1 - multiplier;
            }

            String effectName = effectSource.id();

            if (bundle.containsKey(effectName)) {
                effectName = bundle.getString(effectName);
            }

            marketToolTipText[0] += formatNumber(multiplier * 100) + " " + effectName + "\n";
        }

        marketToolTipText[0] += bundle.getString("market.fee.total") + marketFeeNum.getText();

        this.marketFeeExplain.setText(marketToolTipText[0]);
        this.marketFeeToolTiopLabel.getTooltip().setShowDelay(new Duration(0));
    }

    private String formatNumber(double value) {
        return value % 1 == 0 ? String.format("%.0f%%", value) : String.format("%.2f%%", value);
    }

    private String roundDoubleValue(double value) {
        if (value < 1000) {
            BigDecimal bd = new BigDecimal(Double.toString(value));
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            double roundedValue = bd.doubleValue();
            String formattedNumber = String.format("%.2f", roundedValue);
            if (formattedNumber.endsWith("00")) {
                return formattedNumber.substring(0, formattedNumber.length() - 3);
            }
            return formattedNumber;
        } else if (value < 1000000) {
            return String.format("%.0fk", value / 1000);
        } else {
            return String.format("%.0fM", value / 1000000);
        }
    }

    @OnDestroy
    void destroy() {
        subscriber.dispose();
        if (resourceChoiceListener != null) {
            resourceChoice.getSelectionModel().selectedItemProperty().removeListener(resourceChoiceListener);
        }
        if (amountFieldListener != null) {
            amountField.textProperty().removeListener(amountFieldListener);
        }
    }
}
