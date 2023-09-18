package com.example.bookdatabase;

import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.ArrayList;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ResultActivity extends AppCompatActivity {

    private class BookInfo {
        int ID;
        String title;
        String author;
        String publisher;
        int price;
        String isbn;
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // キーワード入力画面から入力されたキーワードを受け取る
        Intent intent = getIntent();
        String query = intent.getStringExtra("QUERY");

        // ListViewに表示するデータを格納するためのArrayList
        final ArrayList<HashMap<String, String>> listData = new ArrayList<>();

        // 非同期で実行するサーバとの通信処理を行うクラスのインスタンスを作成
        SearchTask task = new SearchTask();
        // サーバから応答が返ってきたときの処理を記述
        task.setListener(new SearchTask.Listener() {
            @Override
            public void onSuccess(String result) {
                //
                // ***サーバから受け取ったJSON形式の検索結果を処理をここに記載***
                final ArrayList<BookInfo> bookList = new ArrayList<BookInfo>();
                try {
                    JSONArray jsonArray = new JSONArray(result);
                    for(int i=0;i<jsonArray.length();i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        // ここにJSON形式からデータを取り出す処理を記述
                        final BookInfo bookInfo = new BookInfo();
                        bookInfo.ID        = jsonObject.getInt("ID");
                        bookInfo.title     = jsonObject.getString("TITLE");
                        bookInfo.author    = jsonObject.getString("AUTHOR");
                        bookInfo.publisher = jsonObject.getString("PUBLISHER");
                        bookInfo.price     = jsonObject.getInt("PRICE");
                        bookInfo.isbn      = jsonObject.getString("ISBN");

                        bookList.add(bookInfo);
                        listData.add(new HashMap() {
                            {put("title", bookInfo.title);}
                            {put("author", bookInfo.author);}
                        });
                    }

                } catch(JSONException e) {
                    e.printStackTrace();
                }

                //

                // ListViewに表示するためのAdapterを生成
                SimpleAdapter adapter = new SimpleAdapter(ResultActivity.this,
                        listData,   // ListViewに表示するデータ
                        android.R.layout.simple_list_item_2, // ListViewで使用するレイアウト（2つのテキスト）
                        new String[]{"title","author"},     // 表示するHashMapのキー
                        new int[]{android.R.id.text1, android.R.id.text2} // データを表示するid
                );

                // ListViewの初期化処理
                ListView listView = (ListView) findViewById(R.id.listView);
                listView.setAdapter(adapter);

                // ListView中の要素がタップされたときの処理を記述
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView parent, View view, int position, long id) {


                        Intent intent = new Intent(ResultActivity.this, BookInfoActivity.class);

                        BookInfo bookInfo = bookList.get(position);
                        intent.putExtra("id", bookInfo.ID);
                        intent.putExtra("title", bookInfo.title);
                        intent.putExtra("author", bookInfo.author);
                        intent.putExtra("publisher", bookInfo.publisher);
                        intent.putExtra("price", bookInfo.price);
                        intent.putExtra("isbn", bookInfo.isbn);

                        startActivity(intent);

                    }

                });
            }
        });

        // サーバとの通信を非同期で起動
        task.execute(query);
    }
}