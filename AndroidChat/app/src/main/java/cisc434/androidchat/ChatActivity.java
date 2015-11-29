package cisc434.androidchat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

class ListUsersTask extends AsyncTask<Void, Void, Boolean> {
    private final M.Recepient recepient;

    public ListUsersTask(M.Recepient recepient) {
        this.recepient = recepient;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        M.ListUsersReq msg = new M.ListUsersReq();
        msg.recepient = recepient;

        try {
            Conn.os.writeObject(msg);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean succeeded) {
        if (!succeeded) {
            // XXX: Handle the error
        }
    }
}

class SendMessageTask extends AsyncTask<Void, Void, Boolean> {
    private final M.Recepient recepient;
    private final String message;

    public SendMessageTask(M.Recepient recepient, String message) {
        this.recepient = recepient;
        this.message = message;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        M.SendMessage msg = new M.SendMessage();
        msg.recepient = recepient;
        msg.body = message;

        try {
            Conn.os.writeObject(msg);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean succeeded) {
        if (!succeeded) {
            // XXX: Handle the error
        }
    }
}

public class ChatActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);

        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        EditText edit_txt = (EditText) findViewById(R.id.txtMessage);

        edit_txt.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != EditorInfo.IME_ACTION_UNSPECIFIED &&
                        actionId != EditorInfo.IME_ACTION_SEND) {
                    return false;
                }
                String text = v.getText().toString();
                if (text.equals("")) {
                    return false;
                }
                new SendMessageTask(Conn.recepient, text).execute();

                Log.w("chatApp", v.getText().toString());
                v.setText("");
                return true;
            }
        });

        Conn.recepient = new M.ChannelRecepient("general");
        Conn.getRoom(this, Conn.recepient);
        Conn.startConnThread(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Conn.clear();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
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

        if (id == R.id.action_users) {
            new ListUsersTask(Conn.recepient).execute();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        String title = item.getTitle().toString();

        if (title.equals("Join room...")) {
            final EditText et = new EditText(this);
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            new AlertDialog.Builder(this)
                    .setTitle("Enter room name")
                    .setView(et)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            M.Recepient recipient =
                                    M.RecepientFactory.parse(et.getText().toString());
                            Conn.getRoom(ChatActivity.this, recipient);
                            Conn.recepient = recipient;
                            updateMessages();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();
        } else {
            M.Recepient recipient = M.RecepientFactory.parse(title);
            ChatRoom room = Conn.getRoom(this, recipient);
            Conn.recepient = recipient;
            updateMessages();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void updateMessages() {
        // XXX: Implement
        ChatRoom room = Conn.getRoom(this, Conn.recepient);
        List<M.RcvMessage> messages = room.getMessages();

        StringBuilder builder = new StringBuilder();
        for (M.RcvMessage message : messages) {
            builder.append("<");
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
            builder.append(sdf.format(message.when));
            // builder.append(message.when.toString());
            builder.append("> ");
            builder.append(message.username);
            builder.append(": ");
            builder.append(message.body);
            builder.append("\n");
        }
        TextView text = (TextView) findViewById(R.id.messages);
        text.setText(builder.toString());
    }

    public void listUsers(M.ListUsers lu) {
        StringBuilder builder = new StringBuilder();
        if (!lu.recepient.equals(Conn.recepient)) {
            return;
        }
        for (String name : lu.users) {
            builder.append(name);
            builder.append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("User List")
                .setMessage(builder.toString())
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                }).show();
    }
}
