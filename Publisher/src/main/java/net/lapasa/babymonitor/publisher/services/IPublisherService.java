package net.lapasa.babymonitor.publisher.services;

public interface IPublisherService
{
    /**
     * Begin to listen for incoming connections from client subscribers
     */
    void startPublishing();

    /**
     * Stop listening for incoming connections; Disconnect connected clients
     */
    void stopPublishing();


    /**
     * Return IP Address
     */
    String getIPAddress();

    /**
     * Return true if connected to the internet via wifi
     */
    boolean isWifiEnabled();
}
