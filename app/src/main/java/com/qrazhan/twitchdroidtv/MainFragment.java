package com.qrazhan.twitchdroidtv;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainFragment extends BrowseFragment {
    private static final String TAG = "MainFragment";

    private static final int NUM_ROWS = 2;
    private static final int NUM_COLS = 10;

    private ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private URI mBackgroundURI;
    Stream mStream;
    CardPresenter mCardPresenter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();
    }

    private void loadRows() {
        List<Stream> games = new ArrayList<Stream>();
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mCardPresenter = new CardPresenter();

        final ArrayObjectAdapter gamesRowAdapter = new ArrayObjectAdapter(mCardPresenter);
        final ArrayObjectAdapter featuredRowAdapter = new ArrayObjectAdapter(mCardPresenter);

        Ion.with(getActivity().getApplicationContext())
                .load("https://api.twitch.tv/kraken/games/top")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // do stuff with the result or error
                        if(result != null) {
                            JsonArray top = result.getAsJsonArray("top");
                            for (int i = 0; i < top.size(); i++) {
                                JsonObject game = top.get(i).getAsJsonObject().get("game").getAsJsonObject();
                                final String gameName = game.get("name").getAsString();
                                gamesRowAdapter.add(StreamList.buildStreamInfo("category", gameName, game.get("box").getAsJsonObject().get("template").getAsString(), "studio", "tempurl",
                                        game.get("box").getAsJsonObject().get("large").getAsString(),
                                        game.get("box").getAsJsonObject().get("large").getAsString(), true));
                            }
                        }
                    }
                });

        Ion.with(getActivity().getApplicationContext())
                .load("https://api.twitch.tv/kraken/streams/featured")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // do stuff with the result or error
                        if(result != null) {
                            JsonArray featured = result.getAsJsonArray("featured");
                            for (int i = 0; i < featured.size(); i++) {
                                JsonObject stream = featured.get(i).getAsJsonObject().get("stream").getAsJsonObject();
                                JsonObject channel = stream.get("channel").getAsJsonObject();
                                featuredRowAdapter.add(StreamList.buildStreamInfo("category", channel.get("status").getAsString(), channel.get("name").getAsString(),
                                        channel.get("display_name").getAsString().trim()+" playing "+channel.get("game").getAsString(),
                                        "http://twitch.tv/" + channel.get("name").getAsString() + "/hls",
                                        stream.get("preview").getAsJsonObject().get("large").getAsString(),
                                        stream.get("preview").getAsJsonObject().get("large").getAsString(), false));

                            }
                        }
                    }
                });

        mRowsAdapter.add(new ListRow(new HeaderItem(0, "Featured Streams"), featuredRowAdapter));
        mRowsAdapter.add(new ListRow(new HeaderItem(0, "Top Games"), gamesRowAdapter));

        setAdapter(mRowsAdapter);

    }

    private void prepareBackgroundManager() {

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.backgroundart);
        try {
            mBackgroundURI = new URI("http://ttv-backgroundart.s3.amazonaws.com/404_backgroundart.jpg");
            startBackgroundTimer();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
                                                    // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());
        setOnItemViewClickedListener(getDefaultItemClickedListener());
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), SearchActivity.class));
            }
        });
    }

    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder viewHolder, Object item, RowPresenter.ViewHolder viewHolder1, Row row) {
//                if (item instanceof Stream) {
//                    mBackgroundURI = ((Stream) item).getBackgroundImageURI();
//                    startBackgroundTimer();
//                }
            }
        };
    }

    protected OnItemViewClickedListener getDefaultItemClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object item, RowPresenter.ViewHolder viewHolder1, Row row) {
                if (item instanceof Stream) {
                    Stream stream = (Stream) item;
                    if(!stream.getVertical()) {
                        Log.d(TAG, "Item: " + item.toString());
                        Intent intent = new Intent(getActivity(), PlayerActivity.class);
                        intent.putExtra(getString(R.string.movie), stream);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getActivity(), SearchResultsActivity.class);
                        intent.putExtra("twitch-api-url", "https://api.twitch.tv/kraken/streams?game=" + stream.getTitle().replace(" ", "+"));
                        try {
                            URI image = new URI(stream.getDescription().replace("{width}", "544").replace("{height}", "760"));
                            intent.putExtra("background-url", image);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                        intent.putExtra("activity-title", stream.getTitle());
                        System.out.println(stream.getDescription().replace("{width}", "544").replace("{height}", "760"));
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

    protected void setDefaultBackground(Drawable background) {
        mDefaultBackground = background;
    }

    protected void setDefaultBackground(int resourceId) {
        mDefaultBackground = getResources().getDrawable(resourceId);
    }

    protected void updateBackground(URI uri) {
        Picasso.with(getActivity())
                .load(uri.toString())
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .centerCrop()
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

    protected void updateBackground(Drawable drawable) {
        BackgroundManager.getInstance(getActivity()).setDrawable(drawable);
    }

    protected void clearBackground() {
        BackgroundManager.getInstance(getActivity()).setDrawable(mDefaultBackground);
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), 300);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundURI != null) {
                        updateBackground(mBackgroundURI);
                    }
                }
            });

        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(200, 200));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}
