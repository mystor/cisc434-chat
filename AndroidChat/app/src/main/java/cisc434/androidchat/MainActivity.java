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
import android.text.TextUtils;
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

    public LoginTask(Activity activity, String server) {
        this.activity = activity;
        this.server = server;
    }

    @Override
    protected Boolean doInBackground(M.Login... logins) {
        if (logins.length != 1) {
            return false;
        }

        M.Login login = logins[0];

        Log.i(TAG, "Connecting to " + server + ":" + 9095);
        try {
            Conn.s = new Socket(server, 9095);
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
                throw new Exception("Something went very wrong.");
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Chat Master 3000");
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
        boolean inputError = false;
        EditText username = (EditText) findViewById(R.id.txtUsername);
        String un = username.getText().toString();
        if (TextUtils.isEmpty(un)){
            username.setError("Must enter a username");
            inputError = true;
        } else if (!un.matches("[A-Za-z0-9]+")){
            username.setError("Username must be alphanumeric with no spaces");
            inputError = true;
        }
        EditText password = (EditText) findViewById(R.id.txtPassword);
        String pass = password.getText().toString();
        if (TextUtils.isEmpty(pass)){
            password.setError("Must enter a password");
            inputError = true;
        }
        EditText server = (EditText) findViewById(R.id.txtServer);
        String srv = server.getText().toString();
        if (!srv.matches("[0-9]+.[0-9]+.[0-9]+.[0-9]+")){
            password.setError("Must enter a server address of the format <ip>");
            inputError = true;
        }

        if (inputError){
            return;
        }

        M.Login login = new M.Login();
        login.username = un;
        login.password = pass;
        Conn.username = un;

        new LoginTask(this, srv).execute(login);
    }

}
