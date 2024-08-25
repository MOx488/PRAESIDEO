package de.uniks.stp24.rest;

import de.uniks.stp24.dto.CreateJobDto;
import de.uniks.stp24.dto.UpdateJobDto;
import de.uniks.stp24.model.Job;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

import java.util.List;

public interface JobsApiService {
    @GET("games/{gameId}/empires/{empireId}/jobs")
    Observable<List<Job>> getJobs(@Path("gameId") String gameId, @Path("empireId") String empireId);

    @GET("games/{gameId}/empires/{empireId}/jobs")
    Observable<List<Job>> getFilteredJobs(@Path("gameId") String gameId, @Path("empireId") String empireId, @Query("type") String type, @Query("fleet") String fleet, @Query("system") String system);

    @GET("games/{gameId}/empires/{empireId}/jobs/{id}")
    Observable<Job> getJobById(@Path("id") String id);

    @POST("games/{gameId}/empires/{empireId}/jobs")
    Observable<Job> createJob(@Path("gameId") String gameId, @Path("empireId") String empireId, @Body CreateJobDto dto);

    @PATCH("games/{gameId}/empires/{empireId}/jobs/{id}")
    Observable<Job> updateJob(@Path("id") String id, @Body UpdateJobDto dto);

    @DELETE("games/{gameId}/empires/{empireId}/jobs/{id}")
    Observable<Job> deleteJob(@Path("gameId") String gameId, @Path("empireId") String empireId, @Path("id") String id);

}
