package com.example.sottomemo;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast; // Toastをインポート

public class MemoEditActivity extends AppCompatActivity {

    // データの受け渡しに使う「キー」を定義しておきます。
    // こうすることで、タイプミスを防げます。
    public static final String EXTRA_NEW_MEMO_TEXT = "com.example.sottomemo.EXTRA_NEW_MEMO_TEXT";

    // 1. レイアウト上の部品を操作するための変数を宣言
    private EditText editTextMemo;
    private TextView buttonSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo_edit);

        // 2. 変数とレイアウト上の部品をIDで結びつける
        editTextMemo = findViewById(R.id.edit_text_memo);
        buttonSave = findViewById(R.id.button_save);

        // 3. 「保存」ボタンがクリックされたときの処理を設定
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMemo(); // 保存処理をするメソッドを呼び出す
            }
        });
    }

    // 保存処理を行うメソッド
    private void saveMemo() {
        // 4. EditTextから入力されたテキストを取得
        String memoText = editTextMemo.getText().toString();

        // 5. テキストが空っぽでないかチェック
        if (memoText.trim().isEmpty()) {
            // もし空なら、メッセージを表示して処理を中断
            Toast.makeText(this, "メモが入力されていません", Toast.LENGTH_SHORT).show();
            return;
        }

        // 6. データを返すための「小包」となるIntentを作成
        Intent resultIntent = new Intent();
        // 7. 小包に、品名ラベル(キー)を付けて、品物(入力されたテキスト)を入れる
        resultIntent.putExtra(EXTRA_NEW_MEMO_TEXT, memoText);
        // 8. 「返送準備OK」の印と、小包(Intent)をセットする
        setResult(RESULT_OK, resultIntent);
        // 9. この画面を閉じる（自動的に前の画面に戻る）
        finish();
    }
}
