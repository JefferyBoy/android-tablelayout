package com.github.jeffery.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.jeffery.app.databinding.ActivityMainBinding;
import com.github.jeffery.tablelayout.TableCell;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private String[] texts = {
            "旺财", "阿旺","旺旺","泰迪","哈士奇","八哥","大黄","金毛"
    };
    private int[] images = {
            R.drawable.ic_box, R.drawable.ic_bra, R.drawable.ic_chips,R.drawable.ic_clothes,
            R.drawable.ic_dianqi,R.drawable.ic_earphone, R.drawable.ic_sofa
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //行列修改
        binding.btnAddRow.setOnClickListener(v -> binding.table.setRowCount(binding.table.getRowCount() + 1));
        binding.btnAddRow2.setOnClickListener(v -> binding.table.setRowCount(binding.table.getRowCount() - 1));
        binding.btnAddCol.setOnClickListener(v -> binding.table.setColumnCount(binding.table.getColumnCount() + 1));
        binding.btnAddCol2.setOnClickListener(v -> binding.table.setColumnCount(binding.table.getColumnCount() - 1));
        binding.btnAddBorder.setOnClickListener(v -> binding.table.setBorderWidth(binding.table.getBorderWidth() + 1));
        binding.btnAddBorder2.setOnClickListener(v -> binding.table.setBorderWidth(binding.table.getBorderWidth() - 1));

        //清除选中
        binding.btnClearFocus.setOnClickListener(v -> binding.table.clearFocusedCell());

        //多选模式
        binding.switchMultiSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.table.setMultiSelectMode(isChecked);
        });
        //单元格点击
        binding.table.setOnItemClickListener((cell) -> {
            switch (cell.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) {
                case Gravity.LEFT:
                    binding.radioHorizontalLeft.setChecked(true);
                    break;
                case Gravity.RIGHT:
                    binding.radioHorizontalRight.setChecked(true);
                    break;
                default:
                    binding.radioHorizontalCenter.setChecked(true);
            }
            switch (cell.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) {
                case Gravity.LEFT:
                    binding.radioVerticalTop.setChecked(true);
                    break;
                case Gravity.RIGHT:
                    binding.radioVerticalBottom.setChecked(true);
                    break;
                default:
                    binding.radioVerticalCenter.setChecked(true);
            }
            return false;
        });
        //合并单元格
        binding.btnCombine.setOnClickListener(v -> {
            if (binding.table.getMultiSelectMode()) {
                binding.table.combineCell(binding.table.getSelectedCells());
            }
        });
        //取消合并单元格
        binding.btnUnCombine.setOnClickListener(v -> {
            if (binding.table.getMultiSelectMode()) {
                binding.table.unCombineCell(binding.table.getSelectedCells());
            } else {
                binding.table.unCombineCell(binding.table.getFocusedCell());
            }
        });
        //水平对齐方式
        binding.radioGroupHorizontal.setOnCheckedChangeListener((group, checkedId) -> {
            TableCell cell = binding.table.getFocusedCell();
            if (cell == null) {
                return;
            }
            switch (checkedId) {
                case R.id.radio_horizontal_left:
                    binding.table.setCellGravity(cell, cell.getGravity() & Gravity.VERTICAL_GRAVITY_MASK | Gravity.LEFT);
                    break;
                case R.id.radio_horizontal_center:
                    binding.table.setCellGravity(cell, cell.getGravity() & Gravity.VERTICAL_GRAVITY_MASK | Gravity.CENTER_HORIZONTAL);
                    break;
                default:
                    binding.table.setCellGravity(cell, cell.getGravity() & Gravity.VERTICAL_GRAVITY_MASK | Gravity.RIGHT);
                    break;
            }
        });
        //垂直对齐方式
        binding.radioGroupVertical.setOnCheckedChangeListener((group, checkedId) -> {
            TableCell cell = binding.table.getFocusedCell();
            if (cell == null) {
                return;
            }
            switch (checkedId) {
                case R.id.radio_vertical_top:
                    binding.table.setCellGravity(cell, cell.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK | Gravity.TOP);
                    break;
                case R.id.radio_vertical_center:
                    binding.table.setCellGravity(cell, cell.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK | Gravity.CENTER_VERTICAL);
                    break;
                default:
                    binding.table.setCellGravity(cell, cell.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK | Gravity.BOTTOM);
                    break;
            }
        });

        //虚线边框
        binding.switchDashBorder.setOnCheckedChangeListener((buttonView, isChecked) -> binding.table.setBorderDashPathEffect(isChecked ? new float[]{10, 10} : null));

        binding.btnAddText.setOnClickListener(v -> {
            TableCell cell = binding.table.getFocusedCell();
            if(cell != null){
                TextView textView = new TextView(v.getContext());
                textView.setText(randomString());
                binding.table.addView(textView, cell);
            }
        });
        binding.btnAddImage.setOnClickListener(v->{
            TableCell cell = binding.table.getFocusedCell();
            if(cell != null){
                ImageView imageView = new ImageView(v.getContext());
                imageView.setImageResource(randomImage());
                binding.table.addView(imageView, cell);
            }
        });
        binding.btnAddClearChilds.setOnClickListener(v->{
            TableCell cell = binding.table.getFocusedCell();
            if(cell != null){
                binding.table.removeViewAt(cell);
            }
        });
    }

    private String randomString(){
        Random random = new Random(System.currentTimeMillis());
        int idx = random.nextInt(texts.length);
        return texts[idx];
    }

    private int randomImage(){
        Random random = new Random(System.currentTimeMillis());
        int idx = random.nextInt(images.length);
        return images[idx];
    }

}