package cn.com.aratek.demo;

import java.io.UnsupportedEncodingException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import cn.com.aratek.printer.PaperExhaustedException;
import cn.com.aratek.printer.Printer;

@SuppressLint("HandlerLeak")
public class PrinterDemo extends Activity implements View.OnClickListener {
    private static final int MSG_SHOW_ERROR = 0;
    private static final int MSG_SHOW_INFO = 1;
    private static final int MSG_UPDATE_BUTTON = 2;
    private static final int MSG_SHOW_PROGRESS_DIALOG = 3;
    private static final int MSG_DISMISS_PROGRESS_DIALOG = 4;
    private static final int MSG_FINISH_ACTIVITY = 5;

    private TextView mInformation;
    private TextView mDetails;
    private Button mBtnPrintTestPage;
    private ProgressDialog mProgressDialog;
    private Printer mPrinter;
    private PrinterTask mTask;
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
                case MSG_UPDATE_BUTTON: {
                    Boolean enable = (Boolean) msg.obj;
                    mBtnPrintTestPage.setEnabled(enable);
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
        setContentView(R.layout.activity_printer);

        mPrinter = Printer.getInstance(this);

        mInformation = (TextView) findViewById(R.id.tv_info);
        mDetails = (TextView) findViewById(R.id.tv_details);
        mBtnPrintTestPage = (Button) findViewById(R.id.bt_printTestPage);

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
            case R.id.bt_printTestPage:
                printTestPage();
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
                synchronized (PrinterDemo.this) {
                    showProgressDialog(getString(R.string.loading), getString(R.string.preparing_device));
                    int error;
                    if ((error = mPrinter.powerOn()) != Printer.RESULT_OK) {
                        showError(getString(R.string.printer_power_on_failed), getPrinterErrorString(error));
                    }
                    if ((error = mPrinter.open()) != Printer.RESULT_OK) {
                        showError(getString(R.string.printer_open_failed), getPrinterErrorString(error));
                    } else {
                        showInformation(getString(R.string.printer_open_success));
                        enableControl(true);
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
                synchronized (PrinterDemo.this) {
                    showProgressDialog(getString(R.string.loading), getString(R.string.closing_device));
                    enableControl(false);
                    int error;
                    if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
                        mTask.cancel(false);
                        mTask.waitForDone();
                    }
                    if ((error = mPrinter.close()) != Printer.RESULT_OK) {
                        showError(getString(R.string.printer_close_failed), getPrinterErrorString(error));
                    } else {
                        showInformation(getString(R.string.printer_close_success));
                    }
                    if ((error = mPrinter.powerOff()) != Printer.RESULT_OK) {
                        showError(getString(R.string.printer_power_off_failed), getPrinterErrorString(error));
                    }
                    if (finish) {
                        finishActivity();
                    }
                    dismissProgressDialog();
                }
            }
        }.start();
    }

    private void printTestPage() {
        mTask = new PrinterTask();
        mTask.execute("printTestPage");
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

    private String getPrinterErrorString(int error) {
        int strid;
        switch (error) {
            case Printer.RESULT_OK:
                strid = R.string.operation_successful;
                break;
            case Printer.RESULT_FAIL:
                strid = R.string.error_operation_failed;
                break;
            case Printer.WRONG_CONNECTION:
                strid = R.string.error_wrong_connection;
                break;
            case Printer.DEVICE_BUSY:
                strid = R.string.error_device_busy;
                break;
            case Printer.DEVICE_NOT_OPEN:
                strid = R.string.error_device_not_open;
                break;
            case Printer.TIMEOUT:
                strid = R.string.error_timeout;
                break;
            case Printer.NO_PERMISSION:
                strid = R.string.error_no_permission;
                break;
            case Printer.WRONG_PARAMETER:
                strid = R.string.error_wrong_parameter;
                break;
            case Printer.DECODE_ERROR:
                strid = R.string.error_decode;
                break;
            case Printer.INIT_FAIL:
                strid = R.string.error_initialization_failed;
                break;
            case Printer.UNKNOWN_ERROR:
                strid = R.string.error_unknown;
                break;
            case Printer.NOT_SUPPORT:
                strid = R.string.error_not_support;
                break;
            case Printer.NOT_ENOUGH_MEMORY:
                strid = R.string.error_not_enough_memory;
                break;
            case Printer.DEVICE_NOT_FOUND:
                strid = R.string.error_device_not_found;
                break;
            case Printer.DEVICE_REOPEN:
                strid = R.string.error_device_reopen;
                break;
            case Printer.NO_PAPER:
                strid = R.string.error_no_paper;
                break;
            default:
                strid = R.string.error_other;
                break;
        }
        return getString(strid);
    }

    private class PrinterTask extends AsyncTask<String, Integer, Void> {
        private boolean mIsDone = false;

        @Override
        protected void onPreExecute() {
            enableControl(false);
        }

        @Override
        protected Void doInBackground(String... params) {
            if (params[0].equals("printTestPage")) {
                showProgressDialog(getString(R.string.loading), getString(R.string.printing));
                try {
                    mPrinter.initializePrinter();
                    mPrinter.centeringJustification();
                    mPrinter.printBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon));
                    mPrinter.leftJustification();
                    mPrinter.turnTwoDotWidthUnderlineModeOn();
                    mPrinter.escPosCommandSend("This is test page.".getBytes("gb2312"));
                    mPrinter.printAndLineFeed();
                    mPrinter.turnTwoDotWidthUnderlineModeOnForKanjiCharacters();
                    mPrinter.escPosCommandSend("这是测试页。".getBytes("gb2312"));
                    mPrinter.printAndLineFeed();
                    showInformation(getString(R.string.print_success));
                } catch (PaperExhaustedException e) {
                    showError(getString(R.string.print_failed), getPrinterErrorString(Printer.NO_PAPER));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    showError(getString(R.string.print_failed), getPrinterErrorString(Printer.DECODE_ERROR));
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    showError(getString(R.string.print_failed), getPrinterErrorString(Printer.UNKNOWN_ERROR));
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
