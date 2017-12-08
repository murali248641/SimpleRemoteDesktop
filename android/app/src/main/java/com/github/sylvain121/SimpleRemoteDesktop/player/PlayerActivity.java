package com.github.sylvain121.SimpleRemoteDesktop.player;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.github.sylvain121.SimpleRemoteDesktop.MainActivity;
import com.github.sylvain121.SimpleRemoteDesktop.player.controllerAdapter.ControlAdapter;
import com.github.sylvain121.SimpleRemoteDesktop.player.controllerAdapter.GamepadToMouseKeyboardAdapter;
import com.github.sylvain121.SimpleRemoteDesktop.player.controllerAdapter.MultiButtonMouseButtonAdapter;
import com.github.sylvain121.SimpleRemoteDesktop.player.controllerAdapter.SimpleTouchAdapter;
import com.github.sylvain121.SimpleRemoteDesktop.player.video.MediaCodecDecoderRenderer;
import com.github.sylvain121.SimpleRemoteDesktop.settings.SettingsActivity;

public class PlayerActivity extends Activity implements SurfaceHolder.Callback, InputManager.InputDeviceListener {

    private String TAG = "PLAYER ACTIVITY";
    private String IPAddress;
    private boolean MouseIsPresent = false;
    private MediaCodecDecoderRenderer mediaCodec;
    private ConnectionThread cnx;
    private ControlAdapter ctlAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(sv);
        ctlAdapter = new GamepadToMouseKeyboardAdapter();
        Intent intent = getIntent();
        this.IPAddress = intent.getStringExtra(MainActivity.IP_ADDRESS);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("SURFACE", "SURFACE CREATED");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if(mediaCodec != null || cnx != null) {
            mediaCodec.stop();
            cnx.close();
        }

        Log.d(TAG, "width : "+width+ "height : "+height);
        ctlAdapter.onSurfaceChange(width, height);

        SharedPreferences sharedPreference = getBaseContext().getSharedPreferences(SettingsActivity.SIMPLE_REMOTE_DESKTOP_PREF, 0);

        mediaCodec = new MediaCodecDecoderRenderer();
        mediaCodec.setRenderTarget(holder);
        mediaCodec.setup(width, height);
        mediaCodec.start();

        cnx = new ConnectionThread(width, height, this.IPAddress, sharedPreference);
        cnx.setDecoderHandler(mediaCodec);
        cnx.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("SURFACE", "SURFACE DESTROYED");
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if(device.getSources() == InputDevice.SOURCE_MOUSE) {
            Log.d(TAG, "Mouse plugged");
            this.MouseIsPresent = true;
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if(device.getSources() == InputDevice.SOURCE_MOUSE) {
            Log.d(TAG, "Mouse Changed");

        }
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if(device.getSources() == InputDevice.SOURCE_MOUSE) {
            Log.d(TAG, "Mouse Unplugged");
            this.MouseIsPresent = false;
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        Log.d(TAG, event.toString());
        return ctlAdapter.onGenericMotion(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, event.toString());
        return ctlAdapter.onGenericKeyEvent(event);
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d(TAG, event.toString());
        return ctlAdapter.onGenericTouchEvent(event);

    }
}