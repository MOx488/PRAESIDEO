package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.model.CastleType;
import de.uniks.stp24.model.Fleet;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.ZoomDragService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Subscription;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.uniks.stp24.util.Constants.*;


@Component(view = "ZoomDragPane.fxml")
public class ZoomDragComponent extends StackPane {
    @FXML
    Canvas canvas;
    @Inject
    public ImageCache imageCache;
    @Inject
    App app;
    @Inject
    public ZoomDragService zoomDragService;
    @FXML
    public Group target;
    @FXML
    public ScrollPane scrollPane;
    @FXML
    ImageView view;
    @FXML
    Group castleContainer;
    @Param("game")
    Game game;

    private Node zoomNode;

    private double castleImageSize;
    private double castleMinDistance;
    private double scaleValue = 1.0d;
    private boolean haveChildrenBeenPlaced = false;
    private final BooleanProperty placeCastlesProperty;
    private Subscription castleContainerSubscription;

    private final ChangeListener<Object> windowChangeListener = (observable, oldValue, newValue) -> this.placeCastleComponent();
    private final EventHandler<ScrollEvent> scrollEventHandler = (event) -> {
        this.handleScroll(event);
        event.consume();
    };

    private final Map<Integer, CastleType> indexToType;
    private ArrayList<ArrayList<String>> routes;


    @Inject
    public ZoomDragComponent() {
        this.placeCastlesProperty = new SimpleBooleanProperty(false);
        this.indexToType = new HashMap<>();
    }

    @OnRender
    public void OnRender() {
        this.zoomNode = new Group(target);
        this.scrollPane.setContent(outerNode(zoomNode));

        this.castleContainer.setCache(true);
        this.castleContainer.setCacheHint(CacheHint.SCALE);

        this.scrollPane.setPannable(true); //so we can move around in the image
        this.scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); //remove horizontal scroll bar
        this.scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); //remove vertical scroll bar
        this.scrollPane.setFitToHeight(true); //center
        this.scrollPane.setFitToWidth(true); //center

        this.scrollPane.addEventFilter(ScrollEvent.ANY, scrollEventHandler);

        final Image image = this.imageCache.get("image/ingame_background.png", false, INGAME_BACKGROUND_WIDTH, INGAME_BACKGROUND_HEIGHT);
        this.view.setImage(image);
        this.view.setCache(true);
        this.view.setSmooth(true);
        this.view.setCacheHint(CacheHint.SCALE);

        this.view.fitWidthProperty().bind(this.widthProperty());
        this.view.fitHeightProperty().bind(this.heightProperty());

        this.canvas.widthProperty().bind(this.widthProperty());
        this.canvas.heightProperty().bind(this.heightProperty());

        this.scrollPane.widthProperty().addListener(this.windowChangeListener);
        this.scrollPane.heightProperty().addListener(this.windowChangeListener);

        this.castleContainerSubscription = this.placeCastlesProperty.subscribe((oldValue, newValue) -> this.placeCastleComponent());

        CastleComponent.displayNameTagProperty.set(false);


        this.castleImageSize = -0.23f * game.settings().size() + 60.f;
        this.castleMinDistance = castleImageSize + 40d;
    }

    private void handleScroll(ScrollEvent scrollEvent) {
        final double wheelDelta = scrollEvent.getDeltaY();
        final double zoomFactor = Math.exp(wheelDelta * INGAME_ZOOM_INTENSITY);

        this.scaleValue = Math.max(1.0, this.scaleValue * zoomFactor); //ensure scale value >= 1.0

        //don't do anything when zoomed in too far + it's not a zoom out
        if (this.scaleValue > INGAME_MAX_SCALE) {
            if (wheelDelta < 0) {
                this.scaleValue = INGAME_MAX_SCALE;
            }

            CastleComponent.displayNameTagProperty.set(true);
            return;
        }

        CastleComponent.displayNameTagProperty.set(false);
        Point2D scrollPoint = new Point2D(scrollEvent.getX(), scrollEvent.getY());
        final Bounds viewportBounds = scrollPane.getViewportBounds();
        final Bounds innerBounds = zoomNode.getBoundsInLocal();
        if (wheelDelta < 0) {
            //it's a zoom out, we want to zoom out from the center of the view
            final double hScale = scrollPane.getHvalue() / scrollPane.getHmax();
            final double vScale = scrollPane.getVvalue() / scrollPane.getVmax();

            final double x = Math.max(0, (innerBounds.getWidth() - viewportBounds.getWidth()) * hScale) + viewportBounds.getWidth() / 2;
            final double y = Math.max(0, (innerBounds.getHeight() - viewportBounds.getHeight()) * vScale) + viewportBounds.getHeight() / 2;

            scrollPoint = zoomNode.parentToLocal(x, y);
        }

        this.target.setScaleX(this.scaleValue);
        this.target.setScaleY(this.scaleValue);

        final double valX = this.scrollPane.getHvalue() * (innerBounds.getWidth() - viewportBounds.getWidth());
        final double valY = this.scrollPane.getVvalue() * (innerBounds.getHeight() - viewportBounds.getHeight());

        this.layout(); //recalculate bounds after scaling (will also update child nodes so no need to .layout on scrollPane or other containers)

        final Point2D adjustment = target.getLocalToParentTransform().deltaTransform(scrollPoint.multiply(zoomFactor - 1));
        final Bounds updatedInnerBounds = zoomNode.getBoundsInLocal();

        this.scrollPane.setHvalue((valX + adjustment.getX()) / (updatedInnerBounds.getWidth() - viewportBounds.getWidth()));
        this.scrollPane.setVvalue((valY + adjustment.getY()) / (updatedInnerBounds.getHeight() - viewportBounds.getHeight()));
    }

    public void placeCastleComponent() {
        // 0,0 starts at the center of the map
        // place castle on window coordinates
        final double windowWidth = this.scrollPane.getWidth();
        final double windowHeight = this.scrollPane.getHeight();

        final ObservableList<Node> castleComponents = this.castleContainer.getChildren();
        final int castleComponentSize = castleComponents.size();

        if (windowWidth == 0 || windowHeight == 0 || castleComponentSize == 0) {
            return;
        }

        final double centerWidth = windowWidth / 2.0;
        final double centerHeight = windowHeight / 2.0;

        final double usableWindowWidth = ORIGINAL_WINDOW_WIDTH * (1 + (0.125d));

        final double scaleWidth = windowWidth / ORIGINAL_WINDOW_WIDTH;
        final double scaleHeight = windowHeight / ORIGINAL_WINDOW_HEIGHT;

        final double widthToHeightRatio = usableWindowWidth / ORIGINAL_WINDOW_HEIGHT;
        final double serverTranslateDimension = 4d / 3d * game.settings().size() + 233.3334d;

        final double originalWidthScale = ORIGINAL_WINDOW_WIDTH / serverTranslateDimension;
        final double originalHeightScale = ORIGINAL_WINDOW_HEIGHT / (serverTranslateDimension * (widthToHeightRatio - 1)); //because width is bigger than height

        final double xShoreStart = 0.125d * (scaleWidth * ORIGINAL_WINDOW_WIDTH);
        final double xShoreEnd = windowWidth - xShoreStart;

        final double xServerDimensionScale = originalWidthScale * scaleWidth;
        final double yServerDimensionScale = originalHeightScale * scaleHeight;

        for (int i = 0; i < castleComponentSize; i++) {
            final Node nodeA = castleComponents.get(i);
            final CastleComponent castleComponentA = (CastleComponent) nodeA;
            final GameSystem system = castleComponentA.system;

            final double castleHeight = castleImageSize * 2;

            final double possibleX = centerWidth + system.x() * xServerDimensionScale;
            final double possibleY = centerHeight + system.y() * yServerDimensionScale;

            final double xScaled = Math.clamp(possibleX, xShoreStart, xShoreEnd);
            final double yScaled = Math.clamp(possibleY, 0, windowHeight - castleHeight);

            castleComponentA.setLayoutX(xScaled);
            castleComponentA.setLayoutY(yScaled);

            //check for the previous i - 1 or n if it's a resize, castles whether they are overlapping or not
            this.repairOverlap(i, castleComponentA, new Rectangle2D(xShoreStart, 0, xShoreEnd, windowHeight - castleHeight));

            //only decide if not already decided
            final CastleType castleType = this.indexToType.computeIfAbsent(i, index -> this.decideCastleType(castleComponentA));
            castleComponentA.setCastleType(castleType);
            castleComponentA.layout();
        }

        //draw streets after overlapping has been repaired
        this.clearCanvas();
        this.drawStreets();
        this.haveChildrenBeenPlaced = true;
    }

    public CastleType decideCastleType(CastleComponent castleComponent) {
        final Point2D imageCenterScreen = getImageCenter(castleComponent);

        final Point2D imageCoordinates = imageViewToImageCoordinates(imageCenterScreen.getX(), imageCenterScreen.getY());

        final double xImage = imageCoordinates.getX();
        final double yImage = imageCoordinates.getY();

        return zoomDragService.determineCastleType(xImage, yImage);
    }

    private void repairOverlap(int indexComponentA, CastleComponent castleComponentA, Rectangle2D bounds) {
        //if it's the first placement of children only the i-1 castles have positions set
        //therefore we only need to check them

        //in the case of a resize however, all n children have positions so we have to check all of them
        int j = indexComponentA - 1;
        if (this.haveChildrenBeenPlaced) {
            //clear whenever we resize
            this.canvas.getGraphicsContext2D().clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());

            j = this.castleContainer.getChildren().size() - 1;
        }

        for (; j > 0; j--) {
            if (indexComponentA == j) {
                //don't check overlap with ourselves obviously
                continue;
            }

            final CastleComponent castleComponentB = (CastleComponent) this.castleContainer.getChildren().get(j);
            final double dx = castleComponentA.getLayoutX() - castleComponentB.getLayoutX();
            final double dy = castleComponentA.getLayoutY() - castleComponentB.getLayoutY();
            final double distance = Math.sqrt(dx * dx + dy * dy);    //distance = sqrt(x^2 + y^2)

            if (distance > this.castleMinDistance) {
                continue; //castles are not too close -> check rest
            }

            // castle are too close to each other -> calculate translation to push them away
            this.adjustCastlePositions(castleComponentA, castleComponentB, bounds, dx, dy, distance);
        }
    }

    private void adjustCastlePositions(CastleComponent castleComponentA, CastleComponent castleComponentB, Rectangle2D bounds, double dx, double dy, double distance) {
        final double overlapAmount = this.castleMinDistance - distance;
        final double translationFactor = overlapAmount / distance;

        final double translationX = dx * translationFactor / 2;
        final double translationY = dy * translationFactor / 2;

        //check for clamping to avoid pushing them out of our bounds
        final double xA = castleComponentA.getLayoutX() + translationX;
        final double yA = castleComponentA.getLayoutY() + translationY;
        final double xB = castleComponentB.getLayoutX() - translationX;
        final double yB = castleComponentB.getLayoutY() - translationY;

        final double xAClamped = Math.clamp(xA, bounds.getMinX(), bounds.getMaxX());
        final double yAClamped = Math.clamp(yA, bounds.getMinY(), bounds.getMaxY());
        final double xBClamped = Math.clamp(xB, bounds.getMinX(), bounds.getMaxX());
        final double yBClamped = Math.clamp(yB, bounds.getMinY(), bounds.getMaxY());

        castleComponentA.setLayoutX(xAClamped);
        castleComponentA.setLayoutY(yAClamped);
        castleComponentB.setLayoutX(xBClamped);
        castleComponentB.setLayoutY(yBClamped);
    }

    public void updateStreets(ArrayList<ArrayList<String>> routes) {
        this.routes = routes;
        drawStreets();
    }

    private void drawStreets() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());

        //don't draw if they are  same system or not connected
        for (int i = 0; i < this.castleContainer.getChildren().size(); i++) {
            Node nodeA = this.castleContainer.getChildren().get(i);
            CastleComponent castleA = (CastleComponent) nodeA;

            for (int j = i + 1; j < this.castleContainer.getChildren().size(); j++) {
                Node nodeB = this.castleContainer.getChildren().get(j);
                CastleComponent castleB = (CastleComponent) nodeB;
                this.drawStreet(gc, castleA, castleB, checkFat(castleA, castleB));
            }
        }
    }

    private boolean checkFat(CastleComponent castleA, CastleComponent castleB) {
        if (routes != null) {
            for (ArrayList<String> route : routes) {
                for (int k = 0; k < route.size() - 1; k++) {
                    if ((castleA.system._id().equals(route.get(k)) && castleB.system._id().equals(route.get(k + 1)))
                            || (castleA.system._id().equals(route.get(k + 1)) && castleB.system._id().equals(route.get(k)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Point2D getImageCenter(CastleComponent castle) {
        //vbox centers the image and label
        final Bounds castleBounds = castle.getLayoutBounds();
        final double imageDisplacement = this.castleImageSize / 2;
        return new Point2D(castle.getLayoutX() + castleBounds.getWidth() / 2, castle.getLayoutY() + imageDisplacement);
    }

    private void drawStreet(GraphicsContext gc, CastleComponent castleA, CastleComponent castleB, boolean fat) {
        if (castleA.system._id().equals(castleB.system._id()) ||
                !castleA.system.links().containsKey(castleB.system._id())) {
            return;
        }

        gc.setStroke(Color.web(INGAME_STREET_COLOR));
        if (fat) {
            gc.setLineWidth(4);
        } else {
            gc.setLineWidth(1);
            gc.setLineDashes(2);
        }
        if (castleA.isNeedsLayout()) {
            Platform.runLater(() -> this.strokeStreet(castleA, castleB, gc));
        } else {
            this.strokeStreet(castleA, castleB, gc);
        }
    }

    private void strokeStreet(CastleComponent castleA, CastleComponent castleB, GraphicsContext gc) {
        Point2D castleACenter = this.getImageCenter(castleA);
        Point2D castleBCenter = this.getImageCenter(castleB);

        gc.strokeLine(
                castleACenter.getX(),
                castleACenter.getY(),
                castleBCenter.getX(),
                castleBCenter.getY()
        );
    }

    private Node outerNode(Node node) {
        return centeredNode(node);
    }

    private Node centeredNode(Node node) {
        VBox vBox = new VBox(node);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }

    private Point2D imageViewToImageCoordinates(double x, double y) {
        final double scale = this.view.getFitWidth() / this.view.getImage().getWidth();

        final double unscaledX = x / scale;
        final double unscaledY = y / scale;

        return new Point2D(unscaledX, unscaledY);
    }

    public Group getCastleContainer() {
        return this.castleContainer;
    }

    public void clearCanvas() {
        this.canvas.getGraphicsContext2D().clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
    }

    public void signalizePlacing() {
        this.placeCastlesProperty.set(!this.placeCastlesProperty.get());
    }

    @OnDestroy
    public void OnDestroy() {
        this.scrollPane.widthProperty().removeListener(this.windowChangeListener);
        this.scrollPane.heightProperty().removeListener(this.windowChangeListener);
        this.scrollPane.removeEventFilter(ScrollEvent.ANY, scrollEventHandler);
        this.castleContainerSubscription.unsubscribe();
    }

    public void updateBattleIcons(Map<String, Boolean> battleMap) {
        for (Node node : castleContainer.getChildren()) {
            CastleComponent castle = (CastleComponent) node;
            castle.updateBattleIcon(battleMap.getOrDefault(castle.system._id(), false));
        }
    }

    public void updateFleets(ObservableList<Fleet> fleets, ObservableList<String> warEnemies, Map<String, ArrayList<String>> jobs) {
        for (Node node : castleContainer.getChildren()) {
            CastleComponent castle = (CastleComponent) node;
            ArrayList<Fleet> castleFleets = new ArrayList<>();
            for (Fleet fleet : fleets) {
                if (fleet.location().equals(castle.system._id())) {
                    castleFleets.add(fleet);
                }
            }
            castle.updateFleets(castleFleets, warEnemies, jobs);
        }
    }
}