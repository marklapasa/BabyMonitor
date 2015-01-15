package net.lapasa.babymonitor.commonlib.commands;

import android.content.Context;

import java.io.File;

public class DeleteFileCommand
{
    private final File file;
    private final IDeleteFileCallback callback;

    public interface IDeleteFileCallback
    {
        void onDeleteSuccessful(String fileName);
    }

    public DeleteFileCommand(IDeleteFileCallback callback, File file)
    {
        this.file = file;
        this.callback = callback;
    }
    
    public DeleteFileCommand(IDeleteFileCallback callback, Context context, String fileName)
    {
        this.file = new File(context.getFilesDir(), fileName);
        this.callback = callback;
    }
    
    public void execute()
    {
        String fileName = file.getName();
        this.file.delete();
        callback.onDeleteSuccessful(fileName);
    }
}
