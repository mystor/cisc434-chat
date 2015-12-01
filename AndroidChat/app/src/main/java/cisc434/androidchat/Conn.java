package cisc434.androidchat;

import android.os.AsyncTask;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.view.Menu;
import android.view.SubMenu;
import android.widget.Toast;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

class ConnThread implements Runnable {

    private final Socket socket;
    private final ObjectInputStream is;
    private final ChatActivity activity;

    public ConnThread(Socket socket, ObjectInputStream is, ChatActivity activity) {
        this.socket = socket;
        this.is = is;
        this.activity = activity;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object message = is.readObject();

                if (message instanceof M.RcvMessage) {
                    M.RcvMessage rm = (M.RcvMessage) message;
                    ChatRoom room = Conn.getRoom(activity, rm.recepient);
                    room.addMessage(rm);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.updateMessages();
                        }
                    });
                }

                if (message instanceof M.ListUsers) {
                    final M.ListUsers lu = (M.ListUsers) message;

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.listUsers(lu);
                        }
                    });
                }

                if (message instanceof M.ListAllChannels) {
                    final M.ListAllChannels lac = (M.ListAllChannels) message;

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.listAllChannels(lac.channels);
                        }
                    });
                }

                if (message instanceof M.DMUsers) {
                    final M.DMUsers du = (M.DMUsers) message;

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.startDirectMessage(du.users);
                        }
                    });

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e) {e.printStackTrace();}
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(activity, "Connection lost", Toast.LENGTH_LONG);
                    toast.show();
                    activity.finish();
                    System.exit(0);
                }
            });
        }
    }
}

public class Conn {
    public static Socket s = null;
    public static ObjectInputStream is = null;
    public static ObjectOutputStream os = null;

    private static M.Recepient recepient = null;
    private static HashMap<M.Recepient, ChatRoom> rooms = new HashMap<>();
    public static String username;

    public static synchronized M.Recepient getRecepient() {
        return recepient;
    }

    public static synchronized void setRecepient(final ChatActivity activity, final M.Recepient recepient) {
        Conn.recepient = recepient;
        getRoom(activity, recepient);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setTitle(recepient.toString());
            }
        });
    }

    public static synchronized ChatRoom getRoom(final ChatActivity activity, M.Recepient recepient) {
        if (!rooms.containsKey(recepient)) {
            rooms.put(recepient, new ChatRoom(recepient));

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NavigationView nv =
                            (NavigationView) activity.findViewById(R.id.nav_view);
                    Menu m = nv.getMenu();
                    m.clear();
                    m.add("Join channel...");
                    m.add("Direct Message...");
                    SubMenu sm = m.addSubMenu("Conversations");

                    ArrayList<M.Recepient> recepients = new ArrayList<>(rooms.keySet());
                    Collections.sort(recepients, new Comparator<M.Recepient>() {
                        @Override
                        public int compare(M.Recepient lhs, M.Recepient rhs) {
                            return lhs.toString().compareTo(rhs.toString());
                        }
                    });

                    for (M.Recepient room : recepients) {
                        sm.add(room.toString());
                    }
                }
            });

            M.JoinChannel req = new M.JoinChannel();
            req.recepient = recepient;
            new AsyncTask<M.JoinChannel, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(M.JoinChannel... params) {
                    try {
                        os.writeObject(params[0]);
                    } catch (Exception e) {
                        return false;
                    }
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean b) {
                    if (!b) {
                        Log.e("chatApp", "Channel Joined");
                    }
                }
            }.execute(req);
        }

        return rooms.get(recepient);
    }

    public static void startConnThread(ChatActivity activity) {
        new Thread(new ConnThread(s, is, activity)).start();
    }

    public static void clear() {
        if (s != null) {
            try {s.close();} catch (Exception e) {e.printStackTrace();}
        }
        s = null;
        is = null;
        os = null;
        recepient = null;
        rooms = new HashMap<>();
        username = null;

        // System.exit(0);
    }
}
