package net.lapasa.babymonitor.subscriber;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class TestAudioPlayback extends ActionBarActivity
{

    private static int COUNT = 100;
    private AudioTrack track;
    private Button btn;
    private int bytesToRead;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_audio_playback);

        btn = (Button) findViewById(R.id.playsound);
        btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                playSound2();
            }
        });


/*
        int minBufferSize = AudioTrack.getMinBufferSize(22050, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        Log.d(TestAudioPlayback.class.getName(), "Playback buffer size is :" + minBufferSize + " bytes");
        track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                22050,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM);

        InputStream sampleInputStream = getResources().openRawResource(R.raw.sample);
        try
        {
            byte[] soundByte = new byte[sampleInputStream.available()];
            sampleInputStream.
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
*/

    }

    private void playSound2()
    {
        int N = AudioTrack.getMinBufferSize(22050, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                22050,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                N * 10,
                AudioTrack.MODE_STREAM);


    }

    public static byte[] convertStreamToByteArray(InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[26336];
        int i = Integer.MAX_VALUE;
        while ((i = is.read(buff, 0, buff.length)) > 0)
        {
            baos.write(buff, 0, i);
        }

        return baos.toByteArray(); // be sure to close InputStream in calling function
    }

    private void playSound()
    {
//        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 26336, AudioTrack.MODE_STREAM);
        InputStream in1 = getResources().openRawResource(R.raw.sample2);

        try
        {
            byte[] music1 = null;
            music1 = new byte[in1.available()];
            music1 = convertStreamToByteArray(in1);
            in1.close();

            byte[] output = new byte[music1.length];

            audioTrack.play();

            audioTrack.write(music1, 0, music1.length);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


    }

    public int getBytesToRead()
    {
        Log.d(TestAudioPlayback.class.getName(), "Count = " + ++TestAudioPlayback.COUNT);
        return 1024 * TestAudioPlayback.COUNT;
    }
}
