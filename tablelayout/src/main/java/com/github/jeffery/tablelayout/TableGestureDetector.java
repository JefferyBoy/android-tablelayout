package com.github.jeffery.tablelayout;

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * @author mxlei
 * @date 2022/5/19
 */
public class TableGestureDetector extends GestureDetector {

    private TableGestureListener gestureListener;

    public TableGestureDetector(Context context, TableGestureListener listener) {
        this(context, listener, null);
    }

    public TableGestureDetector(Context context, TableGestureListener listener, Handler handler) {
        super(context, listener, handler);
        this.gestureListener = listener;
    }

    public TableGestureDetector(Context context, TableGestureListener listener, Handler handler, boolean unused) {
        this(context, listener, handler);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (gestureListener != null) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_UP:
                    gestureListener.onUp(ev);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    gestureListener.onCancel(ev);
                    break;
            }
        }
        return super.onTouchEvent(ev);
    }
}
