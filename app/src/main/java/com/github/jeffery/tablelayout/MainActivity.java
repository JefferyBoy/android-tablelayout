package com.github.jeffery.tablelayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import com.github.jeffery.tablelayout.databinding.ActivityMainBinding;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private TableCell focusedCell = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnAddRow.setOnClickListener(v -> binding.table.setRowCount(binding.table.getRowCount() + 1));
        binding.btnAddRow2.setOnClickListener(v -> binding.table.setRowCount(binding.table.getRowCount() - 1));
        binding.btnAddCol.setOnClickListener(v -> binding.table.setColumnCount(binding.table.getColumnCount() + 1));
        binding.btnAddCol2.setOnClickListener(v -> binding.table.setColumnCount(binding.table.getColumnCount() - 1));
        binding.btnAddBorder.setOnClickListener(v -> binding.table.setBorderWidth(binding.table.getBorderWidth() + 1));
        binding.btnAddBorder2.setOnClickListener(v -> binding.table.setBorderWidth(binding.table.getBorderWidth() - 1));

        binding.btnClearFocus.setOnClickListener(v -> binding.table.clearFocusedCell());

        binding.switchMultiSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.table.setMultiSelectMode(isChecked);
        });
        binding.table.setOnItemClickListener((view, cell) -> {
            focusedCell = cell;
            Set<TableCell> selectedCells = binding.table.getSelectedCells();
            if (selectedCells.isEmpty()) {
                binding.switchCombineCell.setEnabled(false);
            } else {
                binding.switchCombineCell.setEnabled(true);
                boolean t = true;
                for (TableCell c : selectedCells) {
                    if (c.getColSpan() > 1 || c.getRowSpan() > 1) {
                        t = false;
                        binding.switchCombineCell.setText("取消合并");
                        break;
                    }
                }
                if (t) {
                    binding.switchCombineCell.setText("合并单元格");
                }
            }
            return false;
        });
        binding.switchCombineCell.setOnClickListener(v -> binding.table.combineCell(binding.switchCombineCell.getText().equals("合并单元格")));

        binding.radioGroupHorizontal.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_horizontal_left:
                        Set<TableCell> cells = binding.table.getSelectedCells();
                        for (TableCell cell : cells) {
                            binding.table.setCellGravity(cell, cell.getGravity() & Gravity.VERTICAL_GRAVITY_MASK | Gravity.LEFT);
                        }
                        break;
                }
            }
        });
    }
}