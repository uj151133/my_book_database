package com.example.bookdatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.List;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.core.os.HandlerCompat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/*
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}*/
public class MainActivity extends AppCompatActivity {
    CompoundBarcodeView barcodeView;
    private static final String GoogleBooksAPI = "https://www.googleapis.com/books/v1/volumes?q=";
    private String lastResult;

    public void onSearchButtonClicked(View view) {
        Intent intent = new Intent(this, ResultActivity.class);
        EditText keywordText = findViewById(R.id.keywordText);
        intent.putExtra("QUERY", keywordText.getText().toString());
        startActivity(intent);
    }
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText isbn = findViewById(R.id.editIsbn);
        Button search = findViewById(R.id.search_button);
        search.setOnClickListener(v -> {
            try {
                if (isbn.getText().toString().length() != 13){
                    Toast.makeText(MainActivity.this, "適切な値を入れてください", Toast.LENGTH_LONG).show();
                } else{
                    BookSearch(isbn.getText().toString());
                }
            } catch (NullPointerException e){
                Log.d("Null Exception","EditText is null");
                Toast.makeText(MainActivity.this, "適切な値を入れてください", Toast.LENGTH_LONG).show();
            }
        });
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            String[] permissions = {Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 100);
            return;
        }
        CameraSetting();
        readBarcode();


    }

    private void CameraSetting(){
        barcodeView = findViewById(R.id.barcodeView);
        CameraSettings settings = barcodeView.getBarcodeView().getCameraSettings();
        barcodeView.getBarcodeView().setCameraSettings(settings);
        barcodeView.setStatusText("バーコードが読めます");
        barcodeView.resume();
        readBarcode();
    }
    private void readBarcode(){
        barcodeView.decodeContinuous(new BarcodeCallback() {
            final TextView getNumber = findViewById(R.id.getNumber);
            @Override
            public void barcodeResult(BarcodeResult result) {
                //このif文で、不必要な連続読みを防ぐ
                if (result.getText() == null || result.getText().equals(lastResult)){
                    return;
                }
                //このif文で、読み取られたバーコードがJANコードかどうか判定する
                if (result.getBarcodeFormat() != BarcodeFormat.EAN_13){
                    return;
                }
                lastResult = result.getText();
                Toast.makeText(MainActivity.this, "読み取りました", Toast.LENGTH_LONG).show();
                getNumber.setText(result.getText());
            }
            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                return;
            }
        }
        CameraSetting();
    }
    //メインスレッドでの動作を保証するアノテーション
    @UiThread
    private void BookSearch(final String Isbn) {
        String urlFull = GoogleBooksAPI + Isbn;
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);
        BookDataReceiver receiver = new BookDataReceiver(handler, urlFull);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(receiver);
        executorService.shutdown();
    }
    private class BookDataReceiver implements Runnable {
        private final Handler _handler;
        private final String _urlFull;
        private BookDataReceiver(Handler handler, String urlFull) {
            _handler = handler;
            _urlFull = urlFull;
        }
        //サブスレッドでの動作することを保証するアノテーション
        @WorkerThread
        @Override
        public void run() {
            //ここに非同期処理を記述している
            //HTTP通信するオブジェクトの作成
            HttpURLConnection connection = null;
            //連続的なデータを格納するオブジェクト
            InputStream stream = null;
            //取得したJSONデータを格納する
            String result = "";
            try {
                URL url = new URL(_urlFull);
                //接続用のオブジェクトを生成する
                connection = (HttpURLConnection) url.openConnection();
                //接続のタイムアウトを指定する
                connection.setConnectTimeout(1000);
                //値を取得するタイムアウトを時間を指定
                connection.setReadTimeout(1000);
                //値を送信する場合はPOST、取得する場合はGETを指定する
                connection.setRequestMethod("GET");
                //接続を行う
                connection.connect();
                //streamに取得した値を格納する
                stream = connection.getInputStream();
                //129行目に示したinputStream関数を用いてString型のデータに変換
                result = inputStream(stream);
            } catch (MalformedURLException e) {
                Toast.makeText(MainActivity.this,"URLの変換に失敗", Toast.LENGTH_LONG).show();
            } catch (SocketTimeoutException e) {
                Toast.makeText(MainActivity.this, "接続タイムアウト", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "通信できませんでした",Toast.LENGTH_LONG).show();
            } finally {
                if (connection != null){
                    //切断する
                    connection.disconnect();
                }
                if (stream != null){
                    try {
                        stream.close();
                    } catch (IOException e){
                        Toast.makeText(MainActivity.this, "解放失敗", Toast.LENGTH_LONG).show();
                    }
                }
            }
            JsonDecoder decoder = new JsonDecoder(result);
            _handler.post(decoder);
        }
        private String inputStream(InputStream stream) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuffer result = new StringBuffer();
            char[] buffer = new char[1024];
            int line;
            while (0 <= (line = reader.read(buffer))) {
                result.append(buffer, 0, line);
            }
            return result.toString();
        }
    }
    private class JsonDecoder implements Runnable {
        private final String _result;
        private JsonDecoder(String result) {
            _result = result;
        }
        @UiThread
        @Override
        public void run() {
            String title = "";
            String author = "";
            try {
                //JSONオブジェクトを生成
                JSONObject jsonObject = new JSONObject(_result);
                //取得したJSONデータから「items」と名づけられた配列を取得する
                JSONArray items = jsonObject.getJSONArray("items");
                //取得した「items」配列のゼロ番目の要素を取得
                JSONObject itemValue = items.getJSONObject(0);
                //さらに、itemValue内の「volumeInfo」と名付けられた配列を取得
                JSONObject info = itemValue.getJSONObject("volumeInfo");
                //タイトルを取得
                title = info.getString("title");
                //「authors」と名付けられた配列を取得
                JSONArray authors = info.getJSONArray("authors");
                //著者を取得
                author = authors.getString(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            TextView resultTitle = findViewById(R.id.resultTitle);
            TextView resultAuthor = findViewById(R.id.resultAuthor);
            resultTitle.setText("タイトル：" + title);
            resultAuthor.setText("著者：" + author);
        }
    }
}