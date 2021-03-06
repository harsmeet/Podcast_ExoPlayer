package com.example.androidpodcastplayer.ui.fragment;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.devbrackets.android.exomedia.util.TimeFormatUtil;
import com.devbrackets.android.playlistcore.event.MediaProgress;
import com.devbrackets.android.playlistcore.event.PlaylistItemChange;
import com.devbrackets.android.playlistcore.listener.PlaylistListener;
import com.devbrackets.android.playlistcore.listener.ProgressListener;
import com.devbrackets.android.playlistcore.service.PlaylistServiceCore;
import com.example.androidpodcastplayer.PodcastPlayerApplication;
import com.example.androidpodcastplayer.R;
import com.example.androidpodcastplayer.common.Constants;
import com.example.androidpodcastplayer.common.Utils;
import com.example.androidpodcastplayer.model.episode.EpisodesDataCache;
import com.example.androidpodcastplayer.model.episode.Item;
import com.example.androidpodcastplayer.model.podcast.Podcast;
import com.example.androidpodcastplayer.player.manager.PlaylistManager;
import com.example.androidpodcastplayer.player.model.AudioItem;
import com.squareup.picasso.Picasso;

import java.util.LinkedList;
import java.util.List;

public class EpisodeFragment extends ContractFragment<EpisodeFragment.Contract> implements
        PlaylistListener<AudioItem>, ProgressListener{

    public interface Contract {
        void onNavigationIconBackPressed();
        void downloadEpisode();
        void addEpisodeToPlaylist();
    }

    public static final String SELECTED_INDEX = "selected_index";
    private ProgressBar mProgressBar;
    private TextView mEpisodeTitle;
    private TextView mEpisodeDescription;
    private TextView mEpisodePosition;
    private TextView mEpisodeDuration;
    private ImageView mEpisodeBackground;
    private AppCompatSeekBar mSeekBar;
    private ImageButton mPlayPauseButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private PlaylistManager mPlaylistManager;
    private Picasso mPicasso;
    private boolean mShouldSetDuration;
    private boolean mUserInteracting;
    private int mSelectedIndex = 0;
    private long mPlaylistId = 0L;

    public EpisodeFragment() {}

    public static EpisodeFragment newInstance(int position) {
        EpisodeFragment fragment = new EpisodeFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.EPISODE_SELECTED, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_episode, container, false);
        initView(view);
        retrieveValues();
        if (savedInstanceState != null) {
            mSelectedIndex = savedInstanceState.getInt(SELECTED_INDEX);
        } else {
            mSelectedIndex = getArguments().getInt(Constants.EPISODE_SELECTED);
        }
        boolean generatedPlaylist = setupPlaylistManager();
        startPlayback(generatedPlaylist);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_INDEX, mPlaylistManager.getCurrentPosition());
    }

    @Override
    public void onPause() {
        super.onPause();
        mPlaylistManager.unRegisterPlaylistListener(this);
        mPlaylistManager.unRegisterProgressListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPlaylistManager = PodcastPlayerApplication.getsPlaylistManager();
        mPlaylistManager.registerPlaylistListener(this);
        mPlaylistManager.registerProgressListener(this);
        updateCurrentPlaybackInformation();
    }

    @Override
    public boolean onPlaylistItemChanged(@Nullable AudioItem currentItem, boolean hasNext, boolean hasPrevious) {
        mShouldSetDuration = true;

        // update the view
        mNextButton.setEnabled(hasNext);
        mPrevButton.setEnabled(hasPrevious);
        if (currentItem != null) {
            mPicasso.load(currentItem.getArtworkUrl())
                    .error(R.drawable.no_image_600x600)
                    .into(mEpisodeBackground);
        }
        return true;
    }

    @Override
    public boolean onPlaybackStateChanged(@NonNull PlaylistServiceCore.PlaybackState playbackState) {
        switch (playbackState) {
            case STOPPED:
                getActivity().finish();
                break;

            case RETRIEVING:
            case PREPARING:
            case SEEKING:
                restartLoading();
                break;

            case PLAYING:
                doneLoading(true);
                break;

            case PAUSED:
                doneLoading(false);
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    public boolean onProgressUpdated(@NonNull MediaProgress mediaProgress) {
        if (mShouldSetDuration && mediaProgress.getDuration() > 0) {
            mShouldSetDuration = false;
            setDuration(mediaProgress.getDuration());
        }
        if (!mUserInteracting) {
            mSeekBar.setSecondaryProgress((int) (mediaProgress.getDuration() * mediaProgress.getBufferPercentFloat()));
            mSeekBar.setProgress((int) mediaProgress.getPosition());
            mEpisodePosition.setText(TimeFormatUtil.formatMs(mediaProgress.getPosition()));
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_episode, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download:
                getContract().downloadEpisode();
                return true;
            case R.id.action_playlist:
                getContract().addEpisodeToPlaylist();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // helper methods
    private void initToolbar(View view) {
        // instantiate the toolbar with up nav arrow
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white);
            actionBar.setTitle("");
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getContract().onNavigationIconBackPressed();
                }
            });
        }
    }

    private void setupView(View view) {
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mEpisodeBackground = (ImageView) view.findViewById(R.id.episode_thumbnail);
        mEpisodeTitle = (TextView) view.findViewById(R.id.episode_title);
        mEpisodeDescription = (TextView) view.findViewById(R.id.episode_description);
        mEpisodePosition = (TextView) view.findViewById(R.id.episode_position);
        mEpisodeDuration = (TextView) view.findViewById(R.id.episode_duration);
        mSeekBar = (AppCompatSeekBar) view.findViewById(R.id.episode_progress_bar);
        mPlayPauseButton = (ImageButton) view.findViewById(R.id.action_play_pause);
        mPrevButton = (ImageButton) view.findViewById(R.id.action_prev);
        mNextButton = (ImageButton) view.findViewById(R.id.action_next);
    }

    private void setupListeners() {
        mSeekBar.setOnSeekBarChangeListener(new SeekBarChanged());
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlaylistManager.invokePausePlay();
            }
        });
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlaylistManager.invokePrevious();
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlaylistManager.invokeNext();
            }
        });
    }

    private void initView(View view) {
        initToolbar(view);
        setupView(view);
        setupListeners();
        mPicasso = Picasso.with(getActivity().getApplicationContext());
    }

    private void populateView(Item episode, String imageUrl) {
        if (episode != null) {
            // set episode title
            if (episode.getTitle() != null && !episode.getTitle().isEmpty()) {
                mEpisodeTitle.setText(Utils.htmlToStringParser(episode.getTitle()));
            } else {
                mEpisodeTitle.setText(episode.getAuthor() != null ? Utils.htmlToStringParser(episode.getAuthor()) : "");
            }
            // set episode description
            if (episode.getSubtitle() != null && !episode.getSubtitle().isEmpty()) {
                mEpisodeDescription.setText(Utils.htmlToStringParser(episode.getSubtitle()));
            } else {
                mEpisodeDescription.setText(episode.getDescription() != null ? Utils.htmlToStringParser(episode.getDescription()) : "");
            }

            if (imageUrl != null) { // use the full size image
                Utils.loadPreviewWithGlide(getActivity(), imageUrl, mEpisodeBackground);
            } else {
                if (episode.getImage() != null) {
                    if (episode.getImage().getUrl() != null) {
                        Utils.loadPreviewWithGlide(getActivity(), episode.getImage().getUrl(), mEpisodeBackground);
                    }
                } else {
                    Utils.loadPreviewWithGlide(getActivity(), R.drawable.no_image_600x600, mEpisodeBackground);
                }
            }

            // DEBUG
//            String title = null, image = null, feed = null;
//            if (episode.getTitle() != null) title = episode.getTitle();
//            if (imageUrl != null) image = imageUrl;
//            if (episode.getEnclosure().getUrl() != null) feed = episode.getEnclosure().getUrl();
//            Timber.i("%s: title: %s, feed: %s, image: %s", Constants.LOG_TAG, title, feed, image);

        }
    }

    private boolean setupPlaylistManager() {
        mPlaylistManager = PodcastPlayerApplication.getsPlaylistManager();
        if (mPlaylistManager.getId() == mPlaylistId) {
            return false;
        }

        List<AudioItem> items = new LinkedList<>();
        List<Item> list = EpisodesDataCache.getInstance().getChannel().getItemList();
        Podcast podcast = EpisodesDataCache.getInstance().getPodcast();
        for (Item item : list) {
            items.add(new AudioItem(item, podcast));
        }
        mPlaylistManager.setParameters(items, mSelectedIndex);
        mPlaylistManager.setId(mPlaylistId);
        return true;
    }

    private void startPlayback(boolean start) {
        if (start || mPlaylistManager.getCurrentPosition() != mSelectedIndex) {
            mPlaylistManager.setCurrentPosition(mSelectedIndex);
            mPlaylistManager.play(0, false);
        }
    }

    private void setEpisodeDetail() {
        if (mPlaylistManager.getCurrentItem() != null) {
            mEpisodeTitle.setText(mPlaylistManager.getCurrentItem().getTitle());
            mEpisodeDescription.setText(mPlaylistManager.getCurrentItem().getDescription());
        }
    }

    private void setDuration(long duration) {
        mSeekBar.setMax((int) duration);
        mEpisodeDuration.setText(TimeFormatUtil.formatMs(duration));
    }

    private void restartLoading() {
        mPlayPauseButton.setVisibility(View.INVISIBLE);
        mPrevButton.setVisibility(View.INVISIBLE);
        mNextButton.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    public void loadCompleted() {
        mPlayPauseButton.setVisibility(View.VISIBLE);
        mPrevButton.setVisibility(View.VISIBLE);
        mNextButton.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
        setEpisodeDetail();
    }

    private void updatePlayPauseImage(boolean isPlaying) {
        int resId = isPlaying ? R.drawable.ic_pause_white : R.drawable.ic_play_white;
        mPlayPauseButton.setImageResource(resId);
    }

    private void doneLoading(boolean isPlaying) {
        loadCompleted();
        updatePlayPauseImage(isPlaying);
    }

    private void updateCurrentPlaybackInformation() {
        PlaylistItemChange<AudioItem> playlistItem = mPlaylistManager.getCurrentItemChange();
        if (playlistItem != null) {
            onPlaylistItemChanged(playlistItem.getCurrentItem(), playlistItem.hasNext(), playlistItem.hasPrevious());
        }
        PlaylistServiceCore.PlaybackState currentState = mPlaylistManager.getCurrentPlaybackState();
        if (currentState != PlaylistServiceCore.PlaybackState.STOPPED) {
            onPlaybackStateChanged(currentState);
        }
        MediaProgress currentProgress = mPlaylistManager.getCurrentProgress();
        if (currentProgress != null) {
            onProgressUpdated(currentProgress);
        }
    }

    private void retrieveValues() {

        mPlaylistId = Long.valueOf(EpisodesDataCache.getInstance().getPodcast().getCollectionId());
    }

    private class SeekBarChanged implements SeekBar.OnSeekBarChangeListener {

        private int seekPosition = -1;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) return;
            seekPosition = progress;
            mEpisodePosition.setText(TimeFormatUtil.formatMs(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mUserInteracting = true;
            seekPosition = seekBar.getProgress();
            mPlaylistManager.invokeSeekStarted();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mUserInteracting = false;
            //noinspection Range
            mPlaylistManager.invokeSeekEnded(seekPosition);
            seekPosition = -1;
        }
    }

}
