package de.uniks.stp24.service;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;

@SuppressWarnings("BusyWait")
@Singleton
public class DiscordActivityService {

    private boolean running = true;
    private Core core;
    private Activity activity;

    @Inject
    public DiscordActivityService() {
        this.createActivity("  ", "  ");
    }

    // Set parameters for the Core
    public void createActivity(String details, String state) {
        // Set parameters for the Core
        try (CreateParams params = new CreateParams()) {
            params.setClientID(1242100932372860998L);
            params.setFlags(CreateParams.getDefaultFlags());
            // Create the Core
            this.core = new Core(params);
            // Create the Activity
            this.activity = new Activity();

            if (details != null && !details.isEmpty()) {
                activity.setDetails(details);
            }
            if (state != null && !state.isEmpty()) {
                activity.setState(state);
            }

            // Setting a start time causes an "elapsed" field to appear
            activity.timestamps().setStart(Instant.now());

            activity.assets().setLargeImage("logo");
            activity.assets().setLargeText("PRAESIDEO");

            // Finally, update the current activity to our activity
            core.activityManager().updateActivity(activity);

            // Run the Core
            this.runCallbacks();
        } catch (Exception e) {
            System.out.println("Couldn't create Discord Activity");

            if (core != null) {
                core.close();
            }
        }
    }

    public void runCallbacks() {
        // Run callbacks forever
        Thread thread = new Thread(() -> {
            do {
                if (core == null || !core.isOpen()) {
                    running = false;
                    return;
                }

                try {
                    core.runCallbacks();
                    // Sleep a bit to save CPU
                    Thread.sleep(16);
                } catch (Exception e) {
                    System.err.println("Discord Error: " + e.getMessage());
                }
            } while (running);
        });

        thread.setDaemon(true);
        thread.start();
    }

    public void setActivity(String details) {
        this.setActivity(details, "");
    }

    public void setActivity(String details, String state) {
        if (activity == null) {
            return;
        }

        final String blank = " ";
        final int deltaStateLength = state.length() - 2;
        if (deltaStateLength < 0) {
            state += blank.repeat(Math.abs(deltaStateLength));
        }

        if (!details.isEmpty()) {
            activity.setDetails(details);
        }
        if (!state.isEmpty()) {
            activity.setState(state);
        }

        core.activityManager().updateActivity(activity);
    }

    public void stopActivity() {
        running = false;

        if (core != null) {
            core.activityManager().clearActivity();
            core.close();
            core = null;
        }

        activity = null;
    }
}
