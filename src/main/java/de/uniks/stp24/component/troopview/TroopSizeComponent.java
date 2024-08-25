package de.uniks.stp24.component.troopview;

import de.uniks.stp24.model.troopview.TroopSizeItem;
import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
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

import static de.uniks.stp24.util.Methods.showNode;

@Component(view = "TroopSize.fxml")
public class TroopSizeComponent extends HBox implements ReusableItemComponent<TroopSizeItem> {
    @FXML
    HBox troopSizeRootBox;
    @FXML
    ImageView sizeImage;
    @FXML
    Label sizeTypeLabel;
    @FXML
    Label actualSizeLabel;
    @FXML
    Label plannedSizeLabel;

    @Inject
    public ImageCache imageCache;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("listView")
    ListView<TroopSizeItem> listView;
    @Param("dontShowAmounts")
    boolean dontShowAmounts;
    @Param("selectable")
    boolean selectable;

    @Inject
    public TroopSizeComponent() {
    }

    @Override
    public void setItem(@NotNull TroopSizeItem troopSizeItem) {
        sizeImage.setImage(imageCache.get("image/ships/" + troopSizeItem.type() + ".png"));
        sizeTypeLabel.setText(bundle.getString(troopSizeItem.type()));
        actualSizeLabel.setText(bundle.getString("actual") + ": " + troopSizeItem.actual());
        plannedSizeLabel.setText(bundle.getString("planned") + ": " + troopSizeItem.planned());

        if (selectable) {
            troopSizeRootBox.getStyleClass().add("list-view-selectable");
        }

        // Show or hide the amount labels based on the tab of the troop view
        showNode(actualSizeLabel, !dontShowAmounts);
        showNode(plannedSizeLabel, !dontShowAmounts);

        // Responsive design
        troopSizeRootBox.prefWidthProperty().bind(listView.widthProperty().multiply(0.8));
    }
}
