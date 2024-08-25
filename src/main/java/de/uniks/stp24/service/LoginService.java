package de.uniks.stp24.service;

import de.uniks.stp24.dto.LoginDto;
import de.uniks.stp24.dto.LoginResult;
import de.uniks.stp24.dto.RefreshDto;
import de.uniks.stp24.rest.AuthApiService;
import io.reactivex.rxjava3.core.Observable;

import javax.inject.Inject;

public class LoginService {
    @Inject
    AuthApiService authApiService;
    @Inject
    TokenStorage tokenStorage;
    @Inject
    PrefService prefService;

    @Inject
    public LoginService() {
    }

    public boolean autoLogin() {
        final String refreshToken = prefService.getRefreshToken();
        if (refreshToken == null || System.getenv("DISABLE_AUTO_LOGIN") != null) {
            return false;
        }

        try {
            final LoginResult result = authApiService.refresh(new RefreshDto(refreshToken)).blockingFirst();
            tokenStorage.setToken(result.accessToken());
            tokenStorage.setUserId(result._id());
            prefService.setRefreshToken(refreshToken);
            return true;
        } catch (Exception e) {
            prefService.removeRefreshToken();
            return false;
        }
    }

    public void loginClientSwitch(LoginResult loginResult) {
        if (loginResult != null) {
            tokenStorage.setToken(loginResult.accessToken());
            tokenStorage.setUserId(loginResult._id());
            prefService.setRefreshToken(loginResult.refreshToken());
        }
    }

    public Observable<LoginResult> login(String username, String password, boolean rememberMe) {
        return authApiService
                .login(new LoginDto(username, password))
                .doOnNext(loginResult -> {
                    tokenStorage.setToken(loginResult.accessToken());
                    tokenStorage.setUserId(loginResult._id());

                    if (rememberMe) {
                        prefService.setRefreshToken(loginResult.refreshToken());
                    }
                });
    }
}
