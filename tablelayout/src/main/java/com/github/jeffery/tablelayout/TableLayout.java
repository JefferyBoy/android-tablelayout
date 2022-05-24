package com.github.jeffery.tablelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
    private final TableCell mFocusedCell = new TableCell(-1, -1);
    /**
     * 多选模式下选择的单元格
     */
    private final Set<TableCell> mSelectedCell = new HashSet<>();
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
    /**
     * 自定义的单元格属性
     */
    private final Set<TableCell> customCellList = new HashSet<>();

    private final List<TableCell> cellList = new ArrayList<>();
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

            private int[] calTableCell(MotionEvent e) {
                int column = (int) (e.getX() / (mDefaultColumnWidth + mBorderWidth));
                int row = (int) (e.getY() / (mDefaultRowHeight + mBorderWidth));
                //判断触摸点是否在合并单元格区间
                for (TableCell cell : customCellList) {
                    if (row >= cell.getRow()
                            && row < cell.getRow() + cell.getRowSpan()
                            && column >= cell.getCol()
                            && column < cell.getCol() + cell.getColSpan()
                    ) {
                        row = cell.getRow();
                        column = cell.getCol();
                    }
                }
                return new int[]{row, column};
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                int[] rc = calTableCell(e);
                int row = rc[0];
                int column = rc[1];
                TableCell cell = null;
                if (mMultiSelectMode) {
                    for (TableCell c : mSelectedCell) {
                        if (c.getRow() == row && c.getCol() == column) {
                            cell = c;
                            break;
                        }
                    }
                    if (cell == null) {
                        for (TableCell c : customCellList) {
                            if (c.getRow() == row && c.getCol() == column) {
                                cell = c;
                                break;
                            }
                        }
                        if (cell == null) {
                            cell = new TableCell(row, column);
                            mFocusedCell.setRowSpan(1);
                            mFocusedCell.setColSpan(1);
                        } else {
                            mFocusedCell.setRowSpan(cell.getRowSpan());
                            mFocusedCell.setColSpan(cell.getColSpan());
                        }
                        mSelectedCell.add(cell);
                    } else {
                        mSelectedCell.remove(cell);
                    }
                } else {
                    for (TableCell c : customCellList) {
                        if (c.getRow() == row && c.getCol() == column) {
                            cell = c;
                            break;
                        }
                    }
                    if (cell == null) {
                        mFocusedCell.setRowSpan(1);
                        mFocusedCell.setColSpan(1);
                    } else {
                        mFocusedCell.setRowSpan(cell.getRowSpan());
                        mFocusedCell.setColSpan(cell.getColSpan());
                    }
                }
                mFocusedCell.setRow(row);
                mFocusedCell.setCol(column);
                invalidate();
                if (mOnItemClickListener != null) {
                    View c = getChildAt(row, column);
                    return mOnItemClickListener.onItemClick(c, mFocusedCell);
                }
                return super.onSingleTapUp(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mOnItemDoubleClickListener != null) {
                    int[] rc = calTableCell(e);
                    int row = rc[0];
                    int column = rc[1];
                    if (row == mFocusedCell.getRow() && column == mFocusedCell.getCol()) {
                        View c = getChildAt(row, column);
                        return mOnItemDoubleClickListener.onItemClick(c, mFocusedCell);
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

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            mFocusedCell.setRow(-1);
            mFocusedCell.setCol(-1);
            invalidate();
        }
    }

    public interface OnItemClickListener {
        boolean onItemClick(@Nullable View view, TableCell cell);
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
        for (int i = 0, N = getChildCount(); i < N; i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == View.GONE) {
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

    @Nullable
    public View getChildAt(int row, int column) {
        if (row < 0 || column < 0) {
            return null;
        }
        for (int i = 0, n = getChildCount(); i < n; i++) {
            View c = getChildAt(i);
            LayoutParams lp = (LayoutParams) c.getLayoutParams();
            if (lp.row == row && lp.column == column) {
                return c;
            }
        }
        return null;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
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
        for (TableCell cell : customCellList) {
            if (cell.getRowSpan() > 1 || cell.getColSpan() > 1) {
                int left = cell.getCol() * colW + mBorderWidth;
                int top = cell.getRow() * rowH + mBorderWidth;
                if (left < width - mBorderWidth && top < height - mBorderWidth) {
                    canvas.drawRect(left, top,
                            Math.min(left + cell.getColSpan() * colW - mBorderWidth, width - mBorderWidth),
                            Math.min(top + cell.getRowSpan() * rowH - mBorderWidth, height - mBorderWidth), mBorderPaintClear
                    );
                }
            }
        }
        if (mMultiSelectMode) {
            //绘制当前选中的单元格颜色
            for (TableCell cell : mSelectedCell) {
                int left = cell.getCol() * colW + mBorderWidth;
                int top = cell.getRow() * rowH + mBorderWidth;
                if (left < width - mBorderWidth && top < height - mBorderWidth) {
                    canvas.drawRect(left, top,
                            left + cell.getColSpan() * colW - mBorderWidth,
                            top + cell.getRowSpan() * rowH - mBorderWidth, mFocusedCellBackgroundPaint);
                }
            }
        } else {
            //绘制当前焦点的单元格颜色
            if (mFocusedCell.getRow() >= 0 && mFocusedCell.getCol() >= 0) {
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return true;
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if (getLayerType() == LAYER_TYPE_SOFTWARE) {
            invalidate();
        }
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
            mSelectedCell.clear();
            if (multiSelectMode) {
                if (mFocusedCell.getRow() >= 0 && mFocusedCell.getCol() >= 0) {
                    mSelectedCell.add(new TableCell(mFocusedCell.getRow(), mFocusedCell.getCol()));
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
        mFocusedCell.setRow(-1);
        mFocusedCell.setCol(-1);
        mSelectedCell.clear();
        invalidate();
    }

    public boolean isCellSelected(TableCell cell) {
        return mSelectedCell.contains(cell);
    }

    public void clearSelectedCell() {
        if (!mSelectedCell.isEmpty()) {
            mSelectedCell.clear();
            invalidate();
        }
    }

    public Set<TableCell> getSelectedCells(){
        return mSelectedCell;
    }

    /**
     * 合并单元格，根据选中的单元格进行合并
     *
     * @param combine 是否合并单元格
     */
    public void combineCell(boolean combine) {
        if (combine) {
            if (mSelectedCell.size() <= 1) {
                return;
            }
            int minRow = 0, maxRow = 0, minCol = 0, maxCol = 0;
            int idx = 0;
            for (TableCell cell : mSelectedCell) {
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
            TableCell cell = null;
            for (TableCell c : customCellList) {
                if (c.getRow() == minRow && c.getCol() == minCol) {
                    cell = c;
                    break;
                }
            }
            if (cell == null) {
                cell = new TableCell(minRow, minCol);
                customCellList.add(cell);
            }
            //删除合并单元格后被合并的项
            Iterator<TableCell> iterator = customCellList.iterator();
            while (iterator.hasNext()) {
                TableCell c = iterator.next();
                if (c.getRow() > minRow && c.getRow() <= maxRow && c.getCol() > minCol && c.getCol() <= maxCol) {
                    iterator.remove();
                }
            }
            //左上的格子行列进行扩展
            cell.setColSpan(maxCol - minCol + 1);
            cell.setRowSpan(maxRow - minRow + 1);
            //子view布局属性同步更新
            View child = getChildAt(cell.getRow(), cell.getCol());
            if (child != null) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                lp.rowSpan = cell.getRowSpan();
                lp.columnSpan = cell.getColSpan();
            }
            //被合并的格子view不显示
            for (int r = cell.getRow() + 1; r < cell.getRow() + cell.getRowSpan(); r++) {
                for (int c = cell.getCol() + 1; c < cell.getCol() + cell.getColSpan(); c++) {
                    child = getChildAt(r, c);
                    if (child != null) {
                        child.setVisibility(View.GONE);
                    }
                }
            }
        } else {
            Iterator<TableCell> iterator = customCellList.iterator();
            while (iterator.hasNext()) {
                TableCell cell = iterator.next();
                if (cell.getRow() == mFocusedCell.getRow() && cell.getCol() == mFocusedCell.getCol()) {
                    iterator.remove();
                    //子view布局属性更新
                    View child = getChildAt(cell.getRow(), cell.getCol());
                    if (child != null) {
                        LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        lp.rowSpan = 1;
                        lp.columnSpan = 1;
                    }
                }
            }
            //被合并的格子view可以显示
            TableCell cell = mFocusedCell;
            for (int r = cell.getRow() + 1; r < cell.getRow() + cell.getRowSpan(); r++) {
                for (int c = cell.getCol() + 1; c < cell.getCol() + cell.getColSpan(); c++) {
                    View child = getChildAt(r, c);
                    if (child != null) {
                        child.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
        mFocusedCell.setRow(-1);
        mFocusedCell.setCol(-1);
        mFocusedCell.setColSpan(1);
        mFocusedCell.setRowSpan(1);
        mSelectedCell.clear();
        requestLayout();
    }


    /**
     * 设置格子内对齐方式
     */
    public void setCellGravity(TableCell cell, int gravity) {
        cell.setGravity(gravity);
        View child = getChildAt(cell.getRow(), cell.getCol());
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
}
