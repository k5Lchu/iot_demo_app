package com.example.kevin.myapplication;

import android.app.ProgressDialog;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // UI Elements to interact with IOT devices
    private Button onBut_1;
    private Button onBut_2;
    private Button offBut_1;
    private Button offBut_2;

    // handler to schedule recurring requests
    private Handler handler;
    // method to send status request
    private Runnable r;

    // strings for url, development pass, and logging tag
    private final static String url = "http://192.168.29.139:1821/";
    private final static String pass = "12345";
    private final static String TAG = "APP_STATUS";

    // ints for request timeout and time interval between status requests
    private final static int REQUEST_TIMEOUT = 5000;
    private final static int SERVICE_INTERVAL = 3000;
    private final static int RESET_INITIAL = 500;

    // post requests to be executed by volley request queue
    private StringRequest getStatusRequest;

    private ProgressDialog waiting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        waiting = new ProgressDialog(this);
        waiting.setCancelable(false);

        // main queue to execute post requests.
        // made variable final for inner classes to access
        final RequestQueue queue = Volley.newRequestQueue(this);

        getStatusRequest = new StringRequest(Request.Method.POST,url,
                new statusResponseHandler(),new ErrorHandler()) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> data = new HashMap<>();
                // add operation code for getting status of device
                // add password for operation
                data.put("op", "-1");
                data.put("pass", pass);
                return data;
            }
        };
        getStatusRequest.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // initialize handler and handler method
        // method is a loop or recurring status request at an interval of 3 seconds
        handler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                // get device status and start another request in 10 seconds
                queue.add(getStatusRequest);
                handler.postDelayed(r,SERVICE_INTERVAL);
                Log.d(TAG,"Getting status from server...");
            }
        };

        // initialize buttons and text views
        // set onclick listeners for each button
        onBut_1 = (Button) findViewById(R.id.onButton_1);
        onBut_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send turn on request
                queue.add(createRequest(0));
                Log.d(TAG,"Sending turn-on request...");
                // reset handler to allow returned "turned on" message to be seen
                // for a reasonable amount of time
                disableAllButtons();
                stopStatReq();
                waiting.setTitle("Turning on");
                waiting.show();
            }
        });

        onBut_2 = (Button) findViewById(R.id.onButton_2) ;
        onBut_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send turn on request
                queue.add(createRequest(2));
                Log.d(TAG,"Sending turn-on request...");
                // reset handler to allow returned "turned on" message to be seen
                // for a reasonable amount of time
                disableAllButtons();
                stopStatReq();
                waiting.setTitle("Turning on");
                waiting.setMessage("Please wait...");
                waiting.show();
            }
        });

        offBut_1 = (Button) findViewById(R.id.offButton_1);
        offBut_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send turn off request
                queue.add(createRequest(1));
                Log.d(TAG,"Sending turn-off request...");
                // reset handler to allow returned "turned on" message to be seen
                // for a reasonable amount of time
                disableAllButtons();
                stopStatReq();
                waiting.setTitle("Turning off");
                waiting.setMessage("Please wait...");
                waiting.show();
            }
        });

        offBut_2 = (Button) findViewById(R.id.offButton_2);
        offBut_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send turn off request
                queue.add(createRequest(3));
                Log.d(TAG,"Sending turn-off request...");
                // reset handler to allow returned "turned on" message to be seen
                // for a reasonable amount of time
                disableAllButtons();
                stopStatReq();
                waiting.setTitle("Turning off");
                waiting.setMessage("Please wait...");
                waiting.show();
            }
        });

        // begin the initial status request when app is being started
        disableAllButtons();
        queue.add(getStatusRequest);
    }

    private StringRequest createRequest(final int opCode) {
        StringRequest request = new StringRequest(Request.Method.POST,url,
                new onOffResponseHandler(),new ErrorHandler()) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> data = new HashMap<>();
                // add operation code for turning on device
                // add password for operation
                data.put("op", String.valueOf(opCode));
                data.put("pass", pass);
                return data;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return request;
    }

    // begin recurring status requests from server
    private void startStatReq(int initial) {
        // start initial request in 10 seconds
        handler.postDelayed(r,initial);
    }

    // stop recurring status requests from server
    private void stopStatReq() {
        // remove scheduled requests from handler
        handler.removeCallbacks(r);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // stop the recurring status requests
        stopStatReq();
        Log.d(TAG,"Pausing status service");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // start the recurring status requests
        startStatReq(SERVICE_INTERVAL);
        Log.d(TAG,"Starting status service");
    }

    private void disableAllButtons() {
        onBut_1.setEnabled(false);
        offBut_1.setEnabled(false);
        onBut_2.setEnabled(false);
        offBut_2.setEnabled(false);
    }

    // class to handle errors encountered when volley is executing request
    class ErrorHandler implements Response.ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {
            // print error stacktrace and notify user error has occurred
            error.printStackTrace();
            disableAllButtons();
            Log.d(TAG,"Error encountered while sending request");
        }
    }

    // class to handle server response to the sent request
    class statusResponseHandler implements Response.Listener<String> {

        @Override
        public void onResponse(String response) {
            // notify user of server response
            if (response.contains("status")) {
                String[] status = response.split(" ");
                if (status[1].equals("on")) {
                    onBut_1.setEnabled(false);
                    offBut_1.setEnabled(true);
                }
                else {
                    onBut_1.setEnabled(true);
                    offBut_1.setEnabled(false);
                }
                if (status[2].equals("on")) {
                    onBut_2.setEnabled(false);
                    offBut_2.setEnabled(true);
                }
                else {
                    onBut_2.setEnabled(true);
                    offBut_2.setEnabled(false);
                }
                waiting.dismiss();
                Log.d(TAG,"Request successful. Response received");
            }
        }
    }

    class onOffResponseHandler implements Response.Listener<String> {
        @Override
        public void onResponse(String response) {
            startStatReq(RESET_INITIAL);
        }
    }
}