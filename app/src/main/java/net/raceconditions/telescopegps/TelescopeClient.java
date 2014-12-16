package net.raceconditions.telescopegps;

/**
 * Created by ubuntu on 9/16/14.
 */
public interface TelescopeClient {
    void sendMessage(byte[] message);
    void stopClient();
    void startClient();
}
