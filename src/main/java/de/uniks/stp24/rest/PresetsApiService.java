package de.uniks.stp24.rest;

import de.uniks.stp24.dto.EmpireVariables;
import de.uniks.stp24.dto.ResourcesResult;
import de.uniks.stp24.dto.SystemUpgradesResult;
import de.uniks.stp24.model.*;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;
import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
public interface PresetsApiService {
    @GET("presets/resources")
    Observable<ResourcesResult> getResources();

    @GET("presets/system-upgrades")
    Observable<SystemUpgradesResult> getSystemUpgrades();

    @GET("presets/system-types")
    Observable<Map<String, SystemType>> getSystemTypes();

    @GET("presets/empire-variables")
    Observable<EmpireVariables> getEmpireVariables();

    @GET("presets/technologies")
    Observable<List<Technology>> getTechnologies();

    @GET("presets/technologies/tree")
    Observable<String> getTechnologyTreeSVG();

    @GET("presets/technologies/{technologyId}")
    Observable<Technology> getTechnology(@Path("technologyId") String technologyId);

    @GET("presets/buildings")
    Observable<List<Building>> getBuildings();

    @GET("presets/buildings/{buildingId}")
    Observable<Building> getBuilding(@Path("buildingId") String buildingId);

    @GET("presets/districts")
    Observable<List<District>> getDistricts();

    @GET("presets/districts/{districtId}")
    Observable<District> getDistrict(@Path("districtId") String districtId);

    @GET("presets/ships")
    Observable<List<ShipType>> getShips();

    @GET("presets/ships/{shipId}")
    Observable<ShipType> getShip(@Path("shipId") String shipId);

    @GET("presets/traits")
    Observable<List<Trait>> getTraits();

    @GET("presets/traits/{traitId}")
    Observable<Trait> getTrait(@Path("traitId") String traitId);

    @GET("presets/variables/")
    Observable<Map<String, Integer>> getVariables();

    @GET("presets/variables/effects")
    Observable<Map<String, List<String>>> getVariableEffects();

}
