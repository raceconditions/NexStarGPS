package net.raceconditions.telescopegps;

/**
 * Created by ubuntu on 9/16/14.
 */
public interface ConnectionEventHandler {
    public void messageReceived(String message);
    public void connectionEstablished(TCPClient c);
    public void connectionFailed();
}
