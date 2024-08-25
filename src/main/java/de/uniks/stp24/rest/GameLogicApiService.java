package de.uniks.stp24.rest;

import de.uniks.stp24.model.AggregateResult;
import de.uniks.stp24.model.AggregateResultCompare;
import de.uniks.stp24.model.ExplainedVariable;
import de.uniks.stp24.model.ExplainedVariableWithMapValues;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

import java.util.List;
import java.util.Map;

public interface GameLogicApiService {
    @GET("games/{gameId}/empires/{empireId}/variables")
    Observable<List<ExplainedVariable>> getExplainedVariables(@Path("gameId") String gameId, @Path("empireId") String empireId, @Query("variables") List<String> variables);

    @GET("games/{gameId}/empires/{empireId}/variables/{variableId}")
    Observable<ExplainedVariable> getExplainedVariable(@Path("gameId") String gameId, @Path("empireId") String empireId, @Path("variableId") String variableId);

    // Use this when asking for sth like cost or upkeep (e.g. "ships.explorer.cost", this will return a map instead of a double in the "final" field)
    @GET("games/{gameId}/empires/{empireId}/variables/{variableId}")
    Observable<ExplainedVariableWithMapValues> getExplainedVariableWithMapValues(@Path("gameId") String gameId, @Path("empireId") String empireId, @Path("variableId") String variableId);

    @GET("games/{gameId}/empires/{empireId}/aggregates/{aggregate}")
    Observable<AggregateResult> getAggregate(@Path("gameId") String gameId, @Path("empireId") String empireId, @Path("aggregate") String aggregate, @QueryMap Map<String, String> aggregates);

    @GET("games/{gameId}/empires/{empireId}/aggregates/{aggregate}")
    Observable<AggregateResult> getAggregateSystem(@Path("gameId") String gameId, @Path("empireId") String empireId, @Path("aggregate") String aggregate, @Query("system") String system);

    @GET("games/{gameId}/empires/{empireId}/aggregates/{aggregate}")
    Observable<AggregateResult> getAggregateTech(@Path("empireId") String empireId, @Path("aggregate") String aggregate, @Query("technology") String technology);

    @GET("games/{gameId}/empires/{empireId}/aggregates/{aggregate}")
    Observable<AggregateResultCompare> getAggregateCompare(@Path("gameId") String gameId, @Path("empireId") String empireId, @Path("aggregate") String aggregate, @Query("compare") String compare);
}