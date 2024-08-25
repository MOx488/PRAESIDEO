package de.uniks.stp24.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.stp24.dto.ClientChangeDto;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.ws.EventListener;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class ClientChangeService {
    @Inject
    public Subscriber subscriber;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;
    @Inject
    public EventListener eventListener;
    @Inject
    public PrefService prefService;
    @Inject
    public ObjectMapper objectMapper;

    private static final Logger logger = Logger.getLogger(ClientChangeService.class.getName());

    Game game;
    boolean changeRequested = false;
    ClientChangeDto[] clientChangeDtoList;
    ClientChangeDto currentClientChangeDto;
    ClientChangeDto nextClientChangeDto;

    @Inject
    public ClientChangeService() {
    }

    public void initializeClientChangeService(Game game) {
        this.game = game;
        readClientConfig();
        getEndPeriod();
        startClientChangeService();
    }

    // Read client config Json file in same level as the jar file
    private void readClientConfig() {
        try {
            // Get the location of the JAR file
            Path jarPath = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            // Construct the path to the JSON file
            Path jsonFilePath = jarPath.resolve("client_config.json");
            // Read the JSON file
            InputStream inputStream = Files.newInputStream(jsonFilePath);
            try {
                this.clientChangeDtoList = objectMapper.readValue(inputStream, ClientChangeDto[].class);
            } catch (IOException e) {
                System.err.println("Couldn't parse client_config.json: " + e.getMessage());
            }
        } catch (URISyntaxException | IOException e) {
            System.err.println("Couldn't parse client_config.json: " + e.getMessage());
        }
    }

    // Get the end period of the current client and the next client
    private void getEndPeriod() {
        if (this.clientChangeDtoList == null) {
            return;
        }
        for (int i = 0; i < this.clientChangeDtoList.length; i++) {
            if (this.clientChangeDtoList[i].name().equals("Praesideo")) {
                this.currentClientChangeDto = clientChangeDtoList[i];
                if (i + 1 < clientChangeDtoList.length) {
                    this.nextClientChangeDto = clientChangeDtoList[i + 1];
                }
                break;
            }
        }
    }

    // Event listener for the game period and check
    private void startClientChangeService() {
        if (this.currentClientChangeDto == null) {
            return;
        }
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".ticked", Game.class), event -> {
            Game newGame = event.data();
            if (newGame.period() == this.game.period()) {
                return;
            }
            this.game = newGame;
            if (this.game.period() == this.currentClientChangeDto.stopPeriod()) {
                if (!this.changeRequested && this.nextClientChangeDto != null) {
                    this.changeRequested = true;
                    changeClient();
                }
            }
        });
    }

    private void changeClient() {
        new Thread(() -> {
            String[] command = {"java", "-jar", this.nextClientChangeDto.filename(),
                    this.prefService.getRefreshToken(), this.game._id()};
            ProcessBuilder pb = new ProcessBuilder(command);
            try {
                logger.info("Rotate Client");
                pb.start();
                subscriber.dispose();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to rotate client", e);
            }
        }).start();
        System.exit(0);
    }

    public void stopClientChangeService() {
        subscriber.dispose();
    }
}

