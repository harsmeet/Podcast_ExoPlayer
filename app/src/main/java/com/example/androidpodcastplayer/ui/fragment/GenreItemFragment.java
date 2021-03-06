package com.example.androidpodcastplayer.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidpodcastplayer.R;
import com.example.androidpodcastplayer.common.Utils;
import com.example.androidpodcastplayer.custom.AutofitRecyclerView;
import com.example.androidpodcastplayer.custom.ItemSpacerDecoration;
import com.example.androidpodcastplayer.model.genre.ItunesGenre;
import com.example.androidpodcastplayer.model.genre.ItunesGenreDataCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenreItemFragment extends ContractFragment<GenreItemFragment.Contract> {

    public GenreItemFragment() {
    }

    public static GenreItemFragment newInstance() {
        return new GenreItemFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_recycler_autofit, container, false);
        AutofitRecyclerView recyclerView = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.addItemDecoration(new ItemSpacerDecoration(
                getResources().getDimensionPixelOffset(R.dimen.grid_item_margin),
                getResources().getDimensionPixelOffset(R.dimen.grid_item_margin)
        ));
        recyclerView.setHasFixedSize(true);
        List<ItunesGenre> list = new ArrayList<>(Arrays.asList(ItunesGenreDataCache.list));
        GridItemAdapter adapter = new GridItemAdapter(list);
        recyclerView.setAdapter(adapter);

        return view;
    }

    public interface Contract {
        void genreItemClick(int genreId, String title);
    }

    class GridItemAdapter extends RecyclerView.Adapter<GridItemAdapter.ViewHolder> {

        private List<ItunesGenre> mList;
        private Context mContext;

        GridItemAdapter(List<ItunesGenre> list) {
            mList = list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            mContext = parent.getContext();
            View view = LayoutInflater.from(mContext).inflate(R.layout.genre_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bindModelItem(mList.get(position));
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            ImageView mIcon;
            TextView mTitle;
            int mId;
            String mTitleText;

            public ViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                mIcon = (ImageView) itemView.findViewById(R.id.item_icon);
                mTitle = (TextView) itemView.findViewById(R.id.item_title);
            }

            public void bindModelItem(ItunesGenre item) {
                mId = item.getGenreId();
                mTitleText = item.getTitle();
                mTitle.setText(mTitleText);
                Utils.loadPreviewWithGlide(mContext, item.getDrawable(), mIcon);
            }

            @Override
            public void onClick(View view) {
                // forward click event to hosting fragment
                getContract().genreItemClick(mId, mTitleText);
            }
        }
    }
}
