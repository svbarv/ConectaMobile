package com.example.evaluacionnacional;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

public class MqttManager {

    private static final String TAG = "MqttManager";
    private MqttClient client;
    private String brokerUrl = "tcp://broker.hivemq.com:1883"; // Broker MQTT
    private String clientId = MqttClient.generateClientId();
    private MqttConnectOptions options;
    private Context context;

    public MqttManager(Context context) {
        this.context = context;
    }

    // Método para conectar al broker MQTT
    public void connect() {
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            options = new MqttConnectOptions();
            options.setCleanSession(false); // No usar sesión limpia
            options.setConnectionTimeout(10); // Tiempo de espera para la conexión (en segundos)
            options.setKeepAliveInterval(60); // Intervalo para mantener la conexión viva (en segundos)

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "Conexión perdida: " + cause.getMessage());
                    // Si se pierde la conexión, puedes intentar reconectar
                    reconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Procesa el mensaje recibido
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Mensaje entregado con éxito: " + token.getMessageId());
                }
            });

            client.connect(options);  // Intentar conectar al broker
            Log.d(TAG, "Conectado al broker MQTT");

        } catch (Exception e) {
            Log.e(TAG, "Error al conectar con el broker MQTT", e);
        }
    }

    // Método para desconectar del broker MQTT
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                Log.d(TAG, "Desconectado del broker");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al desconectar del broker", e);
        }
    }

    // Método para suscribirse a un tópico
    public void subscribeToTopic(String topic) {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(topic);
                Log.d(TAG, "Suscrito al tópico: " + topic);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al suscribirse al tópico " + topic, e);
        }
    }

    // Método para publicar un mensaje
    public void publishMessage(String topic, String messageContent, String senderEmail, String timestamp) {
        try {
            if (client != null && client.isConnected()) {  // Verificar si el cliente está conectado
                JSONObject message = new JSONObject();
                message.put("message", messageContent);
                message.put("sender", senderEmail);
                message.put("timestamp", timestamp);

                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(message.toString().getBytes());

                Log.d(TAG, "Publicando mensaje: " + message.toString());
                client.publish(topic, mqttMessage);  // Publicar mensaje
            } else {
                Log.e(TAG, "El cliente no está conectado. Intentando reconectar...");
                reconnect();  // Intentar reconectar
                publishMessage(topic, messageContent, senderEmail, timestamp);  // Reintentar publicar el mensaje
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al enviar el mensaje", e);
        }
    }


    // Método para intentar reconectar automáticamente si la conexión se pierde
    private void reconnect() {
        if (client != null && !client.isConnected()) {
            try {
                Log.d(TAG, "Intentando reconectar...");
                client.connect(options);  // Intentar reconectar al broker
                Log.d(TAG, "Reconectado al broker MQTT");
            } catch (Exception e) {
                Log.e(TAG, "Error al reconectar con el broker MQTT", e);
            }
        }
    }

    // Método para configurar el callback que maneja los mensajes entrantes
    public void setCallback(MqttCallback callback) {
        if (client != null) {
            client.setCallback(callback);
        }
    }

    // Callback para manejar la recepción de mensajes
    public class DefaultMqttCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            Log.d(TAG, "Conexión perdida: " + cause.getMessage());
            reconnect();  // Intentar reconectar cuando la conexión se pierde
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String messageContent = new String(message.getPayload());
            Log.d(TAG, "Mensaje recibido en el tópico " + topic + ": " + messageContent);

            // Aquí puedes agregar código para manejar los mensajes recibidos
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "Mensaje entregado con éxito: " + token.getMessageId());
        }
    }
}
