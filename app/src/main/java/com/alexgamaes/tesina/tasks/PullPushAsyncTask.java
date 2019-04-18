package com.alexgamaes.tesina.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.alexgamaes.tesina.AutomaticTestInfo;
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

public class PullPushAsyncTask extends ReplicationAsyncTask {
    private final String TAG = "PullPush";


    public PullPushAsyncTask(Database db, ProgressInterface activity) {
        super(db, activity);

        replicatorType = ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL;

    }

    protected ReplicatorChangeListener InitReplicatorChange() {
        return new ReplicatorChangeListener() {
            @Override
            public void changed(ReplicatorChange change) {
                if (change.getStatus().getError() != null) {
                    Log.i(TAG, "Error code ::  " + change.getStatus().getError().getCode());
                } else {
                    boolean finalized = change.getStatus().getActivityLevel().toString().equals("STOPPED");

                    Log.e(TAG, change.getStatus().getActivityLevel().toString());

                    long diff = System.currentTimeMillis() - startTime;
                    Log.e(TAG, "Time: " + System.currentTimeMillis());

                    String progress = diff + "ms " + change.getStatus().getProgress().toString();

                    if(finalized) {
                        Long[] ans = Utils.CountRows(m_db, true);
                        publishProgress(Long.toString(ans[0]), Long.toString(ans[1]), progress);

                        finalTime = diff;
                    } else {
                        publishProgress(null, null, progress);
                    }
                }
            }
        };
    }
}