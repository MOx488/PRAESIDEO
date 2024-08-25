package de.uniks.stp24.rest;

import de.uniks.stp24.dto.UpdateFriendDto;
import de.uniks.stp24.model.Friend;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;

public interface FriendsApiService {
    @GET("users/{from}/friends")
    Observable<List<Friend>> getFriends(@Path("from") String from);

    @GET("users/{from}/friends")
    Observable<List<Friend>> getFriendsByStatus(@Path("from") String from, @Query("status") String status);

    @PUT("users/{from}/friends/{to}")
    Observable<Friend> createFriendRequest(@Path("from") String from, @Path("to") String to);

    @PATCH("users/{from}/friends/{to}")
    Observable<Friend> acceptFriendRequest(@Path("from") String from, @Path("to") String to, @Body UpdateFriendDto dto);

    @DELETE("users/{from}/friends/{to}")
    Observable<Friend> deleteFriendOrRejectFriendRequest(@Path("from") String from, @Path("to") String to);
}
