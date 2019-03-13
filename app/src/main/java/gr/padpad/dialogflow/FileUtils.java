package gr.padpad.dialogflow;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import timber.log.Timber;

public class FileUtils {
    public FileUtils() {
    }

    public static void saveFile(String directoryRoot, ResponseBody body) {
        checkIfDirectoryExists(directoryRoot);
        File destinationFile = getFile(getDirectoryRoot(directoryRoot), "dialog", ".mp3");

        try {
            BufferedSink sink = Okio.buffer(Okio.sink(destinationFile));
            sink.writeAll(body.source());
            sink.close();
        } catch (IOException e) {
            Timber.e(e);
            new File("");
        }
    }

    public static File getFile(File rootFile, String fileName, String fileType) {
        if (TextUtils.isEmpty(fileName)
                || TextUtils.isEmpty(fileType)
                || !rootFile.exists()) {
            return new File("");
        }
        return new File(rootFile + "/" + fileName + fileType);
    }

    public static boolean checkIfDirectoryExists(String directoryRoot) {
        File file = new File(Environment.getExternalStorageDirectory() + "/" + directoryRoot);
        if (!file.exists()) {
            return new File(file.getAbsolutePath()).mkdirs();
        }
        return file.exists();
    }

    public static File getDirectoryRoot(String directoryName) {
        if (TextUtils.isEmpty(directoryName)) {
            return new File("");
        }
        return new File(Environment.getExternalStorageDirectory() + "/" + directoryName);
    }

    public static boolean checkIfFileExists(File rootFile, String fileName, String fileType) {
        if (!rootFile.exists()
                || TextUtils.isEmpty(fileName)
                || TextUtils.isEmpty(fileType)) {
            return false;
        }
        return new File(rootFile + "/" + fileName + fileType).exists();
    }
}
