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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MemoEditActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "com.example.sottomemo.EXTRA_ID";
    public static final String EXTRA_EXCERPT = "com.example.sottomemo.EXTRA_EXCERPT";
    public static final String EXISTING_CATEGORY_IDS = "EXISTING_CATEGORY_IDS";

    private EditText editTextMemo;
    private TextView buttonSave;
    private ImageView buttonBack;
    private ChipGroup chipGroupCategories;
    private MemoViewModel mMemoViewModel;
    private long currentMemoId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo_edit);

        // 1. UI部品を初期化する
        initializeViews();

        // 2. ViewModelを、最初に初期化する
        mMemoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);

        // 3. Intentを取得する
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            currentMemoId = intent.getLongExtra(EXTRA_ID, -1L);
        }

        // 4. ViewModelの準備ができた後で、カテゴリチップのセットアップを呼ぶ
        setupCategoryChips(intent);

        // 5. 編集モードかどうかの判定と、テキストのセット
        if (currentMemoId != -1) {
            setTitle("メモの編集");
            String excerpt = intent.getStringExtra(EXTRA_EXCERPT);
            editTextMemo.setText(excerpt);
        } else {
            setTitle("新しいメモ");
        }

        // 6. ボタンのリスナーを設定
        buttonSave.setOnClickListener(v -> saveMemo());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        editTextMemo = findViewById(R.id.edit_text_memo);
        buttonSave = findViewById(R.id.button_save);
        buttonBack = findViewById(R.id.button_back);
        chipGroupCategories = findViewById(R.id.chip_group_categories);
    }

    private void setupCategoryChips(Intent intent) {
        mMemoViewModel.getAllCategories().observe(this, allCategories -> {
            chipGroupCategories.removeAllViews();
            if (allCategories == null) return;

            Set<Long> existingCategoryIds = new HashSet<>();
            if (intent.hasExtra(EXISTING_CATEGORY_IDS)) {
                existingCategoryIds.addAll((ArrayList<Long>) intent.getSerializableExtra(EXISTING_CATEGORY_IDS));
            }

            for (Category category : allCategories) {
                Chip chip = new Chip(this);
                chip.setText(category.name);
                chip.setCheckable(true);
                chip.setTag(category.categoryId);
                if (existingCategoryIds.contains(category.categoryId)) {
                    chip.setChecked(true);
                }
                chipGroupCategories.addView(chip);
            }
        });
    }

    private void saveMemo() {
        String memoText = editTextMemo.getText().toString();
        if (memoText.trim().isEmpty()) {
            Toast.makeText(this, "メモが入力されていません", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- 新しいロジック ---
        // 選択されたカテゴリのIDをリストに集める
        ArrayList<Long> selectedCategoryIds = new ArrayList<>();
        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupCategories.getChildAt(i);
            if (chip.isChecked()) {
                selectedCategoryIds.add((Long) chip.getTag());
            }
        }

        // 返信用のIntentを作成する
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_EXCERPT, memoText);
        resultIntent.putExtra("SELECTED_CATEGORY_IDS", selectedCategoryIds);

        // もし編集モードなら、IDもIntentに追加する
        if (currentMemoId != -1) {
            resultIntent.putExtra(EXTRA_ID, currentMemoId);
        }

        // 結果を「OK」としてセットする
        setResult(RESULT_OK, resultIntent);

        // 画面を閉じる
        finish();
    }
}