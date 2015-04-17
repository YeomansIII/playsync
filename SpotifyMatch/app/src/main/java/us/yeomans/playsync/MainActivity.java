package us.yeomans.playsync;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback {

    //URLS
    private static final String MYIDBASEURL="http://10.158.38.56:8999/apiv1/mylistenerid/";
    private static final String INITIATEBASEURL="http://10.158.38.56:8999/apiv1/initiate/";


    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "8b81e3deddce42c4b0f2972e181b8a3a";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "playsync://callback";

    private Player mPlayer;
    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "853482743730";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCM Demo";

    TextView idText;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;

    String myListenerId;
    private String trackUri;
    private ArrayList<TextView> songListArr;

    String regid;

    WifiP2pManager p2pManager;
    WifiP2pManager.Channel p2pChannel;
    BroadcastReceiver p2pReceiver;
    IntentFilter p2pIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        idText = (TextView) findViewById(R.id.yourIdText);


        //AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
        //        AuthenticationResponse.Type.TOKEN,
        //        REDIRECT_URI);
        // builder.setScopes(new String[]{"user-read-private", "streaming"});
        //AuthenticationRequest request = builder.build();

        //AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        p2pChannel = p2pManager.initialize(this, getMainLooper(), null);
        p2pReceiver = new WiFiDirectBroadcastReceiver(p2pManager, p2pChannel, this);

        p2pIntentFilter = new IntentFilter();
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //showListenerId();

        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);
                        mPlayer.play("spotify:track:2TpxZ7JUBn3uw46aR7qd6V");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
        registerReceiver(p2pReceiver, p2pIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(p2pReceiver);
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        //int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + regId);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        //editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        //int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        //int currentVersion = getAppVersion(context);
        //if (registeredVersion != currentVersion) {
        //    Log.i(TAG, "App version changed.");
        //    return "";
       // }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                //mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    // Send an upstream message.
    public void onClick(final View view) {

        if (view == findViewById(R.id.requestButton)) {
            Log.d("GET", "Request Button");
            EditText friendId = (EditText) findViewById(R.id.friendId);
            EditText songText = (EditText) findViewById(R.id.songText);

            if (!friendId.equals("") && trackUri!=null) {
                new AsyncTask<String, Void, String>() {
                    @Override
                    protected String doInBackground(String... params) {
                        String a = "";
                        HttpURLConnection urlConnection;
                        try {
                            URL urlget = new URL(params[0]);
                            urlConnection = (HttpURLConnection) urlget.openConnection();
                            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                            java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");

                            //return "works";
                            String returner = s.hasNext() ? s.next() : "";
                            urlConnection.disconnect();
                            return returner;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return "{\"success\":false, \"error\":\"could not connect to server\"}";
                    }

                    @Override
                    protected void onPostExecute(String msg) {
                        try {
                            JSONObject json = new JSONObject(msg);
                            if (json.getBoolean("success") == true) {
                                Log.wtf("PuttingIntExtra", "sucess request");
                                String pId = json.getString("playId");
                                Intent playIntent = new Intent(getApplicationContext(), PlayingActivity.class);
                                Log.wtf("PuttingIntExtra", pId);
                                playIntent.putExtra("extra_stuff",new String[]{pId, "false"});
                                startActivity(playIntent);
                            } else {
                                idText.setText(json.getString("error"));
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }
                    }
                }.execute(INITIATEBASEURL + trackUri + "/" + friendId.getText() + "/" + myListenerId, null, null);
            } else {
                friendId.requestFocus();
                (findViewById(R.id.friendIdLabel)).setBackgroundColor(Color.RED);
                (findViewById(R.id.songLabel)).setBackgroundColor(Color.RED);
            }
        } else if(view == findViewById(R.id.songSearch)) {
            Log.d("GET","Song Search");
            searchSongs(""+((TextView)findViewById(R.id.songText)).getText());
        }
    }


    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Your implementation here.
    }



    public void showListenerId() {

        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String a = "";
                HttpURLConnection urlConnection;
                try {
                    URL urlget = new URL(params[0]);
                    urlConnection = (HttpURLConnection) urlget.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");

                    //return "works";
                    String returner = s.hasNext() ? s.next() : "";
                    urlConnection.disconnect();
                    return returner;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "{\"success\":false, \"error\":\"could not connect to server\"}";
            }

            @Override
            protected void onPostExecute(String msg) {
                try {
                    JSONObject json = new JSONObject(msg);
                    if(json.getBoolean("success")==true) {
                        idText.setTextSize(40f);
                        myListenerId = json.getString("id");
                        idText.setText("Your ID: "+myListenerId);
                    } else {
                        idText.setText(json.getString("error"));
                    }
                } catch (JSONException je) {
                    je.printStackTrace();
                }
            }
        }.execute(MYIDBASEURL+getRegistrationId(context), null, null);

    }

    public String searchSongs(String searchTerms) {

        try {
            AsyncTask<String, Void, String> get = new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    String a = "";
                    HttpURLConnection urlConnection;
                    try {
                        Log.d("GET","URL: "+params[0]);
                        URL urlget = new URL(params[0]);
                        urlConnection = (HttpURLConnection) urlget.openConnection();
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");

                        //return "works";
                        String returner = s.hasNext() ? s.next() : "";
                        urlConnection.disconnect();
                        return returner;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "{\"success\":false, \"error\":\"could not connect to server\"}";
                }

                @Override
                protected void onPostExecute(String msg) {
                    try {
                        Log.d("GET","PostExecute Song Search");
                        JSONObject json = new JSONObject(msg);
                        JSONArray items = json.getJSONObject("tracks").getJSONArray("items");
                        LinearLayout songList = (LinearLayout)findViewById(R.id.songList);
                        songListArr = new ArrayList<TextView>();
                        for(int i = 0; i<items.length(); i++) {
                            JSONObject curObj = items.getJSONObject(i);
                            TextView tv = new TextView(getApplicationContext());
                            tv.setText(curObj.getString("name") + " by " + curObj.getJSONArray("artists").getJSONObject(0).getString("name"));
                            tv.setTextSize(30f);
                            tv.setTextColor(Color.WHITE);
                            tv.setTag(curObj.getString("uri"));
                            tv.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    trackUri = (String)v.getTag();
                                    for(TextView view : songListArr) {
                                        view.setBackgroundColor(Color.TRANSPARENT);
                                    }
                                    v.setBackgroundColor(Color.GRAY);
                                }
                            });
                            songListArr.add(tv);
                            songList.addView(tv);
                        }
                    } catch (JSONException je) {
                        je.printStackTrace();
                    }
                }
            }.execute("https://api.spotify.com/v1/search?q="+ URLEncoder.encode(searchTerms, "UTF-8")+"&type=track", null, null);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }




        return "";
    }

}