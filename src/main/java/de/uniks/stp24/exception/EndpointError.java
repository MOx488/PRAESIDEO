package de.uniks.stp24.exception;

import de.uniks.stp24.exception.endpoints.*;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Invocation;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class EndpointError {
    final Map<String, ErrorMessageLocalizer> endpointErrorMessages = new HashMap<>(); // retrofit method name maps to error message localizer

    final List<EndpointErrorGroup> endpointErrors = new ArrayList<>();

    @Inject
    public EndpointError() {
        this.populateErrorMessages();
    }

    public void populateErrorMessages() {
        this.endpointErrors.add(new AuthEndpointError());
        this.endpointErrors.add(new GamesEndpointError());
        this.endpointErrors.add(new UsersEndpointError());
        this.endpointErrors.add(new GameLogicEndpointError());
        this.endpointErrors.add(new FriendsEndpointError());

        for (EndpointErrorGroup endpointError : endpointErrors) {
            endpointError.populateEndPointErrorMessages(endpointErrorMessages);
        }
    }

    public String getLocalizedEndpointErrorMessageKey(Response response) {
        Request req = response.request();
        Invocation tag = req.tag(Invocation.class);
        ErrorMessageLocalizer messageHandler = this.endpointErrorMessages.get(tag != null ? tag.method().getName() : null);

        if (messageHandler == null) {
            return null;
        }

        return messageHandler.getLocalizedMessage(response);
    }
}
