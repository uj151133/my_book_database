package com.example.bookdatabase;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SearchTask extends AsyncTask<String, Void, String> {
    private Listener listener;


    protected String doInBackground(String... params) {
        // サーバとの通信処理を記述する
        String result = "[]";
        try {
            URL url = new URL("http://www.cm.is.ritsumei.ac.jp/cgi-bin/saproglab/booksearch_json.py");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setDoOutput(true);
                OutputStream out = urlConnection.getOutputStream();
                String query = "query=" + params[0];	// query=Androidなど
                try {
                    out.write(query.getBytes("UTF-8"));
                    out.flush();
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    out.close();
                }
                InputStream in = urlConnection.getInputStream();
                try {
                    StringBuffer buffer = new StringBuffer();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
                    String str;
                    while((str = reader.readLine()) != null) {
                        buffer.append(str);
                    }
                    result = buffer.toString();
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    in.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        // サーバとの通信が終了したら，画面を更新する
        if (listener != null) {
            listener.onSuccess(result);
        }
    }

    // 画面更新処理を登録するためのメソッド
    void setListener(Listener listener) {
        this.listener = listener;
    }

    // 画面更新処理を呼び出すためのインタフェース
    interface Listener {
        void onSuccess(String result);
    }


}