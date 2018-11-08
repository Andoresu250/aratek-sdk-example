package cn.com.aratek.demo;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import cn.com.aratek.dev.Terminal;

@SuppressLint("InflateParams")
@SuppressWarnings("deprecation")
public class DemoActivity extends TabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Config.HAS_FINGERPRINT_DEVICE) {
            createTab(getString(R.string.fingerprint), new Intent(this, FingerprintDemo.class));
        }
        if (Config.HAS_IDCARD_DEVICE) {
            Locale locale = getResources().getConfiguration().locale;
            if (locale.getCountry().equals("CN")) {
                createTab(getString(R.string.idcard), new Intent(this, IDCardDemo.class));
            }
        }
        if (Config.HAS_ICCARD_DEVICE) {
            createTab(getString(R.string.iccard), new Intent(this, ICCardDemo.class));
        }
        if (Config.HAS_HARD_BARCODE_DEVICE) {
            createTab(getString(R.string.hardware_barcode), new Intent(this, QqcDemo.class));
        }
        if (Config.HAS_SOFT_BARCODE_DEVICE) {
            createTab(getString(R.string.software_barcode), new Intent(this, BarcodeDemo.class));
        }
        if (Config.HAS_PRINTER) {
            createTab(getString(R.string.printer), new Intent(this, PrinterDemo.class));
        }

        final TabWidget tabWidget = getTabHost().getTabWidget();
        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            tabWidget.getChildAt(i).getLayoutParams().height = 52;
        }
        Log.i("BMAPI", "SDK version: v" + Terminal.getSdkVersion() + ".");
    }

    private void createTab(String text, Intent intent) {
        TabHost tabHost = getTabHost();
        View view = LayoutInflater.from(this).inflate(R.layout.tab_indicator, null);
        TextView tv = (TextView) view.findViewById(R.id.title);
        tv.setText(text);
        tabHost.addTab(tabHost.newTabSpec(text).setIndicator(view).setContent(intent));
    }

}