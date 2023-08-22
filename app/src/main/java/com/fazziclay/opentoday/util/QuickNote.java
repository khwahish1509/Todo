package com.fazziclay.opentoday.util;

import com.fazziclay.opentoday.app.items.notification.DayItemNotification;
import com.fazziclay.opentoday.app.items.notification.ItemNotification;
import com.fazziclay.opentoday.gui.fragment.ItemsTabIncludeFragment;

import java.util.ArrayList;
import java.util.List;

public class QuickNote {
    public static final ItemsTabIncludeFragment.QuickNoteInterface QUICK_NOTE_NOTIFICATIONS_PARSE = s -> {
        List<ItemNotification> notifys = new ArrayList<>();
        boolean parseTime = true;
        if (parseTime) {
            char[] chars = s.toCharArray();
            int i = 0;
            for (char aChar : chars) {
                if (aChar == ':') {
                    try {
                        if (i >= 2 && chars.length >= 5) {
                            int hours = Integer.parseInt(String.valueOf(chars[i - 2]) + chars[i - 1]);
                            int minutes = Integer.parseInt(String.valueOf(chars[i + 1]) + chars[i + 2]);

                            DayItemNotification noti = new DayItemNotification();
                            noti.setTime((hours * 60 * 60) + (minutes * 60));
                            noti.setNotifyTextFromItemText(true);
                            notifys.add(noti);
                        }
                    } catch (Exception ignored) {
                    }
                }
                i++;
            }
        }
        return notifys;
    };
}
