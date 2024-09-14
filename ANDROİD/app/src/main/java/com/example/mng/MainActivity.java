package com.example.mng;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    // Değişkenlerin tanımlanması
    private EditText ipAddressEditText;
    private EditText portEditText;
    private TextView statusTextView;
    private ImageView flameDetectedImageView;
    private ImageView flameNotDetectedImageView;
    private boolean isRunning = false;
    private Socket clientSocket;
    private BufferedReader bufferedReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View bileşenlerini başlatıyoruz
        initializeViews();

        // Status TextView'inde metin değişikliklerini dinliyoruz
        setupStatusTextWatcher();

        // Bağlan butonuna tıklama işlemini tanımlıyoruz
        setupConnectButton();
    }

    // Uygulama içerisindeki View bileşenlerini tanımlıyoruz
    private void initializeViews() {
        ipAddressEditText = findViewById(R.id.ipadres); // IP adresini aldığımız EditText
        portEditText = findViewById(R.id.PORT); // Port numarasını aldığımız EditText
        statusTextView = findViewById(R.id.durum); // Sunucudan gelen veriyi gösterecek TextView
        flameDetectedImageView = findViewById(R.id.imageView); // Alev algılandığında gösterilecek ImageView
        flameNotDetectedImageView = findViewById(R.id.imageView2); // Alev algılanmadığında gösterilecek ImageView
    }

    // Status TextView'de değişiklik olduğunda ImageView'leri günceller
    private void setupStatusTextWatcher() {
        statusTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                updateImageViewsBasedOnStatus(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    // Gelen JSON verisine göre ImageView'lerin görünürlüğünü ayarlayan fonksiyon
    private void updateImageViewsBasedOnStatus(String status) {
        try {

            JSONObject jsonObject = new JSONObject(status.trim()); // Boşlukları temizle
            String mutfakStatus = jsonObject.optString("mutfak", ""); // "gaz tespit edildi" veya "temiz hava"
            String flameStatus = jsonObject.optString("flame", ""); // "alev tespit edilmedi" veya "alev tespit edildi"

            // Duruma göre ImageView'lerin görünürlüğünü ayarla
            if ("gaz tespit edildi".equals(mutfakStatus) && "alev tespit edilmedi".equals(flameStatus)) {
                showFlameNotDetectedImage(); // Alev algılanmadığında ilgili resmi göster
            } else if ("temiz hava".equals(mutfakStatus) && "alev tespit edildi".equals(flameStatus)) {
                showFlameDetectedImage(); // Alev algılandığında ilgili resmi göster
            } else {
                hideBothImages(); // Diğer durumlarda her iki resmi de gizle
            }
        } catch (JSONException e) {
            Log.e("JSONError", "JSON parsing hatası: " + status, e); // JSON verisinde hata olursa log kaydını yazdır
        }
    }

    // Alev algılanmadığında ImageView2'yi gösterir, diğerini gizler
    private void showFlameNotDetectedImage() {
        runOnUiThread(() -> {
            flameNotDetectedImageView.setVisibility(View.VISIBLE);
            flameDetectedImageView.setVisibility(View.GONE);
        });
    }

    // Alev algılandığında ImageView1'i gösterir, diğerini gizler
    private void showFlameDetectedImage() {
        runOnUiThread(() -> {
            flameDetectedImageView.setVisibility(View.VISIBLE);
            flameNotDetectedImageView.setVisibility(View.GONE);
        });
    }

    // Her iki ImageView'i de gizler
    private void hideBothImages() {
        runOnUiThread(() -> {
            flameDetectedImageView.setVisibility(View.GONE);
            flameNotDetectedImageView.setVisibility(View.GONE);
        });
    }

    // Bağlan butonunun işlevini ayarlayan fonksiyon
    private void setupConnectButton() {
        findViewById(R.id.button).setOnClickListener(v -> {
            if (!isRunning) {
                isRunning = true;
                startConnectionThread();
            }
        });
    }


    private void startConnectionThread() {
        new Thread(() -> {
            while (isRunning) {
                connectToServer();
            }
        }).start();
    }

    // Sunucuya bağlantı kuran ve veriyi alan fonksiyon
    private void connectToServer() {
        try {
            createSocketConnection(); // Socket bağlantısı oluştur
            receiveDataFromServer(); // Sunucudan veriyi al
        } catch (IOException | InterruptedException e) {
            Log.e("ConnectionError", "Bağlantı hatası: ", e);
        } finally {
            closeResources(); // Bağlantı ve okuma kaynaklarını kapat
        }
    }

    // Socket bağlantısını oluşturan fonksiyon
    private void createSocketConnection() throws IOException {
        String ipAddress = ipAddressEditText.getText().toString(); // IP adresini al
        int port = Integer.parseInt(portEditText.getText().toString()); // Port numarasını al
        clientSocket = new Socket(ipAddress, port); // Socket bağlantısı oluştur
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); // Gelen veriyi okumak için BufferedReader oluştur
    }

    // Sunucudan gelen veriyi okuyan ve UI'ı güncelleyen fonksiyon
    private void receiveDataFromServer() throws IOException, InterruptedException {
        String receivedData = bufferedReader.readLine(); // Sunucudan gelen veriyi oku
        if (receivedData != null) {
            Log.d("ReceivedData", "Gelen veri: " + receivedData); // Gelen veriyi logla
            updateUIWithReceivedData(receivedData); // UI'ı güncelle
        }
        // Tekrar bağlanmadan önce 4 saniye bekle (Zamanlayıcı ile değiştirmek daha iyi olabilir)
        Thread.sleep(4000);
    }

    // UI'ı güncelleyen fonksiyon (ana thread'de çalışır)
    private void updateUIWithReceivedData(final String data) {
        runOnUiThread(() -> {
            statusTextView.setText(data); // Gelen veriyi TextView'de göster
            // TextView içeriği değiştiğinde ImageView'leri güncelle
            updateImageViewsBasedOnStatus(data);
        });
    }


    private void closeResources() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            Log.e("CloseError", "Kaynakları kapatırken hata: ", e);
        }
    }

    // Uygulama kapanırken veya durdurulurken çalışan fonksiyon
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false; // Döngüyü durdur
        closeResources(); // Kaynakları kapat
    }
}

