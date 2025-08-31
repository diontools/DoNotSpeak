package io.github.diontools.donotspeak;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class OptionSwitchView extends LinearLayout {
    private TextView textView;
    private Switch switchView;

    public OptionSwitchView(Context context) {
        super(context);
        init(context, null);
    }

    public OptionSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public OptionSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Inflate the XML layout
        LayoutInflater.from(context).inflate(R.layout.option_switch_view, this, true);
        
        // Get references to the views
        this.textView = findViewById(R.id.text_view);
        this.switchView = findViewById(R.id.switch_view);

        // Handle custom attributes
        if (attrs != null) {
            try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OptionSwitchView)) {
                String text = a.getString(R.styleable.OptionSwitchView_optionText);
                boolean checked = a.getBoolean(R.styleable.OptionSwitchView_optionChecked, false);

                if (text != null) {
                    this.setText(text);
                }
                this.setChecked(checked);
            }
        }

        // Make entire view clickable to toggle switch
        this.setClickable(true);
        this.setOnClickListener(v -> this.switchView.toggle());
    }

    public void setText(String text) {
        this.textView.setText(text);
    }

    public void setText(int resId) {
        this.textView.setText(resId);
    }

    public void setChecked(boolean checked) {
        this.switchView.setChecked(checked);
    }

    public boolean isChecked() {
        return this.switchView.isChecked();
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        this.switchView.setOnCheckedChangeListener(listener);
    }
}