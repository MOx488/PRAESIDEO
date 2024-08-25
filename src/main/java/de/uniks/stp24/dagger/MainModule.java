package de.uniks.stp24.dagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import de.uniks.stp24.App;
import de.uniks.stp24.service.PrefService;
import org.fulib.fx.FulibFxApp;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ResourceBundle;

@Module
public class MainModule {
    @Provides
    ResourceBundle bundle(PrefService prefService) {
        return ResourceBundle.getBundle("de/uniks/stp24/lang/lang", prefService.getLocale());
    }

    @Provides
    @Named("building-icons")
    ResourceBundle buildingIcons(PrefService prefService) {
        return ResourceBundle.getBundle("de/uniks/stp24/lang/building_icons", prefService.getLocale());
    }

    @Provides
    FulibFxApp app(App app) {
        return app;
    }


    @Provides
    @Singleton
    ObjectMapper mapper() {
        return new ObjectMapper()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

    }
}
