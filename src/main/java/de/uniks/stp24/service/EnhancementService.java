package de.uniks.stp24.service;

import de.uniks.stp24.dto.UpdateEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.rest.GameEmpiresApiService;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.BiConsumer;

@Singleton
public class EnhancementService {

    @Inject
    public Subscriber subscriber;

    @Inject
    public GameEmpiresApiService gameEmpiresApiService;

    private HashMap<String, Integer> scientistTypeToImageIndex = new HashMap<>();

    private ArrayList<Integer> scientistOrder = new ArrayList<>();

    private BiConsumer<String, Integer> onNewScientist;

    private final List<String> scientistTypes = List.of("physics", "society", "engineering");

    @Inject
    public EnhancementService() {

    }

    public void initializeService(BiConsumer<String, Integer> onNewScientist) {
        this.onNewScientist = onNewScientist;
    }

    public void initializeScientistHandling(Game game, Empire empire) {
        //already loaded
        if (!this.scientistTypeToImageIndex.isEmpty()) {
            this.loadScientists();
            return;
        }

        //not loaded -> check if saved in _private
        if (empire._private() == null || !empire._private().containsKey("scientistTypeToImageIndex")) {
            this.initializeAsNewGame(game, empire);
        } else {
            this.initializeAsLoadGame(empire);
        }

        this.loadScientists();
    }

    public void initializeAsNewGame(Game game, Empire empire) {
        for (int i = 1; i < 16; i++) {
            scientistOrder.add(i);
        }

        Collections.shuffle(scientistOrder);

        for (String scientistType : scientistTypes) {
            scientistTypeToImageIndex.put(scientistType, scientistOrder.getFirst());
        }

        this.saveScientistDataOnServer(game, empire);
    }

    public void initializeAsLoadGame(Empire empire) {
        this.scientistTypeToImageIndex = (HashMap<String, Integer>) empire._private().get("scientistTypeToImageIndex");
        this.scientistOrder = (ArrayList<Integer>) empire._private().get("scientistOrder");
    }

    private void loadScientists() {
        if (this.onNewScientist != null) {
            for (String scientistType : scientistTypes) {
                this.onNewScientist.accept(scientistType, scientistTypeToImageIndex.get(scientistType));
            }
        }
    }

    public int getScientistIndex(String scientistType) {
        return this.scientistTypeToImageIndex.get(scientistType);
    }

    public void resignScientist(Game game, Empire empire, String scientistType) {
        final int currentScientistImageIndex = scientistTypeToImageIndex.get(scientistType);
        final int currentScientistIndex = scientistOrder.indexOf(currentScientistImageIndex);
        final int newScientistIndex = (currentScientistIndex + 1) % 15;
        final int newScientistImageIndex = scientistOrder.get(newScientistIndex);
        this.scientistTypeToImageIndex.put(scientistType, newScientistImageIndex);

        //notify enhancement component
        this.onNewScientist.accept(scientistType, newScientistImageIndex);

        //save data in private field
        this.saveScientistDataOnServer(game, empire);
    }

    private void saveScientistDataOnServer(Game game, Empire empire) {
        Map<String, Object> _private = new HashMap<>();
        if (empire._private() != null) {
            _private = empire._private();
        }

        _private.put("scientistTypeToImageIndex", scientistTypeToImageIndex);
        _private.put("scientistOrder", scientistOrder);

        UpdateEmpireDto updateEmpireDto = new UpdateEmpireDto(null, null, null, _private, null);
        subscriber.subscribe(gameEmpiresApiService.updateEmpire(game._id(), empire._id(), updateEmpireDto).subscribe());
    }

    public void destroy() {
        this.subscriber.dispose();
    }
}
