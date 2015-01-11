package net.lapasa.babymonitor.publisher.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;

public class PublisherThread extends Thread implements IPublisherService
{
    private static final String TAG = PublisherThread.class.getName();
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private OutputStreamWriter outStreamWriter;
    private InputStreamReader inStreamWriter;

    private boolean isBroadcasting;
    private Context context;
    private Thread listenerThread;

    public PublisherThread(Context context)
    {
        this.context = context;
    }

    @Override
    public void run()
    {
        try
        {
            serverSocket = new ServerSocket(0);
            clientSocket = serverSocket.accept();

            outStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream());
            inStreamWriter = new InputStreamReader(clientSocket.getInputStream());

            isBroadcasting = true;
            while (isBroadcasting)
            {
                Thread.sleep(1000);
                Date d = new Date();
                String str = d.toString();
                Log.d(TAG, str);
                outStreamWriter.append(str);
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        Log.d(TAG, "Publishing Started");
    }

    @Override
    public void startPublishing()
    {
        start();
        run();
    }

    @Override
    public void stopPublishing()
    {
        isBroadcasting = false;
        Log.d(TAG, "Publishing Stopped");
    }

    public String getIPAddress()
    {
/*        if (serverSocket != null)
        {
            return "IP Address: " + serverSocket.getInetAddress().getHostAddress();
        }
        else
        {
            return "IP Address: Not available";
        }*/

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

        return ip;
    }

    @Override
    public boolean isWifiEnabled()
    {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }
}
