package com.example.androidpodcastplayer.ui.activity;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.androidpodcastplayer.R;
import com.example.androidpodcastplayer.common.Constants;
import com.example.androidpodcastplayer.common.Utils;
import com.example.androidpodcastplayer.custom.QuerySuggestionProvider;
import com.example.androidpodcastplayer.model.podcast.Podcast;
import com.example.androidpodcastplayer.model.podcast.Results;
import com.example.androidpodcastplayer.rest.ApiClient;
import com.example.androidpodcastplayer.rest.ApiInterface;
import com.example.androidpodcastplayer.ui.fragment.GenreItemFragment;
import com.example.androidpodcastplayer.ui.fragment.ListItemFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements
        GenreItemFragment.Contract,
        ListItemFragment.Contract {

    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private SearchRecentSuggestions mRecentSuggestions;
    // END
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private CoordinatorLayout mLayout;
    private ProgressBar mProgressBar;
    SearchView.OnQueryTextListener listener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            // re-create ref in case the suggestions has been cleared since startup
            mRecentSuggestions = new SearchRecentSuggestions(
                    MainActivity.this,
                    QuerySuggestionProvider.AUTHORITY,
                    QuerySuggestionProvider.MODE);
            // save queries to suggestions provider
            mRecentSuggestions.saveRecentQuery(query, null);

            // if we're connected, execute the search
            if (Utils.isClientConnected(MainActivity.this)) {
                executeSearchQuery(query);
            } else {
                Utils.showSnackbar(mLayout, getString(R.string.no_network_connection));
            }

            // hide the soft keyboard & close the search view
            Utils.hideKeyboard(MainActivity.this, mSearchView.getWindowToken());
            mSearchItem.collapseActionView();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            // no-op
            return false;
        }
    };
    private TabLayout mTabLayout;
    private int[] mTabIcons = {
            R.drawable.ic_explore,
            R.drawable.ic_subscription,
            R.drawable.ic_playlist
    };
    private String notify="";
    private SharedPreferences sharedPreferences;


    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
    }

    // implementation of interface methods
    @Override
    public void listItemClick(int position) {
        Utils.showSnackbar(mLayout, "Clicked list item " + position);
    }

    @Override
    public void genreItemClick(int genreId, String genreTitle) {
        // launch PodcastActivity which will execute download of podcasts
        // for the relevant genre and display the results
        if (Utils.isClientConnected(this)) {
            // PodcastActivity.launch(this, genreId, genreTitle);
            executeGenreQuery(genreId, genreTitle);
        } else {
            Utils.showSnackbar(mLayout, getString(R.string.no_network_connection));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setY(112f); // center progressbar, move down due to TabLayout
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);

        sharedPreferences = getSharedPreferences("Proadcast", MODE_PRIVATE);
        sharedPreferences.edit().putString("notify", "").apply();



        if (notify.equalsIgnoreCase("")) {
            sendNotification();
        }


        // instantiate the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // instantiate the ViewPager, fragments & icons
        setupViewPager(viewPager);
        mTabLayout.setupWithViewPager(viewPager);
        setupTabIcons();



        // ensures there is a ref to suggestions on startup/device rotation
        mRecentSuggestions = new SearchRecentSuggestions(
                MainActivity.this,
                QuerySuggestionProvider.AUTHORITY,
                QuerySuggestionProvider.MODE
        );

    }

    public void sendNotification() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    JSONObject notifJson = new JSONObject();
                    JSONObject dataJson = new JSONObject();
                    notifJson.put("text", "Hey!");
                    notifJson.put("title", "New Episodes are Added");
                    notifJson.put("priority", "high");
                    dataJson.put("customId", "02");
                    dataJson.put("badge", "1");
                    dataJson.put("alert", "Alert");

                    json.put("notification", notifJson);
                    json.put("data", dataJson);

                    String refreshedToken = FirebaseInstanceId.getInstance().getToken();

//                    json.put("to", sharedPreferences.getString(Constant.ADMIN_TOKEN,""));
                    json.put("to", refreshedToken);

                    RequestBody body = RequestBody.create(JSON, json.toString());
                    Request request = new Request.Builder()
                            .header("Authorization", "key=AAAA-X43dis:APA91bGaUVgZOxF9CuoMpqQc9Rah8dkFePsTWidW2DfBVXefq8edsDAdcE4KIcg-JuP4t3dFBuUi0AdtGp1bmHyL1sGEXFi6N1F6ueAl0lKIAiVTHOSu-PG0HjwnC-9DhGQ-MSMlglxG")
                            .url("https://fcm.googleapis.com/fcm/send")
                            .post(body)
                            .build();
                    okhttp3.Response response = client.newCall(request).execute();
                    String finalResponse = response.body().string();
                    Log.i("hars", finalResponse);

//                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
//                    intent.putExtra("title", "Hey!");
//                    intent.putExtra("message", "New Episodes are Added");
//                    startActivity(intent);

                    sharedPreferences.edit().putString("notify", "yes").apply();
                    sharedPreferences.edit().putString("title", "hey!").apply();
                    sharedPreferences.edit().putString("message", "New Episodes are Added").apply();


                    TriggerNotification();


                } catch (Exception e) {

                    Log.i("hars", e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    private void TriggerNotification() {

        notify = sharedPreferences.getString("notify", "");

        if (notify.equalsIgnoreCase("yes")) {

            String title="", message="";
            title = sharedPreferences.getString("title", "");
            message = sharedPreferences.getString("message", "");

            if (!title.equalsIgnoreCase("")) {
//                Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();

                try {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                    int notificationId = 1;
                    String channelId = "channel-01";
                    String channelName = "Channel Name";
                    int importance = NotificationManager.IMPORTANCE_HIGH;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        NotificationChannel mChannel = new NotificationChannel(
                                channelId, channelName, importance);
                        notificationManager.createNotificationChannel(mChannel);
                    }

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(title)
                            .setContentText(message);

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                    stackBuilder.addNextIntent(intent);
//                    Intent intent1 = new Intent(M.this,NotificationActivity.class);
//                    PendingIntent resultPendingIntent = PendingIntent.getActivity(this,0,intent1,PendingIntent.FLAG_UPDATE_CURRENT);

                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
                    mBuilder.setContentIntent(resultPendingIntent);
                    notificationManager.notify(notificationId, mBuilder.build());

                    sharedPreferences.edit().putString("notify", "yes").apply();
                    sharedPreferences.edit().putString("title", "").apply();
                    sharedPreferences.edit().putString("message", "").apply();

//                    Update Notification Count
//                    setNotificationData(mNotificationsCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Associate searchable config with SearchView widget
        SearchManager search = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        mSearchView.setSearchableInfo(search.getSearchableInfo(getComponentName()));
        mSearchView.setSubmitButtonEnabled(true);
        mSearchView.setQueryRefinementEnabled(true);
        mSearchView.setOnQueryTextListener(listener);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                if (mRecentSuggestions != null) {
                    ClearHistoryDialog dialog = new ClearHistoryDialog();
                    dialog.show(getSupportFragmentManager(), "clear history");
                } else {
                    Utils.showSnackbar(mLayout, "History clear");
                }
                return true;
            case R.id.action_settings:
                Utils.showSnackbar(mLayout, "Clicked on settings");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // search iTunes for podcasts matching the search query
    private void executeSearchQuery(final String query) {
        mProgressBar.setVisibility(View.VISIBLE);
        ApiInterface searchService = ApiClient.getClient().create(ApiInterface.class);
        Call<Results> call = searchService.getGenrePodcasts(
                query, Constants.PODCAST_ID, Constants.REST_LIMIT, Constants.REST_SORT_RECENT
        );
        call.enqueue(new Callback<Results>() {
            @Override
            public void onResponse(Call<Results> call, Response<Results> response) {
                mProgressBar.setVisibility(View.GONE);
                ArrayList<Podcast> results = (ArrayList<Podcast>) response.body().getResults();
                if (results != null && results.size() > 0) {
                    // display results in PodcastActivity/Fragment
                    PodcastActivity.launch(MainActivity.this, results, query, true);
                } else {
                    Utils.showSnackbar(mLayout, "No results returned");
                }
            }

            @Override
            public void onFailure(Call<Results> call, Throwable t) {
                mProgressBar.setVisibility(View.GONE);
                Utils.showSnackbar(mLayout, "Server error, try again");
                Timber.e("%s error executing search query: %s", Constants.LOG_TAG, t.getMessage());
            }
        });
    }

    // return a list of podcasts for the supplied genre
    private void executeGenreQuery(int genreId, final String genreTitle) {
        mProgressBar.setVisibility(View.VISIBLE);
        Timber.i("%s: executing podcast download task", Constants.LOG_TAG);
        ApiInterface restService = ApiClient.getClient().create(ApiInterface.class);
        Call<Results> call = restService.getGenrePodcasts(
                Constants.REST_TERM, genreId, Constants.REST_LIMIT, Constants.REST_SORT_POPULAR
        );
        call.enqueue(new Callback<Results>() {
            @Override
            public void onResponse(Call<Results> call, Response<Results> response) {
                mProgressBar.setVisibility(View.GONE);
                ArrayList<Podcast> list = (ArrayList<Podcast>) response.body().getResults();
                if (list != null && list.size() > 0) {
                    PodcastActivity.launch(MainActivity.this, list, genreTitle, false);
                } else {
                    Utils.showSnackbar(mLayout, "No results found");
                }
            }

            @Override
            public void onFailure(Call<Results> call, Throwable t) {
                mProgressBar.setVisibility(View.GONE);
                Utils.showSnackbar(mLayout, "Server error, try again");
                Timber.e("%s error executing genre query: %s", Constants.LOG_TAG, t.getMessage());
            }
        });
    }

    public void confirmHistoryCleared(boolean historyCleared) {
        if (historyCleared) {
            mRecentSuggestions.clearHistory();
            mRecentSuggestions = null;
            Utils.showSnackbar(mLayout, "History successfully cleared");
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setupTabIcons() {
        mTabLayout.getTabAt(0).setIcon(mTabIcons[0]);
        mTabLayout.getTabAt(1).setIcon(mTabIcons[1]);
        mTabLayout.getTabAt(2).setIcon(mTabIcons[2]);
        //mTabLayout.getTabAt(3).setIcon(mTabIcons[3]);
    }

    private void setupViewPager(ViewPager viewPager) {
        CustomViewPagerAdapter adapter = new CustomViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(GenreItemFragment.newInstance(), "Explore");
        adapter.addFragment(ListItemFragment.newInstance(), "Subscription");
        adapter.addFragment(ListItemFragment.newInstance(), "Playlist");
        // adapter.addFragment(SubscriptionFragment.newInstance(), "Subscription");
        // adapter.addFragment(PlaylistFragment.newInstance(), "Playlist");
        viewPager.setAdapter(adapter);
    }

    public static class ClearHistoryDialog extends DialogFragment implements View.OnClickListener {

        private boolean mHistoryCleared = false;

        public ClearHistoryDialog() {
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_clear_history, container, false);
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            (view.findViewById(R.id.dialog_positive_btn)).setOnClickListener(this);
            (view.findViewById(R.id.dialog_negative_btn)).setOnClickListener(this); // needs to be enabled to be dismissed
            return view;
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.dialog_positive_btn:
                    mHistoryCleared = true;
                    break;
            }
            ((MainActivity) getActivity()).confirmHistoryCleared(mHistoryCleared);
            dismiss();
        }

    }

    private class CustomViewPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> mFragmentList = new ArrayList<>();
        private List<String> mTitleList = new ArrayList<>();

        public CustomViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null; // icon only tab
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mTitleList.add(title);
        }

    }


}
