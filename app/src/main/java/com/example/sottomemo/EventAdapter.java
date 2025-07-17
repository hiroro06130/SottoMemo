package com.example.sottomemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class EventAdapter extends ListAdapter<Event, EventAdapter.EventViewHolder> {

    // 外部（Fragment）からタップイベントを受け取るための変数を宣言
    private OnItemClickListener clickListener;

    public EventAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_event, parent, false);
        return new EventViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event currentEvent = getItem(position);
        // ViewHolderにデータを渡す際に、クリックリスナーも一緒に渡す
        holder.bind(currentEvent, clickListener);
    }

    // ViewHolderクラス
    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewTime;
        private final TextView textViewTitle;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTime = itemView.findViewById(R.id.text_view_event_time);
            textViewTitle = itemView.findViewById(R.id.text_view_event_title);
        }

        /**
         * データとリスナーを元に、Viewの表示を更新し、クリックイベントを設定するメソッド
         * @param event 表示する予定データ
         * @param listener タップされた時に呼び出すリスナー
         */
        public void bind(final Event event, final OnItemClickListener listener) {
            // データをViewにセット
            textViewTime.setText(event.getTime());
            textViewTitle.setText(event.getTitle());

            // View全体（itemView）がクリックされた時の処理
            itemView.setOnClickListener(v -> {
                // リスナーが設定されていれば、タップされたイベント情報を渡して実行する
                if (listener != null) {
                    listener.onItemClick(event);
                }
            });
        }
    }

    // リストの差分を計算するためのコールバック
    private static final DiffUtil.ItemCallback<Event> DIFF_CALLBACK = new DiffUtil.ItemCallback<Event>() {
        @Override
        public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getTime().equals(newItem.getTime());
        }
    };

    // --- ここからが、外部からクリックイベントを扱えるようにするための仕組み ---

    /**
     * クリックイベントをFragmentに伝えるためのインターフェース（設計図）
     */
    public interface OnItemClickListener {
        void onItemClick(Event event);
    }

    /**
     * Fragmentからリスナーを受け取るためのメソッド
     * @param listener Fragmentに実装されたリスナー
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
}