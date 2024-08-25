package de.uniks.stp24.service;

import de.uniks.stp24.dto.UpdateEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Job;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import io.reactivex.rxjava3.core.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.fulib.fx.FulibFxApp.FX_SCHEDULER;

@Singleton
public class JobService {
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public JobsApiService jobsApiService;

    private Game game;
    private Empire empire;

    private boolean isInitialized = false;
    private final ObservableList<Job> jobs = FXCollections.observableArrayList();
    private HashMap<String, Integer> jobIdToStartPeriod = new HashMap<>();

    @Inject
    public JobService() {
    }

    public ObservableList<Job> init(Empire empire, Game game) {
        if (isInitialized) {
            return jobs;
        }

        if (empire._private() != null && empire._private().get("jobIdToStartPeriod") != null) {
            jobIdToStartPeriod = (HashMap<String, Integer>) empire._private().get("jobIdToStartPeriod");
        }

        this.empire = empire;
        this.game = game;
        this.initData();

        this.isInitialized = true;
        return jobs;
    }

    private void initData() {
        subscriber.subscribe(jobsApiService.getJobs(game._id(), empire._id()), jobList -> {
            for (String jobId : jobIdToStartPeriod.keySet()) {
                if (jobList.stream().anyMatch(j -> j._id().equals(jobId))) {
                    continue;
                }

                // Job is not in the list anymore, remove it from our map
                jobIdToStartPeriod.remove(jobId);
                jobs.addAll(jobList);
            }
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".updated", Game.class), event -> this.game = event.data());
    }

    public Observable<Event<Job>> listenForJobEvent(String pattern) {
        return eventListener.listen(pattern, Job.class).observeOn(FX_SCHEDULER).doOnNext(event -> {
            switch (event.suffix()) {
                case "created" -> {
                    final Job job = event.data();
                    if (jobs.stream().anyMatch(j -> j._id().equals(job._id()))) {
                        return;
                    }

                    jobIdToStartPeriod.put(job._id(), this.getJobStartPeriod(game, job));
                    jobs.add(job);
                }
                case "updated" -> jobs.replaceAll(u -> u._id().equals(event.data()._id()) ? event.data() : u);
                case "deleted" -> {
                    jobIdToStartPeriod.remove(event.data()._id());
                    jobs.removeIf(u -> u._id().equals(event.data()._id()));
                }
            }
        });
    }

    public ObservableList<Job> getJobs() {
        return this.jobs;
    }

    public int getJobStartPeriod(Job job) {
        return jobIdToStartPeriod.get(job._id());
    }

    private boolean isNotQueueable(Job jobInQueue) {
        return jobInQueue.type() == null || jobInQueue.type().equals("technology") || jobInQueue.type().equals("upgrade");
    }

    private int getJobStartPeriod(Game game, Job job) {
        // Technology/Upgrade jobs are not queued
        if (isNotQueueable(job)) {
            return game.period();
        }

        // Calculate the start period of the job since there is a queue
        int daysNeededUntilJobStart = 0;
        for (Job jobInQueue : jobs) {
            if (isNotQueueable(jobInQueue)) {
                continue;
            }

            if (jobInQueue._id().equals(job._id())) {
                break;
            }

            daysNeededUntilJobStart += (jobInQueue.total() - jobInQueue.progress());
        }

        return game.period() + daysNeededUntilJobStart;
    }

    public String getJobEndDate(Job job) {
        int startPeriod = this.getJobStartPeriod(job);
        int finishedAtPeriod = startPeriod + job.total();

        Calendar calendar = Calendar.getInstance();
        calendar.set(500, Calendar.JANUARY, 1);
        calendar.add(Calendar.DAY_OF_MONTH, finishedAtPeriod);

        return "-> " + calendar.get(Calendar.DATE) + "." + (calendar.get(Calendar.MONTH) + 1) + "." + calendar.get(Calendar.YEAR);
    }

    public void saveJobStartPeriods(Game game, Empire empire) {
        Map<String, Object> _private = new HashMap<>();
        if (empire._private() != null) {
            _private = empire._private();
        }

        _private.put("jobIdToStartPeriod", jobIdToStartPeriod);

        UpdateEmpireDto updateEmpireDto = new UpdateEmpireDto(null, null, null, _private, null);
        subscriber.subscribe(gameEmpiresApiService.updateEmpire(game._id(), empire._id(), updateEmpireDto)
                .subscribe(res -> this.stopJobService(), err -> this.stopJobService())
        );
    }

    public void stopJobService() {
        subscriber.dispose();
    }
}
