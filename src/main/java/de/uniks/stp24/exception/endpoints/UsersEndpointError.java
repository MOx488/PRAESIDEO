package de.uniks.stp24.exception.endpoints;

import de.uniks.stp24.exception.EndpointErrorGroup;
import de.uniks.stp24.exception.ErrorMessageLocalizer;

import java.util.Map;

public class UsersEndpointError implements EndpointErrorGroup {
    @Override
    public void populateEndPointErrorMessages(Map<String, ErrorMessageLocalizer> endpointErrorContainer) {
        endpointErrorContainer.put("createUser", new ErrorMessageLocalizer()
                .addErrorMessage(409, response -> "error.username.taken")
        );

        endpointErrorContainer.put("updateUser", new ErrorMessageLocalizer()
                .addErrorMessage(409, response -> "error.username.taken")
                .addErrorMessage(403, response -> "error.change.others.name")
        );

        endpointErrorContainer.put("deleteUser", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.delete.other.user")
        );

    }

}
