package com.alexgamaes.tesina;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alexgamaes.tesina.tasks.SingleTestAsyncTask;
import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.URLEndpoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TestActivity extends AppCompatActivity implements ProgressInterface {
    private final String TAG = "TestActivity";

    public final static String IP_SERVER = "142.93.110.254";
    public final static String URL_DB_SERVER = "ws://" + IP_SERVER + ":4984/tesinadb";
    public final static String URL_NODE_SERVER = "http://" + IP_SERVER + ":3000";

    public final static String USERNAME = "sync_gateway";
    public final static String PASSWORD = "tesina";

    protected Database myDb = null;
    protected TextView timeTextView;
    protected TextView rowsTextView;
    protected TextView checksumTextView;
    protected TextView resultLogsTextview;

    protected Button simpleReplicationButton;
    protected Button completeReplicationButton;
    protected Button recalculateButton;
    protected Button deleteDatabaseButton;
    protected Button automaticTestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        CouchbaseLite.init(getApplicationContext());

        timeTextView = findViewById(R.id.textViewTime);
        rowsTextView = findViewById(R.id.textViewRows);
        checksumTextView = findViewById(R.id.textCheksum);

        resultLogsTextview = findViewById(R.id.editTextLogs);

        InitDatabase();
        CountAndShowRows(true);

        simpleReplicationButton = findViewById(R.id.buttonSimpleReplication);
        simpleReplicationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonEnabledStatus(false);
                SimpleReplication();
            }
        });

        completeReplicationButton = findViewById(R.id.buttonCompleteReplication);
        completeReplicationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonEnabledStatus(false);
                CompleteReplication();
            }
        });

        recalculateButton = findViewById(R.id.buttonRecalculate);
        recalculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CountAndShowRows(true);
            }
        });


        deleteDatabaseButton = findViewById(R.id.buttonDeleteDatabase);
        deleteDatabaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonEnabledStatus(false);
                DeleteDatabase();
                InitDatabase();
                CountAndShowRows(true);
                setButtonEnabledStatus(true);
            }
        });

        automaticTestButton = findViewById(R.id.buttonAutomaticTest);
        automaticTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AutomaticTestInfo automaticTestInfo
                        = new AutomaticTestInfo(30, 1, 10);

                setButtonEnabledStatus(false);

                AutomaticTest automaticTest = new AutomaticTest(automaticTestInfo);
                automaticTest.execute();


            }
        });
    }

    protected void setButtonEnabledStatus(boolean status) {
        recalculateButton.setEnabled(status);
        deleteDatabaseButton.setEnabled(status);
        completeReplicationButton.setEnabled(status);
        simpleReplicationButton.setEnabled(status);
        automaticTestButton.setEnabled(status);
    }

    protected void InitDatabase() {
        DatabaseConfiguration config = new DatabaseConfiguration();
        myDb = null;
        try {
            myDb = new Database("mydb", config);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            return;
        }

    }

    protected void CountAndShowRows(final boolean doChecksum) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Long[] ans = Utils.CountRows(myDb, doChecksum);
                rowsTextView.setText("Number of rows:" + ans[0]);
                if (ans[1] != -1) {
                    checksumTextView.setText("Checksum: " + ans[1]);
                }

            }
        };

        runnable.run();
    }

    protected void SimpleReplication() {
        // Create replicators to push and pull changes to and from the cloud.
        Endpoint targetEndpoint = null;
        try {
            targetEndpoint = new URLEndpoint(new URI(URL_DB_SERVER));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(myDb, targetEndpoint);
        replConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);

        // Add authentication.
        replConfig.setAuthenticator(new BasicAuthenticator(USERNAME, PASSWORD));

        // Create replicator.
        final Replicator replicator = new Replicator(replConfig);

        // Listen to replicator change events.
        final Database finalDatabase = myDb;
        final long startTime = System.currentTimeMillis();

        replicator.addChangeListener(new ReplicatorChangeListener() {
            @Override
            public void changed(ReplicatorChange change) {
                if (change.getStatus().getError() != null) {
                    Log.i(TAG, "Error code ::  " + change.getStatus().getError().getCode());
                } else {
                    boolean finalized = change.getStatus().getActivityLevel().toString().equals("STOPPED");

                    Log.e(TAG, change.getStatus().getActivityLevel().toString());

                    long diff = System.currentTimeMillis() - startTime;
                    Log.e(TAG, "Time: " + System.currentTimeMillis());

                    timeTextView.setText("Time last operation: " + diff + "ms " + change.getStatus().getProgress().toString());

                    if (finalized) {
                        CountAndShowRows(true);
                        setButtonEnabledStatus(true);
                    }
                }
            }
        });

        Log.d(TAG, "InitDatabase finished ");


        // Start replication.
        replicator.start();
    }

    protected void CompleteReplication() {
        // Create replicators to push and pull changes to and from the cloud.
        Endpoint targetEndpoint = null;
        try {
            targetEndpoint = new URLEndpoint(new URI(URL_DB_SERVER));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(myDb, targetEndpoint);
        replConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.COMPLETE);

        // Add authentication.
        replConfig.setAuthenticator(new BasicAuthenticator(USERNAME, PASSWORD));

        // Create replicator.
        final Replicator replicator = new Replicator(replConfig);

        // Listen to replicator change events.
        final Database finalDatabase = myDb;
        final long startTime = System.currentTimeMillis();
        boolean showFirst = true;

        replicator.addChangeListener(new ReplicatorChangeListener() {
            @Override
            public void changed(ReplicatorChange change) {
                if (change.getStatus().getError() != null) {
                    Log.i(TAG, "Error code ::  " + change.getStatus().getError().getCode());
                } else {
                    if (change.getStatus().getActivityLevel().toString().equals("CONNECTING")) {
                        CountAndShowRows(true);
                    }

                    boolean finalized = change.getStatus().getActivityLevel().toString().equals("STOPPED");

                    Log.e(TAG, change.getStatus().getActivityLevel().toString());

                    long diff = System.currentTimeMillis() - startTime;
                    Log.e(TAG, "Time: " + System.currentTimeMillis());

                    timeTextView.setText("Time last operation: " + diff + "ms " + change.getStatus().getProgress().toString());

                    if (finalized) {
                        CountAndShowRows(true);
                        setButtonEnabledStatus(true);
                    }
                }
            }
        });

        Log.d(TAG, "InitDatabase finished ");


        // Start replication.
        replicator.start();
    }

    protected void DeleteDatabase() {
        try {
            myDb.delete();
            myDb.close();
            myDb = null;
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showProgress(String rowNumber, String checksum, String progress) {
        if (rowNumber != null) {
            rowsTextView.setText("Number of rows:" + rowNumber);
        }

        if (checksum != null) {
            checksumTextView.setText("Checksum: " + checksum);
        }

        if (progress != null) {
            timeTextView.setText("Time last operation: " + progress);

        }
    }

    @Override
    public void addResult(String result) {
        Date dt = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(dt);

        String info = String.format("\n[%s] %s", currentTime, result);
        resultLogsTextview.setText(resultLogsTextview.getText() + info);

    }


    ////
    protected class AutomaticTest extends AsyncTask<Void, String, Long> {

        AutomaticTestInfo info;

        public AutomaticTest(AutomaticTestInfo automaticTestInfo) {
            info = automaticTestInfo;
        }


        @Override
        protected Long doInBackground(Void... voids) {

            for (int i = 0; i < info.numberOfTests; i++) {
                publishProgress("Init", Integer.toString(i), "PUSH_AND_PULL");

                SingleTestAsyncTask singleTestAsyncTask =
                        new SingleTestAsyncTask(
                                myDb,
                                TestActivity.this,
                                info,
                                ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);

                try {
                    singleTestAsyncTask.execute(Integer.toString(i), "PUSH_AND_PULL").get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    return -1L;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return -2L;
                }

                publishProgress("Finish", Integer.toString(i), "PUSH_AND_PULL");

            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            String info = String.format("%s next test (%s:%s)", progress[0], progress[1], progress[2]);

            TestActivity.this.addResult(info);
        }

        @Override
        protected void onPostExecute(Long result) {
            setButtonEnabledStatus(true);
        }

    }
}
