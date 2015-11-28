import java.io.*;
import java.net.*;
import java.util.*;
import cisc434.androidchat.M;

public class Client {
    public static void main(String[] args) throws Exception {
        System.out.println("A");
        Socket s = new Socket("127.0.0.1", 9095);
        System.out.println("B");
        ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
        M.Login login = new M.Login();
        login.username = "us";
        login.password = "pw";
        os.writeObject(login);

        System.console().readLine();
    }
}
