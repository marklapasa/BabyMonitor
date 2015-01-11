package net.lapasa.babymonitor.publisher;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;
import android.widget.ToggleButton;

public class TestAudioCapture extends ActionBarActivity
{
    private AudioCapture audioCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_audio_capture);

        TextView console = (TextView) findViewById(R.id.console);
        ToggleButton recordToggleBtn = (ToggleButton) findViewById(R.id.recordToggle);

        audioCapture = new AudioCapture(recordToggleBtn, console);
    }

    @Override
    protected void onDestroy()
    {
        audioCapture.release();
    }
}
