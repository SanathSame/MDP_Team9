package mdp.group9.connection;

import java.io.*;
import java.net.Socket;

public class RPIConnector {
    private static String SERVER_IP = "192.168.9.9"; // 192.168.9.9 || 127.0.0.1
    private static int SERVER_PORT = 1234;

    Socket soc;
    DataOutputStream dataOut;
    DataInputStream dataIn;

    public RPIConnector() {}

    public boolean connect() {
        try {
            soc = new Socket(SERVER_IP, SERVER_PORT);
            dataOut = new DataOutputStream(soc.getOutputStream());
            dataIn = new DataInputStream(soc.getInputStream());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean disconnect() {
        try {
            soc.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean send(String message) {
        try {
            dataOut.writeUTF(message);
            System.out.println("  [Algo]: " + message.replace("\n", ""));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String receive() {
        try {
            String received = dataIn.readUTF();
            System.out.println("[Server]: " + received);
            return received;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
