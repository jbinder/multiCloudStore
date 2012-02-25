package at.ac.tuwien.multicloudstore.android.common;

import android.content.Intent;

public class BroadcastService {

    public static final String BROADCAST_ACCOUNT_UPDATE = "at.ac.tuwien.multicloudstore.android.common.ACCOUNT_UPDATE";

    private static final String TAG = BroadcastService.class.getSimpleName();

    public static Intent generateBroadcast(String action) {
        Intent broadcast = new Intent();
        broadcast.setAction(action);
        return broadcast;
    }
}
