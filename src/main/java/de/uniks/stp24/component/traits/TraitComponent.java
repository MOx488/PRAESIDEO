package de.uniks.stp24.component.traits;

import de.uniks.stp24.model.Trait;
import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ResourceBundle;

@Component(view = "Trait.fxml")
public class TraitComponent extends VBox implements ReusableItemComponent<Trait> {

    @FXML
    Label traitName;
    @FXML
    ImageView traitIcon;
    @FXML
    VBox traitBox;

    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;

    @Inject
    public TraitComponent() {

    }

    @Override
    public void setItem(@NotNull Trait trait) {
        traitName.setText(bundle.getString("traits." + trait.id()));
        if (trait.cost() > 0) {
            traitIcon.setImage(imageCache.get("image/icons/face-smile.png"));
        } else if (trait.cost() < 0) {
            traitIcon.setImage(imageCache.get("image/icons/face-frown.png"));
        }
    }

}
