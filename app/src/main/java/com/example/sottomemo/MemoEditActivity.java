package com.example.sottomemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MemoEditActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "com.example.sottomemo.EXTRA_ID";
    public static final String EXTRA_TITLE = "com.example.sottomemo.EXTRA_TITLE";
    public static final String EXTRA_EXCERPT = "com.example.sottomemo.EXTRA_EXCERPT";

    private EditText editTextMemo;
    private TextView buttonSave;
    private ImageView buttonBack;
    private int currentMemoId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo_edit);

        editTextMemo = findViewById(R.id.edit_text_memo);
        buttonSave = findViewById(R.id.button_save);
        buttonBack = findViewById(R.id.button_back);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            setTitle("メモの編集");
            currentMemoId = intent.getIntExtra(EXTRA_ID, -1);
            String excerpt = intent.getStringExtra(EXTRA_EXCERPT);
            editTextMemo.setText(excerpt);
        } else {
            setTitle("新しいメモ");
        }

        buttonSave.setOnClickListener(v -> saveMemo());

        buttonBack.setOnClickListener(v -> {
            finish(); // 画面を閉じる
        });
    }

    private void saveMemo() {
        String memoText = editTextMemo.getText().toString();
        if (memoText.trim().isEmpty()) {
            Toast.makeText(this, "メモが入力されていません", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_EXCERPT, memoText);

        if (currentMemoId != -1) {
            resultIntent.putExtra(EXTRA_ID, currentMemoId);
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}