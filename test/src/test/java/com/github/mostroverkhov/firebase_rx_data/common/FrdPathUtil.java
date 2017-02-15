package com.github.mostroverkhov.firebase_rx_data.common;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by Maksym Ostroverkhov on 15.02.2017.
 */

public class FrdPathUtil {

    private final FirebaseDatabase db;

    public FrdPathUtil(FirebaseDatabase db) {
        this.db = db;
    }

    public DatabaseReference path(String[] paths) {
        if (paths == null) {
            throw new IllegalArgumentException("paths should not be null");
        }
        DatabaseReference ref = db.getReference();
        for (String path : paths) {
            ref = ref.child(path);
        }
        return ref;
    }
}
