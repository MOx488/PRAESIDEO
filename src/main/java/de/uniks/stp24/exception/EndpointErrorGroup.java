package de.uniks.stp24.exception;

import java.util.Map;

public interface EndpointErrorGroup {
    void populateEndPointErrorMessages(Map<String, ErrorMessageLocalizer> endpointErrorContainer);
}
