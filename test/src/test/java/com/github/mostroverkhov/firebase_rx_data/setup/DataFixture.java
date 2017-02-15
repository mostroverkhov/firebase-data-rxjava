package com.github.mostroverkhov.firebase_rx_data.setup;

import com.github.mostroverkhov.firebase_rx_data.common.Data;
import com.github.mostroverkhov.firebase_rx_data.common.FrdPathUtil;
import com.github.mostroverkhov.firebase_rx_data.common.TestAuthenticator;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.internal.NonNull;
import com.google.firebase.tasks.OnCompleteListener;
import com.google.firebase.tasks.Task;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Maksym Ostroverkhov on 15.02.2017.
 */

public class DataFixture {

    public static final int ITEM_COUNT = 10;
    public static final String[] TEST_READ_PATH = {"test", "read"};
    private static final int WRITE_TIMEOUT_SEC = 10;

    private final int itemCount;
    private final DatabaseReference databaseReference;
    private final int timeOutSec;

    public DataFixture(int itemCount, DatabaseReference databaseReference, int timeOutSec) {
        this.itemCount = itemCount;
        this.databaseReference = databaseReference;
        this.timeOutSec = timeOutSec;
    }

    public boolean fillSampleData() {
        final CountDownLatch countDownLatch = new CountDownLatch(itemCount);
        DatabaseReference test = databaseReference;

        for (int i = 0; i < itemCount; i++) {
            DatabaseReference newItem = test.push();
            newItem.setValue(new Data(i, String.valueOf(i)))
                    .addOnCompleteListener(task -> countDownLatch.countDown());
        }
        boolean result = true;
        try {
            countDownLatch.await(timeOutSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            result = false;
        }
        return result;
    }

    public static void main(String... args) {

        TestAuthenticator testAuthenticator = new TestAuthenticator();
        testAuthenticator.authenticate();
        FrdPathUtil pathUtil = new FrdPathUtil(FirebaseDatabase.getInstance());

        DataFixture dataFixture = new DataFixture(
                ITEM_COUNT,
                pathUtil.path(TEST_READ_PATH),
                WRITE_TIMEOUT_SEC);

        dataFixture.fillSampleData();
    }
}
