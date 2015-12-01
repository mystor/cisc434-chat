import java.io.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.ArrayList;
import java.util.TreeSet;

import cisc434.androidchat.M;

class User implements Runnable {
    public String name;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private Socket sock;
    private Server server;
    private boolean disconnected;

    public User(Server srvr, Socket s) throws IOException {
        os = new ObjectOutputStream(s.getOutputStream());
        is = new ObjectInputStream(s.getInputStream());
        sock = s;
        server = srvr;
        disconnected = false;
    }

    public synchronized void send(Serializable o) {
        try {
            os.writeObject(o);
        } catch (IOException e) {
            System.err.println("There was a problem sending a message...");
            disconnect();
        }
    }

    private synchronized void disconnect() {
        server.disconnectUser(name);
        disconnected = true;
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Perform the login process. First we wait for an object from the client
        Object message;
        try {
            message = is.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sock.close();
            } catch (IOException e2) {e2.printStackTrace();}
            return;
        }

        if (message instanceof M.Login) {
            M.Login login = (M.Login) message;

            name = login.username;
            // Attempt to login!
            if (!server.login(this, login.password)) {
                try {
                    sock.close();
                } catch (IOException e) {e.printStackTrace();}
                return;
            }

            // Tell the client that it successfully logged in!
            try {
                os.writeObject(new M.LoggedIn());
            } catch (IOException e) {
                e.printStackTrace();
                try {sock.close();} catch (IOException e2) {e.printStackTrace();}
                return;
            }
        } else {
            System.err.println("There was a problem logging in");
            try {sock.close();} catch (IOException e) {e.printStackTrace();}
            return;
        }

        server.sendAllBacklog(this);

        // The login process is complete! Start the loop
        while (true) {
            if (disconnected) {
                return;
            }

            Object msg;
            try {
                msg = is.readObject();
            } catch (Exception e) {
                disconnect();
                e.printStackTrace();
                msg = null;
                continue;
            }

            if (msg instanceof M.JoinChannel) {
                M.JoinChannel jc = (M.JoinChannel)msg;

                server.joinChannel(jc.recepient, this);
                continue;
            }

            if (msg instanceof M.SendMessage) {
                M.SendMessage sm = (M.SendMessage)msg;

                server.sendMessage(this, sm.recepient, sm.body);
                continue;
            }

            if (msg instanceof M.ListUsersReq) {
                M.ListUsersReq lu = (M.ListUsersReq) msg;

                TreeSet<String> usersList = server.usersList(lu.recepient);
                TreeSet<String> userRealList = new TreeSet<>();

                for (String user : usersList) {
                    if (!server.isOnline(user)) {
                        userRealList.add(user + " (offline)");
                    } else {
                        userRealList.add(user);
                    }
                }

                M.ListUsers response = new M.ListUsers();
                response.recepient = lu.recepient;
                response.users = userRealList;
                send(response);
                continue;
            }

            if (msg instanceof M.ListAllChannelsReq) {
                ArrayList<String> channels = server.allChannels();
                M.ListAllChannels response = new M.ListAllChannels();
                response.channels = channels;
                send(response);
                continue;
            }

            if (msg instanceof M.DMUsersReq) {
                TreeSet<String> users = server.allUsers();
                if (users.contains(name))
                    users.remove(name);
                M.DMUsers response = new M.DMUsers();
                response.users = new ArrayList<>(users);
                send(response);
                continue;
            }

            if (msg instanceof M.LeaveChannel) {
                M.LeaveChannel lc = (M.LeaveChannel)msg;
                if (!(lc.recepient instanceof M.ChannelRecepient)) {
                    continue;
                }
                server.leaveChannel(lc.recepient, this);

                M.LeftChannel response = new M.LeftChannel();
                response.recepient = lc.recepient;
                send(response);
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

        if (recepient instanceof M.DMRecepient) {
            M.DMRecepient dr = (M.DMRecepient)recepient;
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

    public synchronized TreeSet<String> getUsers() {
        return new TreeSet<>(users);
    }

    public synchronized boolean newUser(User user) {
        if (recepient instanceof M.DMRecepient) {
            return false;
        }

        if (users.contains(user.name)) {
            return false;
        }

        // Add the user to the channel
        users.add(user.name);

        // Send all previous messages to the user
        sendAllBacklog(user);

        sendMessage("system", new Date(), user.name + " has joined the channel");
        return true;
    }

    public synchronized void sendAllBacklog(User user) {
        for (M.RcvMessage msg : messages) {
            user.send(msg);
        }
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
        sendMessage("system", new Date(), user + " has left the channel");
    }

    public synchronized TreeSet<String> usersList() {
        return new TreeSet<String>(users);
    }
}

class Server {
    private HashMap<String, User> users;
    private Map<String, String> userPass = new HashMap<String, String>();
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

        // Check the password
        if (userPass.containsKey(user.name)){
            if (!password.equals(userPass.get(user.name))){
                System.err.println("Incorrect password for " + user.name);
                return false;
            }
        } else {
            userPass.put(user.name, password);
        }
        users.put(user.name, user);
        return true;
    }

    public synchronized Room joinChannel(M.Recepient recepient, User user) {
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

    public synchronized void leaveChannel(M.Recepient recepient, User user) {
        Room room = chatrooms.get(recepient);
        if (room == null) {
            return;
        }

        room.removeUser(user.name);
    }

    public synchronized boolean sendMessage(User user, M.Recepient recepient, String message) {
        System.out.println("Sent a message!");
        Date date = new Date();
        Room room = chatrooms.get(recepient);
        if (room == null) {
            room = new Room(this, recepient);
            chatrooms.put(recepient, room);
        }

        room.sendMessage(user.name, date, message);
        return true;
    }

    public synchronized TreeSet<String> usersList(M.Recepient recepient) {
        Room room = chatrooms.get(recepient);
        if (room == null) {
            return new TreeSet<String>();
        }

        return room.usersList();
    }

    public synchronized TreeSet<String> allUsers() {
        TreeSet<String> res = new TreeSet<>();
        for (String it : userPass.keySet()) {
            res.add(it);
        }
        return res;
    }

    public synchronized ArrayList<String> allChannels() {
        ArrayList<String> res = new ArrayList<>();
        for (M.Recepient it : chatrooms.keySet()) {
            if (it instanceof M.ChannelRecepient) {
                res.add(it.toString());
            }
        }
        return res;
    }

    public synchronized void sendAllBacklog(User user) {
        for (Room room : chatrooms.values()) {
            if (room.getUsers().contains(user.name)) {
                room.sendAllBacklog(user);
            }
        }
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

    public synchronized boolean isOnline(String user) {
        return users.containsKey(user);
    }

    public void run() throws IOException {
        ServerSocket listener = new ServerSocket(9095);

        System.out.println("Listener opened");

        try {
            while (true) {
                try {
                    System.out.println("HERE");
                    Socket socket = listener.accept();
                    System.out.println("Accepted connection");
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
