package de.uniks.stp24.service;

import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.model.User;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class BattleResultService {

    @Inject
    public BattleResultService() {
    }

    public List<Object> handleSystemBattles(GameSystem oldSystem, GameSystem newSystem, Empire empire, List<ReadEmpireDto> empiresInGame, List<User> users) {
        ReadEmpireDto enemyEmpire;
        User enemyUser;
        // oldOwner == null -> normal colonize
        // oldOwner == newOwner -> nothing happened regarding a battle
        // ignore both

        final String oldOwner = oldSystem.owner();
        final String newOwner = newSystem.owner();
        if (oldOwner == null || oldOwner.equals(newOwner)) {
            return null;
        }

        // case 1: oldOwner is us and there is new owner that isn't us since we already checked above that the owners have to be different
        if (oldOwner.equals(empire._id())) {
            // lost
            enemyEmpire = empiresInGame.stream().filter(e -> e._id().equals(newSystem.owner())).findFirst().orElse(null);
            enemyUser = getEnemyUser(enemyEmpire, users);
            if (enemyEmpire == null || enemyUser == null) {
                return null;
            }
            return List.of(true, enemyEmpire, enemyUser);
        }

        // case 2: oldOwner isn't us and new owner is us
        if (newSystem.owner().equals(empire._id())) {
            // won
            enemyEmpire = empiresInGame.stream().filter(e -> e._id().equals(oldSystem.owner())).findFirst().orElse(null);
            enemyUser = getEnemyUser(enemyEmpire, users);
            if (enemyEmpire == null || enemyUser == null) {
                return null;
            }
            return List.of(false, enemyEmpire, enemyUser);
        }
        return null;
    }
    private User getEnemyUser(ReadEmpireDto enemyEmpire, List<User> users) {
        if (enemyEmpire == null) {
            return null;
        }

        return users.stream().filter(u -> u._id().equals(enemyEmpire.user())).findFirst().orElse(null);
    }

}
