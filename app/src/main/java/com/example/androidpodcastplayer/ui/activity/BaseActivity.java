package com.example.androidpodcastplayer.ui.activity;

import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import com.example.androidpodcastplayer.R;


public class BaseActivity extends BlankActivity{

    protected void initFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit();
    }

    protected void initToolbar() {
        // instantiate the toolbar with up nav arrow
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_white);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
