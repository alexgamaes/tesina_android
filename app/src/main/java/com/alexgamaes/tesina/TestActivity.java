package com.alexgamaes.tesina;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
import java.util.List;

public class TestActivity extends AppCompatActivity {
    private final String TAG = "TestActivity";


    protected Database myDb = null;
    protected TextView timeTextView;
    protected TextView rowsTextView;
    protected TextView checksumTextView;

    protected Button simpleReplicationButton;
    protected Button completeReplicationButton;
    protected Button recalculateButton;
    protected Button deleteDatabaseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        CouchbaseLite.init(getApplicationContext());

        timeTextView = findViewById(R.id.textViewTime);
        rowsTextView = findViewById(R.id.textViewRows);
        checksumTextView = findViewById(R.id.textCheksum);

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
    }

    protected void setButtonEnabledStatus(boolean status) {
        recalculateButton.setEnabled(status);
        deleteDatabaseButton.setEnabled(status);
        completeReplicationButton.setEnabled(status);
        simpleReplicationButton.setEnabled(status);
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
                // Create a query to fetch documents of type SDK.
                Query query = QueryBuilder.select(SelectResult.all())
                        .from(DataSource.database(myDb));
                ResultSet result = null;
                try {
                    result = query.execute();

                    List<Result> results = result.allResults();

                    rowsTextView.setText("Number of rows:" + results.size());

                    if (doChecksum) {
                        long n = 0;
                        for (int i = 0; i < results.size(); i++) {
                            n = (n + results.get(i).getDictionary("mydb").getInt("v")) % 1000000007;
                        }
                        checksumTextView.setText("Checksum: " + n);
                    }
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                    return;
                }
            }
        };

        runnable.run();
    }

    protected void SimpleReplication() {
        // Create replicators to push and pull changes to and from the cloud.
        Endpoint targetEndpoint = null;
        try {
            targetEndpoint = new URLEndpoint(new URI("ws://165.22.134.21:4984/tesinadb"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(myDb, targetEndpoint);
        replConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);

        // Add authentication.
        replConfig.setAuthenticator(new BasicAuthenticator("sync_gateway", "tesina"));

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
                    CountAndShowRows(finalized);

                    if(finalized) {
                        setButtonEnabledStatus(true);
                    }
                }
            }

            @Override
            protected void finalize() throws Throwable {
                super.finalize();

                Log.e("ULTIMO", "FINALIZOOOO");
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
}
