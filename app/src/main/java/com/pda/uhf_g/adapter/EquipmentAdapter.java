package com.pda.uhf_g.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pda.uhf_g.R;
import com.pda.uhf_g.entity.Equipment;

import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder> {

    private List<Equipment> equipmentList;

    public EquipmentAdapter(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(String epc);
    }

    private OnItemLongClickListener longClickListener;

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void updateData(List<Equipment> newEquipmentList) {
        this.equipmentList = newEquipmentList;
        notifyDataSetChanged();
    }

    private long lastUpdateTime = 0;

    public void markAsFound(String epc) {
        for (int i = 0; i < equipmentList.size(); i++) {
            if (equipmentList.get(i).getEpc().equalsIgnoreCase(epc)) {
                equipmentList.get(i).setFound(true);
                equipmentList.get(i).incrementReadCount();
                
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime > 200) {
                    notifyDataSetChanged();
                    lastUpdateTime = currentTime;
                }
                break;
            }
        }
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        
        holder.tvDesc.setText(equipment.getDescription());
        holder.tvBrandModel.setText(equipment.getBrand() + " - " + equipment.getModel() + " (S/N: " + equipment.getSerialNumber() + ")");
        holder.tvEpc.setText("EPC: " + equipment.getEpc());

        if (equipment.isFound()) {
            holder.tvStatus.setText("Encontrado");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
            holder.itemView.setBackgroundColor(Color.parseColor("#E8F5E9")); // Light green background
        } else {
            holder.tvStatus.setText("No Encontrado");
            holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // Red
            holder.itemView.setBackgroundColor(Color.parseColor("#FAFAFA")); // Default
        }
        
        holder.tvCount.setText("Lecturas: " + equipment.getReadCount());

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(equipment.getEpc());
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return equipmentList == null ? 0 : equipmentList.size();
    }

    static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvDesc, tvBrandModel, tvEpc, tvStatus, tvCount;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDesc = itemView.findViewById(R.id.tv_equipment_desc);
            tvBrandModel = itemView.findViewById(R.id.tv_equipment_brand_model);
            tvEpc = itemView.findViewById(R.id.tv_equipment_epc);
            tvStatus = itemView.findViewById(R.id.tv_equipment_status);
            tvCount = itemView.findViewById(R.id.tv_equipment_count);
        }
    }
}
