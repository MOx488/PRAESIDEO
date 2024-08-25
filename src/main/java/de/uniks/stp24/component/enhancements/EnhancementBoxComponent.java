package de.uniks.stp24.component.enhancements;

import de.uniks.stp24.model.Technology;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Locale;
import java.util.ResourceBundle;

@Component(view = "EnhancementBox.fxml")
public class EnhancementBoxComponent extends VBox implements ReusableItemComponent<Technology> {
    @FXML
    VBox box;
    @FXML
    HBox boxCategory;
    @FXML
    Label boxLabel;

    @Param("enhancementSubject")
    String enhancementSubject;

    @Inject
    @Resource
    public ResourceBundle bundle;

    final ResourceBundle englishBundle = ResourceBundle.getBundle("de/uniks/stp24/lang/lang", Locale.ENGLISH);

    @Inject
    public EnhancementBoxComponent() {
    }

    @Override
    public void setItem(@NotNull Technology tech) {
        boxLabel.setText(bundle.getString(tech.id()));

        boxCategory.getChildren().clear();
        for (String tag : tech.tags()) {
            if (tag.equals(enhancementSubject)) {
                continue;
            }

            boolean writeTag = true;
            for (int i = 0; i < boxCategory.getChildren().size(); i++) {
                if (boxCategory.getChildren().get(i) instanceof Label && ((Label) boxCategory.getChildren().get(i)).getText().contains(bundle.getString("tag." + tag))) {
                    writeTag = false;
                    break;
                }
            }

            if (writeTag) {
                Label label = new Label();
                label.setText(bundle.getString("tag." + tag));
                setColor(tag, label);
                label.getStyleClass().add("enhancementBox");
                label.setPadding(new Insets(0, 5, 0, 5));
                boxCategory.getChildren().add(label);
            }
        }
    }

    //use a map?
    //even the resource bundle maps the keys to the values (tag.engineering = Engineering) -> label.getStyleClass().add(bundle.getString("tag." + tag))
    //which would make this method an oneliner
    // -> label.getStyleClass().add(englishBundle.getString("tag." + tag));
    private void setColor(String tag, Label label) {
        label.getStyleClass().add(englishBundle.getString("tag." + tag).toLowerCase().replace(" ", ""));
    }
}
