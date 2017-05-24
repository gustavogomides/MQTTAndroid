package com.gomides.gustavo.mqttandroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MqttCallback {

    private TextView info;
    private EditText topico;
    private EditText mensagem;
    private Button enviar;
    private LinearLayout linearLayout;

    private MqttAndroidClient client;

    private ArrayList<String> topicosSubscribe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        info = (TextView) findViewById(R.id.lbl_info);
        topico = (EditText) findViewById(R.id.txt_topico);
        mensagem = (EditText) findViewById(R.id.txt_mensagem);
        enviar = (Button) findViewById(R.id.btn_enviar);
        linearLayout = (LinearLayout) findViewById(R.id.ll_mensagens);
    }

    @Override
    protected void onStart() {
        super.onStart();

        topicosSubscribe = new ArrayList<>();

        info.setText("Conectando...");
        Log.i("MQTTapp", "Conectando...");

        client = new MqttAndroidClient(this, "tcp://iot.eclipse.org:1883", "mqttTeste");

        if (!client.isConnected()) {
            try {
                MqttConnectOptions options = new MqttConnectOptions();

                // false -> subscribe durável quando o cliente sair ou reconectar
                options.setCleanSession(false);

                client.connect(options, new IMqttActionListener() {

                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i("MQTTapp", "Conectado");
                        info.setText("Conectado");

                        enviar.setEnabled(true);

                        // setando o callback para o recebimento das mensagens
                        client.setCallback(MainActivity.this);

                        enviar.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String strTopico = topico.getText().toString();
                                if (strTopico.equals("")) {
                                    Toast.makeText(MainActivity.this, "Digite um tópico!", Toast.LENGTH_SHORT).show();
                                } else {
                                    publish(strTopico);
                                }

                            }
                        });
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    }
                });

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * PUBLICAR UMA MENSAGEM
     */
    public void publish(final String strTopico) {
        MqttMessage message = new MqttMessage(mensagem.getText().toString().getBytes());
        message.setRetained(true);

        try {

            Log.i("MQTTapp", "Publicando '" + message.toString() + "' em " + strTopico);
            info.setText("Publicando '" + message.toString() + "' em " + strTopico);

            client.publish(strTopico, message).setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("MQTTapp", "Publicado '" + mensagem.getText().toString() + "' em " + strTopico);
                    info.setText("Publicado '" + mensagem.getText().toString() + "' em " + strTopico);

                    subscribe(strTopico);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i("MQTTapp", "Falha no Publish");
                    Log.i("MQTTapp", exception.toString());
                    info.setText("Falha no Publish");
                }
            });

        } catch (MqttException ex) {
            String errorText = "";
            for (StackTraceElement error : ex.getStackTrace())
                errorText += error.toString() + "\n";
            info.setText(errorText);
        }
    }

    /**
     * SUBSCRIBE EM UM TÓPICO
     *
     * @param strTopico
     */
    public void subscribe(final String strTopico) {
        if (isSubscribe(strTopico)) {
            Log.i("MQTTapp", "Subscribe no tópico '" + strTopico + "' já foi adicionado anteriormente!");
        } else {
            try {
                client.subscribe(strTopico, 2).setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i("MQTTapp", "Subscribe no tópico: " + strTopico);
                        topicosSubscribe.add(strTopico);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isSubscribe(String strTopico) {
        boolean retorno = false;
        for (String s : topicosSubscribe) {
            if (s.equals(strTopico)) {
                retorno = true;
                break;
            }
        }
        return retorno;
    }

    /**
     * DESCONECTAR QUANDO A ACTIVITY É ENCERRADA
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            client.disconnect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("MQTTapp", "Desconectado");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i("MQTTapp", "Falha Desconexão");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
    }

    /**
     * RECEBER UMA MENSAGEM - SUBSCRIBE
     *
     * @param topic   - topico da mensagem recebida
     * @param message - mensagem recebida
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        TextView tv = new TextView(this);
        tv.setTextColor(Color.BLACK);

        String str = (linearLayout.getChildCount() == 0)
                ? "Top: " + topic + " \n\t\t Msg: " + message.toString()
                : "\n\nTop: " + topic + " \n\t\t Msg: " + message.toString();
        tv.setText(str);

        linearLayout.addView(tv);

        Log.i("MQTTapp", "messageArrived Top: " + topic + " -> Msg: " + message.toString());

        gerarNotificacao(713, topic, message.toString());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    private void gerarNotificacao(int requestCode, String topico, String mensagem) {
        Context context = this;
        Log.i("MQTTapp", "gerarNotificação");
        NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        PendingIntent p = PendingIntent.getActivity(context, requestCode, new Intent(context, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(mensagem);
        builder.setContentText("Tópico: " + topico + " / Mensagem: " + mensagem);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(p);

        Notification n = builder.build();
        n.vibrate = new long[]{150, 300, 150, 600};
        n.flags = Notification.FLAG_AUTO_CANCEL;
        nm.notify(R.mipmap.ic_launcher, n);

        try {
            Uri som = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone toque = RingtoneManager.getRingtone(context, som);
            toque.play();
        } catch (Exception ignored) {
        }
    }
}
