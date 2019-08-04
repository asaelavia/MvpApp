package com.example.mvpapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mvpapp.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etUsername = (EditText) findViewById(R.id.etUsername);
        EditText etPassword = (EditText) findViewById(R.id.etPassword);
        Spinner langSpinner = (Spinner) findViewById(R.id.spLanguages);
        Button btFinish = (Button) findViewById(R.id.btFinish);

        //Spinner settings
        String[] languages = new String[]{"English", "Hebrew"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        langSpinner.setAdapter(adapter);

        //Login button
        //btFinish.setOnClickListener();

    }
}
