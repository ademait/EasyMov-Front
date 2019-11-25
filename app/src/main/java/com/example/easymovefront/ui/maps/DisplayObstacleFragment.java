package com.example.easymovefront.ui.maps;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.easymovefront.R;
import com.example.easymovefront.data.model.LoggedUser;
import com.example.easymovefront.data.model.ObstacleMap;
import com.example.easymovefront.network.CreateMarkerTask;
import com.example.easymovefront.network.GetMarkerTask;
import com.example.easymovefront.network.GetSingleMarkerTask;
import com.example.easymovefront.network.LikeObstacleTask;
import com.google.android.gms.maps.model.Marker;

import com.like.LikeButton;
import com.like.OnLikeListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class DisplayObstacleFragment extends DialogFragment {

    private JSONObject json;
    private String mId;
    private String mIdCreador;
    
    private Context mContext;
    private Marker mMarker;
    private LikeButton mLike;
    private LikeButton mDislike;
    private LikeButton mResolved;

    private TextView mLikenumber;
    private TextView mDislikenumber;

    private ProgressBar mapsLoading;

    public DisplayObstacleFragment(Context context, Marker marker, ProgressBar loading) {
        mapsLoading = loading;
        mContext = context;
        mMarker = marker;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        obtainMarkerID();
        getUpdatedMarker();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        final View editTextView = inflater.inflate(R.layout.fragment_display_obstacle, null);
        ImageView pic = editTextView.findViewById(R.id.obstacleView);
        TextView title = editTextView.findViewById(R.id.titleObstacle);
        TextView desc = editTextView.findViewById(R.id.descriptionObstacle);
        mLikenumber = editTextView.findViewById(R.id.likenumber);
        mDislikenumber = editTextView.findViewById(R.id.dislikenumber);
        mLike = editTextView.findViewById(R.id.likeButton);
        mDislike = editTextView.findViewById(R.id.dislikeButton);
        updateLikeStatus();
        mLike.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                if (mDislike.isLiked()) {
                    mDislike.setLiked(false);
                    updateLikeNumber("treuredislike");
                }
                updateLikeNumber("like");
                LikeObstacleTask myTask = new LikeObstacleTask(mContext);
                myTask.execute(mId, "like", LoggedUser.getInstance().getId());

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                updateLikeNumber("treurelike");
                LikeObstacleTask myTask = new LikeObstacleTask(mContext);
                myTask.execute(mId, "treurelike", LoggedUser.getInstance().getId());
            }
        });
        mDislike.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                if (mLike.isLiked()) {
                    mLike.setLiked(false);
                    updateLikeNumber("treurelike");
                }
                updateLikeNumber("dislike");
                LikeObstacleTask myTask = new LikeObstacleTask(mContext);
                myTask.execute(mId, "dislike", LoggedUser.getInstance().getId());
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                updateLikeNumber("treuredislike");
                LikeObstacleTask myTask = new LikeObstacleTask(mContext);
                myTask.execute(mId, "treuredislike", LoggedUser.getInstance().getId());
            }
        });
        String picString = "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";
        pic.setImageBitmap(MapsActivity.StringToBitMap(picString));
        title.setText(mMarker.getTitle());
        desc.setText(mMarker.getSnippet());
        builder.setView(editTextView);

        mapsLoading.setVisibility(View.GONE);

        return builder.create();
    }

    private void updateLikeNumber(String type) {
        Integer number;
        if (type == "like") {
            number = Integer.parseInt(mLikenumber.getText().toString());
            number++;
            mLikenumber.setText(String.valueOf(number));
        }
        else if (type == "treurelike") {
            number = Integer.parseInt(mLikenumber.getText().toString());
            number--;
            mLikenumber.setText(String.valueOf(number));
        }
        else if (type == "dislike") {
            number = Integer.parseInt(mDislikenumber.getText().toString());
            number++;
            mDislikenumber.setText(String.valueOf(number));
        }
        else if (type == "treuredislike") {
            number = Integer.parseInt(mDislikenumber.getText().toString());
            number--;
            mDislikenumber.setText(String.valueOf(number));
        }
    }

    private void getUpdatedMarker() {
        GetSingleMarkerTask myTask = new GetSingleMarkerTask(mContext);
        myTask.execute(mId);
        try {
            String result = myTask.get();
            json = new JSONObject(result);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateLikeStatus() {
        try {
            JSONArray jarray = json.getJSONArray("usuarisLike");
            mLikenumber.setText(String.valueOf(jarray.length()));
            for(int i=0; i<jarray.length(); i++) {
                if (jarray.getInt(i) == Integer.parseInt(LoggedUser.getInstance().getId())) {
                    mLike.setLiked(true);
                    break;
                }
            }
            jarray = json.getJSONArray("usuarisDisLike");
            mDislikenumber.setText(String.valueOf(jarray.length()));
            if (!mLike.isLiked()) {
                for (int i = 0; i < jarray.length(); i++) {
                    if (jarray.getInt(i) == Integer.parseInt(LoggedUser.getInstance().getId())) {
                        mDislike.setLiked(true);
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void obtainMarkerID() {
        json = ObstacleMap.getInstance().getMap().get(mMarker);
        try {
            mId = json.getString("id");
            mIdCreador = json.getString("idUsuariCreador");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_obstacle, container);
        return view;
    }
}