package de.uniks.stp24.component.troopview;


import de.uniks.stp24.model.troopview.SizeUpdateItem;
import de.uniks.stp24.model.troopview.TroopSizeItem;
import de.uniks.stp24.service.ImageCache;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ResourceBundle;

@Component(view = "SizeUpdate.fxml")
public class SizeUpdateComponent extends HBox implements ReusableItemComponent<SizeUpdateItem> {
    @FXML
    ImageView typeImageView;
    @FXML
    Label nameLabel;
    @FXML
    Button decreaseButton;
    @FXML
    ImageView decreaseImageView;
    @FXML
    Label amountLabel;
    @FXML
    Button increaseButton;
    @FXML
    ImageView increaseImageView;

    @Inject
    public ImageCache imageCache;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("listView")
    ListView<TroopSizeItem> listView;

    private SimpleIntegerProperty amount;

    @Inject
    public SizeUpdateComponent() {
    }

    @Override
    public void setItem(@NotNull SizeUpdateItem item) {
        amount = new SimpleIntegerProperty(0);

        typeImageView.setImage(imageCache.get("image/ships/" + item.type() + ".png"));
        nameLabel.setText(bundle.getString(item.type()));
        amountLabel.textProperty().bind(Bindings.convert(amount));
        decreaseImageView.setImage(imageCache.get("image/arrow-left-orange.png"));
        increaseImageView.setImage(imageCache.get("image/arrow-right-orange.png"));

        // Do not let the player decrease a planned size to negative values
        decreaseButton.visibleProperty().bind(amount.greaterThan(-item.planned()));

        decreaseButton.setOnAction(event -> decreaseAmount(item));
        increaseButton.setOnAction(event -> increaseAmount(item));

        // Responsive design
        this.prefWidthProperty().bind(listView.widthProperty().multiply(0.8));
    }

    public void decreaseAmount(SizeUpdateItem item) {
        amount.set(amount.get() - 1);
        item.setAmount(item.amount() - 1);
    }

    public void increaseAmount(SizeUpdateItem item) {
        amount.set(amount.get() + 1);
        item.setAmount(item.amount() + 1);
    }
}
