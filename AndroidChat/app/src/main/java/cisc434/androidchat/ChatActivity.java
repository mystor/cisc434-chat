package cisc434.androidchat;

import android.app.AlertDialog;
import android.content.Context;
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
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

class LeaveRoomTask extends AsyncTask<Void, Void, Void> {

    private M.Recepient channel;

    public LeaveRoomTask(M.Recepient channel) {
        this.channel = channel;
    }

    @Override
    protected Void doInBackground(Void... params) {
        M.LeaveChannel msg = new M.LeaveChannel();
        msg.recepient = channel;
        try {
            Conn.os.writeObject(msg);
        } catch (IOException e) { /* ignore */ }
        return null;
    }
}


class DMUsersTask extends AsyncTask<Void, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Void... params) {
        M.DMUsersReq msg = new M.DMUsersReq();

        try {
            Conn.os.writeObject(msg);
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}

class AllChannelsTask extends AsyncTask<Void, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Void... params) {
        M.ListAllChannelsReq msg = new M.ListAllChannelsReq();

        try {
            Conn.os.writeObject(msg);
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}

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

        Conn.setRecepient(this, new M.ChannelRecepient("general"));
        Conn.startConnThread(this);
    }

    public void sendMessage(View view){
        EditText v = (EditText) findViewById(R.id.txtMessage);
        String text = v.getText().toString();
        if (text.equals("")) {
            return;
        }
        new SendMessageTask(Conn.getRecepient(), text).execute();

        Log.w("chatApp", v.getText().toString());
        v.setText("");
        return;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // Conn.clear();
            System.exit(0);
            // super.onBackPressed();
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
        } else if (id == R.id.action_users) {
            new ListUsersTask(Conn.getRecepient()).execute();
        } else if (id == R.id.action_all_channels) {
            new AllChannelsTask().execute();
        } else if (id == R.id.action_leave_channel){
            // TODO implement leave channel
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        String title = item.getTitle().toString();

        if (title.equals("Join channel...")) {
            final EditText et = new EditText(this);
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            new AlertDialog.Builder(this)
                    .setTitle("Enter channel name")
                    .setView(et)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            M.Recepient recipient =
                                    M.RecepientFactory.parseChannel(et.getText().toString());
                            if (recipient == null) {
                                Context context = ChatActivity.this.getApplicationContext();
                                CharSequence text = "Invalid channel format. Channels must be " +
                                        "alphanumeric, and start with a #";
                                int duration = Toast.LENGTH_LONG;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();
                                return;
                            }
                            Conn.setRecepient(ChatActivity.this, recipient);
                            updateMessages();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();
        } else if (title.equals("Direct Message...")) {
            new DMUsersTask().execute();
        } else {
            M.Recepient recipient = M.RecepientFactory.parse(title);
            Conn.setRecepient(this, recipient);
            updateMessages();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void updateMessages() {
        // XXX: Implement
        ChatRoom room = Conn.getRoom(this, Conn.getRecepient());
        List<M.RcvMessage> messages = room.getMessages();

        StringBuilder builder = new StringBuilder();
        for (M.RcvMessage message : messages) {
            builder.append("<");
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
            builder.append(sdf.format(message.when));
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
        if (!lu.recepient.equals(Conn.getRecepient())) {
            return;
        }
        for (String name : lu.users) {
            builder.append(name);
            builder.append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Users in Channel")
                .setMessage(builder.toString())
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                }).show();
    }

    public void listAllChannels(ArrayList<String> channels) {
        StringBuilder builder = new StringBuilder();
        for (String name : channels) {
            builder.append(name);
            builder.append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("All Channels")
                .setMessage(builder.toString())
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                }).show();
    }

    public void startDirectMessage(final ArrayList<String> users) {
        final ListView v = new ListView(this);
        v.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        v.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice,
                users));

        new AlertDialog.Builder(this)
                .setTitle("Direct Message")
                .setView(v)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SparseBooleanArray checked = v.getCheckedItemPositions();
                        ArrayList<String> selectedItems = new ArrayList<String>();
                        for (int i = 0; i < checked.size(); i++) {
                            // Item position in adapter
                            int position = checked.keyAt(i);
                            // Add sport if it is checked i.e.) == TRUE!
                            if (checked.valueAt(i))
                                selectedItems.add(users.get(position));
                        }

                        selectedItems.add(Conn.username);

                        Conn.setRecepient(ChatActivity.this,
                                new M.DMRecepient(new TreeSet<String>(selectedItems)));
                        updateMessages();
                    }
                })
                .show();
    }
}
