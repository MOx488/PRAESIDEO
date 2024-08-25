package de.uniks.stp24.service;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.UpdateEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import java.util.*;

public class EmojiService {

    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public ImageCache imageCache;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public UsersApiService usersApiService;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;
    @Inject
    public App app;
    @Inject
    public NotificationService notificationService;

    @Inject
    public EmojiService() {
    }

    public void initializeEmojiService(Empire empire, Game game) {
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires.*.updated", Empire.class), event -> {
            // show received if not already shown
            handleEmojiReceived(event, empire, game);

            // check if already shown
            deleteEmojiSend(event, empire, game);
            deleteEmojiReceived(event, empire, game);
        });
    }

    private void deleteEmojiSend(Event<Empire> event, Empire empire, Game game) {
        Map<String, Integer> emojiSend = getMap(empire._public(), "emojiSend");
        Map<String, Integer> emojiReceived = getMap(event.data()._public(), "emojiReceived");

        if (emojiReceived.containsKey(empire._id())) {
            emojiSend.remove(event.data()._id());
            updateEmpirePublicData(empire, "emojiSend", emojiSend, game);
        }
    }

    private void deleteEmojiReceived(Event<Empire> event, Empire empire, Game game) {
        Map<String, Integer> emojiReceived = getMap(empire._public(), "emojiReceived");

        if (!emojiReceived.containsKey(event.data()._id())) return;

        boolean delete = !getMap(event.data()._public(), "emojiSend").containsKey(empire._id());

        if (delete) {
            emojiReceived.remove(event.data()._id());
            updateEmpirePublicData(empire, "emojiReceived", emojiReceived, game);
        }
    }

    public void handleEmojiReceived(Event<Empire> event, Empire empire, Game game) {
        Map<String, Integer> emojiSend = getMap(event.data()._public(), "emojiSend");
        if (!emojiSend.containsKey(empire._id())) return;

        Map<String, Integer> emojiReceived = getMap(empire._public(), "emojiReceived");
        if (emojiReceived.containsKey(event.data()._id()) && emojiReceived.get(event.data()._id()).equals(emojiSend.get(empire._id())))
            return;

        subscriber.subscribe(usersApiService.getUser(event.data().user()), result -> {
            Map<String, Object> _public = Optional.ofNullable(empire._public()).orElse(new HashMap<>());
            Map<String, Integer> updateEmojiReceived = getMap(_public, "emojiReceived");

            updateEmojiReceived.put(event.data()._id(), emojiSend.get(empire._id()));
            _public.put("emojiReceived", updateEmojiReceived);

            subscriber.subscribe(
                    gameEmpiresApiService.updateEmpire(game._id(), empire._id(), new UpdateEmpireDto(null, null, null, null, _public)),
                    updateEvent -> {
                        List<String> emojiNames = Arrays.asList("slightly_smiling_face", "pouting_face", "face_with_tears_of_joy", "skull", "pensive_face");
                        String emojiPath = "image/emojis/" + emojiNames.get(emojiSend.get(empire._id())) + ".png";
                        String senderMessage = " " + bundle.getString("send.by") + " " + result.name();
                        notificationService.displayNotification(senderMessage, true, emojiPath);
                    }
            );
        });
    }

    private Map<String, Integer> getMap(Map<String, Object> _public, String key) {
        return Optional.ofNullable(_public)
                .map(data -> (Map<String, Integer>) data.get(key))
                .orElse(new HashMap<>());
    }

    private void updateEmpirePublicData(Empire empire, String key, Map<String, Integer> data, Game game) {
        Map<String, Object> _public = Optional.ofNullable(empire._public()).orElse(new HashMap<>());
        _public.put(key, data);
        subscriber.subscribe(gameEmpiresApiService.updateEmpire(game._id(), empire._id(),
                new UpdateEmpireDto(null, null, null, null, _public)));
    }

    public void stopEmojiService() {
        subscriber.dispose();
    }
}
