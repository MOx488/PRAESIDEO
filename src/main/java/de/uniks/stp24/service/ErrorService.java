package de.uniks.stp24.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.stp24.App;
import de.uniks.stp24.exception.EndpointError;
import de.uniks.stp24.model.ErrorResponse;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.fulib.fx.annotation.controller.Resource;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ResourceBundle;

@Singleton
public class ErrorService {

    @Inject
    App app;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ImageCache imageCache;

    @Inject
    EndpointError endpointError;

    @Inject
    @Resource
    public ResourceBundle bundle;

    @Inject
    public ErrorService() {
    }

    public ErrorService setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        return this;
    }


    public String getLocalizedErrorMessage(Response response) {
        int code = response.code();

        switch (code) {
            case 400 -> {
                return bundle.getString("error.bad.request");
            }
            case 401 -> {
                if (isAuthorizedRequest(response)) {
                    return bundle.getString("error.unauthorized");
                }
            }
            case 404 -> {
                return bundle.getString("error.not.found");
            }
            case 429 -> {
                return bundle.getString("error.rate.limit.reached");
            }
        }

        String key = endpointError.getLocalizedEndpointErrorMessageKey(response);
        if (key != null) {
            return bundle.getString(key);
        }

        //its a ErrorResponse -> serialize it and return message json value
        try {
            ResponseBody responseBody = response.peekBody(1024);
            ErrorResponse errorResponse = objectMapper.readValue(responseBody.string(), ErrorResponse.class);
            return errorResponse.message().getFirst();
        } catch (IOException e) {
            return bundle.getString("error.unknown");
        }
    }

    private boolean isAuthorizedRequest(Response response) {
        return response.headers().get("Authorization") != null;
    }
}