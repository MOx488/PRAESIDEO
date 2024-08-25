package de.uniks.stp24.exception;

import okhttp3.Response;
import org.fulib.fx.annotation.controller.Resource;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public class ErrorMessageLocalizer {
    final public Map<Integer, Function<Response, String>> errorMessages; // response code maps to function that receives report and returns message

    @Inject
    @Resource
    ResourceBundle bundle;

    public ErrorMessageLocalizer() {
        this.errorMessages = new HashMap<>();
    }

    public ErrorMessageLocalizer addErrorMessage(int statusCode, Function<Response, String> messageHandler) {
        errorMessages.put(statusCode, messageHandler);
        return this;
    }

    public String getLocalizedMessage(Response response) {
        int statusCode = response.code();
        Function<Response, String> messageHandler = errorMessages.get(statusCode);
        if (messageHandler == null) {
            return null;
        }

        return messageHandler.apply(response);
    }

}
