package net.lapasa.babymonitor.commonlib.filemgr;

import android.content.Context;

import net.lapasa.babymonitor.commonlib.commands.DeleteFileCommand;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueuedFileManager extends ConcurrentLinkedQueue<File>
{
    private static QueuedFileManager _instance;
    private final Context context;
    private SimpleDateFormat fmt = new SimpleDateFormat("HH_mm_ss");
    private DeleteFileCommand.IDeleteFileCallback callback;


    /**
     * Constructor* 
     * @param context
     */
    private QueuedFileManager(Context context)
    {
        this.context = context;
        this.callback = callback;
    }

    /**
     * Singleton access
     * @return
     */
    public static QueuedFileManager getInstance(Context context)
    {
        if (_instance == null)
        {
            _instance = new QueuedFileManager(context);
        }
        return _instance;
        
    }

    /**
     * In order to record an audio file, a File object needs to be created to 
     * contain the audio data
     * * 
     * @return
     */
    public File createFile()
    {
        String fileName = fmt.format(new Date()) + ".wav";
        File f = new File(context.getFilesDir(), fileName);
        this.add(f);
        return f;
    }
    
    public void deleteFile(File file)
    {
        DeleteFileCommand cmd = new DeleteFileCommand(callback, file);
        cmd.execute();
    }
}
