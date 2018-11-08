package cn.com.aratek.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import cn.com.aratek.iccard.ICCardReader;
import cn.com.aratek.util.Result;

@SuppressLint("HandlerLeak")
public class ICCardDemo extends Activity implements View.OnClickListener {

    private static final int MSG_SHOW_ERROR = 0;
    private static final int MSG_SHOW_INFO = 1;
    private static final int MSG_UPDATE_INFO = 2;
    private static final int MSG_UPDATE_BUTTON = 3;
    private static final int MSG_SHOW_PROGRESS_DIALOG = 4;
    private static final int MSG_DISMISS_PROGRESS_DIALOG = 5;
    private static final int MSG_FINISH_ACTIVITY = 6;

    private TextView mInformation;
    private TextView mDetails;
    private TextView mIcText;
    private Button mBtnReadIcCard;
    private Button mBtnWriteIcCard;
    private ProgressDialog mProgressDialog;
    private ICCardReader mReader;
    private ICCardTask mTask;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_ERROR: {
                    mInformation.setTextColor(getResources().getColor(R.color.error_text_color));
                    mDetails.setTextColor(getResources().getColor(R.color.error_details_text_color));
                    mInformation.setText(((Bundle) msg.obj).getString("information"));
                    mDetails.setText(((Bundle) msg.obj).getString("details"));
                    break;
                }
                case MSG_SHOW_INFO: {
                    mInformation.setTextColor(getResources().getColor(R.color.information_text_color));
                    mDetails.setTextColor(getResources().getColor(R.color.information_details_text_color));
                    mInformation.setText((String) msg.obj);
                    mDetails.setText("");
                    break;
                }
                case MSG_UPDATE_INFO: {
                    mIcText.setText((String) msg.obj);
                    break;
                }
                case MSG_UPDATE_BUTTON: {
                    Boolean enable = (Boolean) msg.obj;
                    mBtnReadIcCard.setEnabled(enable);
                    mBtnWriteIcCard.setEnabled(enable);
                    break;
                }
                case MSG_SHOW_PROGRESS_DIALOG: {
                    String[] info = (String[]) msg.obj;
                    mProgressDialog.setTitle(info[0]);
                    mProgressDialog.setMessage(info[1]);
                    mProgressDialog.show();
                    break;
                }
                case MSG_DISMISS_PROGRESS_DIALOG: {
                    mProgressDialog.dismiss();
                    break;
                }
                case MSG_FINISH_ACTIVITY: {
                    finish();
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReader = ICCardReader.getInstance(this);

        setContentView(R.layout.activity_iccard);

        mInformation = (TextView) findViewById(R.id.tv_info);
        mDetails = (TextView) findViewById(R.id.tv_details);
        mIcText = (TextView) findViewById(R.id.ictext);
        mBtnReadIcCard = (Button) findViewById(R.id.bt_readiccard);
        mBtnWriteIcCard = (Button) findViewById(R.id.bt_writeiccard);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);

        enableControl(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        openDevice();
    }

    @Override
    protected void onPause() {
        closeDevice(false);

        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_readiccard:
                sectorReadCard();
                break;
            case R.id.bt_writeiccard:
                sectorWriteCard();
                break;
        }
    }

    private void enableControl(boolean enable) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_BUTTON, enable));
    }

    private void finishActivity() {
        mHandler.sendEmptyMessage(MSG_FINISH_ACTIVITY);
    }

    private void openDevice() {
        new Thread() {
            @Override
            public void run() {
                synchronized (ICCardDemo.this) {
                    showProgressDialog(getString(R.string.loading), getString(R.string.preparing_device));
                    int error;
                    if ((error = mReader.powerOn()) != ICCardReader.RESULT_OK) {
                        showError(getString(R.string.ic_card_reader_power_on_failed), getICCardErrorString(error));
                    }
                    if ((error = mReader.open()) != ICCardReader.RESULT_OK) {
                        showError(getString(R.string.ic_card_reader_open_failed), getICCardErrorString(error));
                    } else {
                        enableControl(true);
                        showInformation(getString(R.string.ic_card_reader_open_success));
                    }
                    dismissProgressDialog();
                }
            }
        }.start();
    }

    private void closeDevice(final boolean finish) {
        new Thread() {
            @Override
            public void run() {
                synchronized (ICCardDemo.this) {
                    showProgressDialog(getString(R.string.loading), getString(R.string.closing_device));
                    enableControl(false);
                    int error;
                    if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
                        mTask.cancel(false);
                        mTask.waitForDone();
                    }
                    if ((error = mReader.close()) != ICCardReader.RESULT_OK) {
                        showError(getString(R.string.ic_card_reader_close_failed), getICCardErrorString(error));
                    } else {
                        showInformation(getString(R.string.ic_card_reader_close_success));
                    }
                    if ((error = mReader.powerOff()) != ICCardReader.RESULT_OK) {
                        showError(getString(R.string.ic_card_reader_power_off_failed), getICCardErrorString(error));
                    }
                    if (finish) {
                        finishActivity();
                    }
                    dismissProgressDialog();
                }
            }
        }.start();
    }

    private void readCard() {
        mTask = new ICCardTask();
        mTask.execute("read");
    }

    private void writeCard() {
        mTask = new ICCardTask();
        mTask.execute("write");
    }

    private void sectorReadCard() {
        mTask = new ICCardTask();
        mTask.execute("sector_read");
    }

    private void sectorWriteCard() {
        mTask = new ICCardTask();
        mTask.execute("sector_write");
    }

    private static String toHexString(byte[] data, int start, int length) {
        if (data == null) {
            return "";
        }

        int realLen = data.length - start < length ? data.length - start : length;

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < realLen; i++) {
            sb.append(String.format("%02x ", data[start + i]));
        }
        return sb.toString();
    }

    private void showCardInfo(String info) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_INFO, info));
    }

    private void showProgressDialog(String title, String message) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SHOW_PROGRESS_DIALOG, new String[] { title, message }));
    }

    private void dismissProgressDialog() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_DISMISS_PROGRESS_DIALOG));
    }

    private void showError(String info, String details) {
        Bundle bundle = new Bundle();
        bundle.putString("information", info);
        bundle.putString("details", details);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SHOW_ERROR, bundle));
    }

    private void showInformation(String info) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SHOW_INFO, info));
    }

    private String getICCardErrorString(int error) {
        int strid;
        switch (error) {
            case ICCardReader.RESULT_OK:
                strid = R.string.operation_successful;
                break;
            case ICCardReader.RESULT_FAIL:
                strid = R.string.error_operation_failed;
                break;
            case ICCardReader.WRONG_CONNECTION:
                strid = R.string.error_wrong_connection;
                break;
            case ICCardReader.DEVICE_BUSY:
                strid = R.string.error_device_busy;
                break;
            case ICCardReader.DEVICE_NOT_OPEN:
                strid = R.string.error_device_not_open;
                break;
            case ICCardReader.TIMEOUT:
                strid = R.string.error_timeout;
                break;
            case ICCardReader.NO_PERMISSION:
                strid = R.string.error_no_permission;
                break;
            case ICCardReader.WRONG_PARAMETER:
                strid = R.string.error_wrong_parameter;
                break;
            case ICCardReader.DECODE_ERROR:
                strid = R.string.error_decode;
                break;
            case ICCardReader.INIT_FAIL:
                strid = R.string.error_initialization_failed;
                break;
            case ICCardReader.UNKNOWN_ERROR:
                strid = R.string.error_unknown;
                break;
            case ICCardReader.NOT_SUPPORT:
                strid = R.string.error_not_support;
                break;
            case ICCardReader.NOT_ENOUGH_MEMORY:
                strid = R.string.error_not_enough_memory;
                break;
            case ICCardReader.DEVICE_NOT_FOUND:
                strid = R.string.error_device_not_found;
                break;
            case ICCardReader.DEVICE_REOPEN:
                strid = R.string.error_device_reopen;
                break;
            case ICCardReader.NO_CARD:
                strid = R.string.error_no_card;
                break;
            case ICCardReader.INVALID_CARD:
                strid = R.string.error_invalid_card;
                break;
            case ICCardReader.CARD_VALIDATE_FAIL:
                strid = R.string.error_card_validate_failed;
                break;
            case ICCardReader.CARD_NOT_ACTIVATE:
                strid = R.string.error_card_not_activate;
                break;
            default:
                strid = R.string.error_other;
                break;
        }
        return getString(strid);
    }

    private class ICCardTask extends AsyncTask<String, Integer, Void> {
        private boolean mIsDone = false;

        @Override
        protected void onPreExecute() {
            enableControl(false);
            showCardInfo("");
        }

        @Override
        protected Void doInBackground(String... params) {
            Result res;
            if (params[0].equals("read")) {
                showProgressDialog(getString(R.string.loading), getString(R.string.place_iccard));
                do {
                    res = mReader.activate();
                } while (res.error == ICCardReader.NO_CARD && !isCancelled());
                if (!isCancelled()) {
                    if (res.error == ICCardReader.RESULT_OK) {
                        byte[] cardID = (byte[]) res.data;
                        StringBuffer sb = new StringBuffer("Card number:");
                        for (int i = 0; i < cardID.length; i++) {
                            sb.append(String.format(" %02x", cardID[i]));
                        }
                        for (int i = 0; i < 16; i++) {
                            int error = mReader.authenticateSectorWithKeyA(i, ICCardReader.KEY_DEFAULT);
                            if (error == ICCardReader.NOT_SUPPORT) {
                                break;
                            }
                            for (int j = 0; j < 4; j++) {
                                int block = i * 4 + j;
                                if (error == ICCardReader.RESULT_OK) {
                                    res = mReader.readBlock(block);
                                    if (res.error == ICCardReader.RESULT_OK) {
                                        byte[] data = (byte[]) res.data;
                                        sb.append("\nBlock[" + block + "] " + toHexString(data, 0, data.length));
                                    } else {
                                        sb.append("\nBlock[" + block + "] read error: " + res.error);
                                    }
                                } else {
                                    sb.append("\nBlock[" + block + "] validate key error: " + error);
                                    // If authenticate failed, reactivate is needed.
                                    if (error == ICCardReader.CARD_VALIDATE_FAIL) {
                                        mReader.activate();
                                    }
                                }
                            }
                        }
                        showInformation(getString(R.string.ic_card_read_success));
                        showCardInfo(sb.toString());
                    } else {
                        showError(getString(R.string.ic_card_read_failed), getICCardErrorString(res.error));
                    }
                }
                dismissProgressDialog();
            } else if (params[0].equals("write")) {
                showProgressDialog(getString(R.string.loading), getString(R.string.place_iccard));
                do {
                    res = mReader.activate();
                } while (res.error == ICCardReader.NO_CARD && !isCancelled());
                if (!isCancelled()) {
                    if (res.error == ICCardReader.RESULT_OK) {
                        int block = 1;
                        int error = mReader.authenticateSectorWithKeyA(mReader.blockToSector(block), ICCardReader.KEY_DEFAULT);
                        if (error == ICCardReader.RESULT_OK) {
                            error = mReader.writeBlock(block, new byte[] { (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef, (byte) 0x00, (byte) 0x11,
                                    (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77 });
                            if (error == ICCardReader.RESULT_OK) {
                                showInformation(getString(R.string.ic_card_write_success));
                            } else {
                                showError(getString(R.string.ic_card_write_failed), getICCardErrorString(res.error));
                            }
                        } else if (error == ICCardReader.NOT_SUPPORT) {
                            // showError(getString(R.string.ic_card_not_support), null);
                        } else {
                            showError(getString(R.string.ic_card_validate_key_failed), null);
                        }
                    } else {
                        showError(getString(R.string.ic_card_write_failed), getICCardErrorString(res.error));
                    }
                }
                dismissProgressDialog();
            } else if (params[0].equals("sector_read")) {
                showProgressDialog(getString(R.string.loading), getString(R.string.place_iccard));
                do {
                    res = mReader.activate();
                } while (res.error == ICCardReader.NO_CARD && !isCancelled());
                if (!isCancelled()) {
                    if (res.error == ICCardReader.RESULT_OK) {
                        byte[] cardID = (byte[]) res.data;
                        StringBuffer sb = new StringBuffer("Card number:");
                        for (int i = 0; i < cardID.length; i++) {
                            sb.append(String.format(" %02x", cardID[i]));
                        }
                        for (int i = 0; i < 16; i++) {
                            int error = mReader.authenticateSectorWithKeyA(i, ICCardReader.KEY_DEFAULT);
                            if (error == ICCardReader.RESULT_OK) {
                                res = mReader.readSector(i);
                                if (res.error == ICCardReader.RESULT_OK) {
                                    for (int j = 0; j < 4; j++) {
                                        int block = i * 4 + j;
                                        byte[] data = (byte[]) res.data;
                                        sb.append("\nBlock[" + block + "] " + toHexString(data, j * 16, 16));
                                    }
                                } else {
                                    for (int j = 0; j < 4; j++) {
                                        int block = i * 4 + j;
                                        sb.append("\nBlock[" + block + "] read error: " + res.error);
                                    }
                                }
                            } else if (error == ICCardReader.NOT_SUPPORT) {
                                break;
                            } else {
                                for (int j = 0; j < 4; j++) {
                                    int block = i * 4 + j;
                                    sb.append("\nBlock[" + block + "] validate key error: " + error);
                                }
                                // If authenticate failed, reactivate is needed.
                                if (error == ICCardReader.CARD_VALIDATE_FAIL) {
                                    mReader.activate();
                                }
                            }
                        }
                        showInformation(getString(R.string.ic_card_read_success));
                        showCardInfo(sb.toString());
                    } else {
                        showError(getString(R.string.ic_card_read_failed), getICCardErrorString(res.error));
                    }
                }
                dismissProgressDialog();
            } else if (params[0].equals("sector_write")) {
                showProgressDialog(getString(R.string.loading), getString(R.string.place_iccard));
                do {
                    res = mReader.activate();
                } while (res.error == ICCardReader.NO_CARD && !isCancelled());
                if (!isCancelled()) {
                    if (res.error == ICCardReader.RESULT_OK) {
                        int sector = 1;
                        int error = mReader.authenticateSectorWithKeyA(sector, ICCardReader.KEY_DEFAULT);
                        if (error == ICCardReader.RESULT_OK) {
                            error = mReader.writeSector(sector, new byte[] { (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88, (byte) 0x99,
                                    (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff, (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66,
                                    (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff, (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33,
                                    (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff, (byte) 0xff,
                                    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x07, (byte) 0x80, (byte) 0x69, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                                    (byte) 0xff, (byte) 0xff });
                            if (error == ICCardReader.RESULT_OK) {
                                showInformation(getString(R.string.ic_card_write_success));
                            } else {
                                showError(getString(R.string.ic_card_write_failed), getICCardErrorString(error));
                            }
                        } else if (error == ICCardReader.NOT_SUPPORT) {
                            // showError(getString(R.string.ic_card_not_support), null);
                        } else {
                            showError(getString(R.string.ic_card_validate_key_failed), null);
                        }
                    } else {
                        showError(getString(R.string.ic_card_write_failed), getICCardErrorString(res.error));
                    }
                }
                dismissProgressDialog();
            }
            enableControl(true);
            mIsDone = true;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onCancelled() {
        }

        public void waitForDone() {
            while (!mIsDone) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
