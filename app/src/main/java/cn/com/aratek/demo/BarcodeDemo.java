package cn.com.aratek.demo;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import cn.com.aratek.barcode.BarcodeItem;

public class BarcodeDemo extends Activity implements View.OnClickListener {
    private static final int REQ_SCAN = 0;
    private static final int REQ_SCAN_SEVERAL = 1;
    private static final String ACTION_SCAN = "cn.com.aratek.barcode.SCAN";
    private static final String ACTION_SCAN_SEVERAL = "cn.com.aratek.barcode.SCAN_SEVERAL";
    private static final String RESULT = "SCAN_RESULT";
    private static final String FLAGS = "SCAN_FLAGS";
    private static final long FLAG_IGNORE_DUPLICATE = 0x00000001;// Ignore the same barcode in one multi-scan
    private static final long FLAG_SEND_BROADCAST = 0x00000004;// Send the barcode data through the broadcast immediately

    private static final long flags = FLAG_IGNORE_DUPLICATE | FLAG_SEND_BROADCAST;

    private Button mBtnScan;
    private Button mBtnMultiScan;
    private EditText mScanInfo;
    private EditText mBarcodeFormat;
    private EditText mBarcodeSize;

    private static List<BarcodeItem> barcodeList = new ArrayList<BarcodeItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        mBarcodeFormat = (EditText) findViewById(R.id.et_format);
        mBarcodeSize = (EditText) findViewById(R.id.et_size);
        mScanInfo = (EditText) findViewById(R.id.et_scaninfo);
        mBtnScan = (Button) findViewById(R.id.bt_scan);
        mBtnMultiScan = (Button) findViewById(R.id.bt_multi_scan);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_SCAN:
                if (data != null) {
                    Bundle b = data.getExtras();
                    BarcodeItem item = b.getParcelable(RESULT);
                    mBarcodeFormat.setText(item.format);
                    mBarcodeSize.setText(Integer.toString(item.rawData.length));
                    mScanInfo.setText(new String(item.rawData));
                }
                break;
            case REQ_SCAN_SEVERAL:
                if (data != null) {
                    Bundle b = data.getExtras();
                    ArrayList<BarcodeItem> list = b.getParcelableArrayList(RESULT);
                    if (list.size() > 0) {
                        mBarcodeFormat.setText(list.get(0).format);
                        mBarcodeSize.setText(Integer.toString(list.get(0).rawData.length));
                        mScanInfo.setText(getString(R.string.barcode_scanned, list.size()));
                    } else {
                        mBarcodeFormat.setText("");
                        mBarcodeSize.setText("0");
                        mScanInfo.setText(getString(R.string.barcode_scanned_none));
                    }
                } else {
                    mScanInfo.setText(getString(R.string.barcode_scanned, barcodeList.size()));
                }
                break;
        }
    }

    static void addToList(BarcodeItem item) {
        barcodeList.add(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_scan: {
                mBtnScan.setEnabled(false);
                mScanInfo.setText("");
                Intent i = new Intent(ACTION_SCAN);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                startActivityForResult(i, REQ_SCAN);
                mBtnScan.setEnabled(true);
                break;
            }
            case R.id.bt_multi_scan: {
                mBtnMultiScan.setEnabled(false);
                mScanInfo.setText("");
                barcodeList = new ArrayList<BarcodeItem>();
                Intent i = new Intent(ACTION_SCAN_SEVERAL);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.putExtra(FLAGS, flags);
                startActivityForResult(i, REQ_SCAN_SEVERAL);
                mBtnMultiScan.setEnabled(true);
                break;
            }
        }
    }
}
