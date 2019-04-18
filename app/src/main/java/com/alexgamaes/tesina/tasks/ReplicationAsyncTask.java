package com.alexgamaes.tesina.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.alexgamaes.tesina.ProgressInterface;
import com.alexgamaes.tesina.TestActivity;
import com.alexgamaes.tesina.Utils;
import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.Database;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.URLEndpoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

public abstract class ReplicationAsyncTask extends AsyncTask<Void, String, Long> {
    protected final String TAG = "ReplicationAsyncTask";


    protected Database m_db;
    protected ProgressInterface m_ui;
    protected ReplicatorConfiguration.ReplicatorType replicatorType;

    public ReplicationAsyncTask(Database db, ProgressInterface activity) {
        super();

        m_db = db;
        m_ui = activity;
    }

    protected long finalTime;
    protected long startTime;

    @Override
    protected Long doInBackground(Void... voids) {
        // Create replicators to push and pull changes to and from the cloud.
        Endpoint targetEndpoint = null;
        try {
            targetEndpoint = new URLEndpoint(new URI(TestActivity.URL_DB_SERVER));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return -1L;
        }
        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(m_db, targetEndpoint);
        replConfig.setReplicatorType(replicatorType);

        // Add authentication.
        replConfig.setAuthenticator(new BasicAuthenticator(TestActivity.USERNAME, TestActivity.PASSWORD));

        // Create replicator.
        final Replicator replicator = new Replicator(replConfig);

        // Listen to replicator change events.
        startTime = System.currentTimeMillis();
        finalTime = -1;

        replicator.addChangeListener(InitReplicatorChange());

        Log.d(TAG, "InitDatabase finished ");


        // Start replication.
        replicator.start();

        while (finalTime == -1) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return -2L;
            }
        }

        return finalTime;
    }

    protected abstract ReplicatorChangeListener InitReplicatorChange();


    @Override
    protected void onProgressUpdate(String... progress) {
        m_ui.showProgress(progress[0], progress[1], progress[2]);
    }
}