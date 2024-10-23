package com.example.suncloud;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private RadioGroup radioGroup;
    private RadioButton rbTemperature, rbHumidity;
    private Button btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radioGroup = findViewById(R.id.radioGroup);
        rbTemperature = findViewById(R.id.rbTemperature);
        rbHumidity = findViewById(R.id.rbHumidity);
        btnSend = findViewById(R.id.btnSend);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = "";
                if (rbTemperature.isChecked()) {
                    command = "T";
                } else if (rbHumidity.isChecked()) {
                    command = "H";
                }

                if (!command.isEmpty()) {
                    sendCommandToMicrocontroller(command);
                } else {
                    Toast.makeText(MainActivity.this, "Veuillez sélectionner une option", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendCommandToMicrocontroller(final String command) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    InetAddress serverAddress = InetAddress.getByName("192.168.1.100");
                    int port = 10000;

                    byte[] data = command.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
                    socket.send(packet);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Commande envoyée: " + command, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}