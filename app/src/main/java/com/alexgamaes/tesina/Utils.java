package com.alexgamaes.tesina;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;

import java.util.List;

public class Utils {

    public static Long[] CountRows(Database db, boolean doChecksum) {
        // Create a query to fetch documents of type SDK.
        Query query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.database(db));
        ResultSet result = null;

        Long[] ans = new Long[2];
        try {
            result = query.execute();

            List<Result> results = result.allResults();
            ans[0] = (long)results.size();



            if (doChecksum) {
                long n = 0;
                for (int i = 0; i < results.size(); i++) {
                    n = (n + results.get(i).getDictionary("mydb").getInt("v")) % 1000000007;
                }
                ans[1] = n;
            } else {
                ans[1] = -1L;
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }


        return ans;
    }
}
