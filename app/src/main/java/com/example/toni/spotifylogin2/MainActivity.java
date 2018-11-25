package com.example.toni.spotifylogin2;

import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONArray;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private static final String CLIENT_ID = "22c38c02421f4087a5c0fe6a9fafd666";
    private static final String REDIRECT_URI = "com.example.toni.spotifylogin://callback";
    private static final int REQUEST_CODE = 1337;
    private SpotifyAppRemote mSpotifyAppRemote;
    private String TOKEN = "";
    private String USER_ID= "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming", "playlist-modify-public", "playlist-modify-private"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    Log.d("TOKEN", response.getAccessToken());
                    TOKEN = response.getAccessToken();

                    ConnectionParams connectionParams =
                            new ConnectionParams.Builder(CLIENT_ID)
                                    .setRedirectUri(REDIRECT_URI)
                                    .showAuthView(true)
                                    .build();

                    SpotifyAppRemote.connect(this, connectionParams,
                            new Connector.ConnectionListener() {

                                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                                    mSpotifyAppRemote = spotifyAppRemote;
                                    Log.d("MainActivity", "Connected! Yay!");

                                    // Now you can start interacting with App Remote
                                    //connected();
                                    try {
                                        getCurrentUserID();
                                        Gson gson = new Gson();
                                        String recommended = query_spotify("0.8", "0.8", "0.5", "180000");
                                        Tracks tr = gson.fromJson(recommended, com.example.toni.spotifylogin2.Tracks.class);


                                        String listEmptyID = create_playlist("heyyyw", USER_ID);
                                        fullfill_playlist(tr.tracks, listEmptyID);

                                        connected(listEmptyID);




                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }

                                public void onFailure(Throwable throwable) {
                                    Log.e("MyActivity", throwable.getMessage(), throwable);

                                    // Something went wrong when attempting to connect! Handle errors here
                                }
                            });

                    // Handle successful response
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }

    private void connected(String listID) {
        // Play a playlist
        mSpotifyAppRemote.getPlayerApi().play("spotify:user:spotify:playlist:" + listID);

        // Subscribe to PlayerState
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(new Subscription.EventCallback<PlayerState>() {

                    public void onEvent(PlayerState playerState) {
                        final Track track = playerState.track;
                        if (track != null) {
                            Log.d("MainActivity", track.name + " by " + track.artist.name);
                        }
                    }
                });
    }

    private String query_spotify(String danceability, String energy, String valance, String duration) throws IOException {

        int SDK_INT = android.os.Build.VERSION.SDK_INT;

        Response response = null;

        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here

            Random r = new Random();
            double random = 0.0 + r.nextDouble() * (1.0 - 0.0);
            String total2 = String.valueOf(random);

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/recommendations?seed_genres=work-out&target_danceability=" + danceability + "&target_energy=" + energy +"&valence=" + valance + "&duration_ms=" + duration + "&popularity=" + total2)
                    .get()
                    .addHeader("Authorization", "Bearer " + TOKEN)
                    .build();

            response = client.newCall(request).execute();

        }

        return response.body().string();

    }

    private String create_playlist(String playlistName, String userID) throws IOException {

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        String pID = "";
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here

            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, "{\n\t\"name\" : \"" + playlistName + "Hello9\"\n}");
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/users/" + userID + "/playlists")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + TOKEN)
                    .build();

            Response response = client.newCall(request).execute();

            Gson gson = new Gson();

          Playlist playlist = gson.fromJson(response.body().string(), Playlist.class);

          Log.i("ID", playlist.id);
          pID = playlist.id;

        }

        return pID;


    }

    private void fullfill_playlist(com.example.toni.spotifylogin2.Track[] tracks, String playlistID) throws IOException {
        ArrayList<String> stracks = new ArrayList<>();
        JSONArray jsonArray;


        for (com.example.toni.spotifylogin2.Track track : tracks) {
            stracks.add(track.uri);
        }

        jsonArray = new JSONArray(stracks);


        int SDK_INT = android.os.Build.VERSION.SDK_INT;

        Response response = null;

        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here

            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, "{\n\t\"uris\" : " + jsonArray + "\n}");
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/playlists/" + playlistID + "/tracks")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Authorization", "Bearer " + TOKEN)
                    .build();

            response = client.newCall(request).execute();
        }
    }

    private void getCurrentUserID() throws IOException {

        int SDK_INT = android.os.Build.VERSION.SDK_INT;

        Response response = null;

        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/me")
                    .get()
                    .addHeader("Authorization", "Bearer " + TOKEN)
                    .build();

            response = client.newCall(request).execute();
            Gson gson = new Gson();
            User user = gson.fromJson(response.body().string(), User.class);
            USER_ID = user.id;
        }

    }



    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }
}
