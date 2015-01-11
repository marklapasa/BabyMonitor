package net.lapasa.babymonitor.publisher;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class AudioCapture
{
    private static final String TAG = AudioCapture.class.getName();
    private final int MAX_RECORDING_SIZE = -1;//1024 * 8; //1000000;
    private SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

    private final TextView console;
    private ToggleButton recordToggleBtn;
    private MediaRecorder mediaRecorder;
    private Handler handler = new Handler();
    private String activeRecordingPath;
    private FileDescriptor socketFileDescriptor;
    private AudioRecord audioRec;

    private boolean isRecording = false;
    private Socket socket;
    private Activity activity;
    private BufferedOutputStream outputStream;


    /**
     * Constructor
     */
    public AudioCapture(ToggleButton recordToggleBtn, TextView console)
    {
        this.recordToggleBtn = recordToggleBtn;
        this.console = console;
        this.activity = (Activity) recordToggleBtn.getContext();
        initOld();
    }

    private void initOld()
    {
        recordToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
//                    startRecordingOLD();
                    startRecording();
                }
                else
                {
//                    stopRecordingOLD();
                    stopRecording();
                }
            }
        });
        recordToggleBtn.setEnabled(false);
    }

    private void startRecording()
    {
//        int N = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
//        audioRec = new AudioRecord(
//                MediaRecorder.AudioSource.MIC,
//                8000, AudioFormat.CHANNEL_IN_MONO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                1024 * 2);
        audioRec = findAudioRecord();
        audioRec.startRecording();

        isRecording = true;
        new Thread(new AudioRecordingRunnable(outputStream)).start();
    }

    private void stopRecording()
    {
        isRecording = false;
        audioRec.stop();
        audioRec.release();

        consoleMsg("Recording stopped. " );
    }

    private void startRecordingOLD()
    {
        if (mediaRecorder == null)
        {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setMaxFileSize(MAX_RECORDING_SIZE);
            mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener()
            {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra)
                {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
                    {
                        consoleMsg("MAX FILE SIZE LIMIT HIT");
                        recordToggleBtn.setChecked(false);
                    }
                    else
                    {
                        consoleMsg("Unknown Error, Code =  " + what);
                    }
                }
            });

            mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener()
            {
                @Override
                public void onError(MediaRecorder mr, int what, int extra)
                {
                    String msg;
                    switch (what)
                    {
                        case MediaRecorder.MEDIA_ERROR_SERVER_DIED:
                            msg = "MEDIA_ERROR_SERVER_DIED";
                            break;
                        case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN:
                            msg = "MEDIA_RECORDER_ERROR_UNKNOWN";
                            break;
                        default:
                            msg = "Unknown";

                    }
                    consoleMsg("Recording Error Occurred: " + msg);
                }
            });
        }

        /* String based approach
        activeRecordingPath = getOutputFilePath();
        mediaRecorder.setOutputFile(activeRecordingPath);
        */


        try
        {
            mediaRecorder.setOutputFile(socketFileDescriptor);
        }
        catch (NullPointerException npe)
        {
            consoleMsg("You must have a connected client before being able to record");
            return;
        }


        try
        {
            mediaRecorder.prepare();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            stopRecordingOLD();
        }

        mediaRecorder.start();

        consoleMsg("Recording audio...");
    }

    private void stopRecordingOLD()
    {
        if (mediaRecorder != null)
        {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            consoleMsg("Recording stopped... " + activeRecordingPath);
        }
    }


    public void release()
    {
        if (mediaRecorder != null)
        {
            mediaRecorder.release();
        }
    }

    public FileDescriptor getFileDescriptorFromSocket(Socket socket)
    {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);
        return pfd.getFileDescriptor();
    }

    private String getOutputFilePath()
    {
        String str = null;

        SimpleDateFormat fmt = new SimpleDateFormat("HH_mm");

        String prefix = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AUDIO_CAPTURE_";

        return prefix + fmt.format(new Date()) + ".aac";
    }


    /**
     * TODO: Normalize this into shared library
     *
     * @param msg
     */

    private void consoleMsg(String msg)
    {
        handler.post(new UpdateUIRunnable(msg));
    }

    public void setSocket(Socket socket)
    {
        this.socket = socket;
        if (socket == null)
        {
            recordToggleBtn.setEnabled(false);
        }
        else
        {
            recordToggleBtn.setEnabled(true);
            this.socketFileDescriptor = getFileDescriptorFromSocket(socket);
        }
    }

    public void setOutputStream(BufferedOutputStream bufferedOutputStream)
    {
        this.outputStream = bufferedOutputStream;
    }


    class UpdateUIRunnable implements Runnable
    {
        private String msg;
        private int LINE_COUNT = 25;

        public UpdateUIRunnable(String str)
        {
            this.msg = str;
        }

        @Override
        public void run()
        {
            Calendar c = Calendar.getInstance();
            String str = fmt.format(c.getTime()) + ": " + msg + "\n";

            if (console.getLineCount() % LINE_COUNT == 0)
            {
                console.setText("");
            }
            console.append(str);

            final int scrollAmount = console.getLayout().getLineTop(console.getLineCount()) - console.getHeight();
            if (scrollAmount > 0)
            {
                console.scrollTo(0, console.getLineCount());
            }
            else
            {
                console.scrollTo(0, 0);
            }
        }
    }

    private class AudioRecordingRunnable implements Runnable
    {
        private final BufferedOutputStream out;

        public AudioRecordingRunnable(BufferedOutputStream out)
        {
            this.out = out;
        }

        @Override
        public void run()
        {
            consoleMsg("Audio Recording has begun...");
            byte[] audioData = new byte[1024 * 4];
//            short[] audioData = new short[1024 ];

            int byteDataCounter = 0;
            int audioDataCounter = 0;
            int id = 0;
            while (isRecording)
            {
                // Collect data from hardware microphone
                int N = audioRec.read(audioData, 0, audioData.length);

                try
                {
//                    // Send over the wire
                    byteDataCounter += audioData.length;
                    audioDataCounter += audioData.length;
//
                    consoleMsg(id++ + "| " + audioData.length + " | " + audioData.length + " | " + audioDataCounter);
//                    out.write(byteData, 0, N);
//                    out.flush();
//
//
//                    new String(bytes, "UTF-8");

                    out.write(audioData,0,audioData.length);
                    out.flush();
                }
                catch (IOException e)
                {
                    e.printStackTrace();

                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            recordToggleBtn.setChecked(false);
                        }
                    });

                }
            }
            consoleMsg("...Audio Recording has stopped streaming");
        }
    }

    //convert short to byte
    private byte[] short2byte(short[] sData)
    {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++)
        {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }


    private static int[] mSampleRates = new int[]{8000, 11025, 22050, 44100};

    public AudioRecord findAudioRecord()
    {
        for (int rate : mSampleRates)
        {
            for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT})
            {
                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO})
                {
                    try
                    {
                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE)
                        {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                            {
                                return recorder;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, rate + "Exception, keep trying.", e);
                    }
                }
            }
        }
        return null;
    }
}
