package de.int80.gothbingo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by jens on 29.09.17.
 */

public class BingoFieldView extends AppCompatTextView implements View.OnClickListener {
    private int x;
    private int y;

    public int getFieldX() {
        return x;
    }

    public int getFieldY() {
        return y;
    }

    public boolean isChecked() {
        return checked;
    }

    private boolean checked = false;

    private MainActivity parent;

    public BingoFieldView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BingoFieldView);
        x = a.getInteger(R.styleable.BingoFieldView_coordX, 0);
        y = a.getInteger(R.styleable.BingoFieldView_coordY, 0);

        setOnClickListener(this);
        parent = (MainActivity)context;
    }

    public void toggle() {
        if (checked) {
            this.setTextColor(Color.BLACK);
            this.setBackgroundColor(Color.WHITE);
            checked = false;
        } else {
            this.setTextColor(Color.WHITE);
            this.setBackgroundColor(Color.parseColor("#666666"));
            checked = true;
        }
    }

    @Override
    public void onClick(View view) {
        parent.onBingoFieldClick(view);
    }
}
