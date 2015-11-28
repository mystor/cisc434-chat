import java.io.*;
import java.net.*;
import java.util.*;

class User implements Runnable {
    public String name;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private Socket sock;
    private Server server;
    private ArrayList<Room> rooms;
    private boolean disconnected;

    public User(Server srvr, Socket s) throws IOException {
        os = new ObjectOutputStream(s.getOutputStream());
        is = new ObjectInputStream(s.getInputStream());
        sock = s;
        server = srvr;
        rooms = new ArrayList<Room>();
        disconnected = false;
    }

    public synchronized void send(Serializable o) {
        try {
            os.writeObject(o);
        } catch (Exception e) {
            System.err.println("There was a problem sending a message...");
            disconnect();
        }
    }

    private synchronized void disconnect() {
        server.disconnectUser(name);
        for (Room r : rooms) {
            r.removeUser(name);
        }
        rooms.clear();
        disconnected = true;
        try {
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Perform the login process. First we wait for an object from the client
        M.Login login;
        try {
            login = (M.Login)is.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            try {sock.close();} catch (Exception e2) {}
            return;
        }
        if (login == null) {
            System.err.println("There was a problem logging in");
            try {sock.close();} catch (Exception e) {}
            return;
        }

        name = login.username;
        // Attempt to login!
        if (!server.login(this, login.password)) {
            try {sock.close();} catch (Exception e) {}
            return;
        }

        // The login process is complete! Start the loop
        while (true) {
            if (disconnected) {
                return;
            }

            Object msg;
            try {
                msg = is.readObject();
            } catch (Exception e) {
                e.printStackTrace();
                msg = null;
            }

            M.JoinChannel jc = (M.JoinChannel)msg;
            if (jc != null) {
                Room r = server.joinChannel(jc.channel, this);
                if (r != null) {
                    rooms.add(r);
                }
                continue;
            }

            M.SendMessage sm = (M.SendMessage)msg;
            if (sm != null) {
                server.sendMessage(this, sm.recepient, sm.body);
                continue;
            }

            M.ListUsersReq lu = (M.ListUsersReq) msg;
            if (lu != null) {
                TreeSet<String> usersList = server.usersList(lu.channel);
                M.ListUsers response = new M.ListUsers();
                response.channel = lu.channel;
                response.users = usersList;
                send(response);
                continue;
            }

            System.err.println("Unrecognized message kind");
        }
    }
}

class Room {
    private M.Recepient recepient;
    private ArrayList<M.RcvMessage> messages;
    private TreeSet<String> users;
    private Server server;

    public Room(Server s, M.Recepient r) {
        recepient = r;
        messages = new ArrayList<M.RcvMessage>();
        server = s;

        M.DMRecepient dr = (M.DMRecepient)recepient;
        if (dr != null) {
            // Add all of the users to this room
            users = new TreeSet<String>(dr.recepients);
            // XXX: Do we want to tell users that they joined a group chat?
            for (String username : users) {
                sendMessage("system", new Date(), username + " has joined the channel");
            }
        } else {
            users = new TreeSet<String>();
        }
    }

    public synchronized boolean newUser(User user) {
        // Add the user to the channel
        users.add(user.name);

        // Send all previous messages to the user
        for (M.RcvMessage msg : messages) {
            user.send(msg);
        }

        sendMessage("system", new Date(), user.name + " has joined the channel");
        return true;
    }

    public synchronized void sendMessage(String user, Date date, String body) {
        M.RcvMessage rmsg = new M.RcvMessage();
        rmsg.recepient = recepient;
        rmsg.username = user;
        rmsg.when = date;
        rmsg.body = body;
        messages.add(rmsg);

        // Send the message to each user
        for (String username : users) {
            server.sendToUser(username, rmsg);
        }
    }

    public synchronized void removeUser(String user) {
        users.remove(user);
    }

    public synchronized TreeSet<String> usersList() {
        return new TreeSet<String>(users);
    }
}

class Server {
    private HashMap<String, User> users;
    private HashMap<M.Recepient, Room> chatrooms;

    public Server() {
        users = new HashMap<String, User>();
        chatrooms = new HashMap<M.Recepient, Room>();
    }

    public synchronized boolean login(User user, String password) {
        if (user.name.equals("system")) {
            System.err.println("Cannot log in as system");
            return false;
        }

        if (users.containsKey(user.name)) {
            System.err.println("There was a problem logging in, user already logged in");
            return false;
        }

        // XXX: Check the password
        users.put(user.name, user);
        return true;
    }

    public synchronized Room joinChannel(String channel, User user) {
        M.Recepient recepient = new M.ChannelRecepient(channel);
        Room room = chatrooms.get(recepient);
        if (room == null) {
            room = new Room(this, recepient);
            chatrooms.put(recepient, room);
        }

        if (room.newUser(user)) {
            return room;
        }
        return null; // The user is already in this room
    }

    public synchronized boolean sendMessage(User user, M.Recepient recepient, String message) {
        Date date = new Date();
        Room room = chatrooms.get(recepient);
        if (room == null) {
            room = new Room(this, recepient);
            chatrooms.put(recepient, room);
        }

        room.sendMessage(user.name, date, message);

        return true;
    }

    public synchronized TreeSet<String> usersList(String channel) {
        M.Recepient recepient = new M.ChannelRecepient(channel);
        Room room = chatrooms.get(recepient);
        if (room == null) {
            return new TreeSet<String>();
        }

        return room.usersList();
    }

    public synchronized boolean sendToUser(String username, Serializable msg) {
        User u = users.get(username);
        if (u == null) {
            return false;
        }

        u.send(msg);
        return true;
    }

    public synchronized void disconnectUser(String username) {
        users.remove(username);
    }

    public void run() throws IOException {
        ServerSocket listener = new ServerSocket(9000);

        try {
            while (true) {
                try {
                    Socket socket = listener.accept();
                    User user = new User(this, socket);
                    Thread t = new Thread(user);
                    t.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        finally {
            listener.close();
        }
    }

    public static void main(String[] args) throws IOException {
        new Server().run();
    }
}
