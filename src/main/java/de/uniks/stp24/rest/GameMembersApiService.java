package de.uniks.stp24.rest;

import de.uniks.stp24.dto.CreateMemberDto;
import de.uniks.stp24.dto.UpdateMemberDto;
import de.uniks.stp24.model.Member;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;

public interface GameMembersApiService {
    @POST("games/{gameId}/members")
    Observable<Member> joinGame(@Path("gameId") String gameId, @Body CreateMemberDto dto);

    @GET("games/{gameId}/members")
    Observable<List<Member>> getMembersOfGame(@Path("gameId") String gameId);

    @GET("games/{gameId}/members/{memberId}")
    Observable<Member> getMember(@Path("gameId") String gameId, @Path("memberId") String memberId);

    @PATCH("games/{gameId}/members/{memberId}")
    Observable<Member> updateMember(@Path("gameId") String gameId, @Path("memberId") String memberId, @Body UpdateMemberDto dto);

    @DELETE("games/{gameId}/members/{memberId}")
    Observable<Member> leaveGame(@Path("gameId") String gameId, @Path("memberId") String memberId);
}
