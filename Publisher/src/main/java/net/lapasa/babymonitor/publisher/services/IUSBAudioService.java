package net.lapasa.babymonitor.publisher.services;

public interface IUSBAudioService
{
    /**
     * Return true if a USB Microphone is connected
     */
    boolean isMicrophoneAvailable();

    /**
     * Begin listening for audio coming from microphone
     */
    void start();

    /**
     * Disconnect - Stop listening to audio data
     */
    void stop();

}
