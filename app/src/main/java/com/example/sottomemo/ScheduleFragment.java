package com.example.sottomemo;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import java.util.Calendar;
import java.util.TimeZone;

public class ScheduleFragment extends Fragment {

    private TabLayout tabLayout;
    private LinearLayout calendarPageLayout;
    private LinearLayout todoPageLayout;
    private CalendarView calendarView;
    private TextView textViewSelectedDateHeader;
    private RecyclerView recyclerViewDailyEvents;
    private EventAdapter eventAdapter;
    private RecyclerView recyclerViewTodos;
    private TodoAdapter todoAdapter;
    private MemoViewModel mMemoViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        mMemoViewModel = new ViewModelProvider(requireActivity()).get(MemoViewModel.class);

        setupTabs();
        setupCalendarPage();
        setupTodoList();
    }

    private void initializeViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout_schedule);
        calendarPageLayout = view.findViewById(R.id.calendar_page_layout);
        todoPageLayout = view.findViewById(R.id.todo_page_layout);
        calendarView = view.findViewById(R.id.calendar_view);
        textViewSelectedDateHeader = view.findViewById(R.id.text_view_selected_date_header);
        recyclerViewDailyEvents = view.findViewById(R.id.recycler_view_daily_events);
        recyclerViewTodos = view.findViewById(R.id.recycler_view_todos);
    }

    private void setupTabs() {
        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("カレンダー"));
            tabLayout.addTab(tabLayout.newTab().setText("ToDoリスト"));
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    calendarPageLayout.setVisibility(View.VISIBLE);
                    todoPageLayout.setVisibility(View.GONE);
                } else {
                    calendarPageLayout.setVisibility(View.GONE);
                    todoPageLayout.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupCalendarPage() {
        eventAdapter = new EventAdapter();
        recyclerViewDailyEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewDailyEvents.setAdapter(eventAdapter);

        mMemoViewModel.getEventsForSelectedDate().observe(getViewLifecycleOwner(), events -> {
            eventAdapter.submitList(events);
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateSelectedDate(year, month, dayOfMonth);
        });

        eventAdapter.setOnItemClickListener(event -> {
            showEditEventDialog(event);
        });

        setupEventItemTouchHelper();

        Calendar today = Calendar.getInstance();
        updateSelectedDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
    }

    private void setupEventItemTouchHelper() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Event eventToDelete = eventAdapter.getCurrentList().get(position);
                mMemoViewModel.delete(eventToDelete);

                Snackbar.make(requireView(), "予定を削除しました", Snackbar.LENGTH_LONG)
                        .setAction("元に戻す", v -> mMemoViewModel.insert(eventToDelete))
                        .show();
            }
        }).attachToRecyclerView(recyclerViewDailyEvents);
    }

    private void showEditEventDialog(final Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("予定の編集");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText titleInput = new EditText(requireContext());
        titleInput.setHint("件名");
        titleInput.setText(event.getTitle());
        layout.addView(titleInput);

        final EditText timeInput = new EditText(requireContext());
        timeInput.setHint("時間 (例: 14:00 または 終日)");
        timeInput.setText(event.getTime());
        layout.addView(timeInput);

        builder.setView(layout);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newTitle = titleInput.getText().toString();
            String newTime = timeInput.getText().toString();
            if (!newTitle.trim().isEmpty() && !newTime.trim().isEmpty()) {
                event.setTitle(newTitle);
                event.setTime(newTime);
                mMemoViewModel.update(event);
            }
        });
        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void setupTodoList() {
        todoAdapter = new TodoAdapter();
        recyclerViewTodos.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewTodos.setAdapter(todoAdapter);

        mMemoViewModel.getAllTodos().observe(getViewLifecycleOwner(), todos -> {
            todoAdapter.submitList(todos);
        });

        todoAdapter.setOnTodoCheckedChangeListener((todo, isChecked) -> {
            // ★修正：コンストラクタを新しいものに変更
            Todo updatedTodo = new Todo(todo.getTitle(), isChecked, todo.getMemoId());
            updatedTodo.setId(todo.getId());
            mMemoViewModel.update(updatedTodo);
        });

        todoAdapter.setOnItemClickListener(todo -> {
            showEditTodoDialog(todo);
        });

        setupTodoItemTouchHelper();
    }

    private void setupTodoItemTouchHelper() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Todo todoToDelete = todoAdapter.getCurrentList().get(position);
                mMemoViewModel.delete(todoToDelete);

                Snackbar.make(requireView(), "ToDoを削除しました", Snackbar.LENGTH_LONG)
                        .setAction("元に戻す", v -> mMemoViewModel.insert(todoToDelete))
                        .show();
            }
        }).attachToRecyclerView(recyclerViewTodos);
    }

    private void showEditTodoDialog(final Todo todo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("ToDoの編集");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(todo.getTitle());
        builder.setView(input);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newTitle = input.getText().toString();
            if (!newTitle.trim().isEmpty()) {
                // ★★★ ここが修正箇所です！ ★★★
                // 元のToDoからmemoIdを引き継いで、新しいコンストラクタを呼び出します。
                Todo updatedTodo = new Todo(newTitle, todo.isCompleted(), todo.getMemoId());
                updatedTodo.setId(todo.getId());
                mMemoViewModel.update(updatedTodo);
            }
        });
        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateSelectedDate(int year, int month, int dayOfMonth) {
        String headerText = year + "年" + (month + 1) + "月" + dayOfMonth + "日";
        textViewSelectedDateHeader.setText(headerText + "の予定");

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(year, month, dayOfMonth, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        mMemoViewModel.setSelectedDate(calendar.getTimeInMillis());
    }
}