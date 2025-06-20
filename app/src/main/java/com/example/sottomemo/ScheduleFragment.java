package com.example.sottomemo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

        tabLayout = view.findViewById(R.id.tab_layout_schedule);
        calendarPageLayout = view.findViewById(R.id.calendar_page_layout);
        todoPageLayout = view.findViewById(R.id.todo_page_layout);
        calendarView = view.findViewById(R.id.calendar_view);
        textViewSelectedDateHeader = view.findViewById(R.id.text_view_selected_date_header);
        recyclerViewDailyEvents = view.findViewById(R.id.recycler_view_daily_events);
        recyclerViewTodos = view.findViewById(R.id.recycler_view_todos);

        mMemoViewModel = new ViewModelProvider(this).get(MemoViewModel.class);

        setupTabs();
        setupCalendarPage();
        setupTodoList();
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
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            String selectedDate = year + "年" + (month + 1) + "月" + dayOfMonth + "日";
            textViewSelectedDateHeader.setText(selectedDate + "の予定");
            showDummyEvents(dayOfMonth);
        });
        setInitialDate();
    }

    private void setupTodoList() {
        todoAdapter = new TodoAdapter();
        recyclerViewTodos.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewTodos.setAdapter(todoAdapter);

        mMemoViewModel.getAllTodos().observe(getViewLifecycleOwner(), todos -> {
            todoAdapter.submitList(todos);
        });

        todoAdapter.setOnTodoCheckedChangeListener((todo, isChecked) -> {
            // 元のtodoオブジェクトは変更せず、新しいオブジェクトを作成する
            Todo updatedTodo = new Todo(todo.getTitle(), isChecked);
            // 更新するにはIDが必要なので、古いIDをセットする
            updatedTodo.setId(todo.getId());

            // この「新しい」オブジェクトをViewModelに渡してデータベースを更新する
            mMemoViewModel.update(updatedTodo);
        });
    }

    private void setInitialDate() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        String initialDate = year + "年" + (month + 1) + "月" + dayOfMonth + "日";
        textViewSelectedDateHeader.setText(initialDate + "の予定");
        showDummyEvents(dayOfMonth);
    }

    private void showDummyEvents(int day) {
        List<Event> dummyEvents = new ArrayList<>();
        if (day % 3 == 0) {
            dummyEvents.add(new Event(1, "10:00", "チーム定例会議"));
            dummyEvents.add(new Event(2, "15:00", "歯医者の予約"));
        } else if (day % 3 == 1) {
            dummyEvents.add(new Event(3, "19:00", "友人とのディナー"));
        }
        eventAdapter.submitList(dummyEvents);
    }
}