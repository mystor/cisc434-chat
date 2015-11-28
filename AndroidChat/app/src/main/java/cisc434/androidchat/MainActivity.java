package cisc434.androidchat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

class LoginTask extends AsyncTask<M.Login, Void, Boolean> {

    static String TAG = "chatApp";

    private Activity activity;
    private String server;
    private int port;

    public LoginTask(Activity activity, String server, int port) {
        this.activity = activity;
        this.server = server;
        this.port = port;
    }

    @Override
    protected Boolean doInBackground(M.Login... logins) {
        if (logins.length != 1) {
            return false;
        }

        M.Login login = logins[0];

        Log.i(TAG, "Connecting to " + server + ":" + port);
        try {
            Conn.s = new Socket(server, port);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            Conn.clear();
            return false;
        }

        try {
            Conn.is = new ObjectInputStream(Conn.s.getInputStream());
            Conn.os = new ObjectOutputStream(Conn.s.getOutputStream());

            Conn.os.writeObject(login);
            Object response = Conn.is.readObject();

            if (!(response instanceof M.LoggedIn)) {
                throw new Exception("SHIT");
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            Conn.clear();
            return false;
        }

        Log.w(TAG, "Logged In");
        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            Intent intent = new Intent(activity, ChatActivity.class);
            activity.startActivity(intent);
        } else {
            Context context = activity.getApplicationContext();
            CharSequence text = "Login failed...";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

}


public class MainActivity extends AppCompatActivity {

    static String TAG = "chatApp";

    Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {
            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }

    public void sayHello(View v){
        EditText username = (EditText) findViewById(R.id.txtUsername);
        String un = username.getText().toString();
        EditText password = (EditText) findViewById(R.id.txtPassword);
        String pass = password.getText().toString();
        EditText server = (EditText) findViewById(R.id.txtServer);
        String srv = server.getText().toString();

        String[] parts = srv.split(":");

        if (parts.length != 2) {
            Log.w(TAG, "The server parts were wrong! What's wrong with you evil person!");
        }

        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);

        M.Login login = new M.Login();
        login.username = un;
        login.password = pass;

        new LoginTask(this, ip, port).execute(login);
    }


}
