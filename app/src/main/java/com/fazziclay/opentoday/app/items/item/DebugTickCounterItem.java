package com.fazziclay.opentoday.app.items.item;

import androidx.annotation.NonNull;

import com.fazziclay.opentoday.app.data.Cherry;
import com.fazziclay.opentoday.app.items.tick.TickSession;
import com.fazziclay.opentoday.app.items.tick.TickTarget;
import com.fazziclay.opentoday.util.annotation.Getter;
import com.fazziclay.opentoday.util.annotation.RequireSave;
import com.fazziclay.opentoday.util.annotation.SaveKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DebugTickCounterItem extends TextItem {
    // START - Save
    public final static DebugTickCounterItemCodec CODEC = new DebugTickCounterItemCodec();
    public static class DebugTickCounterItemCodec extends TextItemCodec {
        @NonNull
        @Override
        public Cherry exportItem(@NonNull Item item) {
            DebugTickCounterItem debugTickCounterItem = (DebugTickCounterItem) item;
            return super.exportItem(debugTickCounterItem)
                    .put("counter", debugTickCounterItem.counter);
        }

        private final DebugTickCounterItem defaultValues = new DebugTickCounterItem();
        @NonNull
        @Override
        public Item importItem(@NonNull Cherry cherry, Item item) {
            DebugTickCounterItem debugTickCounterItem = item != null ? (DebugTickCounterItem) item : new DebugTickCounterItem();
            super.importItem(cherry, debugTickCounterItem);
            debugTickCounterItem.counter = cherry.optInt("counter", defaultValues.counter);
            return debugTickCounterItem;
        }
    }
    // END - Save

    @NonNull
    public static DebugTickCounterItem createEmpty() {
        return new DebugTickCounterItem("", 0);
    }

    @SaveKey(key = "counter") @RequireSave private int counter;
    private String debugStat = "";

    protected DebugTickCounterItem() {
        super();
    }

    public DebugTickCounterItem(String text, int counter) {
        super(text);
        this.counter = counter;
    }

    // Append
    public DebugTickCounterItem(TextItem textItem, int counter) {
        super(textItem);
        this.counter = counter;
    }

    // Copy
    public DebugTickCounterItem(DebugTickCounterItem copy) {
        super(copy);
        this.counter = copy.counter;
        this.debugStat = "";
    }

    @Override
    public void tick(TickSession tickSession) {
        if (!tickSession.isAllowed(this)) {
            debugStat = "tickSession not allowed tick me.";
            visibleChanged();
            return;
        }
        counter++;
        final List<String> targets = new ArrayList<>();
        for (TickTarget value : TickTarget.values()) {
            boolean allow = tickSession.isTickTargetAllowed(value);
            if (allow) {
                targets.add("$[-#00ff00]"+value.name()+"$[||]");
            } else {
                targets.add("$[-#ff0000]"+value.name()+"$[||]");
            }
        }
        debugStat = String.format("""
        === Debug tick counter ===
        ID: %s
        $[-#ffff00;S16]Counter: $[-#00aaff] %s$[||]
        $[-#f0f0f0]Allowed targets: %s$[||]
        $[-#00ffff]Whitelist(%s): %s$[||]
        $[-$fff00f]PathToMe: %s$[||]
        """, getId(), counter, targets, tickSession._isWhitelist(), tickSession._getWhitelist(),
                Arrays.toString(ItemUtil.getPathToItem(this)));
        visibleChanged();

        super.tick(tickSession);
        if (tickSession.isTickTargetAllowed(TickTarget.ITEM_DEBUG_TICK_COUNTER_UPDATE)) {
            tickSession.saveNeeded();
        }
    }

    @Getter
    public int getCounter() { return counter; }

    @Getter public String getDebugStat() {
        return debugStat;
    }
}
