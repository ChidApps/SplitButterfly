package com.chidapps.android.splitbutterfly;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class MainListActivity extends ListActivity {

    public static final int NUMBER_OF_POSTS = 20;
    public static final String TAG = MainListActivity.class.getSimpleName();
    protected JSONObject mBlogData;
    protected ProgressBar mProgressBar;
    private final String KEY_TITLE = "title";
    private final String KEY_AUTHOR = "author";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);

        if (isNetworkAvailable()){
            mProgressBar.setVisibility(View.VISIBLE);
            GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
            getBlogPostsTask.execute();
        } else {
            Toast.makeText(this, "Network unavailable", Toast.LENGTH_LONG).show();
        }


        //Toast.makeText(this, getString(R.string.no_items), Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        JSONArray jsonPosts = null;

        try {
            jsonPosts = mBlogData.getJSONArray("posts");
            JSONObject jsonPost = jsonPosts.getJSONObject(position);
            String blogURL = jsonPost.getString("url");
            Intent intent = new Intent(this, BlogWebViewActivity.class);
            intent.setData(Uri.parse(blogURL));
            startActivity(intent);
        } catch (JSONException e) {
            logException(e);
        }
    }

    private void logException(Exception e) {
        Log.e(TAG, "Exception caught!", e);
    }

    private boolean isNetworkAvailable() {

        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;

        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }

        return isAvailable;
    }

    private void handleBlogResponse() {

        mProgressBar.setVisibility(View.INVISIBLE);

        if (mBlogData == null) {
            updateDisplayForError();
        } else {
            try {
                JSONArray jsonPosts = mBlogData.getJSONArray("posts");
                ArrayList<HashMap<String, String>> blogPosts = new ArrayList<HashMap<String, String>>();

                for (int i = 0; i < jsonPosts.length(); i++) {
                    JSONObject post = jsonPosts.getJSONObject(i);
                    String title = post.getString(KEY_TITLE);
                    title = Html.fromHtml(title).toString();
                    String author = post.getString(KEY_AUTHOR);
                    author = Html.fromHtml(author).toString();

                    HashMap<String, String> blogPost = new HashMap<String, String>();
                    blogPost.put(KEY_TITLE, title);
                    blogPost.put(KEY_AUTHOR, author);

                    blogPosts.add(blogPost);

                }

                String[] keys = {KEY_TITLE, KEY_AUTHOR};
                int[] ids = {android.R.id.text1, android.R.id.text2};
                SimpleAdapter adapter = new SimpleAdapter(this, blogPosts,
                                                            android.R.layout.simple_list_item_2,
                                                                keys, ids);
                setListAdapter(adapter);

            } catch (JSONException e) {
                logException(e);
            }
        }
    }

    private void updateDisplayForError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.alert_title));
        builder.setMessage(getString(R.string.alert_text));
        builder.setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();

        TextView emptyTextView = (TextView) getListView().getEmptyView();
        emptyTextView.setText(getString(R.string.no_items));
    }

    private class GetBlogPostsTask extends AsyncTask<Object, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Object[] objects) {
            int responseCode = -1;
            JSONObject jsonResponse = null;

            try {
                URL blogFeedURL = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POSTS);
                HttpURLConnection connection = (HttpURLConnection) blogFeedURL.openConnection();
                connection.connect();

                responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    Reader reader = new InputStreamReader(inputStream);

                    int nextCharacter; // read() returns an int, we cast it to char later
                    String responseData = "";
                    while (true){ // Infinite loop, can only be stopped by a "break" statement
                        nextCharacter = reader.read(); // read() without parameters returns one character
                        if(nextCharacter == -1) {// A return value of -1 means that we reached the end
                            break;
                        }
                        responseData += (char) nextCharacter; // The += operator appends the character to the end of the string
                    }

                    jsonResponse = new JSONObject(responseData);
                } else {
                    Log.i(TAG, "Unsuccessful HTTP Response Code: " + responseCode);
                }
            } catch (MalformedURLException e) {
                logException(e);
            } catch (IOException e) {
                logException(e);
            } catch (Exception e) {
                logException(e);
            }

            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject result){
            mBlogData = result;
            handleBlogResponse();
        }

    }


}
