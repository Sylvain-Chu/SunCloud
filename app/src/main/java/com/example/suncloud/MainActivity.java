package com.example.suncloud;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private SensorAdapter adapter;
    private ArrayList<String> sensorList;

    private DatagramSocket receiveSocket;
    private Thread receiveThread;
    private boolean isReceiving = false;

    private TextView tvDataDisplay;

    private EditText etServerIP;
    private EditText etServerPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        etServerIP = findViewById(R.id.etServerIP);
        etServerPort = findViewById(R.id.etServerPort);
        tvDataDisplay = findViewById(R.id.tvDataDisplay);

        // Initialisation de la liste des capteurs
        sensorList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.sensor_array)));

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SensorAdapter(sensorList);
        recyclerView.setAdapter(adapter);

        // Configuration du drag-and-drop
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

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

                // Démarrer le thread de réception si ce n'est pas déjà fait
                if (!isReceiving) {
                    startReceiving(port);
                }

                // Construction de la commande avec la première lettre de chaque capteur
                StringBuilder orderBuilder = new StringBuilder();
                for (String sensor : sensorList) {
                    orderBuilder.append(sensor.charAt(0));
                }
                String order = orderBuilder.toString();

                Log.d(TAG, "L'ordre à envoyer : " + order);

                sendCommandToServer(serverIP, port, order);
            }
        });
    }

    // Configuration du drag-and-drop
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

        @Override
        public boolean onMove(RecyclerView recyclerView,
                              RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {

            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            Collections.swap(sensorList, fromPosition, toPosition);
            adapter.notifyItemMoved(fromPosition, toPosition);

            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Pas d'action sur le swipe
        }
    };

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

    private void startReceiving(final int port) {
        Log.d(TAG, "Démarrage du thread de réception sur le port : " + port);
        isReceiving = true;
        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    receiveSocket = new DatagramSocket(port);
                    Log.d(TAG, "Socket de réception initialisée sur le port : " + port);
                    byte[] buffer = new byte[1024];
                    while (isReceiving) {
                        Log.d(TAG, "En attente de données...");
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        receiveSocket.receive(packet);
                        Log.d(TAG, "Paquet reçu du serveur.");
                        String receivedData = new String(packet.getData(), 0, packet.getLength());

                        Log.d(TAG, "Données reçues du serveur : " + receivedData);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvDataDisplay.setText(receivedData);
                                Log.d(TAG, "Données affichées à l'utilisateur.");
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la réception des données", e);
                } finally {
                    if (receiveSocket != null && !receiveSocket.isClosed()) {
                        receiveSocket.close();
                        Log.d(TAG, "Socket de réception fermée.");
                    }
                }
            }
        });
        receiveThread.start();
        Log.d(TAG, "Thread de réception démarré.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopReceiving();
    }

    private void stopReceiving() {
        isReceiving = false;
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
        }
    }
}
