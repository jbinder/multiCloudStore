package at.ac.tuwien.multicloudstore.android.test;

import android.test.ActivityInstrumentationTestCase2;
import at.ac.tuwien.multicloudstore.android.activities.FileBrowserActivity;

public class FileBrowserActivityTest extends ActivityInstrumentationTestCase2<FileBrowserActivity> {

    public FileBrowserActivityTest() {
        super("activities", FileBrowserActivity.class);
    }

    public void testActivity() {
        FileBrowserActivity activity = getActivity();
        assertNotNull(activity);
    }
}

