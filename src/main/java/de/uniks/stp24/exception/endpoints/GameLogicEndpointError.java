package de.uniks.stp24.exception.endpoints;

import de.uniks.stp24.exception.EndpointErrorGroup;
import de.uniks.stp24.exception.ErrorMessageLocalizer;

import java.util.Map;

public class GameLogicEndpointError implements EndpointErrorGroup {
    @Override
    public void populateEndPointErrorMessages(Map<String, ErrorMessageLocalizer> endpointErrorContainer) {
        endpointErrorContainer.put("getExplainedVariables", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.cannot.view.another.users.empire.variables")
        );

        endpointErrorContainer.put("getExplainedVariable", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.cannot.view.another.users.empire.variables")
        );

        endpointErrorContainer.put("getAggregate", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.cannot.view.another.users.empire.aggregates")
        );

    }

}
