package com.example.tts_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String LOG_TAG = "/TAG/"+MainActivity.class.getSimpleName();

    EditText editText;
    SeekBar seekBar_speed, seekBar_pitch;
    TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.input_text);
        seekBar_speed = findViewById(R.id.seekbar_speed);
        seekBar_pitch = findViewById(R.id.seekbar_pitch);


        // ------------------------------------------------------
        // Text to Speech
        // ------------------------------------------------------

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i){
                if(i==TextToSpeech.SUCCESS){

                    Spinner spin = (Spinner) findViewById(R.id.spinner1);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, getLanguages(textToSpeech));

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spin.setAdapter(adapter);
                    spin.setOnItemSelectedListener(MainActivity.this);
                }
            }
        });


        addListenerOnButton();
    }


    // ------------------------------------------------------
    // Languages in the Dropdown Menu
    // ------------------------------------------------------

    public static List<String> getLanguages( TextToSpeech tts) {
        Locale[] locales = Locale.getAvailableLocales();
        List<String> list = new ArrayList<>();

        for (Locale locale : locales) {
            int res = tts.isLanguageAvailable(locale);
            if (res == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                if (locale.getLanguage().length() == 2) {
                    if (!isLanguageInList(list, locale)) {
                        list.add(locale.getCountry());
                    }
                }
            }
        }
        Collections.sort(list);
        list.add(0," ");
        return list;
    }

    private static boolean isLanguageInList(List<String> list, Locale locale) { // per i doppioni
        if (list == null) {
            return false;
        }
        for (String item: list) {
            if (item.equalsIgnoreCase(locale.getDisplayLanguage())){
                return true;
            }
        }
        return false;
    }


    // ------------------------------------------------------
    // Dropdown Menu
    // ------------------------------------------------------

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        String selected = parent.getItemAtPosition(position).toString();
        selected = selected.toUpperCase();

        if (parent.getItemAtPosition(position).equals(" ")){
        }else {

            int lang = textToSpeech.setLanguage(Locale.forLanguageTag(selected));

            if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getApplicationContext(), selected + " language not supported", Toast.LENGTH_LONG).show();
                Log.d("TAG", selected + " Language not supported");
            } else {
                Toast.makeText(getApplicationContext(), selected + " language supported", Toast.LENGTH_LONG).show();
                Log.d("TAG", selected + " Language supported");
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        textToSpeech.setLanguage(Locale.getDefault());
        Log.d("TAG", "Default lang: "+Locale.getDefault());
    }


    // ------------------------------------------------------
    // Speak  button
    // ------------------------------------------------------

    public void addListenerOnButton() {
        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                String data = editText.getText().toString();

                float speed = (float) seekBar_speed.getProgress()/50;
                if(speed<0.1) speed=0.1f;
                textToSpeech.setSpeechRate(speed);

                float pitch = (float) seekBar_pitch.getProgress()/50;
                if(pitch<0.1) pitch=0.1f;
                textToSpeech.setPitch(pitch);

                textToSpeech.speak(data, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }


    // ------------------------------------------------------
    // Three dots Menu and items
    // ------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_opt_1) { // export
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                ActivityCompat.requestPermissions(this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                saveToFile();
            }

            return true;
        }

        if (id == R.id.action_opt_2) { // share
            share_text();
            return true;
        }

        if (id == R.id.action_opt_3) { // clear text and stop audio
            editText.setText("");
            if(textToSpeech!=null)
                textToSpeech.stop();
            return true;
        }

        if (id == R.id.action_opt_4) { // help
            startSettings();
            return true;
        }

        if (id == R.id.action_opt_5) { // more languages
            installVoiceData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // ------------------------------------------------------
    // Functions
    // ------------------------------------------------------

    public void startSettings() {
        Intent i = new Intent(this, AppSettings.class);
        startActivity(i);
    }

    private void saveToFile() {
        Uri fileUri;
        String SAVE_FOLDER = "TextToSpeech_app";
        File folder = new File(Environment.getExternalStorageDirectory(),SAVE_FOLDER);
        if(!folder.exists())
        {
            folder.mkdir();
        }

        long now = System.currentTimeMillis();
        Date day = new Date(now);
        SimpleDateFormat getCurrentTime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "TTS_"+String.valueOf(getCurrentTime.format(day));

        File sd = Environment.getExternalStorageDirectory();
        String savePath = "//" + SAVE_FOLDER + "//" + fileName + ".wav";

        File fileTTS = new File(sd, savePath);

        EditText text = findViewById(R.id.input_text);
        String textToConvert = text.getText().toString();

        HashMap<String, String> myHashRender = new HashMap();
        myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, textToConvert);
        Log.d(LOG_TAG, "successfully created hashmap");

        //String envPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download";
       // String destFileName = envPath + "/" + "tts_file.wav";

        int sr = textToSpeech.synthesizeToFile(textToConvert, myHashRender, fileTTS.toString());
        Log.d(LOG_TAG, "synthesize returns = " + sr);

        if (fileTTS.exists()) {
            Log.d(LOG_TAG, "successfully created fileTTS");
            Toast.makeText(getApplicationContext(), "Successfully created \n"+Uri.fromFile(fileTTS), Toast.LENGTH_LONG).show();
        }
        else {
            Log.d(LOG_TAG, "failed while creating fileTTS");
            Toast.makeText(getApplicationContext(), "Failed while creating", Toast.LENGTH_SHORT).show();
        }

        fileUri = Uri.fromFile(fileTTS);
        Log.d(LOG_TAG, "successfully created uri link: " + fileUri.getPath());

    }

    private  void  share_text(){
        EditText text = findViewById(R.id.input_text);
        Intent intent = new Intent(android.content.Intent.ACTION_SEND); /*Create an ACTION_SEND Intent*/
        String shareBody = text.getText().toString(); /*This will be the actual content you wish you share.*/
        intent.setType("text/plain"); /*The type of the content is text, obviously.*/

       // intent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        intent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody); /*Applying information Subject and Body.*/

        startActivity(Intent.createChooser(intent, "Share using... "));
    }

    private void installVoiceData() {
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.google.android.tts"/*replace with the package name of the target TTS engine*/);
        try {
            Log.v(LOG_TAG, "Installing voice data: " + intent.toUri(0));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(LOG_TAG, "Failed to install TTS data, no activity found for " + intent + ")");
        }
    }

    // ------------------------------------------------------
    // Activity States
    // ------------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(LOG_TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "OnPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(textToSpeech!=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        Log.d(LOG_TAG, "onDestroy");
    }

}