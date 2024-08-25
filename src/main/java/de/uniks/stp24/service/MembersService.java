package de.uniks.stp24.service;

import de.uniks.stp24.dto.UpdateMemberDto;
import de.uniks.stp24.model.Member;
import de.uniks.stp24.rest.GameMembersApiService;
import de.uniks.stp24.rest.GamesApiService;
import io.reactivex.rxjava3.core.Observable;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;

public class MembersService {
    @Inject
    GameMembersApiService gameMembersApiService;

    @Inject
    GamesApiService gamesApiService;

    @Inject
    Subscriber subscriber;

    @Inject
    public MembersService() {

    }

    public Observable<Member> leaveGame(String gameId, String userId) {
        return gameMembersApiService.leaveGame(gameId, userId);
    }

    public Observable<Member> updateMember(String gameId, String userId, UpdateMemberDto updateMemberDto) {
        return gameMembersApiService.updateMember(gameId, userId, updateMemberDto);
    }


}
