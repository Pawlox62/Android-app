package com.example.lab3.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lab3.R;
import com.example.lab3.data.entity.Phone;
import java.util.List;

public class PhoneListAdapter
        extends RecyclerView.Adapter<PhoneListAdapter.PhoneViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Phone phone);
    }

    private final LayoutInflater inflater;
    private List<Phone> phones;
    private final OnItemClickListener listener;

    public PhoneListAdapter(Context context, OnItemClickListener listener) {
        inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull @Override
    public PhoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.phone_list_item, parent, false);
        return new PhoneViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PhoneViewHolder holder, int position) {
        if (phones != null) {
            Phone p = phones.get(position);
            holder.makerText.setText(p.getMaker());
            holder.modelText.setText(p.getModel());
            holder.itemView.setOnClickListener(v -> listener.onItemClick(p));
        }
    }

    @Override
    public int getItemCount() {
        return phones == null ? 0 : phones.size();
    }

    public void setPhones(List<Phone> data) {
        phones = data;
        notifyDataSetChanged();
    }

    public Phone getPhoneAtPosition(int position) {
        return phones.get(position);
    }

    static class PhoneViewHolder extends RecyclerView.ViewHolder {
        final TextView makerText;
        final TextView modelText;

        PhoneViewHolder(View itemView) {
            super(itemView);
            makerText = itemView.findViewById(R.id.item_maker);
            modelText = itemView.findViewById(R.id.item_model);
        }
    }
}
