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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.sottomemo.api.ApiClient;
import com.example.sottomemo.api.Content;
import com.example.sottomemo.api.GeminiApiService;
import com.example.sottomemo.api.GeminiRequest;
import com.example.sottomemo.api.GeminiResponse;
import com.example.sottomemo.api.Part;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MemoEditActivity extends AppCompatActivity {

    // ★★★ ここにAPIキーを入れてください ★★★
    private static final String GEMINI_API_KEY = "AIzaSyC_qmCOhNd5YJ2rfCzfq0ZDsftVXjQaqSI";

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
    private int lastPartialTextLength = 0;

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
                    lastPartialTextLength = 0;
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
                    deletePartialText();
                    if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                        Toast.makeText(MemoEditActivity.this, "聞き取れませんでした", Toast.LENGTH_SHORT).show();
                    }
                    isListening = false;
                }
                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        deletePartialText();
                        insertText(text);
                        lastPartialTextLength = text.length();
                    }
                }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        deletePartialText();

                        boolean isCommandExecuted = false;
                        if (spokenText.equals("クリア") || spokenText.equals("全部消して") || spokenText.equals("全消去")) {
                            editTextMemo.setText("");
                            Toast.makeText(MemoEditActivity.this, "全て消去しました", Toast.LENGTH_SHORT).show();
                            isCommandExecuted = true;
                        } else if (spokenText.equals("削除") || spokenText.equals("消して") || spokenText.equals("戻って")) {
                            deleteLastChar();
                            Toast.makeText(MemoEditActivity.this, "⌫ 削除しました", Toast.LENGTH_SHORT).show();
                            isCommandExecuted = true;
                        }

                        if (!isCommandExecuted) {
                            String processedText = spokenText;
                            processedText = processedText.replace("改行", "\n")
                                    .replace("開業", "\n")
                                    .replace("会場", "\n");
                            processedText = processedText.replace("スペース", " ")
                                    .replace("空白", " ")
                                    .replace("空けて", " ");
                            insertText(processedText);
                        }
                    }
                    isListening = false;
                    lastPartialTextLength = 0;
                }
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
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

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

    private void deletePartialText() {
        if (lastPartialTextLength > 0) {
            int end = editTextMemo.getSelectionEnd();
            int start = Math.max(end - lastPartialTextLength, 0);
            if (start < end) {
                editTextMemo.getText().replace(start, end, "");
            }
            lastPartialTextLength = 0;
        }
    }

    private void deleteLastChar() {
        int start = Math.max(editTextMemo.getSelectionStart(), 0);
        int end = Math.max(editTextMemo.getSelectionEnd(), 0);
        if (start != end) {
            editTextMemo.getText().delete(Math.min(start, end), Math.max(start, end));
        } else if (start > 0) {
            editTextMemo.getText().delete(start - 1, start);
        }
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
        } else if (item.getItemId() == R.id.action_fix_text) {
            showAiCorrectionDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAiCorrectionDialog() {
        String originalText = editTextMemo.getText().toString();
        if (originalText.trim().isEmpty()) {
            Toast.makeText(this, "修正するテキストがありません", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ai_correction, null);
        TextView textOriginal = dialogView.findViewById(R.id.text_original);
        TextView textFixed = dialogView.findViewById(R.id.text_fixed);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        Button buttonApply = dialogView.findViewById(R.id.button_apply);

        textOriginal.setText(originalText);
        textFixed.setText("AIが修正・校正案を考えています...");
        buttonApply.setEnabled(false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        dialog.show();

        GeminiApiService service = ApiClient.getService();

        // ★★★ プロンプトを再度修正・強化しました ★★★
        String promptText = "あなたは自分用のメモを整理・校正する優秀なアシスタントです。\n" +
                "以下のテキストの誤字脱字を修正した上で、読みやすく簡潔なメモ書きに変換してください。\n" +
                "【変換ルール】\n" +
                "1. **人名**: 「さん」「様」などの敬称は**絶対に削除せず、そのまま残す**（呼び捨て禁止）。\n" +
                "2. **時間**: 「25時」「26時半」などの表記は、「翌1:00」「翌2:30」のように正しい時刻表記に修正する。\n" +
                "3. **校正**: 誤字、脱字、変換ミスを修正する。\n" +
                "4. **整理**: 挨拶（お疲れ様です、等）や無駄な言葉（えーと、等）は削除し、用件のみにする。\n" +
                "5. **形式**: 日時と用件が分かる短い形式にする。\n" +
                "6. **出力**: 結果のテキストのみを出力する。\n\n" +
                "対象テキスト: " + originalText;

        GeminiRequest request = new GeminiRequest();
        Content content = new Content();
        Part part = new Part();
        part.setText(promptText);
        content.setParts(Collections.singletonList(part));
        request.setContents(Collections.singletonList(content));

        Call<GeminiResponse> call = service.generateContent(GEMINI_API_KEY, request);
        call.enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String fixedText = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();
                        textFixed.setText(fixedText);
                        buttonApply.setEnabled(true);

                        buttonApply.setOnClickListener(v -> {
                            editTextMemo.setText(fixedText);
                            dialog.dismiss();
                        });
                    } catch (Exception e) {
                        textFixed.setText("修正案の取得に失敗しました。");
                    }
                } else {
                    textFixed.setText("AIサーバーとの通信エラー: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                textFixed.setText("エラーが発生しました: " + t.getMessage());
            }
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
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