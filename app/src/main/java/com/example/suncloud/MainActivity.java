package com.example.suncloud;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etServerIP = findViewById(R.id.etServerIP);
        EditText etServerPort = findViewById(R.id.etServerPort);

        Spinner spinner1 = findViewById(R.id.spinner1);
        Spinner spinner2 = findViewById(R.id.spinner2);
        Spinner spinner3 = findViewById(R.id.spinner3);
        Spinner spinner4 = findViewById(R.id.spinner4);
        Spinner spinner5 = findViewById(R.id.spinner5);
        Spinner spinner6 = findViewById(R.id.spinner6);

        Button btnSendConfig = findViewById(R.id.btnSendConfig);
        btnSendConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String serverIP = etServerIP.getText().toString();
                String portString = etServerPort.getText().toString();

                if (serverIP.isEmpty() || portString.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Veuillez entrer une adresse IP et un port", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Adresse IP ou port manquant.");
                    return;
                }

                int port;
                try {
                    port = Integer.parseInt(portString);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Le port doit être un nombre", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Format de port incorrect.");
                    return;
                }


                String order = "" + spinner1.getSelectedItem().toString().charAt(0) +
                        spinner2.getSelectedItem().toString().charAt(0) +
                        spinner3.getSelectedItem().toString().charAt(0) +
                        spinner4.getSelectedItem().toString().charAt(0) +
                        spinner5.getSelectedItem().toString().charAt(0) +
                        spinner6.getSelectedItem().toString().charAt(0);

                Log.d(TAG, "L'ordre à envoyer : " + order);


                sendCommandToServer(serverIP, port, order);
            }
        });
    }

    private void sendCommandToServer(final String serverIP, final int port, final String command) {
        Log.d(TAG, "Envoi de la commande au serveur : " + serverIP + ":" + port);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    DatagramSocket socket = new DatagramSocket();
                    InetAddress serverAddress = InetAddress.getByName(serverIP);

                    byte[] data = command.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
                    socket.send(packet);

                    Log.d(TAG, "Commande envoyée avec succès : " + command);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Commande envoyée: " + command, Toast.LENGTH_SHORT).show();
                        }
                    });

                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de l'envoi de la commande", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Erreur lors de l'envoi de la commande", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}
