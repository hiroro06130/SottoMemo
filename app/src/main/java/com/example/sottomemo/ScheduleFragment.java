package com.example.sottomemo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
        // ViewModelを、親のActivityと共有する形で取得
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

        // ViewModelに、選択された日付のイベントを監視させる
        mMemoViewModel.getEventsForSelectedDate().observe(getViewLifecycleOwner(), events -> {
            // データベースから取得したイベントのリストをアダプターに渡す
            eventAdapter.submitList(events);
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateSelectedDate(year, month, dayOfMonth);
        });

        // 初回表示時に、今日の日付をセットする
        Calendar today = Calendar.getInstance();
        updateSelectedDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
    }

    private void setupTodoList() {
        todoAdapter = new TodoAdapter();
        recyclerViewTodos.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewTodos.setAdapter(todoAdapter);

        // ViewModelのToDoリストを監視し、変更があればUIに反映
        mMemoViewModel.getAllTodos().observe(getViewLifecycleOwner(), todos -> {
            todoAdapter.submitList(todos);
        });

        todoAdapter.setOnTodoCheckedChangeListener((todo, isChecked) -> {
            // チェック状態の変更をDBに反映させる
            Todo updatedTodo = new Todo(todo.getTitle(), isChecked);
            updatedTodo.setId(todo.getId());
            mMemoViewModel.update(updatedTodo);
        });
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