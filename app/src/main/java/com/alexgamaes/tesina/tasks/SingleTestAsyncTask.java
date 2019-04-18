package com.alexgamaes.tesina.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.alexgamaes.tesina.AutomaticTestInfo;
import com.alexgamaes.tesina.ProgressInterface;
import com.couchbase.lite.Database;
import com.couchbase.lite.ReplicatorConfiguration;

import java.util.concurrent.ExecutionException;

public class SingleTestAsyncTask extends AsyncTask<String, String, Long> {
    private final String TAG = "TestActivity";


    private Database m_db;
    private ProgressInterface m_ui;
    private AutomaticTestInfo testInfo;
    private ReplicatorConfiguration.ReplicatorType replicatorType;

    public SingleTestAsyncTask(Database db, ProgressInterface activity, AutomaticTestInfo testInfo, ReplicatorConfiguration.ReplicatorType replicatorType) {
        super();

        m_db = db;
        m_ui = activity;
        this.testInfo = testInfo;
        this.replicatorType = replicatorType;

    }


    @Override
    protected Long doInBackground(String... strings) {

        // Show log of start replication
        publishProgress(strings[0], strings[1]);

        ReplicationAsyncTask replicationAsyncTask =
                (replicatorType == ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL)?
                        new PullPushAsyncTask(m_db, m_ui) : new CompleteAsyncTask(m_db, m_ui);

        try {
            Long replicationTime = replicationAsyncTask.execute().get();

            return replicationTime;
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return -1L;
    }


    @Override
    protected void onProgressUpdate(String... progress) {
        String info = String.format("(%s:%s) %s", progress[0], progress[1], progress[2]);
        m_ui.addResult(info);

    }
}
