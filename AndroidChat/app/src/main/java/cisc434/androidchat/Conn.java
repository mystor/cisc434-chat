package cisc434.androidchat;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

/**
 * Created by mlayzell on 2015-11-28.
 */
public class Conn {
    public static Socket s = null;
    public static ObjectInputStream is = null;
    public static ObjectOutputStream os = null;

    public static HashMap<M.Recepient, ChatRoom> rooms = new HashMap<>();

    public static void clear() {
        if (s != null) {
            try {s.close();} catch (Exception e) {e.printStackTrace();}
        }
        s = null;
        is = null;
        os = null;
        rooms = new HashMap<>();
    }
}
