package com.github.mostroverkhov.firebase_rx_data;

import com.github.mostroverkhov.firebase_data_rxjava.FirebaseDatabaseManager;
import com.github.mostroverkhov.firebase_rx_data.common.FrdPathUtil;
import com.github.mostroverkhov.firebase_rx_data.common.Data;
import com.github.mostroverkhov.firebase_rx_data.common.Recorder;
import com.github.mostroverkhov.firebase_data_rxjava.model.DataQuery;
import com.github.mostroverkhov.firebase_data_rxjava.model.Window;
import com.github.mostroverkhov.firebase_rx_data.setup.DataFixture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.schedulers.Schedulers;

public class DataWindowQueryFuncTest extends AbstractTest{

    private static final int SAMPLE_ITEM_COUNT = DataFixture.ITEM_COUNT;
    private static final String[] TEST_READ_PATH = DataFixture.TEST_READ_PATH;
    private static final int WINDOW_SIZE = 2;

    private FirebaseDatabaseManager databaseManager;
    private DatabaseReference dbRef;

    @Before
    public void setUp() throws Exception {
        FrdPathUtil pathUtil = new FrdPathUtil(FirebaseDatabase.getInstance());
        dbRef = pathUtil.path(TEST_READ_PATH);
        databaseManager = new FirebaseDatabaseManager(dbRef);
    }

    @Test(timeout = 10_000)
    public void dataQueryAscTest() throws Exception {

        final DataQuery dataQuery = new DataQuery.Builder(dbRef)
                .asc()
                .windowWithSize(WINDOW_SIZE)
                .build();

        final Recorder recorder = performQuery(dataQuery, Data.class);
        List<Recorder.Event> events = recorder.getEvents();
        List<Recorder.Event> nexts = recorder.getNexts();
        List<Recorder.Event> errors = recorder.getErrors();

        assertEvents(events, nexts, errors);

        List<Data> allData = allNextEvents(nexts);
        for (int i = 0; i < allData.size(); i++) {
            int expectedId = i;
            Data actualData = allData.get(i);
            Assert.assertEquals("Data order should be asc", expectedId, actualData.getId());
        }
    }

    @Test(timeout = 10_000)
    public void dataQueryDescTest() throws Exception {

        final DataQuery dataQuery = new DataQuery.Builder(dbRef)
                .desc()
                .windowWithSize(WINDOW_SIZE)
                .build();

        final Recorder recorder = performQuery(dataQuery, Data.class);
        List<Recorder.Event> events = recorder.getEvents();
        List<Recorder.Event> nexts = recorder.getNexts();
        List<Recorder.Event> errors = recorder.getErrors();
        assertEvents(events, nexts, errors);

        List<Data> allData = allNextEvents(nexts);
        for (int i = 0; i < allData.size(); i++) {
            Data actualData = allData.get(i);
            int expectedId = allData.size() - 1 - i;
            Assert.assertEquals("Data order should be desc", expectedId, actualData.getId());
        }
    }

    private List<Data> allNextEvents(List<Recorder.Event> nexts) {
        List<Data> allData = new ArrayList<>();
        for (Recorder.Event next : nexts) {
            Window<Data> window = next.getData();
            allData.addAll(window.dataWindow());
        }
        return allData;
    }

    private void assertEvents(List<Recorder.Event> events, List<Recorder.Event> nexts, List<Recorder.Event> errors) {
        Assert.assertEquals("window() onCompleted should be consistent",
                Recorder.Event.Type.COMPLETE, events.get(events.size() - 1).getType());
        Assert.assertEquals("window() onError should be consistent",
                0, errors.size());
        Assert.assertEquals("window() onNext should be consistent",
                SAMPLE_ITEM_COUNT / WINDOW_SIZE,
                nexts.size());
        for (Recorder.Event next : nexts) {
            Window<Data> window = next.getData();
            List<Data> data = window.dataWindow();
            Assert.assertEquals("Window Data should respect requested backpressure",
                    WINDOW_SIZE,
                    data.size());
        }
    }

    private <T> Recorder performQuery(DataQuery dataQuery, Class<T> itemType)
            throws InterruptedException {
        final Recorder recorder = new Recorder();

        databaseManager.data().window(dataQuery, itemType)
                .observeOn(Schedulers.io())
                .toBlocking()
                .subscribe(new Subscriber<Window<T>>() {
                    @Override
                    public void onCompleted() {
                        recorder.recordComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        recorder.recordError(e);
                    }

                    @Override
                    public void onNext(Window<T> dataWindow) {
                        recorder.recordNext(dataWindow);
                        request(1);
                    }

                    @Override
                    public void onStart() {
                        request(1);
                    }
                });

        return recorder;
    }
}
