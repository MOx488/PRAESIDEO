package de.uniks.stp24.exception.endpoints;

import de.uniks.stp24.exception.EndpointErrorGroup;
import de.uniks.stp24.exception.ErrorMessageLocalizer;

import java.util.Map;

public class GamesEndpointError implements EndpointErrorGroup {
    @Override
    public void populateEndPointErrorMessages(Map<String, ErrorMessageLocalizer> endpointErrorContainer) {
        endpointErrorContainer.put("updateGame", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.change.other.game")
                .addErrorMessage(409, response -> "error.game.already.running")
        );

        endpointErrorContainer.put("deleteGame", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.delete.other.game")
        );

        endpointErrorContainer.put("joinGame", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.invalid.password")
                .addErrorMessage(409, response -> "error.join.failed")
        );

        endpointErrorContainer.put("updateMember", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.change.other.member")
                .addErrorMessage(409, response -> "error.game.already.started.or.empire")
        );

        endpointErrorContainer.put("leaveGame", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.kick.someone.else")
                .addErrorMessage(409, response -> "error.game.already.started.or.owner.leave")
        );

        endpointErrorContainer.put("updateSystem", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.not.system.owner")
        );

        endpointErrorContainer.put("updateEmpire", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.not.empire.owner")
        );

    }

}
