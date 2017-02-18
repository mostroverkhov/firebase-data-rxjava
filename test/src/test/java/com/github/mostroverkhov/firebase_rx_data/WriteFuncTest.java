package com.github.mostroverkhov.firebase_rx_data;

import com.github.mostroverkhov.firebase_data_rxjava.rx.FirebaseDatabaseManager;
import com.github.mostroverkhov.firebase_data_rxjava.rx.model.WriteResult;
import com.github.mostroverkhov.firebase_rx_data.common.Data;
import com.github.mostroverkhov.firebase_rx_data.common.FrdPathUtil;
import com.github.mostroverkhov.firebase_rx_data.common.Recorder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by Maksym Ostroverkhov on 15.02.2017.
 */

public class WriteFuncTest extends AbstractTest {

    private static final String[] TEST_WRITE_PATH = {"test", "write"};
    private DatabaseReference dbRef;
    private FirebaseDatabaseManager databaseManager;

    @Before
    public void setUp() throws Exception {
        FrdPathUtil pathUtil = new FrdPathUtil(FirebaseDatabase.getInstance());
        dbRef = pathUtil.path(TEST_WRITE_PATH);
    }

    @Test(timeout = 10_000)
    public void writeChild() throws Exception {

        final Data data = new Data(42, String.valueOf(42));
        final DatabaseReference writeRef = dbRef.push();
        final Recorder recorder = new Recorder();
        try {
            databaseManager = new FirebaseDatabaseManager(writeRef);
            databaseManager.data().setValue(data)
                    .observeOn(Schedulers.io())
                    .toBlocking()
                    .subscribe(new WriteSubscriber(recorder, data));
        } finally {
            writeRef.removeValue();
        }
    }

    @Test(timeout = 10_000)
    public void deleteChild() throws Exception {

        final Data data = new Data(42, String.valueOf(42));
        final DatabaseReference writeRef = dbRef.push();
        final Recorder recorder = new Recorder();
        try {
            databaseManager = new FirebaseDatabaseManager(writeRef);
            databaseManager.data().setValue(data)
                    .flatMap(writeResult ->
                            databaseManager.data(__ ->
                                    writeResult.getDatabaseReference()).removeValue())
                    .observeOn(Schedulers.io())
                    .toBlocking()
                    .subscribe(new DeleteSubscriber(writeRef, recorder));

            List<Recorder.Event> events = recorder.getEvents();
            List<Recorder.Event> errors = recorder.getErrors();
            List<Recorder.Event> nexts = recorder.getNexts();
            Assert.assertEquals("window() onCompleted should be consistent",
                    Recorder.Event.Type.COMPLETE, events.get(events.size() - 1).getType());
            Assert.assertEquals("window() onError should be consistent", 0, errors.size());
            Assert.assertEquals("window() onNext should be consistent", 1, nexts.size());
        } finally {
            writeRef.removeValue();
        }
    }

    private static class WriteSubscriber extends Subscriber<WriteResult> {

        private final Recorder recorder;
        private final Data data;
        private final CountDownLatch latch;

        public WriteSubscriber(Recorder recorder, Data data) {
            this.recorder = recorder;
            this.data = data;
            this.latch = new CountDownLatch(1);
        }

        @Override
        public void onCompleted() {
            recorder.recordComplete();
        }

        @Override
        public void onError(Throwable e) {
            recorder.recordError(e);
        }

        @Override
        public void onNext(WriteResult writeResult) {
            recorder.recordNext(writeResult);
            writeResult.getDatabaseReference().addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Data value = dataSnapshot.getValue(Data.class);
                            Assert.assertEquals("Data written should be consistent", data, value);
                            latch.countDown();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            latch.countDown();
                            throw new IllegalStateException("Database error while reading back written data: " + databaseError);
                        }
                    });
            try {
                latch.await();
            } catch (InterruptedException e) {
                //safe to ignore
            }
        }
    }

    private static class DeleteSubscriber extends Subscriber<WriteResult> {
        private final DatabaseReference originalWriteRef;
        private final Recorder recorder;

        public DeleteSubscriber(DatabaseReference originalWriteRef, Recorder recorder) {
            this.originalWriteRef = originalWriteRef;
            this.recorder = recorder;
        }

        @Override
        public void onCompleted() {
            recorder.recordComplete();
        }

        @Override
        public void onError(Throwable e) {
            recorder.recordError(e);
        }

        @Override
        public void onNext(WriteResult writeResult) {
            recorder.recordNext(writeResult);
            final CountDownLatch latch = new CountDownLatch(1);
            originalWriteRef.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Object value = dataSnapshot.getValue();
                    Assert.assertNull(value);
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    latch.countDown();
                    throw new IllegalStateException("Database error while reading deleted data: " + databaseError);
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                //safe to ignore
            }
        }
    }
}
