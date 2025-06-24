package com.example.sottomemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class MemoEditActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "com.example.sottomemo.EXTRA_ID";
    public static final String EXTRA_TITLE = "com.example.sottomemo.EXTRA_TITLE";
    public static final String EXTRA_EXCERPT = "com.example.sottomemo.EXTRA_EXCERPT";

    private EditText editTextMemo;
    private TextView buttonSave;
    private ImageView buttonBack;
    private ChipGroup chipGroupCategories;
    private MemoViewModel mMemoViewModel;
    private int currentMemoId = -1;
    private List<Long> selectedCategoryIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo_edit);

        // UI部品の初期化
        editTextMemo = findViewById(R.id.edit_text_memo);
        buttonSave = findViewById(R.id.button_save);
        buttonBack = findViewById(R.id.button_back);
        chipGroupCategories = findViewById(R.id.chip_group_categories);

        // ViewModelの初期化
        mMemoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);

        // 利用可能なすべてのカテゴリを監視し、チップとして表示
        mMemoViewModel.getAllCategories().observe(this, categories -> {
            chipGroupCategories.removeAllViews();
            for (Category category : categories) {
                Chip chip = new Chip(this);
                chip.setText(category.name);
                chip.setCheckable(true);
                chip.setTag(category.categoryId); // チップにカテゴリIDを紐付ける

                // TODO: 編集中のメモが持つカテゴリを選択状態にする

                chipGroupCategories.addView(chip);
            }
        });

        // Intentからデータを受け取る
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            setTitle("メモの編集");
            currentMemoId = intent.getIntExtra(EXTRA_ID, -1);
            String excerpt = intent.getStringExtra(EXTRA_EXCERPT);
            editTextMemo.setText(excerpt);
        } else {
            setTitle("新しいメモ");
        }

        // ボタンのリスナーを設定
        buttonSave.setOnClickListener(v -> saveMemo());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void saveMemo() {
        String memoText = editTextMemo.getText().toString();
        if (memoText.trim().isEmpty()) {
            Toast.makeText(this, "メモが入力されていません", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- カテゴリの保存ロジック（次のステップで実装） ---
        // 選択されているチップのIDを取得
        selectedCategoryIds.clear();
        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupCategories.getChildAt(i);
            if (chip.isChecked()) {
                selectedCategoryIds.add((Long) chip.getTag());
            }
        }

        // --- メモの保存ロジック（ViewModelへの命令） ---
        String title = memoText.split("\n")[0];
        long currentTime = System.currentTimeMillis();

        if (currentMemoId == -1) { // 新規作成
            Memo newMemo = new Memo(title, memoText, currentTime);
            // TODO: ViewModelにカテゴリIDも渡す
            mMemoViewModel.insert(newMemo, selectedCategoryIds);
            Toast.makeText(this, "メモが保存されました", Toast.LENGTH_SHORT).show();
        } else { // 更新
            Memo updatedMemo = new Memo(title, memoText, currentTime);
            updatedMemo.setId(currentMemoId);
            // TODO: ViewModelにカテゴリIDも渡す
            mMemoViewModel.update(updatedMemo, selectedCategoryIds);
            Toast.makeText(this, "メモが更新されました", Toast.LENGTH_SHORT).show();
        }

        finish(); // 画面を閉じる
    }
}