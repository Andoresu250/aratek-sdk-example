package cn.com.aratek.barcode;

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

public class BarcodeItem implements Parcelable {

    public byte[] rawData;
    public String format;

    public BarcodeItem() {
    }

    public BarcodeItem(String format, byte[] data) {
        this.format = format;
        this.rawData = data;
    }

    private BarcodeItem(Parcel source) {
        format = source.readString();
        rawData = source.createByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(format);
        dest.writeByteArray(rawData);
    }

    public static final Parcelable.Creator<BarcodeItem> CREATOR = new Creator<BarcodeItem>() {
        @Override
        public BarcodeItem createFromParcel(Parcel source) {
            return new BarcodeItem(source);
        }

        @Override
        public BarcodeItem[] newArray(int size) {
            return new BarcodeItem[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (o instanceof BarcodeItem) {
            BarcodeItem i = (BarcodeItem) o;
            if (!i.format.equals(this.format)) {
                return false;
            }
            return Arrays.equals(i.rawData, this.rawData);
        }
        return false;
    }
}