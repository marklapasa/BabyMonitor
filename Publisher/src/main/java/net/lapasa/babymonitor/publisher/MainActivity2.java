package net.lapasa.babymonitor.publisher;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;

/**
 * Notes - http://examples.javacodegeeks.com/android/core/socket-core/android-socket-example/
 */
public class MainActivity2 extends ActionBarActivity implements Handler.Callback
{
    public static final int SERVERPORT = 6000;
    public static final String PING = "PING";

    private Handler handler;
    private Thread serverThread;
    private TextView console;
    private ToggleButton serviceToggleBtn, pingToggleBtn;
    private SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
    private ServerSocketRunnable serverSocketRunnable;
    private boolean isPingEnabled;
    private Thread commThread;
    private BufferedWriter writerOutput;
    private FileDescriptor socketFileDescriptor;
    private AudioCapture audioCapture;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.publisher_dashboard);


        console = (TextView) findViewById(R.id.console);


        String ipAddress = getIPAddress();
        TextView ipAddressTextView = (TextView) findViewById(R.id.ipaddress);
        ipAddressTextView.setText(ipAddress);

        serviceToggleBtn = (ToggleButton) findViewById(R.id.serviceToggleBtn);
        serviceToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    startService();
                }
                else
                {
                    stopService();
                }
            }
        });

        pingToggleBtn = (ToggleButton) findViewById(R.id.pingToggleBtn);
        pingToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                pingEnabled(isChecked);
            }
        });

        handler = new Handler(this);

        ToggleButton recordToggleBtn = (ToggleButton) findViewById(R.id.recordToggle);
        audioCapture = new AudioCapture(this, recordToggleBtn, console);
    }

    @Override
    protected void onDestroy()
    {
        audioCapture.release();
    }


    /**
     * Write the time into the OutputStreamOut
     *
     * @param isChecked
     */
    private void pingEnabled(boolean isChecked)
    {
        isPingEnabled = isChecked;

        if (isPingEnabled && handler != null)
        {
            Message msg = handler.obtainMessage();
            msg.obj = PING;
            handler.sendMessage(msg);
        }
    }


    /**
     * Show message in debug console
     *
     * @param msg
     */
    private void consoleMsg(String msg)
    {
        if (handler != null)
        {
            handler.post(new UpdateUIRunnable(msg));
        }
    }

    private void startService()
    {
        serverSocketRunnable = new ServerSocketRunnable();
        serverThread = new Thread(serverSocketRunnable);
        consoleMsg("Service Started on port " + serverSocketRunnable.getPort());
        serverThread.start();
    }

    private void stopService()
    {
        console.setText("");
        if (serverThread != null)
        {
            serverSocketRunnable.close();
            serverThread.interrupt();
            serverThread = null;
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

    @Override
    protected void onStop()
    {
        super.onStop();
        if (serverSocketRunnable != null)
        {
            serverSocketRunnable.close();
        }
    }

    @Override
    public boolean handleMessage(Message msg)
    {
        if (msg.obj != null && writerOutput != null)
        {
            String codeStr = (String) msg.obj;
            if (codeStr.equals(PING) && isPingEnabled)
            {
                new PingRunnable(writerOutput).run();
                Message _msg = handler.obtainMessage();
                _msg.obj = PING;
                handler.sendMessageDelayed(_msg, 5000);
            }
        }
        return true;
    }

    class ServerSocketRunnable implements Runnable
    {
        public Socket socket = null;
        public ServerSocket serverSocket;


        public void run()
        {
            try
            {
                serverSocket = new ServerSocket(SERVERPORT);
                socket = serverSocket.accept();

                new Thread(new DataInputStreamMonitorRunnable(socket)).start();
                writerOutput = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        consoleMsg("Setting Socket address @ " + socket.toString());
                        audioCapture.setSocket(socket);
                        pingToggleBtn.setEnabled(true);
                    }
                });
            }
            catch (SocketException e2)
            {
                consoleMsg("Socket is now closed");
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        serviceToggleBtn.setChecked(false);
                        pingToggleBtn.setChecked(false);
                        pingToggleBtn.setEnabled(false);
                    }
                });
            }
            catch (IOException e)
            {
                consoleMsg("Failed to accept new connection");
                pingToggleBtn.setChecked(false);
                pingToggleBtn.setEnabled(false);
                serviceToggleBtn.setChecked(false);
            }
        }

        public String getPort()
        {
            return String.valueOf(SERVERPORT);
        }

        public void close()
        {
            try
            {
                if (socket != null)
                {
                    socket.close();
                }
                else if (serverSocket != null)
                {
                    serverSocket.close();
                }

                audioCapture.setSocket(null);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /*class CommunicationRunnable implements Runnable
    {

        // For sending bytes to subscribing clients
        private DataOutputStream dataOutput = null;

        // For sending strings to subscribing clients
        private BufferedWriter writerOutput = null;

        private Socket clientSocket = null;


        public CommunicationRunnable(Socket clientSocket)
        {

            this.clientSocket = clientSocket;

            try
            {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                this.dataOutput = new DataOutputStream(clientSocket.getOutputStream());
                this.writerOutput = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        public void run()
        {
            if (commThread != null && !commThread.isInterrupted())
            {
                try
                {
                    String str;
                    while ((str = input.readLine()) != null)
                    {

                        handler.post(new UpdateUIRunnable("BLAH DATA"));
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

    }*/


    class UpdateUIRunnable implements Runnable
    {
        private String msg;

        public UpdateUIRunnable(String str)
        {
            this.msg = str;
        }

        @Override
        public void run()
        {
            Calendar c = Calendar.getInstance();
            String str = fmt.format(c.getTime()) + ": " + msg + "\n";

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


    class PingRunnable implements Runnable
    {
        private final BufferedWriter writer;

        public PingRunnable(BufferedWriter writer)
        {
            this.writer = writer;
        }

        @Override
        public void run()
        {
            String pingMsg = "The time is " + fmt.format(Calendar.getInstance().getTime());

            try
            {
                writer.write(pingMsg);
                writer.flush();
            }
            catch (SocketException e2)
            {
                consoleMsg("Cannot Ping because client disconnected");
                pingToggleBtn.setChecked(false);
            }
            catch (IOException e)
            {
                consoleMsg("Cannot Ping because writer is not available");
                pingToggleBtn.setChecked(false);
            }
        }
    }

    class DataInputStreamMonitorRunnable implements Runnable
    {
        private final DataInputStream inputStream;
        private Socket socket;
        private byte[] byteBuffer = new byte[1024];

        DataInputStreamMonitorRunnable(Socket socket) throws IOException
        {
            this.socket = socket;
            inputStream = new DataInputStream(socket.getInputStream());
        }

        @Override
        public void run()
        {
            consoleMsg("Now listening for a response from the client...");
            synchronized (socket)
            {
                while (socket != null)
                {
                    try
                    {
                        inputStream.read(byteBuffer);
                        String str = new String(byteBuffer, "UTF-8");
                        if (str.contains("HANDSHAKE"))
                        {
                            str = "Publisher has received HANDSHAKE";
                            handler.post(new UpdateUIRunnable(str));
                        }
                    }
                    catch (IOException e)
                    {
                        consoleMsg("Failed to read data from Input Stream");
                        socket = null;
                    }
                }
            }
            consoleMsg("...stopped listening to the client.");
        }
    }

}