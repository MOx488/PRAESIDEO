package de.uniks.stp24.service;

import de.uniks.stp24.rest.PresetsApiService;
import io.reactivex.rxjava3.core.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class PresetsService {
    @Inject
    public PresetsApiService presetsApiService;

    private final Map<String, Observable<?>> methodNameToCache = new HashMap<>();

    @Inject
    public PresetsService() {
    }

    public Observable<?> getCachedPreset(String methodName) {
        final Observable<?> cache = methodNameToCache.get(methodName);
        if (cache != null) {
            //we already know this value -> no need to fetch it again
            return cache;
        }

        //we don't know the value yet
        try {
            //fetch it
            final Class<?> c = Class.forName("de.uniks.stp24.rest.PresetsApiService");
            final Method method = c.getMethod(methodName);
            final Observable<?> observableValue = ((Observable<?>) method.invoke(presetsApiService)).cache();

            // cache it
            this.methodNameToCache.put(methodName, observableValue);

            //return it
            return observableValue;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
