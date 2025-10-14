package com.example.sottomemo;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// ★★★★★★★★★ ここの import 文を修正しました ★★★★★★★★★
import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.listener.ColorListener; // ColorPickerListener から ColorListener に変更
import com.github.dhaval2404.colorpicker.model.ColorShape;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CategoryManageActivity extends AppCompatActivity {

    private MemoViewModel memoViewModel;
    private CategoryAdapter categoryAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        Toolbar toolbar = findViewById(R.id.toolbar_category_manage);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("カテゴリの管理");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view_categories);
        categoryAdapter = new CategoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(categoryAdapter);

        memoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);
        memoViewModel.getAllCategories().observe(this, categories -> {
            categoryAdapter.submitList(categories);
        });

        FloatingActionButton fab = findViewById(R.id.fab_new_category);
        fab.setOnClickListener(v -> {
            showCategoryEditDialog(null);
        });

        categoryAdapter.setOnCategoryMenuClickListener((view, category) -> {
            PopupMenu popup = new PopupMenu(CategoryManageActivity.this, view);
            popup.getMenuInflater().inflate(R.menu.category_item_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_edit_category) {
                    showCategoryEditDialog(category);
                    return true;
                } else if (itemId == R.id.action_delete_category) {
                    showDeleteConfirmDialog(category);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void showCategoryEditDialog(final Category category) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_category_edit, null);
        final EditText editTextName = dialogView.findViewById(R.id.edit_text_category_name);
        final View viewSelectedColor = dialogView.findViewById(R.id.view_selected_color);

        String dialogTitle = (category == null) ? "新しいカテゴリを追加" : "カテゴリを編集";
        int initialColor = (category == null) ? Color.GRAY : category.color;

        if (category != null) {
            editTextName.setText(category.name);
        }
        viewSelectedColor.setBackground(new ColorDrawable(initialColor));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle(dialogTitle);

        viewSelectedColor.setOnClickListener(v -> {
            ColorDrawable drawable = (ColorDrawable) viewSelectedColor.getBackground();
            int currentColor = drawable.getColor();

            new ColorPickerDialog
                    .Builder(this)
                    .setTitle("色を選択")
                    .setColorShape(ColorShape.SQAURE)
                    .setDefaultColor(currentColor)
                    .setColorListener((color, colorHex) -> {
                        viewSelectedColor.setBackground(new ColorDrawable(color));
                    })
                    .show();
        });

        builder.setPositiveButton("保存", (dialog, which) -> {
            String name = editTextName.getText().toString();
            if (name.trim().isEmpty()) {
                Toast.makeText(this, "カテゴリ名を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            ColorDrawable drawable = (ColorDrawable) viewSelectedColor.getBackground();
            int color = drawable.getColor();

            if (category == null) {
                memoViewModel.insert(new Category(name, color));
                Toast.makeText(this, "カテゴリを追加しました", Toast.LENGTH_SHORT).show();
            } else {
                category.name = name;
                category.color = color;
                memoViewModel.update(category);
                Toast.makeText(this, "カテゴリを更新しました", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("キャンセル", null);

        builder.create().show();
    }

    private void showDeleteConfirmDialog(final Category category) {
        new AlertDialog.Builder(this)
                .setTitle("カテゴリの削除")
                .setMessage("「" + category.name + "」を削除しますか？\nこのカテゴリに属するメモからカテゴリの関連付けが解除されますが、メモ自体は削除されません。")
                .setPositiveButton("削除", (dialog, which) -> {
                    memoViewModel.delete(category);
                    Toast.makeText(this, "カテゴリを削除しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}