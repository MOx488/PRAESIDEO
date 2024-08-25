package de.uniks.stp24.rest;

import de.uniks.stp24.dto.CreateUserDto;
import de.uniks.stp24.dto.UpdateUserDto;
import de.uniks.stp24.model.User;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;

public interface UsersApiService {
    @POST("users")
    Observable<User> createUser(@Body CreateUserDto dto);

    @GET("users")
    Observable<List<User>> getUsers();

    @GET("users")
    Observable<List<User>> getUsersByIDs(@Query("ids") List<String> ids);

    @GET("users/{id}")
    Observable<User> getUser(@Path("id") String id);

    @PATCH("users/{id}")
    Observable<User> updateUser(@Path("id") String id, @Body UpdateUserDto dto);

    @DELETE("users/{id}")
    Observable<User> deleteUser(@Path("id") String id);
}
