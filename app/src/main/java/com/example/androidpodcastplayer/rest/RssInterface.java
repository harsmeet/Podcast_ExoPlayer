package com.example.androidpodcastplayer.rest;


import com.example.androidpodcastplayer.model.episode.Feed;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface RssInterface {

//    @GET("{path}")
//    Call<Feed> getItems(@Path("path") String feed);

//    https://www.awcull.com/2018/12/17/itunes-podcast-rss-feed.html
//    http://rss.itunes.apple.com/en-us


    @GET
    Call<Feed> getItems(@Url String url);


}
