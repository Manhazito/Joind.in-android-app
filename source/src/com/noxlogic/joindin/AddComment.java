package com.noxlogic.joindin;

/*
 * Comment activity. This activity will handle both Events comments and talk comments.
 */

import android.app.Activity;
import android.content.Intent;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

public class AddComment extends JIActivity implements OnClickListener {
    ProgressDialog saveDialog;
    private String commentType;
    private JSONObject talkJSON;
    private JSONObject eventJSON;
    private String lastError = "";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.addcomment);

        // Get comment type from the intent scratch board
        this.commentType = getIntent().getStringExtra("commentType");
        setTitle(String.format(getString(R.string.activityAddCommentTitle), this.commentType));

        EditText te = (EditText) findViewById(R.id.CommentText);
        te.setText("");
        te.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                JIActivity.setCommentHistory(s.toString());
            }

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });


        // Are we commenting on an event?
        if (this.commentType.compareTo("event") == 0) {
            // Get event information
            try {
                this.eventJSON = new JSONObject(getIntent().getStringExtra("eventJSON"));
            } catch (JSONException e) {
                android.util.Log.e("JoindInApp", "No event passed to activity", e);
            }

            // Set rating bar and checkbox to invisible since they are not used while
            // commenting on events
            View v;
            v = (View) findViewById(R.id.CommentRatingBar);
            v.setVisibility(View.GONE);
            v = (View) findViewById(R.id.TextViewRating);
            v.setVisibility(View.GONE);
            v = (View) findViewById(R.id.CommentPrivate);
            v.setVisibility(View.GONE);
        }

        // Add handler to the buttons
        Button button;
        button = (Button) findViewById(R.id.ButtonAddCommentCancel);
        button.setOnClickListener(this);
        button = (Button) findViewById(R.id.ButtonAddCommentSend);
        button.setOnClickListener(this);
    }


    // This must be done in onStart instead of onCreate. For instance, we could have started writing a comment,
    // the user sees he's sending anonymous comments, starts the preferences activity from the menu and returns
    // back to this activity. In that case, the onCreate is not called, but onStart is..
    public void onStart() {
        super.onStart();

        // Check if we have entered correct credentials in the preferences. If not, we display the
        // text that the user is commenting anonymously..

        View v = (View) findViewById(R.id.AnonymousPost);
        if (isAuthenticated()) {
            v.setVisibility(View.GONE);
        } else {
            v.setVisibility(View.VISIBLE);
        }
    }

    // Called when button is clicked
    public void onClick(View v) {
        // Did we click cancel button?
        if (v == findViewById(R.id.ButtonAddCommentCancel)) {
            // Cancel, do nothing. No comment will be posted
            this.finish();
        }

        // Did we click send button?
        if (v == findViewById(R.id.ButtonAddCommentSend)) {
            // Show progress dialog
            saveDialog = ProgressDialog.show(this, getString(R.string.generalPleaseWait), getString(R.string.activityAddCommentSendingComment), true);

            // Create new thread, otherwise the progress dialog does not show
            new Thread() {
                public void run() {
                    // Send comment and fetch result
                    final boolean result = sendComment();

                    // Dismiss the dialog
                    saveDialog.dismiss();

                    // Must be done if we want to display the toast on screen. This must be done by a thread that can update the UI
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // Display result from sendcomment()
                            Toast toast = Toast.makeText(getApplicationContext(), result ? getString(R.string.generalSuccessPostComment) : lastError, Toast.LENGTH_LONG);
                            toast.show();

                            // If successful, close this activity and return
                            if (result) {
                                Intent resultIntent = new Intent();
                                setResult(Activity.RESULT_OK, resultIntent);
                                finish();
                            }
                        }
                    });
                }
            }.start();
        }
    }


    // Sendcomment() will do 2 things. Sending comments for talks AND sending comments
    // for events.
    public boolean sendComment() {
        String url;

        // Display progress bar
        displayProgressBar(true);

        lastError = "";

        // Get information from the layout
        RatingBar ratingbar = (RatingBar) findViewById(R.id.CommentRatingBar);
        int rating = (int) ratingbar.getRating();
        EditText tmp1 = (EditText) findViewById(R.id.CommentText);
        String comment = tmp1.getText().toString();
        CheckBox tmp2 = (CheckBox) findViewById(R.id.CommentPrivate);
        int priv = tmp2.isChecked() ? 1 : 0;

        JSONObject data = new JSONObject();

        try {
            data.put("comment", comment);
        } catch (JSONException e) {
            // do nothing
        }

        // Are we sending a talk comment?
        if (this.commentType.compareTo("talk") == 0) {
            try {
                this.talkJSON = new JSONObject(getIntent().getStringExtra("talkJSON"));
            } catch (JSONException e) {
                android.util.Log.e("JoindInApp", "No talk passed to activity", e);
            }

            // Talk comments have ratings
            try {
                data.put("rating", rating);
            } catch (JSONException e) {
                // do nothing
            }

            url = this.talkJSON.optString("comments_uri");

        } else {
            // We are sending an event comment
            try {
                this.eventJSON = new JSONObject(getIntent().getStringExtra("eventJSON"));
            } catch (JSONException e) {
                android.util.Log.e("JoindInApp", "No event passed to activity", e);
            }

            url = this.eventJSON.optString("comments_uri");
        }

        // Send data to joind.in API
        JIRest rest = new JIRest(AddComment.this);
        int result = rest.requestToFullURI(url, data, JIRest.METHOD_POST);
        if (result != JIRest.OK) {
            lastError = rest.getError();
        }

        // Remove progress bar
        displayProgressBar(false);

        return result == JIRest.OK;
    }

}
