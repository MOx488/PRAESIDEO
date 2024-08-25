package de.uniks.stp24.service;

import de.uniks.stp24.component.CastleComponent;
import de.uniks.stp24.component.ZoomDragComponent;
import de.uniks.stp24.dto.CreateJobDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.JobsApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapService {

    @Inject
    Subscriber subscriber;
    @Inject
    JobsApiService jobsApiService;
    @Inject
    JobService jobService;

    @Inject
    MapService() {
    }

    private void updateBattleIcons(ObservableList<Fleet> fleets, ObservableList<GameSystem> systems, ObservableList<War> wars, ZoomDragComponent zoomDragComponent, Empire empire) {
        Map<String, Boolean> battleMap = new HashMap<>();
        for (Fleet fleet1 : fleets) {
            if (fleet1.empire() == null || !fleet1.empire().equals(empire._id())) {
                continue;
            }
            for (Fleet fleet2 : fleets) {
                if (fleet1.location().equals(fleet2.location()) && (!fleet1.empire().equals(fleet2.empire()))) {
                    if (checkWar(fleet1.empire(), fleet2.empire(), wars)) {
                        battleMap.put(fleet1.location(), true);
                    }
                }
            }
            for (GameSystem system : systems) {
                if (!fleet1.empire().equals(system.owner()) && fleet1.location().equals(system._id()) && checkWar(fleet1.empire(), system.owner(), wars)) {
                    battleMap.put(fleet1.location(), true);
                }
            }
        }
        zoomDragComponent.updateBattleIcons(battleMap);
    }

    private boolean checkWar(String fleet1, String fleet2, ObservableList<War> wars) {
        for (War war : wars) {
            if ((war.attacker().equals(fleet1) && war.defender().equals(fleet2)) || (war.attacker().equals(fleet2) && war.defender().equals(fleet1))) {
                return true;
            }
        }
        return false;
    }

    public boolean handleCastleClicked(CastleComponent castleComponent, CastleComponent selectedCastle, Graph<String, DefaultWeightedEdge> graph, Game game, Empire empire) {
        if ((selectedCastle == null || selectedCastle.equals(castleComponent))) {
            return false;
        }
        DijkstraShortestPath<String, DefaultWeightedEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
        List<String> path = dijkstraAlg.getPath(selectedCastle.getSystem()._id(), castleComponent.getSystem()._id()).getVertexList();
        Fleet selectedFleet = selectedCastle.getSelectedFleet();
        CreateJobDto travelJob = new CreateJobDto(null, 0, "travel", null, null, null, selectedFleet._id(), null, path);
        subscriber.subscribe(jobsApiService.createJob(game._id(), empire._id(), travelJob), job -> selectedCastle.setTravelling());
        return true;
    }

    public ArrayList<ArrayList<String>> findRoutes(ObservableList<Fleet> fleets, ObservableList<String> warEnemies, ZoomDragComponent zoomDragComponent) {
        Map<String, ArrayList<String>> jobs = new HashMap<>();
        ArrayList<ArrayList<String>> routes = new ArrayList<>();
        if (jobService.getJobs() == null) {
            return null;
        }
        jobService.getJobs().forEach(job -> {
            if (job.fleet() == null) {
                return;
            }
            ArrayList<String> jobInfo = new ArrayList<>();
            jobInfo.add(job.path().getLast());
            jobInfo.add(job._id());
            jobs.put(job.fleet(), jobInfo);
            ArrayList<String> path = (ArrayList<String>) job.path();
            fleets.stream().filter(fleet -> fleet._id().equals(job.fleet())).forEach(fleet -> {
                if (path.contains(fleet.location())) {
                    while (!path.getFirst().equals(fleet.location())) {
                        path.removeFirst();
                    }
                } else {
                    path.addFirst(fleet.location());
                }
            });
            routes.add(path);
        });
        zoomDragComponent.updateFleets(fleets, warEnemies, jobs);
        return routes;
    }

    public void updateFleets(ZoomDragComponent zoomDragComponent, ObservableList<Fleet> fleets, ObservableList<String> warEnemies, ObservableList<GameSystem> systems, ObservableList<War> wars, Empire empire) {
        zoomDragComponent.updateStreets(this.findRoutes(fleets, warEnemies, zoomDragComponent));
        updateBattleIcons(fleets, systems, wars, zoomDragComponent, empire);
    }

    public ObservableList<String> findWarEnemies(@NotNull List<War> wars, Empire empire) {
        ObservableList<String> warEnemies = FXCollections.observableArrayList();
        for (War war : wars) {
            if (war.attacker().equals(empire._id())) {
                warEnemies.add(war.defender());
            } else if (war.defender().equals(empire._id())) {
                warEnemies.add(war.attacker());
            }
        }
        return warEnemies;
    }
}
