package net.lapasa.babymonitor.publisher;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends ActionBarActivity
{
    TextView infoTextView, infoipTextView, msgTextView;
    String message = "";
    ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoTextView = (TextView) findViewById(R.id.info);
        msgTextView = (TextView) findViewById(R.id.msg);

        infoipTextView = (TextView) findViewById(R.id.infoip);
        infoipTextView.setText(getIpAddress());

        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }



    private String getIpAddress()
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
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    private class SocketServerThread extends Thread
    {

        static final int SocketServerPORT = 8080;
        int count = 0;

        @Override
        public void run()
        {
            try
            {
                serverSocket = new ServerSocket(SocketServerPORT);
                MainActivity.this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        infoTextView.setText("I'm waiting here: " + serverSocket.getLocalPort());
                    }
                });

                while (true)
                {
                    Socket socket = serverSocket.accept();
                    count++;
                    message += "#" + count + " from " + socket.getInetAddress() + ":" + socket.getPort() + "\n";

                    MainActivity.this.runOnUiThread(new Runnable()
                    {

                        @Override
                        public void run()
                        {
                            msgTextView.setText(message);
                        }
                    });

                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(socket, count);
                    socketServerReplyThread.run();

//                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

//                    while(true)
//                    {
//                        outputStreamWriter.write(count++);
//                    }

                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

    }

    private class SocketServerReplyThread extends Thread
    {

        private Socket hostThreadSocket;
        int cnt;

        SocketServerReplyThread(Socket socket, int c)
        {
            hostThreadSocket = socket;
            cnt = c;
        }

        @Override
        public void run()
        {
            OutputStream outputStream;
            String msgReply = "Hello from Android, you are #" + cnt;

            try
            {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msgReply);
//                printStream.close();

                message += "replayed: " + msgReply + "\n";

                MainActivity.this.runOnUiThread(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        msgTextView.setText(message);
                    }
                });

            }
            catch (IOException e)
            {
                e.printStackTrace();
                message += "Something wrong! " + e.toString() + "\n";
            }

            MainActivity.this.runOnUiThread(new Runnable()
            {

                @Override
                public void run()
                {
                    msgTextView.setText(message);
                }
            });
        }

    }
}
