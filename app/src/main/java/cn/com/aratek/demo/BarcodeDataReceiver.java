package cn.com.aratek.demo;

import cn.com.aratek.barcode.BarcodeItem;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class BarcodeDataReceiver extends BroadcastReceiver {

    private static final String RESULT = "SCAN_RESULT";
    private static final String ACTION_ON_SCAN_RESULT = "cn.com.aratek.barcode.ACTION_ON_SCAN_RESULT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_ON_SCAN_RESULT)) {
            if (intent != null) {
                Bundle b = intent.getExtras();
                BarcodeItem item = b.getParcelable(RESULT);
                if (item != null) {
                    BarcodeDemo.addToList(item);
                }
            }
        }
    }
}
