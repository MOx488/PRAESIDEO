package de.uniks.stp24.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.stp24.dto.EventEffectDto;
import de.uniks.stp24.dto.EventEffectsDto;
import de.uniks.stp24.dto.UpdateEmpireDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.ws.EventListener;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

import static de.uniks.stp24.model.EventType.REGULAR;

@Singleton
public class EventService {
    @Inject
    public Subscriber subscriber;

    @Inject
    public ObjectMapper objectMapper;

    @Inject
    public GameEmpiresApiService gameEmpiresApiService;

    @Inject
    public EventListener eventListener;

    @Inject
    public PrefService prefService;

    final Random random;

    List<EventEffectDto> eventEffects;

    EventEffectDto activeEvent;
    int activeEventStartPeriod;
    boolean hasEventStarted;

    List<Double> cumulativeProbabilities;

    Game game;
    Empire empire;
    private Consumer<String> newEventConsumer;
    private Consumer<String> endEventConsumer;
    private int previousChoice;
    private int effectIndex;

    @Inject
    public EventService() {
        this.random = new Random();

    }

    public void initializeEventService(Game game, Empire empire) {
        this.game = game;
        this.empire = empire;

        this.populateEventEffectsDto();
        this.populatePossibilityField();

        //check if there was an active event when the user left before
        if (empire._private() != null && empire._private().containsKey("activeEventId") && empire._private().containsKey("ticksLeft") && empire._private().containsKey("effectIndex")) {
            final String eventIdAtLeave = (String) empire._private().get("activeEventId");
            final int ticksLeft = (int) empire._private().get("ticksLeft");
            final int effectIndex = (int) empire._private().get("effectIndex");

            this.activeEvent = getEventEffectById(eventIdAtLeave);
            this.startEvent(eventIdAtLeave, effectIndex);
            this.activeEvent.setDuration(ticksLeft);
        }

        this.startEventService();
    }

    private void populatePossibilityField() {
        final List<Double> mtths = eventEffects.stream().map(EventEffectDto::mtth).toList();
        final List<Double> probabilities = mtths.stream().map(mtth -> 1 / mtth).toList();
        final double totalEventProbability = probabilities.stream().reduce(0d, Double::sum);
        final double noEventProbability = 1 - totalEventProbability;

        List<Double> cumulativeProbabilities = new ArrayList<>();
        cumulativeProbabilities.add(noEventProbability);

        double cumulativeSum = 0d;
        for (double probability : probabilities) {
            cumulativeSum += probability;
            cumulativeProbabilities.add(noEventProbability + cumulativeSum);
        }

        this.cumulativeProbabilities = cumulativeProbabilities;
    }

    public void startEventService() {
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".*", Game.class), event -> {
            Game newGame = event.data();
            if (newGame.period() == this.game.period()) {
                return;
            }


            this.game = newGame;

            if (activeEvent != null) {
                if (hasEventStarted) {
                    //if duration is over -> remove event
                    if (this.game.period() - activeEventStartPeriod >= activeEvent.duration()) {
                        this.removeEvent();
                    }
                }
                return;
            }

            //there wasn't an active event before

            //check within which interval the randomValue falls
            String pickedEventId = null;
            final double randomValue = random.nextDouble();
            for (int i = 0; i < cumulativeProbabilities.size(); i++) {
                if (randomValue > cumulativeProbabilities.get(i)) {
                    continue;
                }

                //no event
                if (i == 0) {
                    break;
                }

                // i - 1 as no event shifted every event up by 1
                pickedEventId = eventEffects.get(i - 1).id();
                break;
            }

            if (pickedEventId == null) {
                return;
            }

            this.setEvent(pickedEventId);
        });

        //keep empire effects updated
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), event -> this.empire = event.data()
        );
    }

    public void setEvent(String pickedEventId) {
        this.activeEvent = getEventEffectById(pickedEventId);
        this.activeEventStartPeriod = -1;
        this.newEventConsumer.accept(pickedEventId);
    }

    public void removeEvent() {
        if (this.activeEvent == null) {
            return;
        }

        if (empire._private() != null) {
            final Object savedActiveEventId = empire._private().getOrDefault("activeEventId", null);
            //we previously saved an event -> we need to remove it as the event ran it
            if (savedActiveEventId != null) {
                this.clearEventData();
            }
        }

        this.removeEventEffects(activeEvent.id());
        this.endEventConsumer.accept(activeEvent.id());
        this.activeEvent = null;
        this.hasEventStarted = false;
        this.activeEventStartPeriod = -1;
    }

    public void populateEventEffectsDto() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("de/uniks/stp24/constants/event_effects.json");
        if (inputStream == null) {
            System.err.println("Couldn't find event_effects.json");
        }

        try {
            this.eventEffects = objectMapper.readValue(inputStream, EventEffectsDto.class).eventEffects();
        } catch (IOException e) {
            System.err.println("Couldn't parse event_effects.json");
        }
    }

    public EventEffectDto getEventEffectById(String eventId) {
        return eventEffects.stream().filter(e -> e.id().equals(eventId)).findFirst().orElse(null);
    }

    public boolean isSpecialEvent(String eventId) {
        return eventEffects.stream().filter(e -> e.id().equals(eventId) && e.type() == EventType.SPECIAL).findFirst().orElse(null) != null;
    }

    public List<Effect> getEventEffects(String eventId, int effectIndex) {
        final EventEffectDto eventEffect = getEventEffectById(eventId);
        if (eventEffect == null || eventEffect.type() == EventType.RESIGNATION) {
            return null;
        }

        if (eventEffect.type() == REGULAR) {
            return eventEffect.effects();
        }

        return eventEffect.effects_accept().get(effectIndex - 1);
    }

    public EventEffectDto getEventEffectDto(String eventId, int effectIndex) {
        final EventEffectDto eventEffect = getEventEffectById(eventId);
        if (eventEffect == null) {
            return null;
        }

        if (eventEffect.type() == REGULAR) {
            return eventEffect;
        }

        if (effectIndex == -1) {
            return null;
        }

        return new EventEffectDto(eventEffect.id(), eventEffect.type(), eventEffect.mtth(), eventEffect.duration(), eventEffect.effects_accept().get(effectIndex - 1), null);
    }

    public void setEventEffects(String eventId, int effectIndex) {
        if (isSpecialEvent(eventId) && effectIndex == -1) {
            //we set event effect after user made his choice, if he didn't we don't apply any effects yet
            return;
        }

        List<Effect> eventEffects = getEventEffects(eventId, effectIndex);
        if (eventEffects == null) {
            return;
        }

        List<EffectSource> eventEffectsSources = getEventEffects(eventId, effectIndex).stream().filter(effect -> effect.base() == 0).map(effect -> new EffectSource(eventId, List.of(effect))).toList();
        if (eventEffectsSources.isEmpty()) {
            return;
        }

        List<EffectSource> effectSources = empire.effects();
        effectSources.addAll(eventEffectsSources);
        UpdateEmpireDto updateEmpireDto = new UpdateEmpireDto(null, null, effectSources, null, null);
        subscriber.subscribe(gameEmpiresApiService.updateEmpire(game._id(), empire._id(), updateEmpireDto).subscribe());
    }

    public void removeEventEffects(String eventId) {
        List<EffectSource> effectSources = empire.effects();
        effectSources.removeIf(effectSource -> effectSource.id().equals(eventId));

        UpdateEmpireDto updateEmpireDto = new UpdateEmpireDto(null, null, effectSources, null, null);
        subscriber.subscribe(gameEmpiresApiService.updateEmpire(game._id(), empire._id(), updateEmpireDto).subscribe());
    }

    public void startEvent(String eventId, int effectIndex) {
        if (hasEventStarted) {
            //already started
            return;
        }

        this.activeEventStartPeriod = this.game.period();
        this.hasEventStarted = true;
        this.effectIndex = effectIndex;

        this.setEventEffects(eventId, effectIndex);
    }

    public void saveEventData() {
        if (this.activeEvent == null) {
            return;
        }

        final int ticksLeft = this.activeEvent.duration() - (this.game.period() - activeEventStartPeriod);
        Map<String, Object> _private = new HashMap<>();
        if (empire._private() != null) {
            _private = empire._private();
        }
        _private.put("activeEventId", this.activeEvent.id());
        _private.put("ticksLeft", ticksLeft);
        _private.put("previousChoice", this.previousChoice);
        _private.put("effectIndex", this.effectIndex);

        final UpdateEmpireDto updateEmpireDto = new UpdateEmpireDto(null, null, null, _private, null);
        subscriber.subscribe(gameEmpiresApiService.updateEmpire(game._id(), empire._id(), updateEmpireDto).subscribe());
    }

    public void clearEventData() {
        final UpdateEmpireDto updateEmpireDto = new UpdateEmpireDto(null, null, null, null, null);
        subscriber.subscribe(gameEmpiresApiService.updateEmpire(game._id(), empire._id(), updateEmpireDto).subscribe());
    }

    public int getActiveEventStartPeriod() {
        return activeEventStartPeriod;
    }

    public int getDuration() {
        if (activeEvent == null) {
            return -1;
        }

        return activeEvent.duration();
    }

    public void setPreviousChoice(int ordinal) {
        this.previousChoice = ordinal;
    }

    public void stopEventService() {
        subscriber.dispose();
    }

    public void setConsumers(Consumer<String> newEventConsumer, Consumer<String> endEventConsumer) {
        this.newEventConsumer = newEventConsumer;
        this.endEventConsumer = endEventConsumer;
    }

    public void onTick(Runnable onFinish) {
        if (this.activeEvent == null || !hasEventStarted) {
            onFinish.run();
            return;
        }

        Map<String, Double> resourceChangeMap = new HashMap<>();
        for (Effect effect : this.getEventEffects(activeEvent.id(), effectIndex)) {
            if (effect.base() == 0) {
                continue;
            }

            resourceChangeMap.put(effect.variable(), effect.base());
        }

        if (resourceChangeMap.isEmpty()) {
            onFinish.run();
            return;
        }

        final ChangeRessource resourceChange = new ChangeRessource(resourceChangeMap);
        subscriber.subscribe(gameEmpiresApiService.updateResources(game._id(), empire._id(), resourceChange, true).subscribe((empire) -> onFinish.run(), (error) -> onFinish.run()));
    }
}
