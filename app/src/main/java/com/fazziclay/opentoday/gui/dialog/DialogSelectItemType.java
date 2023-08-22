package com.fazziclay.opentoday.gui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ListView;
import android.widget.Spinner;

import com.fazziclay.opentoday.R;
import com.fazziclay.opentoday.app.App;
import com.fazziclay.opentoday.app.items.item.ItemType;
import com.fazziclay.opentoday.app.items.item.ItemsRegistry;
import com.fazziclay.opentoday.util.EnumUtil;
import com.fazziclay.opentoday.util.SimpleSpinnerAdapter;

public class DialogSelectItemType {
    private final Context context;
    private final App app;
    private final String selectButtonText;
    private String title;
    private String message;
    private final OnSelected onSelected;
    private View view;
    private final SimpleSpinnerAdapter<ItemType> simpleSpinnerAdapter;
    private final Dialog dialog;
    private final ItemTypeValidator itemTypeValidator;

    public DialogSelectItemType(Context context, OnSelected onSelected) {
        this(context, null, onSelected);
    }

    public DialogSelectItemType(Context context, OnSelected onSelected, ItemTypeValidator itemTypeValidator) {
        this(context, null, onSelected, itemTypeValidator);
    }

    public DialogSelectItemType(Context context, int resId, OnSelected onSelected) {
        this(context, context.getString(resId), onSelected);
    }

    public DialogSelectItemType(Context context, String selectButtonText, OnSelected onSelected) {
        this(context, selectButtonText, onSelected, type -> ItemsRegistry.REGISTRY.get(type).isCompatibility(App.get(context).getFeatureFlags()));
    }


    public DialogSelectItemType(Context context, String selectButtonText, OnSelected onSelected, ItemTypeValidator itemTypeValidator) {
        this.context = context;
        this.app = App.get(context);
        this.itemTypeValidator = itemTypeValidator;
        if (selectButtonText == null) {
            this.selectButtonText = context.getString(R.string.dialog_selectItemType_select);
        } else {
            this.selectButtonText = selectButtonText;
        }
        this.onSelected = onSelected;

        this.simpleSpinnerAdapter = new SimpleSpinnerAdapter<>(context);
        for (ItemType value : ItemType.values()) {
            if (this.itemTypeValidator.validate(value)) {
                EnumUtil.addToSimpleSpinnerAdapter(context, simpleSpinnerAdapter, value);
            }
        }

        final byte MODE = 2; // 1 - spinner; 2 - list
        // TODO: 2023.05.25 Add MODE selecting in constructor
        // TODO: 2023.05.25 add StartValue to constructor
        if (MODE == 1) {
            Spinner spinner = new Spinner(context);
            this.view = spinner;
            spinner.setAdapter(simpleSpinnerAdapter);
        }
        if (MODE == 2) {
            ListView listView = new ListView(context);
            this.view = listView;
            listView.setAdapter(simpleSpinnerAdapter);
            listView.setOnItemClickListener((parent, ignoreView, position, id) -> {
                ItemType itemType = simpleSpinnerAdapter.getItem(position);
                onSelected.onSelected(itemType);
                cancel();
            });
        }

        this.dialog = new AlertDialog.Builder(context)
                .setTitle(this.title)
                .setMessage(this.message)
                .setView(this.view)
                .setNegativeButton(context.getString(R.string.dialog_selectItemAction_cancel), null)
                .setPositiveButton(this.selectButtonText, (i2, i1) -> {
                    if (MODE == 1) {
                        ItemType itemType = simpleSpinnerAdapter.getItem(((Spinner) view).getSelectedItemPosition());
                        onSelected.onSelected(itemType);
                    }
                })
                .create();
    }

    public DialogSelectItemType setTitle(String m) {
        this.title = m;
        return this;
    }


    public DialogSelectItemType setMessage(String m) {
        this.message = m;
        return this;
    }

    public void show() {
        dialog.show();
    }

    public void cancel() {
        dialog.cancel();
    }

    @FunctionalInterface
    public interface OnSelected {
        void onSelected(ItemType type);
    }

    @FunctionalInterface
    public interface ItemTypeValidator {
        boolean validate(ItemType type);
    }
}
