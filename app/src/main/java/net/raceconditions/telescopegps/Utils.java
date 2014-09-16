package net.raceconditions.telescopegps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Created by ubuntu on 9/16/14.
 */
public class Utils {
    public static void alertOkDialog(Context context, String title, String message)
    {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //close
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
}
