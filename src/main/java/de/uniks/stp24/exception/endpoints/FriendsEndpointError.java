package de.uniks.stp24.exception.endpoints;

import de.uniks.stp24.exception.EndpointErrorGroup;
import de.uniks.stp24.exception.ErrorMessageLocalizer;

import java.util.Map;

public class FriendsEndpointError implements EndpointErrorGroup {
    @Override
    public void populateEndPointErrorMessages(Map<String, ErrorMessageLocalizer> endpointErrorContainer) {
        endpointErrorContainer.put("getFriends", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.access.only.own.friendlist")
        );

        endpointErrorContainer.put("createFriendRequest", new ErrorMessageLocalizer()
                .addErrorMessage(403, response -> "error.create.friend.request.only.own.account")
        );

    }

}
