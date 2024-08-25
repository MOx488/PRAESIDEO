package de.uniks.stp24.component.events;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Effect;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.ws.EventListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.createLabel;

@Component(view = "EventEffect.fxml")
public class EventEffectComponent extends VBox {

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public ImageCache imageCache;

    @Inject
    public EventListener eventListener;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @FXML
    VBox effectContainer;
    @FXML
    Label effectingVariableName;


    @Param("game")
    Game game;

    @Param("empire")
    Empire empire;

    @Param("effect")
    Effect effect;

    @Param("duration")
    int duration;

    @Param("startPeriod")
    int startPeriod;


    @Inject
    public EventEffectComponent() {
    }

    @OnRender
    public void onRender() {
        this.effectingVariableName.setText(bundle.getString(effect.variable()));
        this.fillEffectContainer();
    }

    private void fillEffectContainer() {
        this.effectContainer.getChildren().clear();

        final double base = effect.base();
        if (base != 0) {
            String basePrefix = "+";
            if (base < 0) {
                basePrefix = "";
            }


            final Label baseLabel = createLabel(basePrefix + base + " " + bundle.getString("per_tick"), "small-large");
            this.effectContainer.getChildren().add(baseLabel);

            final Label totalBaseChangeLabel = createLabel(base * duration + " " + bundle.getString("variable.final"), "small-large");
            this.effectContainer.getChildren().add(totalBaseChangeLabel);
        } else {
            final double multiplier = (effect.multiplier() - 1d) * 100d;
            String multiplierPrefix = "+";
            if (multiplier < 0) {
                multiplierPrefix = "";
            }

            final String multiplierRounded = String.format("%.2f", multiplier);
            final Label multiplierLabel = createLabel(multiplierPrefix + multiplierRounded + "%", "small-large");
            this.effectContainer.getChildren().add(multiplierLabel);
        }
    }

    @OnDestroy
    public void destroy() {
        this.subscriber.dispose();
    }
}
