package com.jay.smartvehicle;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by H151136 on 5/24/2016.
 */
public class DeviceInputDialog extends DialogFragment {

    private EditText mEditText;
    private DeviceInputListern mListener;
    private TextView mTextView;
    private String mTitle;
    private String mText;
    private AlertDialog.Builder builder;

    public interface DeviceInputListern {
        void onSettingInputComplete(String id);
    }

    public void init(String title, String text, DeviceInputListern listener) {
        mTitle = title;
        mText = text;
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_deviceid, null);
        mEditText = (EditText) view.findViewById(R.id.dialog_et);
        mTextView = (TextView) view.findViewById(R.id.dialog_tv);
        mTextView.setText(mText);

        builder.setView(view).setPositiveButton(getString(R.string.ok_string),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            mListener.onSettingInputComplete(mEditText.getEditableText().toString());
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel_string), null)
                .setTitle(mTitle).setIcon(R.mipmap.ic_launcher);

        return builder.create();
    }
}
