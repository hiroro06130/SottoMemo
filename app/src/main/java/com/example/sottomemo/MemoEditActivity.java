package com.example.sottomemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MemoEditActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "com.example.sottomemo.EXTRA_ID";
    public static final String EXTRA_EXCERPT = "com.example.sottomemo.EXTRA_EXCERPT";
    public static final String EXISTING_CATEGORY_IDS = "EXISTING_CATEGORY_IDS";
    public static final String EXTRA_START_VOICE_INPUT = "EXTRA_START_VOICE_INPUT";

    private EditText editTextMemo;
    private ChipGroup chipGroupCategories;
    private MemoViewModel mMemoViewModel;
    private long currentMemoId = -1;
    private ImageView buttonManageCategories;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private MenuItem micMenuItem;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startListening();
                } else {
                    Toast.makeText(this, "音声入力にはマイクの許可が必要です", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo_edit);

        Toolbar toolbar = findViewById(R.id.toolbar_memo_edit);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        initializeViews();
        mMemoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            currentMemoId = intent.getLongExtra(EXTRA_ID, -1L);
        }

        setupCategoryChips(intent);

        if (currentMemoId != -1) {
            getSupportActionBar().setTitle("メモの編集");
            String excerpt = intent.getStringExtra(EXTRA_EXCERPT);
            editTextMemo.setText(excerpt);
        } else {
            getSupportActionBar().setTitle("新しいメモ");
        }

        buttonManageCategories.setOnClickListener(v -> {
            Intent manageIntent = new Intent(MemoEditActivity.this, CategoryManageActivity.class);
            startActivity(manageIntent);
        });

        initializeSpeechRecognizer();

        if (intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)) {
            // UIの準備が整うのを少し待ってから開始
            editTextMemo.postDelayed(this::checkPermissionAndStartListening, 500);
        }
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Toast.makeText(MemoEditActivity.this, "お話しください...", Toast.LENGTH_SHORT).show();
                    updateMicIconState(true);
                }
                @Override
                public void onBeginningOfSpeech() {}
                @Override
                public void onRmsChanged(float rmsdB) {}
                @Override
                public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {
                    updateMicIconState(false);
                }
                @Override
                public void onError(int error) {
                    updateMicIconState(false);
                    if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                        Toast.makeText(MemoEditActivity.this, "聞き取れませんでした", Toast.LENGTH_SHORT).show();
                    }
                    isListening = false;
                }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        insertText(spokenText);
                    }
                    isListening = false;
                }
                @Override
                public void onPartialResults(Bundle partialResults) {}
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private void checkPermissionAndStartListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startListening() {
        if (speechRecognizer == null) {
            Toast.makeText(this, "このデバイスは音声入力をサポートしていません", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isListening) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            // ★★★ ここを修正しました！ 日本語 ("ja-JP") に固定 ★★★
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP");
            // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            speechRecognizer.startListening(intent);
            isListening = true;
        } else {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    private void updateMicIconState(boolean isActive) {
        if (micMenuItem != null) {
            if (isActive) {
                micMenuItem.setIconTintList(ColorStateList.valueOf(Color.RED));
            } else {
                micMenuItem.setIconTintList(ColorStateList.valueOf(getColorFromAttr(com.google.android.material.R.attr.colorOnSurface)));
            }
        }
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void insertText(String text) {
        int start = Math.max(editTextMemo.getSelectionStart(), 0);
        int end = Math.max(editTextMemo.getSelectionEnd(), 0);
        editTextMemo.getText().replace(Math.min(start, end), Math.max(start, end), text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.memo_edit_menu, menu);
        micMenuItem = menu.findItem(R.id.action_voice_input);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save_memo) {
            saveMemo();
            return true;
        } else if (item.getItemId() == R.id.action_voice_input) {
            checkPermissionAndStartListening();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        editTextMemo = findViewById(R.id.edit_text_memo);
        chipGroupCategories = findViewById(R.id.chip_group_categories);
        buttonManageCategories = findViewById(R.id.button_manage_categories);
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

                int categoryColor = category.color;
                chip.setChipBackgroundColor(ColorStateList.valueOf(categoryColor).withAlpha(40));
                chip.setChipStrokeWidth(0);
                chip.setTextColor(categoryColor);

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

        ArrayList<Long> selectedCategoryIds = new ArrayList<>();
        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupCategories.getChildAt(i);
            if (chip.isChecked()) {
                selectedCategoryIds.add((Long) chip.getTag());
            }
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_EXCERPT, memoText);
        resultIntent.putExtra("SELECTED_CATEGORY_IDS", selectedCategoryIds);

        if (currentMemoId != -1) {
            resultIntent.putExtra(EXTRA_ID, currentMemoId);
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}