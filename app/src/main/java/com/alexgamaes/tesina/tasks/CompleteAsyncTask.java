package com.alexgamaes.tesina.tasks;

import android.util.Log;

import com.alexgamaes.tesina.ProgressInterface;
import com.alexgamaes.tesina.Utils;
import com.couchbase.lite.Database;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;


public class CompleteAsyncTask extends ReplicationAsyncTask {
    private final String TAG = "TestActivity";


    public CompleteAsyncTask(Database db, ProgressInterface activity) {
        super(db, activity);

        replicatorType = ReplicatorConfiguration.ReplicatorType.COMPLETE;

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

                    if(change.getStatus().getActivityLevel().toString().equals("CONNECTING")) {
                        Long[] ans = Utils.CountRows(m_db, true);
                        publishProgress(Long.toString(ans[0]), Long.toString(ans[1]), progress);
                    }


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