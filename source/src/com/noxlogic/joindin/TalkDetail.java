package com.noxlogic.joindin;

/*
 * Displays detailed information about a talk (info, comments etc)
 */


import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class TalkDetail extends JIActivity implements OnClickListener {
    private JSONObject talkJSON;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set talk detail layout
        setContentView(R.layout.talkdetails);


        // Get info from the intent scratch board
        try {
            this.talkJSON = new JSONObject(getIntent().getStringExtra("talkJSON"));
        } catch (JSONException e) {
            android.util.Log.e("JoindInApp", "No talk passed to activity", e);
        }

        // Set correct text in layout
        TextView t;
        t = (TextView) this.findViewById(R.id.TalkDetailCaption);
        t.setText(this.talkJSON.optString("talk_title"));
        t = (TextView) this.findViewById(R.id.TalkDetailSpeaker);

        ArrayList<String> speakerNames = new ArrayList<String>();
        try {
            JSONArray speakerEntries = this.talkJSON.getJSONArray("speakers");
            for (int i = 0; i < speakerEntries.length(); i++) {
                speakerNames.add(speakerEntries.getJSONObject(i).getString("speaker_name"));
            }
        } catch (JSONException e) {
            Log.d("JoindInApp", "Couldn't get speaker names");
            e.printStackTrace();
        }
        if (speakerNames.size() == 1) {
            t.setText("Speaker: " + speakerNames.get(0));
        } else if (speakerNames.size() > 1) {
            String allSpeakers = TextUtils.join(", ", speakerNames);
            t.setText("Speakers: " + allSpeakers);
        } else {
            t.setText("");
        }
        t = (TextView) this.findViewById(R.id.TalkDetailDescription);
        String s = this.talkJSON.optString("talk_description");
        // Strip away newlines and additional spaces. Somehow these get added when
        // adding talks. It doesn't really look nice when viewing.
        s = s.replace("\n", "");
        s = s.replace("  ", "");
        t.setText(s);
        Linkify.addLinks(t, Linkify.ALL);

        // Update view X comments button
        Button b = (Button) this.findViewById(R.id.ButtonViewComment);
        int commentCount = this.talkJSON.optInt("comment_count");
        if (commentCount == 1) {
            b.setText(String.format(getString(R.string.generalViewCommentSingular), commentCount));
        } else {
            b.setText(String.format(getString(R.string.generalViewCommentPlural), commentCount));
        }

        // Add handlers to button
        Button button = (Button) findViewById(R.id.ButtonNewComment);
        button.setOnClickListener(this);
        button = (Button) findViewById(R.id.ButtonViewComment);
        button.setOnClickListener(this);
    }


    public void onClick(View v) {
        if (v == findViewById(R.id.ButtonNewComment)) {
            // Goto comment activity and add new comment to this talk
            Intent myIntent = new Intent();
            myIntent.setClass(getApplicationContext(), AddComment.class);

            myIntent.putExtra("commentType", "talk");
            myIntent.putExtra("talkJSON", getIntent().getStringExtra("talkJSON"));
            startActivity(myIntent);
        }
        if (v == findViewById(R.id.ButtonViewComment)) {
            // Goto talk comments activity and display all comments about this talk
            Intent myIntent = new Intent();
            myIntent.setClass(getApplicationContext(), TalkComments.class);

            myIntent.putExtra("talkJSON", getIntent().getStringExtra("talkJSON"));
            startActivity(myIntent);
        }
    }

    ;
}
