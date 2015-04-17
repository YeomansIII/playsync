package us.yeomans.playsync;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by jason on 4/11/15.
 */
public class PlayingActivity extends Activity implements  PlayerNotificationCallback, ConnectionStateCallback {

    private static final String READYBASEURL="http://10.158.38.56:8999/apiv1/ready/";
    private static final String ISREADYBASEURL="http://10.158.38.56:8999/apiv1/isready/";

    static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "8b81e3deddce42c4b0f2972e181b8a3a";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "playsync://callback";

    private Player mPlayer;
    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;
    private AuthenticationResponse response;
    private Boolean validated;
    private long pingtime = 0;

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private String playId;
    private boolean joined;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playId="";
        Bundle startingIntentBundle = getIntent().getExtras();
        if(startingIntentBundle!=null){
            String[] extras = startingIntentBundle.getStringArray("extra_stuff");
              playId = extras[0];
            joined = Boolean.parseBoolean(extras[1]);
            Log.wtf("Test","Joined: "+joined);
        }
        Log.wtf("Intent Extras", playId);
        if(!playId.equals("")) {
            setContentView(R.layout.playing);
            ((TextView)findViewById(R.id.syncRoom)).setText("Sync Room:"+playId);


            AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
            AuthenticationResponse.Type.TOKEN,REDIRECT_URI);
            builder.setScopes(new String[]{"user-read-private", "streaming"});
            AuthenticationRequest request = builder.build();
            AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                if(joined) {
                    Log.wtf("Test","Joined");
                    readyup();
                } else {
                    Log.wtf("Test","Joined");
                    readyLoop();
                }
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
    public void onPlaybackEvent(PlayerNotificationCallback.EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(PlayerNotificationCallback.ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }


    private void readyup() {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String a = "";
                HttpURLConnection urlConnection;
                try {
                    URL urlget = new URL(params[0]);
                    long beforeMs = System.currentTimeMillis();
                    urlConnection = (HttpURLConnection) urlget.openConnection();
                    long afterMs = System.currentTimeMillis();
                    pingtime = (afterMs - beforeMs)/2;
                    Log.d("NET","pingtime: "+pingtime);
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
                TextView waiting = (TextView)findViewById(R.id.waitingForFriend);
                try {
                    JSONObject json = new JSONObject(msg);
                    if (json.getBoolean("success") == true) {
                        String startTime = json.getString("start");
                        String nowTime = json.getString("now");
                        final String track = json.getString("track");
                        Log.wtf("NOW","Now: " + nowTime);

                        Calendar timeNow = Calendar.getInstance();
                        timeNow.setTime(new SimpleDateFormat("kk':'mm':'ss", Locale.ENGLISH).parse(nowTime));
                        Calendar calNow = Calendar.getInstance();
                        calNow.setTimeZone(TimeZone.getTimeZone("UTC"));
                        calNow.set(Calendar.HOUR,timeNow.get(Calendar.HOUR));
                        calNow.set(Calendar.HOUR_OF_DAY,timeNow.get(Calendar.HOUR_OF_DAY));
                        calNow.set(Calendar.MINUTE,timeNow.get(Calendar.MINUTE));
                        calNow.set(Calendar.SECOND,timeNow.get(Calendar.SECOND));
                        String[] splited = nowTime.split("[.]");
                        int mili = Integer.parseInt(splited[1])/1000;
                        calNow.setTimeInMillis(calNow.getTimeInMillis()+mili);

                        Calendar calStart = Calendar.getInstance();
                        calStart.setTime(new SimpleDateFormat("kk':'mm':'ss", Locale.ENGLISH).parse(startTime));
                        Calendar timeStart = Calendar.getInstance();
                        timeStart.setTimeZone(TimeZone.getTimeZone("UTC"));
                        timeStart.set(Calendar.HOUR,calStart.get(Calendar.HOUR));
                        timeStart.set(Calendar.HOUR_OF_DAY,calStart.get(Calendar.HOUR_OF_DAY));
                        timeStart.set(Calendar.MINUTE,calStart.get(Calendar.MINUTE));
                        timeStart.set(Calendar.SECOND, calStart.get(Calendar.SECOND));
                        splited = startTime.split("[.]");
                        mili = Integer.parseInt(splited[1])/1000;
                        timeStart.setTimeInMillis(timeStart.getTimeInMillis()+mili);

                        Log.wtf("CAL","Time Difference: "+(timeStart.getTimeInMillis() - calNow.getTimeInMillis()));
                        new CountDownTimer(timeStart.getTimeInMillis() - calNow.getTimeInMillis(), 1000) {

                            TextView waitingInner = (TextView)findViewById(R.id.waitingForFriend);

                            public void onTick(long millisUntilFinished) {
                                waitingInner.setText("seconds remaining: " + millisUntilFinished / 1000);
                            }

                            public void onFinish() {
                                waitingInner.setText("Enjoy the music.");
                                playSong(track);
                            }
                        }.start();
                    } else {
                        waiting.setText(json.getString("error"));
                    }
                } catch (JSONException je) {
                    je.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }.execute(READYBASEURL + playId, null, null);
    }

    public void readyLoop() {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String a = "";
                HttpURLConnection urlConnection;
                while(true) {
                    Log.wtf("readyLoop","Running readyloop");
                    try {
                        URL urlget = new URL(params[0]);
                        long beforeMs = System.currentTimeMillis();
                        urlConnection = (HttpURLConnection) urlget.openConnection();
                        long afterMs = System.currentTimeMillis();
                        pingtime = (afterMs - beforeMs)/2;
                        Log.d("NET","pingtime: "+pingtime);
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");

                        //return "works";
                        String returner = s.hasNext() ? s.next() : "";
                        urlConnection.disconnect();
                        JSONObject json = new JSONObject(returner);
                        if(json.getBoolean("success")) {
                            if(json.getBoolean("ready")) {
                                return returner;
                            }
                        }
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void onPostExecute(String msg) {
                TextView waiting = (TextView)findViewById(R.id.waitingForFriend);
                try {
                    JSONObject json = new JSONObject(msg);
                    if (json.getBoolean("success") == true) {
                        String startTime = json.getString("start");
                        String nowTime = json.getString("now");
                        final String track = json.getString("track");
                        Log.wtf("NOW","Now: " + nowTime);

                        Calendar timeNow = Calendar.getInstance();
                        timeNow.setTime(new SimpleDateFormat("kk':'mm':'ss", Locale.ENGLISH).parse(nowTime));
                        Calendar calNow = Calendar.getInstance();
                        calNow.setTimeZone(TimeZone.getTimeZone("UTC"));
                        calNow.set(Calendar.HOUR,timeNow.get(Calendar.HOUR));
                        calNow.set(Calendar.HOUR_OF_DAY,timeNow.get(Calendar.HOUR_OF_DAY));
                        calNow.set(Calendar.MINUTE,timeNow.get(Calendar.MINUTE));
                        calNow.set(Calendar.SECOND, timeNow.get(Calendar.SECOND));
                        String[] splited = nowTime.split("[.]");
                        int mili = Integer.parseInt(splited[1])/1000;
                        calNow.setTimeInMillis(calNow.getTimeInMillis()+mili);

                        Calendar calStart = Calendar.getInstance();
                        calStart.setTime(new SimpleDateFormat("kk':'mm':'ss", Locale.ENGLISH).parse(startTime));
                        Calendar timeStart = Calendar.getInstance();
                        timeStart.setTimeZone(TimeZone.getTimeZone("UTC"));
                        timeStart.set(Calendar.HOUR,calStart.get(Calendar.HOUR));
                        timeStart.set(Calendar.HOUR_OF_DAY,calStart.get(Calendar.HOUR_OF_DAY));
                        timeStart.set(Calendar.MINUTE, calStart.get(Calendar.MINUTE));
                        timeStart.set(Calendar.SECOND,calStart.get(Calendar.SECOND));
                        splited = startTime.split("[.]");
                        mili = Integer.parseInt(splited[1])/1000;
                        timeStart.setTimeInMillis(timeStart.getTimeInMillis() + mili);

                        Log.wtf("CAL","Time Difference: "+(timeStart.getTimeInMillis() - calNow.getTimeInMillis()));
                        new CountDownTimer(timeStart.getTimeInMillis() - calNow.getTimeInMillis(), 1000) {

                            TextView waitingInner = (TextView)findViewById(R.id.waitingForFriend);

                            public void onTick(long millisUntilFinished) {
                                waitingInner.setText("seconds remaining: " + millisUntilFinished / 1000);
                            }

                            public void onFinish() {
                                waitingInner.setText("Enjoy the music.");
                                playSong(track);
                            }
                        }.start();
                    } else {
                        waiting.setText(json.getString("error"));
                    }
                } catch (JSONException je) {
                    je.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }.execute(ISREADYBASEURL + playId, null, null);
    }

    private void playSong(final String track) {
        Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
        mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
            @Override
            public void onInitialized(Player player) {
                mPlayer.addConnectionStateCallback(PlayingActivity.this);
                mPlayer.addPlayerNotificationCallback(PlayingActivity.this);
                mPlayer.play(track);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
            }
        });
    }

    public static Date GetUTCdatetimeAsDate()
    {
        //note: doesn't check for null
        return StringDateToDate(GetUTCdatetimeAsString());
    }

    public static String GetUTCdatetimeAsString()
    {
        final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = sdf.format(new Date());

        return utcTime;
    }

    public static Date StringDateToDate(String StrDate)
    {
        Date dateToReturn = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);

        try
        {
            dateToReturn = (Date)dateFormat.parse(StrDate);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        return dateToReturn;
    }

}
