package com.example.suncloud;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler; // Import pour le Handler
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
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

// Imports pour le JSON
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private SensorAdapter adapter;
    private ArrayList<String> sensorList;

    private DatagramSocket receiveSocket;
    private Thread receiveThread;
    private boolean isReceiving = false;

    private TextView tvDataDisplay;

    // Variables pour l'envoi périodique
    private Handler handler = new Handler();
    private Runnable sendRunnable;

    // Variables pour l'adresse IP et le port
    private String serverIP = "";
    private int serverPort = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        tvDataDisplay = findViewById(R.id.tvDataDisplay);

        // Trouver le bouton engrenage
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showServerConfigDialog();
            }
        });

        // Initialisation de la liste des capteurs
        sensorList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.sensor_array)));

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SensorAdapter(sensorList);
        recyclerView.setAdapter(adapter);

        // Configuration du drag-and-drop
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Configuration du Runnable pour l'envoi périodique
        sendRunnable = new Runnable() {
            @Override
            public void run() {
                if (serverIP.isEmpty() || serverPort == 0) {
                    Log.e(TAG, "Adresse IP ou port manquant pour l'envoi périodique.");
                    return;
                }

                // Le message à envoyer périodiquement
                String periodicMessage = "Requête de données";

                sendCommandToServer(serverIP, serverPort, periodicMessage);

                // Planifier le prochain envoi dans 5 secondes
                handler.postDelayed(this, 5000);
            }
        };

        Button btnSendConfig = findViewById(R.id.btnSendConfig);
        btnSendConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (serverIP.isEmpty() || serverPort == 0) {
                    Toast.makeText(MainActivity.this, "Veuillez configurer le serveur d'abord", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Démarrer le thread de réception si ce n'est pas déjà fait
                if (!isReceiving) {
                    startReceiving(serverPort);
                }

                // Construction de la commande avec la première lettre de chaque capteur
                StringBuilder orderBuilder = new StringBuilder();
                for (String sensor : adapter.getSensorList()) {
                    orderBuilder.append(sensor.charAt(0));
                }
                String order = orderBuilder.toString();

                Log.d(TAG, "L'ordre à envoyer : " + order);

                // Envoyer le message lors de la première connexion
                sendCommandToServer(serverIP, serverPort, order);

                // Démarrer l'envoi périodique
                handler.postDelayed(sendRunnable, 5000);
            }
        });
    }

    private void showServerConfigDialog() {
        // Créer un AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurer le serveur");

        // Inflater le layout du dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_server_config, null);
        builder.setView(dialogView);

        // Récupérer les EditText du dialog
        EditText etDialogServerIP = dialogView.findViewById(R.id.etDialogServerIP);
        EditText etDialogServerPort = dialogView.findViewById(R.id.etDialogServerPort);

        // Pré-remplir les champs si des valeurs existent
        etDialogServerIP.setText(serverIP);
        etDialogServerPort.setText(serverPort > 0 ? String.valueOf(serverPort) : "");

        // Boutons du dialog
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Récupérer les valeurs saisies
                String ipInput = etDialogServerIP.getText().toString();
                String portInput = etDialogServerPort.getText().toString();

                if (ipInput.isEmpty() || portInput.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Veuillez entrer une adresse IP et un port", Toast.LENGTH_SHORT).show();
                    return;
                }

                serverIP = ipInput;

                try {
                    serverPort = Integer.parseInt(portInput);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Le port doit être un nombre", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(MainActivity.this, "Configuration enregistrée", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", null);

        // Afficher le dialog
        AlertDialog dialog = builder.create();
        dialog.show();
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

            // Échanger les éléments dans les listes de l'adaptateur
            Collections.swap(adapter.getSensorList(), fromPosition, toPosition);
            Collections.swap(adapter.getSensorValues(), fromPosition, toPosition);
            Collections.swap(adapter.getSensorUnits(), fromPosition, toPosition);

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
                            // Toast.makeText(MainActivity.this, "Commande envoyée: " + command, Toast.LENGTH_SHORT).show();
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
                    byte[] buffer = new byte[4096];
                    while (isReceiving) {
                        Log.d(TAG, "En attente de données...");
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        receiveSocket.receive(packet);
                        Log.d(TAG, "Paquet reçu du serveur.");
                        String receivedData = new String(packet.getData(), 0, packet.getLength());

                        Log.d(TAG, "Données reçues du serveur : " + receivedData);

                        // Mettre à jour l'interface utilisateur avec les données reçues
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvDataDisplay.setText(receivedData);
                                Log.d(TAG, "Données affichées à l'utilisateur.");
                                parseAndDisplayData(receivedData);
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

    private void parseAndDisplayData(String data) {
        try {
            data = data.trim();

            // Parse le JSON reçu
            JSONObject jsonData = new JSONObject(data);
            ArrayList<String> sensorValues = new ArrayList<>();

            // Pour chaque capteur dans la liste actuelle
            for (String sensor : adapter.getSensorList()) {
                // Obtenir la clé correspondante dans le JSON
                String jsonKey = getJsonKeyForSensor(sensor);

                // Vérifier si la clé existe dans le JSON
                if (jsonData.has(jsonKey)) {
                    String value = jsonData.getString(jsonKey);
                    sensorValues.add(value);
                } else {
                    sensorValues.add("N/A"); // Valeur par défaut si le capteur n'est pas présent
                }
            }

            // Mettre à jour l'adaptateur avec les nouvelles valeurs
            adapter.updateSensorValues(sensorValues);
            Log.d(TAG, "Valeurs des capteurs mises à jour dans l'adaptateur.");
        } catch (JSONException e) {
            Log.e(TAG, "Erreur lors de l'analyse des données JSON", e);
            Toast.makeText(this, "Erreur : données reçues invalides.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getJsonKeyForSensor(String sensor) {
        switch (sensor.toLowerCase()) {
            case "température":
                return "temperature";
            case "humidité":
                return "humidite";
            case "pression":
                return "pression";
            case "luminosité":
            case "lumière":
                return "lumiere";
            case "uv":
                return "uv";
            case "infrarouge":
                return "ir";
            default:
                return sensor.toLowerCase();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopReceiving();

        // Arrêter le Handler pour éviter les fuites de mémoire
        if (handler != null && sendRunnable != null) {
            handler.removeCallbacks(sendRunnable);
            Log.d(TAG, "Envoi périodique arrêté.");
        }
    }

    private void stopReceiving() {
        isReceiving = false;
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
            Log.d(TAG, "Socket de réception fermée.");
        }
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
            Log.d(TAG, "Thread de réception interrompu.");
        }
    }
}
