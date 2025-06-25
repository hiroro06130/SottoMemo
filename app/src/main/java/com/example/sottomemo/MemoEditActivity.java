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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo_edit);

        editTextMemo = findViewById(R.id.edit_text_memo);
        buttonSave = findViewById(R.id.button_save);
        buttonBack = findViewById(R.id.button_back);
        chipGroupCategories = findViewById(R.id.chip_group_categories);

        mMemoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            currentMemoId = intent.getIntExtra(EXTRA_ID, -1);
        }

        mMemoViewModel.getAllCategories().observe(this, allCategories -> {
            chipGroupCategories.removeAllViews();
            if (allCategories == null) return;

            for (Category category : allCategories) {
                Chip chip = new Chip(this);
                chip.setText(category.name);
                chip.setCheckable(true);
                chip.setTag(category.categoryId);
                chipGroupCategories.addView(chip);
            }

            if (currentMemoId != -1) {
                mMemoViewModel.getMemoWithCategories(currentMemoId).observe(this, memoWithCategories -> {
                    if (memoWithCategories != null && memoWithCategories.categories != null) {
                        Set<Long> categoryIdsForMemo = new HashSet<>();
                        for (Category cat : memoWithCategories.categories) {
                            categoryIdsForMemo.add(cat.categoryId);
                        }
                        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
                            Chip chip = (Chip) chipGroupCategories.getChildAt(i);
                            long chipId = (long) chip.getTag();
                            if (categoryIdsForMemo.contains(chipId)) {
                                chip.setChecked(true);
                            }
                        }
                    }
                });
            }
        });

        if (currentMemoId != -1) {
            setTitle("メモの編集");
            String excerpt = intent.getStringExtra(EXTRA_EXCERPT);
            editTextMemo.setText(excerpt);
        } else {
            setTitle("新しいメモ");
        }

        buttonSave.setOnClickListener(v -> saveMemo());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void saveMemo() {
        String memoText = editTextMemo.getText().toString();
        if (memoText.trim().isEmpty()) {
            Toast.makeText(this, "メモが入力されていません", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Long> selectedCategoryIds = new ArrayList<>();
        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupCategories.getChildAt(i);
            if (chip.isChecked()) {
                selectedCategoryIds.add((Long) chip.getTag());
            }
        }

        String title = memoText.split("\n")[0];
        long currentTime = System.currentTimeMillis();

        if (currentMemoId == -1) {
            Memo newMemo = new Memo(title, memoText, currentTime);
            mMemoViewModel.insert(newMemo, selectedCategoryIds);
            Toast.makeText(this, "メモが保存されました", Toast.LENGTH_SHORT).show();
        } else {
            Memo updatedMemo = new Memo(title, memoText, currentTime);
            updatedMemo.setId(currentMemoId);
            mMemoViewModel.update(updatedMemo, selectedCategoryIds);
            Toast.makeText(this, "メモが更新されました", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}