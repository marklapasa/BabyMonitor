package net.lapasa.babymonitor.subscriber;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;


public class MainActivity2 extends ActionBarActivity
{

    private static final int SERVERPORT = 6000;
    private static final String SERVER_IP = "192.168.1.35";
    private static int count = 1;
    private Thread networkThread;
    private SubscriberRunnable subscriberRunnable;
    private TextView console;
    private ToggleButton connectToggleBtn;
    private SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
    private Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscriber_dashboard);

        console = (TextView) findViewById(R.id.console);
        console.setMovementMethod(new ScrollingMovementMethod());

        String ipAddress = getIPAddress();
        TextView ipAddressTextView = (TextView) findViewById(R.id.ipaddress);
        ipAddressTextView.setText(ipAddress);

        connectToggleBtn = (ToggleButton) findViewById(R.id.connectToggleBtn);
        connectToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    startConnection();
                }
                else
                {
                    stopConnection();
                }
            }
        });

    }

    private void startConnection()
    {
        subscriberRunnable = new SubscriberRunnable();
        networkThread = new Thread(subscriberRunnable);
        networkThread.start();
    }

    private void stopConnection()
    {
        console.setText("");
        networkThread.interrupt();
        subscriberRunnable.close();
    }

    class DataInputStreamMonitorRunnable implements Runnable
    {
        private final BufferedInputStream inputStream;
        private Socket socket;
        private byte[] byteBuffer = new byte[1024 * 8];
        private AudioTrack track;

        DataInputStreamMonitorRunnable(Socket socket) throws IOException
        {
            this.socket = socket;
//            inputStream = new DataInputStream(socket.getInputStream());
            inputStream = new BufferedInputStream(socket.getInputStream());
        }

        @Override
        public void run()
        {
            int id = 0;
            int N = AudioRecord.getMinBufferSize(11025, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, 11025,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10, AudioTrack.MODE_STREAM);
            track.play();


            consoleMsg("Now listening for a response from the server...");
            while (socket != null)
            {
                try
                {
                    int read = inputStream.read(byteBuffer);
//                    String str = new String(byteBuffer, "UTF-8");
//                    consoleMsg(str);

                    track.write(byteBuffer, 0, byteBuffer.length);
//                    consoleMsg(Arrays.toString(byteBuffer));
                    consoleMsg(id++ + " | " + String.valueOf(byteBuffer.length));

                }
                catch (IOException e)
                {
                    consoleMsg("Failed to read data from Input Stream");
                    stopConnection();
                }
            }
            consoleMsg("...stopped listening to the server.");
        }
    }


    class SubscriberRunnable implements Runnable
    {
        private Socket socket;


        @Override
        public void run()
        {
            try
            {
                String helloStr = "Subscriber is sending HANDSHAKE";
                consoleMsg("Connecting to " + SERVER_IP + ":" + SERVERPORT);
                socket = new Socket(SERVER_IP, SERVERPORT);

                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
//                BufferedWriter writer = new BufferedWriter(outputStreamWriter);
                outputStreamWriter.write(helloStr);
                outputStreamWriter.flush();
                consoleMsg(helloStr);


                new Thread(new DataInputStreamMonitorRunnable(socket)).start();
            }
            catch (UnknownHostException e1)
            {
                e1.printStackTrace();
            }
            catch(EOFException eof)
            {
                consoleMsg("Download Complete");
            }
            catch (SocketException e2)
            {
                consoleMsg("Client disconnected");
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        connectToggleBtn.setChecked(false);
                    }
                });

            }
            catch (IOException e1)
            {
                consoleMsg("Failed to get OutputStream from socket");
            }

        }

        public void close()
        {
            if (socket != null)
            {
                try
                {
                    socket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            consoleMsg("Disconnected from " + SERVER_IP + ":" + SERVERPORT);
        }

    }


    /**
     * Show message in debug console
     * @param msg
     */
    private void consoleMsg(String msg)
    {
        handler.post(new UpdateUIRunnable(msg));
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
                console.scrollTo(0, console.getLineCount());
            else
                console.scrollTo(0, 0);
        }
    }

    private String getIPAddress()
    {

        String ip = "";

        try
        {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements())
            {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();

                while (enumInetAddress.hasMoreElements())
                {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress())
                    {
                        ip += "SiteLocalAddress: " + inetAddress.getHostAddress() + "\n";
                    }
                }
            }
        }
        catch (SocketException e)
        {
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        ip = ip.substring(0, ip.lastIndexOf("\n"));

        return ip;
    }

    /*class AudioPlaybackRunnable implements Runnable
    {

        private final DataInputStream inputStream;
        private AudioTrack track;
        short[] buffer  = new short[1]; // byte is 8-bit integer (-128 to +127), short 16-bit integer (-32,768 to 32,767)
        int i = 0;
        private boolean isPlaying = true;

        public AudioPlaybackRunnable(DataInputStream inputStream)
        {
            this.inputStream = inputStream;
        }

        @Override
        public void run()
        {
            int N = AudioRecord.getMinBufferSize(11025, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, 11025,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10, AudioTrack.MODE_STREAM);
            track.play();

            while(isPlaying)
            {
                try
                {
                    buffer[0] = inputStream.readShort();
                    track.write(buffer, 0, 1);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    isPlaying = false;
                }
            }

            track.stop();
            track.release();
        }
    }*/

}
