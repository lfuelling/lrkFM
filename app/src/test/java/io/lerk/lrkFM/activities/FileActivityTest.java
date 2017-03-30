package io.lerk.lrkFM.activities;

import android.content.Intent;
import android.os.Bundle;
import android.test.mock.MockContext;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.lerk.lrkFM.R;

import static org.junit.Assert.*;

/**
 * Created by lfuelling on 31.03.17.
 */
public class FileActivityTest {

    private static final String PATH_SE0 = "/storage/emulated/0";
    MockContext context;
    FileActivity activity;

    @Test
    public void getFileListView() throws Exception {
        Assert.assertTrue(activity.getFileListView() != null);
    }

    @Test
    public void onPostResume() throws Exception {
        activity.onPostResume();
        Assert.assertFalse(((TextView) activity.getNavDrawer().findViewById(R.id.diskUsage)).getText().toString().isEmpty());
    }

    @Test
    public void onPostCreate() throws Exception {
        activity.onPostCreate(Mockito.mock(Bundle.class));
    }

    @Before
    @Test
    public void onCreate() throws Exception {
        context = new MockContext();
        activity = Mockito.mock(FileActivity.class);
        activity.onCreate(Mockito.mock(Bundle.class));
        //TODO: implement more tests
    }

    @Test
    public void getTitleFromPath() throws Exception {
        Assert.assertTrue(activity.getTitleFromPath(PATH_SE0).equals("0"));
    }

    @Test
    public void reloadCurrentDirectory() throws Exception {
    }

    @Test
    public void loadDirectory() throws Exception {
    }

    @Test
    public void onConfigurationChanged() throws Exception {
    }

    @Test
    public void onBackPressed() throws Exception {
    }

    @Test
    public void onPrepareOptionsMenu() throws Exception {
    }

    @Test
    public void onCreateOptionsMenu() throws Exception {
    }

    @Test
    public void onOptionsItemSelected() throws Exception {
    }

    @Test
    public void getCurrentDirectory() throws Exception {
    }

    @Test
    public void onNavigationItemSelected() throws Exception {
    }

    @Test
    public void onStop() throws Exception {
    }

    @Test
    public void onDestroy() throws Exception {
    }

    @Test
    public void onResume() throws Exception {
    }

    @Test
    public void launchSettings() throws Exception {
    }

    @Test
    public void verifyStoragePermissions() throws Exception {
    }

    @Test
    public void getDefaultPreferences() throws Exception {
    }

    @Test
    public void addFileToOpContext() throws Exception {
    }

    @Test
    public void getFileOpContext() throws Exception {
    }

}