package com.friendsapp.missedcallresponder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.aakira.expandablelayout.ExpandableLinearLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView textView;
    private ListView listView;
    private EditText editText;
    private ImageView expan1;
    private Switch aSwitch1;

    private SharedPreferences sharedPref;
    private List<String> list;
    private ArrayAdapter<String> arrayAdapter;
    private ExpandableLinearLayout content1;

    private boolean exp1, exp2;
    public static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            textView = findViewById(R.id.setsmsview);
            listView = findViewById(R.id.smslist);
            editText = findViewById(R.id.edittext);
            expan1 = findViewById(R.id.expansion);
            content1 = (ExpandableLinearLayout) findViewById(R.id.content);
            aSwitch1 = findViewById(R.id.switch1);

            sharedPref = getSharedPreferences("com.friendsapp.missedcallresponder.sp", Context.MODE_PRIVATE);

            list = new ArrayList<>();
            arrayAdapter = new ArrayAdapter<>(this, R.layout.listitem, list);

            exp1 = false;
            exp2 = false;

            initialisation();
            expansionpanel();
            switchchanges();
            smsPermissions();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }


    private void initialisation() {
        setdefaultsms();
        setlistview();
    }

    private void setdefaultsms() {

        String value = sharedPref.getString("setsmsstring", "empty");
        if (value.equals("empty")) {
            savemsg(getString(R.string.item1));
        } else
            displaysms(value);
    }

    private void savemsg(String s) {

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("setsmsstring", s);
        editor.apply();
        displaysms(s);
    }

    private void displaysms(String msg) {

        textView.setText(msg);
    }

    public void changesms(View view) {

        String msg = editText.getText().toString();
        if(!msg.equals("") && !list.contains(msg))
        {
            savemsg(msg);
            addtolist(msg);
        }
        else
        {
            Toast.makeText(this, getString(R.string.toast),Toast.LENGTH_LONG).show();
        }

    }

    private void setlistview() {

        int count = sharedPref.getInt("count", 0);
        if (count == 0) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("item1", getString(R.string.item1));
            editor.putString("item2", getString(R.string.item2));
            editor.putString("item3", getString(R.string.item3));
            editor.putString("item4", getString(R.string.item4));
            editor.putInt("count", 4);
            editor.putBoolean("servicestate", true);
            editor.apply();
        }
        displaylist();
    }

    private void displaylist() {

        for (int i = 1; i < 11; i++) {
            String value = sharedPref.getString("item" + i, "empty");
            if (!value.equals("empty")) {
                list.add(value);
            }
        }
        listView.setAdapter(arrayAdapter);
        listviewclickitem();
    }

    private void listviewclickitem() {

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String value = sharedPref.getString("item" + (position + 1), "empty");
                savemsg(value);
                editText.setText(value);
            }
        });
    }

    private void addtolist(String msg) {

        int count = sharedPref.getInt("count", 0);

        if(count == 10) count = 0;

        count++;
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("item"+count, msg);
        editor.putInt("count", count);
        editor.apply();

        if(list.size() >= count)
            list.set(count-1, msg);
        else
            list.add(msg);
        arrayAdapter.notifyDataSetChanged();
    }

    private void expansionpanel() {

        expan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                content1.toggle();
                if(exp1) {
                    expan1.setImageDrawable(getDrawable(R.drawable.expandown));
                    exp1 = false;
                }
                else {
                    expan1.setImageDrawable(getDrawable(R.drawable.expanup));
                    exp1 = true;
                }

            }
        });
    }

    private void switchchanges() {

        switchdata(true);
        aSwitch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switchdata(isChecked);
            }
        });

    }

    private void switchdata(boolean bool)
    {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("servicestate", bool);
        editor.apply();
    }

    public void clear(View view) {
        editText.getText().clear();
    }

    private void smsPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS,Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_SEND_SMS) {
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                   showPermissionDialog();
                }
        }
    }

    private void showPermissionDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Accept Permissions")
                .setMessage("Please accept the permissions, otherwise incoming call can't detected and sms will not send. Click Okay to accept")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        smsPermissions();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(R.drawable.notification)
                .show();
    }

}
