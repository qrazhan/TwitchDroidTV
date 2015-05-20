package com.qrazhan.twitchdroidtv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

/**
 * Created by prashan on 10/4/14.
 */
public class TwitchSearchFragment extends SearchFragment
        implements SearchFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 300;
    private static final int NUM_GAMES_IN_SEARCH = 3;
    private ArrayObjectAdapter mRowsAdapter;
    private Handler mHandler = new Handler();
    private SearchRunnable mDelayedLoad;
    private SearchCardPresenter mCardPresenter;

    private class SearchRunnable implements Runnable{

        String searchQuery="";

        public void setSearchQuery(String newQuery){
            searchQuery = newQuery;
        }

        @Override
        public void run() {

            final ArrayObjectAdapter gamesRowAdapter = new ArrayObjectAdapter(mCardPresenter);

            Ion.with(getActivity().getApplicationContext())
                    .load("https://api.twitch.tv/kraken/search/games?type=suggest&live=true&q="+searchQuery.replace(" ","+"))
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            // do stuff with the result or error
                            if(result != null) {
                                JsonArray games = result.getAsJsonArray("games");
                                int count = searchQuery.length() < 3 ? 10 : NUM_GAMES_IN_SEARCH;
                                for (int i = 0; i < games.size() && i < count; i++) {
                                    JsonObject game = games.get(i).getAsJsonObject();
                                    final String gameName = game.get("name").getAsString();
                                    gamesRowAdapter.add(i, StreamList.buildStreamInfo("category", gameName, "description", "studio", "tempurl",
                                            game.get("box").getAsJsonObject().get("large").getAsString(),
                                            game.get("box").getAsJsonObject().get("large").getAsString(), true));
                                }
                            }
                        }
                    });

            Ion.with(getActivity().getApplicationContext())
                    .load("https://api.twitch.tv/kraken/search/streams?q="+searchQuery.replace(" ","+"))
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            // do stuff with the result or error
                            Log.w("SEARCH",result+"");
                            if(result != null) {
                                JsonArray featured = result.getAsJsonArray("streams");
                                for (int i = 0; i < featured.size(); i++) {
                                    JsonObject stream = featured.get(i).getAsJsonObject();
                                    JsonObject channel = stream.get("channel").getAsJsonObject();
                                    gamesRowAdapter.add(StreamList.buildStreamInfo("category", channel.get("status").getAsString(), channel.get("name").getAsString(),
                                            channel.get("display_name").getAsString().trim()+" playing "+channel.get("game").getAsString(),
                                            "http://twitch.tv/" + channel.get("name").getAsString() + "/hls",
                                            stream.get("preview").getAsJsonObject().get("large").getAsString(),
                                            stream.get("preview").getAsJsonObject().get("large").getAsString(), false));

                                }
                            }
                        }
                    });


            mRowsAdapter.add(new ListRow(gamesRowAdapter));

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCardPresenter = new SearchCardPresenter();
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);
        setOnItemViewClickedListener(getDefaultItemClickedListener());
        mDelayedLoad = new SearchRunnable();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        mRowsAdapter.clear();
        if (!TextUtils.isEmpty(newQuery)) {
            mDelayedLoad.setSearchQuery(newQuery);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mRowsAdapter.clear();
        if (!TextUtils.isEmpty(query)) {
            mDelayedLoad.setSearchQuery(query);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }
        return true;
    }

    protected OnItemViewClickedListener getDefaultItemClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object item, RowPresenter.ViewHolder viewHolder1, Row row) {
                if (item instanceof Stream) {
                    Stream stream = (Stream) item;
                    if(!stream.getVertical()) {
                        //Log.d(TAG, "Item: " + item.toString());
                        Intent intent = new Intent(getActivity(), PlayerActivity.class);
                        intent.putExtra(getString(R.string.movie), stream);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getActivity(), SearchResultsActivity.class);
                        intent.putExtra("twitch-api-url", "https://api.twitch.tv/kraken/streams?game=" + stream.getTitle().replace(" ", "+"));
                        intent.putExtra("background-url", stream.getBackgroundImageURI());
                        System.out.println("https://api.twitch.tv/kraken/streams?game=" + stream.getTitle().replace(" ", "+"));
                        startActivity(intent);
                    }
                }
                else if (item instanceof String) {
                    Toast.makeText(getActivity(), (String) item, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        };
    }
}
