package de.uniks.stp24.exception.endpoints;

import de.uniks.stp24.exception.EndpointErrorGroup;
import de.uniks.stp24.exception.ErrorMessageLocalizer;

import java.util.Map;

public class AuthEndpointError implements EndpointErrorGroup {
    @Override
    public void populateEndPointErrorMessages(Map<String, ErrorMessageLocalizer> endpointErrorContainer) {
        endpointErrorContainer.put("login", new ErrorMessageLocalizer()
                .addErrorMessage(401, response -> "error.invalid.data")
        );

        endpointErrorContainer.put("refresh", new ErrorMessageLocalizer()
                .addErrorMessage(401, response -> "error.invalid.refresh")
        );
    }

}
