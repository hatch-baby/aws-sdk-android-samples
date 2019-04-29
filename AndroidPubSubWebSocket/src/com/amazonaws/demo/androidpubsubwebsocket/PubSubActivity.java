/**
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.demo.androidpubsubwebsocket;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.FederatedSignInOptions;
import com.amazonaws.mobile.client.IdentityProvider;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import static com.amazonaws.mobile.client.internal.oauth2.OAuth2Client.TAG;

public class PubSubActivity extends Activity {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();

    private static final String CUSTOMER_SPECIFIC_IOT_ENDPOINT = "aacsjje0gbbcc-ats.iot.us-west-2.amazonaws.com";
    private static final String IDENTITY_POOL_ID = "us-west-2:5651fc22-f313-49d5-b379-fdab2c535d9c";


    private static final String TOPIC = "$aws/things/CC:50:E3:AB:4F:B6/shadow/update";
    //private static final String TOPIC = "hello/world";
    private static final String identityId = "us-west-2:8961f39c-9c06-4293-9054-63fe0882826f";
    private static final String token = "eyJraWQiOiJ1cy13ZXN0LTIxIiwidHlwIjoiSldTIiwiYWxnIjoiUlM1MTIifQ.eyJzdWIiOiJ1cy13ZXN0LTI6ODk2MWYzOWMtOWMwNi00MjkzLTkwNTQtNjNmZTA4ODI4MjZmIiwiYXVkIjoidXMtd2VzdC0yOjU2NTFmYzIyLWYzMTMtNDlkNS1iMzc5LWZkYWIyYzUzNWQ5YyIsImFtciI6WyJhdXRoZW50aWNhdGVkIiwiaGF0Y2gud2lmaS5yZXN0LmxvZ2luIiwiaGF0Y2gud2lmaS5yZXN0LmxvZ2luOnVzLXdlc3QtMjo1NjUxZmMyMi1mMzEzLTQ5ZDUtYjM3OS1mZGFiMmM1MzVkOWM6cWEtNjUyODY1Il0sImlzcyI6Imh0dHBzOi8vY29nbml0by1pZGVudGl0eS5hbWF6b25hd3MuY29tIiwiZXhwIjoxNTU2MDQyMjM5LCJpYXQiOjE1NTYwNDEzMzl9.EgSgVZCZR_k32m06B2IScEPqZ7xWwP4mVQW37jytjVXulJ4neNp4LpYIz0yDXrElLRnc-EnrCtweWihnpy_SEvQEUWndcY6z8u4TBtE7Q-hwF_brqDtOeDeBTCNv99fb0TYi5sK0uT_NuXb7uR8nkBu0wLFvIn703eJpOtdTRJ3IEVOFmdyi7gXTpBrWdXAy6csnWZyDcmkPyiDk51DxGkoBORLEd14oi4ltzukeWeunhY5zUvU3XAw3SYOuK37Zv_kX9SCL6s74tO3ldrHlNLRxvOsNkesw2lO0rT2I7c6EUTIeMZnRSSLxvaEOUDynlAWaxD9Ji13U0nKASg_0lw";

    EditText txtSubscribe;
    EditText txtTopic;
    EditText txtMessage;

    TextView tvLastMessage;
    TextView tvClientId;
    TextView tvStatus;

    Button btnConnect;

    AWSIotMqttManager mqttManager;
    String clientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtSubscribe = findViewById(R.id.txtSubscribe);
        txtTopic = findViewById(R.id.txtTopic);
        txtMessage = findViewById(R.id.txtMessage);

        tvLastMessage = findViewById(R.id.tvLastMessage);
        tvClientId = findViewById(R.id.tvClientId);
        tvStatus = findViewById(R.id.tvStatus);

        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setEnabled(false);

        java.util.logging.Logger.getLogger("com.amazonaws").setLevel(Level.ALL);
        java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.ALL);
        // adb shell setprop log.tag.com.amazonaws.request DEBUG
        // adb shell setprop log.tag.org.apache.http.headers VERBOSE
        // adb shell setprop log.tag.org.apache.http.wire VERBOSE

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        tvClientId.setText(clientId);

        // Initialize the credentials provider
        final CountDownLatch latch = new CountDownLatch(1);
        AWSMobileClient.getInstance().initialize(
                getApplicationContext(),
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        latch.countDown();
                        Log.e(LOG_TAG, "onError: ", e);
                    }
                }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_IOT_ENDPOINT);

        // Enable button once all clients are ready
        btnConnect.setEnabled(true);
    }

    public void connect(final View view) {
        Log.i(LOG_TAG, "Connecting");
        Log.i(LOG_TAG, "clientId = " + clientId);

        // Initialize the Amazon Cognito credentials provider
//        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
//                getApplicationContext(),
//                IDENTITY_POOL_ID,
//                Regions.US_WEST_2 // Region
//        );
//        Map<String, String> logins = new HashMap<>();
//        logins.put(CUSTOMER_SPECIFIC_IOT_ENDPOINT + "/" + IDENTITY_POOL_ID, "hatch.wifi.rest.login");
//        credentialsProvider.setLogins(logins);

        FederatedSignInOptions options = FederatedSignInOptions.builder()
                .cognitoIdentityId(identityId)
                .build();

        AWSMobileClient.getInstance().federatedSignIn(IdentityProvider.DEVELOPER.toString(), token, options, new Callback<UserStateDetails>() {
            @Override
            public void onResult(final UserStateDetails userStateDetails) {
                // Handle the result
                Log.i(LOG_TAG, userStateDetails.toString());
            }

            @Override
            public void onError(Exception e) {
                Log.e(LOG_TAG, "federated sign-in error", e);
            }
        });

        try {
            //mqttManager.connect(AWSMobileClient.getInstance(), new AWSIotMqttClientStatusCallback() {
            mqttManager.connect(AWSMobileClient.getInstance(), new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.i(LOG_TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText(status.toString());
                            if (throwable != null) {
                                Log.e(LOG_TAG, "Connection error.", throwable);
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            tvStatus.setText("Error! " + e.getMessage());
        }
    }

    public void subscribe(final View view) {
        Log.i(LOG_TAG, "Subscribing");
        String topic = txtSubscribe.getText().toString();

        topic = TOPIC + "/delta";
        Log.i(LOG_TAG, "Subscribe to topic = " + topic);

        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String message = new String(data, "UTF-8");
                                        Log.i(LOG_TAG, "Message arrived:");
                                        Log.i(LOG_TAG, "   Topic: " + topic);
                                        Log.i(LOG_TAG, " Message: " + message);

                                        tvLastMessage.setText(message);

                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                    }
                                }
                            });
                        }
                    });
            Log.i(LOG_TAG, "Successfully subscribed to topic = " + topic);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }

    public void publish(final View view) {
        Log.i(LOG_TAG, "Publishing");
        String topic = txtTopic.getText().toString();
        String msg = txtMessage.getText().toString();  // better by "true" or "false"
        msg = "{ \"state\": { \"desired\": { \"isPowered\": " + msg + "}}}";
        Log.i(LOG_TAG, msg);

        try {
            mqttManager.publishString(msg, TOPIC, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    public void disconnect(final View view) {
        Log.i(LOG_TAG, "Disconnecting");
        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
    }
}
