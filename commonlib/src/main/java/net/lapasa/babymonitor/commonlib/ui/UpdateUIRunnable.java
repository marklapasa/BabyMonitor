package net.lapasa.babymonitor.commonlib.ui;

import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class UpdateUIRunnable implements Runnable
{
    private String msg;
    private int LINE_COUNT = 25;
    private SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
    private TextView console;


    public UpdateUIRunnable(String str, TextView console)
    {
        this.msg = str;
        this.console = console;
    }

    @Override
    public void run()
    {
        Calendar c = Calendar.getInstance();
        String str = fmt.format(c.getTime()) + ": " + msg + "\n";

        if (console.getLineCount() % LINE_COUNT == 0)
        {
            console.setText("");
        }
        console.append(str);

        final int scrollAmount = console.getLayout().getLineTop(console.getLineCount()) - console.getHeight();
        if (scrollAmount > 0)
            console.scrollTo(0, console.getLineCount());
        else
            console.scrollTo(0, 0);
    }
}
