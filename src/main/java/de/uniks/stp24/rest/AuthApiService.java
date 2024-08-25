package de.uniks.stp24.rest;

import de.uniks.stp24.dto.LoginDto;
import de.uniks.stp24.dto.LoginResult;
import de.uniks.stp24.dto.RefreshDto;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {
    @POST("auth/login")
    Observable<LoginResult> login(@Body LoginDto dto);

    @POST("auth/refresh")
    Observable<LoginResult> refresh(@Body RefreshDto dto);

    @POST("auth/logout")
    Completable logout();
}
