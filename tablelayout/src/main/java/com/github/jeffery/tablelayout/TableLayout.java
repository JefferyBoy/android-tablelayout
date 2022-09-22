package com.github.jeffery.tablelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import com.github.jeffery.tablelayout.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mxlei
 * @date 2022/5/17
 */
public class TableLayout extends ViewGroup {

    /**
     * 列数
     */
    private int mColumnCount = 3;
    /**
     * 行数
     */
    private int mRowCount = 3;
    /**
     * 边框大小
     */
    private int mBorderWidth = 4;
    /**
     * 边框画笔
     */
    private final Paint mBorderPaint;
    private final Paint mBorderPaintClear;
    /**
     * 颜色
     */
    private int mColor = Color.BLACK;
    /**
     * 默认列宽
     */
    private int mDefaultColumnWidth = 0;
    /**
     * 默认行高
     */
    private int mDefaultRowHeight = 0;
    /**
     * 当前获取焦点的单元格
     */
    private TableCell mFocusedCell = null;

    /**
     * 当前焦点单元格背景色
     */
    private int mFocusedCellBackgroundColor = Color.parseColor("#80b0bec5");
    /**
     * 当前焦点单元格背景画笔
     */
    private final Paint mFocusedCellBackgroundPaint;
    /**
     * 是否处于多选模式
     */
    private boolean mMultiSelectMode = false;
    private boolean mConsumeTouchEvent = true;

    private final Map<String, TableCell> cellData = new ConcurrentHashMap<>();
    private final GestureDetectorCompat gestureDetectorCompat;
    private OnItemClickListener mOnItemClickListener;
    private OnItemClickListener mOnItemDoubleClickListener;

    private static final String TAG = "TableLayout";

    public TableLayout(Context context) {
        this(context, null);
    }

    public TableLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TableLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setColor(Color.BLACK);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaintClear = new Paint();
        mBorderPaintClear.setAlpha(0xFF);
        mBorderPaintClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mFocusedCellBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFocusedCellBackgroundPaint.setColor(mFocusedCellBackgroundColor);
        mFocusedCellBackgroundPaint.setStyle(Paint.Style.FILL);
        TableGestureListener gestureListener = new TableGestureListener() {

            private TableCell calTableCell(MotionEvent e) {
                int column = (int) (e.getX() / (mDefaultColumnWidth + mBorderWidth));
                int row = (int) (e.getY() / (mDefaultRowHeight + mBorderWidth));
                TableCell eventCell = null;
                //判断触摸点是否在合并单元格区间
                for (TableCell cell : cellData.values()) {
                    if (row >= cell.getRow()
                            && row < cell.getRow() + cell.getRowSpan()
                            && column >= cell.getCol()
                            && column < cell.getCol() + cell.getColSpan()
                    ) {
                        eventCell = cell;
                        break;
                    }
                }
                return eventCell == null ? new TableCell(row, column) : eventCell;
            }


            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                TableCell cell = calTableCell(e);
                int row = cell.getRow();
                int column = cell.getCol();
                if (mMultiSelectMode) {
                    cell.setSelected(!cell.isSelected());
                    if (cell.isSelected()) {
                        cellData.put(genCellMapKey(row, column), cell);
                    } else if (isDefaultCellLayoutParam(cell)) {
                        cellData.remove(cell);
                    }
                }
                mFocusedCell = cell;
                invalidate();
                if (mOnItemClickListener != null) {
                    return mOnItemClickListener.onItemClick(mFocusedCell);
                }
                return super.onSingleTapUp(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mOnItemDoubleClickListener != null) {
                    TableCell cell = calTableCell(e);
                    int row = cell.getRow();
                    int column = cell.getCol();
                    if (mFocusedCell != null && row == mFocusedCell.getRow() && column == mFocusedCell.getCol()) {
                        return mOnItemDoubleClickListener.onItemClick(mFocusedCell);
                    }
                }
                return super.onDoubleTap(e);
            }
        };
        new TableGestureDetector(context, gestureListener);
        gestureDetectorCompat = new GestureDetectorCompat(context, gestureListener);
        setWillNotDraw(false);
        initAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    public interface OnItemClickListener {
        boolean onItemClick(TableCell cell);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        private int gravity = Gravity.CENTER;
        private int columnSpan = 1;
        private int rowSpan = 1;
        private int row = 0;
        private int column = 0;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.TableLayout_Layout);
            row = a.getInteger(R.styleable.TableLayout_Layout_android_layout_row, 0);
            column = a.getInteger(R.styleable.TableLayout_Layout_android_layout_column, 0);
            rowSpan = a.getInteger(R.styleable.TableLayout_Layout_android_layout_rowSpan, 1);
            columnSpan = a.getInteger(R.styleable.TableLayout_Layout_android_layout_columnSpan, 1);
            gravity = a.getInteger(R.styleable.TableLayout_Layout_android_layout_gravity, Gravity.CENTER);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public int getGravity() {
            return gravity;
        }

        public void setGravity(int gravity) {
            this.gravity = gravity;
        }

        public int getColumnSpan() {
            return columnSpan;
        }

        public void setColumnSpan(int columnSpan) {
            this.columnSpan = columnSpan;
        }

        public int getRowSpan() {
            return rowSpan;
        }

        public void setRowSpan(int rowSpan) {
            this.rowSpan = rowSpan;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getColumn() {
            return column;
        }

        public void setColumn(int column) {
            this.column = column;
        }
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.TableLayout,
                defStyleAttr, defStyleRes);
        mColumnCount = a.getInteger(R.styleable.TableLayout_android_columnCount, mColumnCount);
        mRowCount = a.getInteger(R.styleable.TableLayout_android_rowCount, mRowCount);
        mBorderWidth = (int) a.getDimension(R.styleable.TableLayout_android_strokeWidth, mBorderWidth);
        mColor = a.getColor(R.styleable.TableLayout_android_color, mColor);
        mBorderPaint.setColor(mColor);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int allBorderWidth = mColumnCount == 0 ? 0 : (mColumnCount + 1) * mBorderWidth;
        int allBorderHeight = mRowCount == 0 ? 0 : (mRowCount + 1) * mBorderWidth;
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        boolean changedMeasuredSize = false;
        if (measuredWidth < allBorderWidth) {
            measuredWidth = allBorderWidth;
            changedMeasuredSize = true;
        }
        if (measuredHeight < allBorderHeight) {
            measuredHeight = allBorderHeight;
            changedMeasuredSize = true;
        }
        if (changedMeasuredSize) {
            setMeasuredDimension(measuredWidth, measuredHeight);
        }
        if (mColumnCount == 0) {
            mDefaultColumnWidth = 0;
        } else {
            mDefaultColumnWidth = (int) Math.ceil((getMeasuredWidth() - allBorderWidth) / (float) mColumnCount);
        }
        if (mRowCount == 0) {
            mDefaultRowHeight = 0;
        } else {
            mDefaultRowHeight = (int) Math.ceil((getMeasuredHeight() - allBorderHeight) / (float) mRowCount);
        }

        for (int i = 0, N = getChildCount(); i < N; i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == GONE) {
                continue;
            }
            LayoutParams lp = (LayoutParams) c.getLayoutParams();
            int childMaxWidth = lp.columnSpan * mDefaultColumnWidth;
            int childMaxHeight = lp.rowSpan * mDefaultRowHeight;
            int childWidthSpec;
            int childHeightSpec;
            switch (lp.width) {
                case LayoutParams.MATCH_PARENT:
                    childWidthSpec = MeasureSpec.makeMeasureSpec(childMaxWidth, MeasureSpec.EXACTLY);
                    break;
                case LayoutParams.WRAP_CONTENT:
                    childWidthSpec = MeasureSpec.makeMeasureSpec(childMaxWidth, MeasureSpec.AT_MOST);
                    break;
                default:
                    childWidthSpec = MeasureSpec.makeMeasureSpec(Math.min(lp.width, childMaxWidth), MeasureSpec.EXACTLY);
            }
            switch (lp.height) {
                case LayoutParams.MATCH_PARENT:
                    childHeightSpec = MeasureSpec.makeMeasureSpec(childMaxHeight, MeasureSpec.EXACTLY);
                    break;
                case LayoutParams.WRAP_CONTENT:
                    childHeightSpec = MeasureSpec.makeMeasureSpec(childMaxHeight, MeasureSpec.AT_MOST);
                    break;
                default:
                    childHeightSpec = MeasureSpec.makeMeasureSpec(Math.min(lp.height, childMaxHeight), MeasureSpec.EXACTLY);
            }
            c.measure(childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (TableCell cell : cellData.values()) {
            View c = cell.getView();
            if (c == null || c.getVisibility() == View.GONE) {
                continue;
            }
            LayoutParams lp = (LayoutParams) c.getLayoutParams();
            int gravity = lp.gravity;
            int x = lp.column * (mBorderWidth + mDefaultColumnWidth) + mBorderWidth;
            int y = lp.row * (mBorderWidth + mDefaultRowHeight) + mBorderWidth;
            int cellWidth = lp.columnSpan * mDefaultColumnWidth;
            int cellHeight = lp.rowSpan * mDefaultRowHeight;
            int measuredWidth = c.getMeasuredWidth();
            int measuredHeight = c.getMeasuredHeight();
            c.setLeft(x);
            c.setTop(y);
            c.setRight(x + cellWidth);
            c.setBottom(y + cellHeight);

            final int layoutDirection = getLayoutDirection();
            final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
            final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
            switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                case Gravity.CENTER_HORIZONTAL:
                    x = x + cellWidth / 2 - measuredWidth / 2;
                    break;
                case Gravity.RIGHT:
                    x = x + cellWidth - measuredWidth;
            }
            switch (verticalGravity) {
                case Gravity.CENTER_VERTICAL:
                    y = y + cellHeight / 2 - measuredHeight / 2;
                    break;
                case Gravity.BOTTOM:
                    y = y + cellHeight - measuredHeight;
                    break;
            }
            c.layout(x, y, x + measuredWidth, y + measuredHeight);
        }
    }


    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private LayoutParams generateLayoutParams(TableCell cell) {
        if (cell == null) {
            return (LayoutParams) generateDefaultLayoutParams();
        }
        LayoutParams lp = (LayoutParams) generateDefaultLayoutParams();
        lp.row = cell.getRow();
        lp.column = cell.getCol();
        lp.rowSpan = cell.getRowSpan();
        lp.columnSpan = cell.getColSpan();
        lp.gravity = cell.getGravity();
        return lp;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p.width, p.height);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int colW = mBorderWidth + mDefaultColumnWidth;
        int rowH = mBorderWidth + mDefaultRowHeight;
        float offset = mBorderWidth / 2f;
        //绘制横线边框
        for (int i = 1; i < mRowCount; i++) {
            float y = (mDefaultRowHeight + mBorderWidth) * i + offset;
            canvas.drawLine(0, y, width, y, mBorderPaint);
        }
        //绘制竖线边框
        for (int i = 1; i < mColumnCount; i++) {
            float x = (mDefaultColumnWidth + mBorderWidth) * i + offset;
            canvas.drawLine(x, 0, x, height, mBorderPaint);
        }
        //绘制边缘边框
        canvas.drawRoundRect(offset, offset, width - offset, height - offset, 0f, 0f, mBorderPaint);
        //清除合并单元格的内边框
        for (TableCell cell : cellData.values()) {
            if (cell.getRowSpan() > 1 || cell.getColSpan() > 1) {
                int left = cell.getCol() * colW + mBorderWidth;
                int top = cell.getRow() * rowH + mBorderWidth;
                if (cell.getRowSpan() > 1 || cell.getColSpan() > 1) {
                    if (left < width - mBorderWidth && top < height - mBorderWidth) {
                        canvas.drawRect(left, top,
                                Math.min(left + cell.getColSpan() * colW - mBorderWidth, width - mBorderWidth),
                                Math.min(top + cell.getRowSpan() * rowH - mBorderWidth, height - mBorderWidth), mBorderPaintClear
                        );
                    }
                }
            }
        }
        if (mMultiSelectMode) {
            //绘制当前选中的单元格颜色
            for (TableCell cell : cellData.values()) {
                int left = cell.getCol() * colW + mBorderWidth;
                int top = cell.getRow() * rowH + mBorderWidth;
                if (cell.isSelected() && left < width - mBorderWidth && top < height - mBorderWidth) {
                    canvas.drawRect(left, top,
                            left + cell.getColSpan() * colW - mBorderWidth,
                            top + cell.getRowSpan() * rowH - mBorderWidth, mFocusedCellBackgroundPaint);
                }
            }
        } else {
            //绘制当前焦点的单元格颜色
            if (mFocusedCell != null && mFocusedCell.getRow() >= 0 && mFocusedCell.getCol() >= 0) {
                int left = mFocusedCell.getCol() * colW + mBorderWidth;
                int top = mFocusedCell.getRow() * rowH + mBorderWidth;
                if (left < width - mBorderWidth && top < height - mBorderWidth) {
                    canvas.drawRect(left, top,
                            left + mFocusedCell.getColSpan() * colW - mBorderWidth,
                            top + mFocusedCell.getRowSpan() * rowH - mBorderWidth, mFocusedCellBackgroundPaint);
                }
            }
        }
    }

    public void addView(@NonNull View child, @NonNull TableCell cell) {
        LayoutParams lp = generateLayoutParams(cell);
        addView(child, -1, lp);
    }

    @Override
    public void addView(@NonNull View child, int index, @Nullable ViewGroup.LayoutParams params) {
        LayoutParams lp = (LayoutParams) params;
        if (!checkLayoutParams(params)) {
            lp = (LayoutParams) generateDefaultLayoutParams();
        }
        String key = genCellMapKey(lp.row, lp.column);
        TableCell cell = cellData.get(key);
        if (cell != null) {
            View v = cell.getView();
            if (v != null && v != child) {
                removeView(v);
                cell.setView(child);
            }
        }
        super.addView(child, index, params);
    }

    public void removeViewAt(@Nullable TableCell cell) {
        if (cell != null) {
            View v = cell.getView();
            if (v != null) {
                removeView(v);
            }
        }
    }

    public void removeViewAt(int row, int column) {
        View v = getChildAt(row, column);
        if (v != null) {
            removeView(v);
        }
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        String key = genCellMapKey(lp.row, lp.column);
        TableCell cell = new TableCell(lp.row, lp.column);
        cell.setGravity(lp.gravity);
        cell.setRowSpan(lp.rowSpan);
        cell.setColSpan(lp.columnSpan);
        cell.setView(child);
        cellData.put(key, cell);
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        String key = genCellMapKey(lp.row, lp.column);
        TableCell cell = cellData.get(key);
        if (cell != null) {
            cell.setView(null);
            if (isDefaultCellLayoutParam(cell)) {
                cellData.remove(key);
            }
        }
    }


    @Nullable
    public View getChildAt(int row, int column) {
        if (row < 0 || column < 0) {
            return null;
        }
        TableCell cell = cellData.get(genCellMapKey(row, column));
        if (cell != null) {
            return cell.getView();
        }
        return null;
    }

    @Nullable
    public View getChildAt(@NonNull TableCell cell) {
        return getChildAt(cell.getRow(), cell.getCol());
    }

    public void setTableCellData(Collection<TableCell> cells) {
        cellData.clear();
        removeAllViews();
        if (cells != null && cells.size() > 0) {
            for (TableCell cell : cells) {
                if (!isDefaultCellLayoutParam(cell)) {
                    cellData.put(genCellMapKey(cell.getRow(), cell.getCol()), cell);
                }
                if(cell.getView() != null){
                    addView(cell.getView(), cell);
                }
            }
        }
        requestLayout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return mConsumeTouchEvent;
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if (getLayerType() == LAYER_TYPE_SOFTWARE) {
            invalidate();
        }
    }


    private boolean isDefaultCellLayoutParam(TableCell cell) {
        return cell != null &&
                cell.getView() == null &&
                cell.getRowSpan() == 1 &&
                cell.getColSpan() == 1 &&
                cell.getGravity() == Gravity.CENTER &&
                !cell.isSelected();
    }

    public void setConsumeTouchEvent(boolean consume) {
        this.mConsumeTouchEvent = consume;
    }

    /**
     * 设置列数
     */
    public void setColumnCount(int columnCount) {
        if (this.mColumnCount != columnCount) {
            this.mColumnCount = Math.max(columnCount, 1);

            requestLayout();
        }
    }

    /**
     * 获取列数
     */
    public int getColumnCount() {
        return mColumnCount;
    }

    /**
     * 设置行数
     */
    public void setRowCount(int rowCount) {
        if (this.mRowCount != rowCount) {
            this.mRowCount = Math.max(rowCount, 1);
            requestLayout();
        }
    }

    /**
     * 获取行数
     */
    public int getRowCount() {
        return mRowCount;
    }

    /**
     * 设置边框颜色
     *
     * @param color 颜色
     */
    public void setColor(int color) {
        if (this.mColor != color) {
            this.mColor = color;
            mBorderPaint.setColor(color);
            invalidate();
        }
    }

    /**
     * 设置选中格子后的背景色
     *
     * @param color 颜色值
     */
    public void setFocusedCellBackgroundColor(int color) {
        if (this.mFocusedCellBackgroundColor != color) {
            this.mFocusedCellBackgroundColor = color;
            mFocusedCellBackgroundPaint.setColor(color);
            invalidate();
        }
    }

    /**
     * 设置边框大小
     *
     * @param mBorderWidth 边框大小（px）
     */
    public void setBorderWidth(int mBorderWidth) {
        if (this.mBorderWidth != mBorderWidth) {
            this.mBorderWidth = Math.max(2, mBorderWidth);
            mBorderPaint.setStrokeWidth(mBorderWidth);
            mBorderPaintClear.setStrokeWidth(mBorderWidth);
            requestLayout();
        }
    }

    /**
     * 获取边框大小
     *
     * @return 边框大小（px）
     */
    public int getBorderWidth() {
        return mBorderWidth;
    }

    /**
     * 设置边框虚线格式
     */
    public void setBorderDashPathEffect(float[] intervals) {
        if (intervals != null && intervals.length > 0) {
            mBorderPaint.setPathEffect(new DashPathEffect(intervals, 0));
        } else {
            mBorderPaint.setPathEffect(null);
        }
        invalidate();
    }

    /**
     * 设置单击格子监听
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    /**
     * 设置双击格子监听
     */
    public void setOnItemDoubleClickListener(OnItemClickListener mOnItemDoubleClickListener) {
        this.mOnItemDoubleClickListener = mOnItemDoubleClickListener;
    }

    /**
     * 设置为多选模式
     *
     * @param multiSelectMode 是否多选模式
     */
    public void setMultiSelectMode(boolean multiSelectMode) {
        if (this.mMultiSelectMode != multiSelectMode) {
            this.mMultiSelectMode = multiSelectMode;
            if (multiSelectMode) {
                if (mFocusedCell != null) {
                    mFocusedCell.setSelected(true);
                    cellData.put(genCellMapKey(mFocusedCell.getRow(), mFocusedCell.getCol()), mFocusedCell);
                }
            } else {
                Set<String> keySet = cellData.keySet();
                for (String key : keySet) {
                    TableCell cell = cellData.get(key);
                    cell.setSelected(false);
                    if (isDefaultCellLayoutParam(cell)) {
                        cellData.remove(key);
                    }
                }
            }
            invalidate();
        }
    }

    /**
     * 当前是否为多选模式
     */
    public boolean getMultiSelectMode() {
        return this.mMultiSelectMode;
    }

    /**
     * 清除已选中的单元格
     */
    public void clearFocusedCell() {
        mFocusedCell = null;
        Set<String> keySet = cellData.keySet();
        for (String key : keySet) {
            TableCell cell = cellData.get(key);
            cell.setSelected(false);
            if (isDefaultCellLayoutParam(cell)) {
                cellData.remove(cell);
            }
        }
        invalidate();
    }


    public List<TableCell> getSelectedCells() {
        List<TableCell> result = new ArrayList<>();
        for (TableCell cell : cellData.values()) {
            if (cell.isSelected()) {
                result.add(cell);
            }
        }
        return result;
    }

    public List<TableCell> getTableCellData() {
        return new ArrayList<>(cellData.values());
    }

    public void combineCell(TableCell cell) {
        if(cell != null){
            List<TableCell> list = new ArrayList<>();
            list.add(cell);
            combineCell(list);
        }
    }

    /**
     * 合并单元格
     *
     * @param cells 需要合并的格子列表
     */
    public void combineCell(Collection<TableCell> cells) {
        if (cells == null || cells.size() == 0) {
            return;
        }
        for (TableCell cell : cells) {
            cell.setSelected(false);
        }
        int minRow = 0, maxRow = 0, minCol = 0, maxCol = 0;
        int idx = 0;
        if (cells.size() <= 1) {
            return;
        }
        for (TableCell cell : cells) {
            if (idx++ == 0) {
                minRow = cell.getRow();
                maxRow = minRow;
                minCol = cell.getCol();
                maxCol = minCol;
            }
            minRow = Math.min(minRow, cell.getRow());
            maxRow = Math.max(maxRow, cell.getRow());
            minCol = Math.min(minCol, cell.getCol());
            maxCol = Math.max(maxCol, cell.getCol());
        }
        if (minRow == maxRow && minCol == maxCol) {
            return;
        }
        TableCell cell = cellData.get(genCellMapKey(minRow, minCol));
        if (cell == null) {
            cell = new TableCell(minRow, minCol);
            cellData.put(genCellMapKey(cell.getRow(), cell.getCol()), cell);
        }
        //删除合并单元格后被合并的项
        Set<String> keySet = cellData.keySet();
        cellData.keySet().iterator();
        for (String key : keySet) {
            TableCell c = cellData.get(key);
            if (!c.equals(cell) && isDefaultCellLayoutParam(c)) {
                cellData.remove(key);
            }
        }
        //左上的格子行列进行扩展
        cell.setColSpan(maxCol - minCol + 1);
        cell.setRowSpan(maxRow - minRow + 1);
        //左上的格子view布局更新
        View child = cell.getView();
        if (child != null) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.rowSpan = cell.getRowSpan();
            lp.columnSpan = cell.getColSpan();
        }
        //被合并的格子view不显示
        for (int r = cell.getRow(); r < cell.getRow() + cell.getRowSpan(); r++) {
            for (int c = cell.getCol(); c < cell.getCol() + cell.getColSpan(); c++) {
                if (r == cell.getRow() && c == cell.getCol()) {
                    continue;
                }
                child = getChildAt(r, c);
                if (child != null) {
                    child.setVisibility(View.GONE);
                }
            }
        }
        mFocusedCell = null;
        requestLayout();
    }

    public void unCombineCell(TableCell cell){
        if(cell != null){
            List<TableCell> list = new ArrayList<>();
            list.add(cell);
            unCombineCell(list);
        }
    }
    /**
     * 取消合并单元格
     *
     * @param cells 需要合并的格子列表
     */
    public void unCombineCell(Collection<TableCell> cells) {
        if (cells == null || cells.size() == 0) {
            return;
        }
        for (TableCell cell : cells) {
            cell.setSelected(false);
        }
        for (TableCell cell : cells) {
            if (cell.getRowSpan() > 1 || cell.getColSpan() > 1) {
                //被合并的格子view可以显示
                View child = null;
                for (int r = cell.getRow(); r < cell.getRow() + cell.getRowSpan(); r++) {
                    for (int c = cell.getCol(); c < cell.getCol() + cell.getColSpan(); c++) {
                        if (r == cell.getRow() && c == cell.getCol()) {
                            continue;
                        }
                        child = getChildAt(r, c);
                        if (child != null) {
                            child.setVisibility(View.VISIBLE);
                        }
                    }
                }
                cell.setRowSpan(1);
                cell.setColSpan(1);
                //子view布局属性更新
                child = cell.getView();
                if (child != null) {
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    lp.rowSpan = 1;
                    lp.columnSpan = 1;

                }
            }
        }
        mFocusedCell = null;
        requestLayout();
    }

    /**
     * 获取当前焦点的格子
     */
    @Nullable
    public TableCell getFocusedCell() {
        return mFocusedCell;
    }

    /**
     * 设置格子内对齐方式
     */
    public void setCellGravity(TableCell cell, int gravity) {
        cell.setGravity(gravity);
        if (gravity != Gravity.CENTER) {
            cellData.put(genCellMapKey(cell.getRow(), cell.getCol()), cell);
        } else {
            String key = genCellMapKey(cell.getRow(), cell.getCol());
            TableCell cell1 = cellData.get(key);
            if (isDefaultCellLayoutParam(cell1)) {
                cellData.remove(key);
            }
        }
        View child = cell.getView();
        if (child != null) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.getGravity() != gravity) {
                lp.setGravity(gravity);
                requestLayout();
            }
        }
    }

    /**
     * 获取格子的对齐方式
     *
     * @return 对齐方式
     */
    public int getCellGravity(int row, int column) {
        View child = getChildAt(row, column);
        if (child != null) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            return lp.getGravity();
        }
        return Gravity.CENTER;
    }

    private String genCellMapKey(int row, int col) {
        return row + "," + col;
    }

    private String genCellMapKey(TableCell cell) {
        return cell.getRow() + "," + cell.getCol();
    }
}
