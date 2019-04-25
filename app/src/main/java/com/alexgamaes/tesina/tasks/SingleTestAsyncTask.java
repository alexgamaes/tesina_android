package com.alexgamaes.tesina.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.alexgamaes.tesina.AutomaticTestInfo;
import com.alexgamaes.tesina.ProgressInterface;
import com.alexgamaes.tesina.TestActivity;
import com.alexgamaes.tesina.Utils;
import com.couchbase.lite.Database;
import com.couchbase.lite.ReplicatorConfiguration;

import java.util.concurrent.ExecutionException;

public class SingleTestAsyncTask extends AsyncTask<String, String, Long> {
    private final String TAG = "SingleTestAsyncTask";


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
        Log.e(TAG, "INICIO JEJJE");
        Long replicationTime = -1L;

        try {
            publishProgress(strings[0], strings[1], "Request to modify");
            HttpAsyncTask httpModify = new HttpAsyncTask();
            String URL_MODIFY = TestActivity.URL_NODE_SERVER + "/modify_database/" + testInfo.databaseSize + "/" + testInfo.percentage;
            httpModify.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL_MODIFY).get();

            publishProgress(strings[0], strings[1], "Waiting modification time");

            Thread.sleep(testInfo.modificationDelay * 1000);

            // Show log of start replication
            publishProgress(strings[0], strings[1], "Start replication");

            ReplicationAsyncTask replicationAsyncTask =
                    (replicatorType == ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL)?
                            new PullPushAsyncTask(m_db, m_ui) : new CompleteAsyncTask(m_db, m_ui);


            replicationTime = replicationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();

            publishProgress(strings[0], strings[1], "Finished replication after " + replicationTime + "ms");


            HttpAsyncTask httpView = new HttpAsyncTask();
            String URL_VIEW = TestActivity.URL_NODE_SERVER + "/view_database";
            String serverChecksum = httpView.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL_VIEW).get();

            Long[] mobileChecksum = Utils.CountRows(m_db, true);
            String checksumsInfo = String.format("Checksum(%s): server-%s | mobile-%s", Long.toString(mobileChecksum[0]), serverChecksum, Long.toString(mobileChecksum[1]));

            publishProgress(strings[0], strings[1], checksumsInfo);

            if(!serverChecksum.equals(Long.toString(mobileChecksum[1]))) {
                publishProgress(strings[0], strings[1], "ERROR: CHECKSUMS DON'T MATCH");

                return -1L;
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return replicationTime;
    }


    @Override
    protected void onProgressUpdate(String... progress) {
        String info = String.format("(%s:%s) %s", progress[0], progress[1], progress[2]);
        m_ui.addResult(info);

    }
}
