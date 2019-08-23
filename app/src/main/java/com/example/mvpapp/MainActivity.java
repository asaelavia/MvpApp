package com.example.mvpapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText etUsername = findViewById(R.id.etUsername);
        final EditText etPassword = findViewById(R.id.etPassword);
        final Spinner langSpinner = findViewById(R.id.spLanguages);
        final Button btFinish = findViewById(R.id.btFinish);

        //Spinner settings
        String[] languages = new String[]{"English", "Hebrew"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        langSpinner.setAdapter(adapter);

        //Login button
        btFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lang = langSpinner.getSelectedItem().toString();
                final String username = etUsername.getText().toString() + "-" + lang;
                final String password = etPassword.getText().toString();
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            if (success) {
                                String name = jsonResponse.getString("name");
                                String listOfSites = jsonResponse.getString("listOfSites");
                                String sites_coordinates = jsonResponse.getString("sitesCoordinates");
                                String destination = jsonResponse.getString("destination");


                                Intent intent = new Intent(MainActivity.this, OverviewActivity.class);
                                intent.putExtra("name", name);
                                intent.putExtra("listOfSites", listOfSites);
                                intent.putExtra("sites_coordinates", sites_coordinates);
                                intent.putExtra("username", username);
                                intent.putExtra("destination", destination);
                                MainActivity.this.startActivity(intent);
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage("Login Failed")
                                        .setNegativeButton("Retry", null)
                                        .create()
                                        .show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                };

                LoginRequest loginRequest = new LoginRequest(username, password, responseListener);
                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                queue.add(loginRequest);
            }
        });

    }
}
