package guo.yifan.thesheetofmusicjava;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    /**
     * -------- Init ----
     */

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int MAX_16_BIT = 32768;

    // Template song
    public MediaPlayer mySong;


    // Button Params
    Button PauseResumeButton;
    public int PauseResumeFlag, PausedLength;

    // The path to the root of this app's internal storage
    private File privateRootDir;

    // The path to the "songs" subdirectory
    public static File songsDir;

    // Array of files in the songs subdirectory
    File[] songFiles;

    // Array of filenames corresponding to songFiles
    String[] songFilenames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PauseResumeButton = findViewById(R.id.PauseButton);
        mySong = MediaPlayer.create(this, R.raw.kimi_wo_nosete_trim);

        // Set up an Intent to send back to apps that request a file
        Intent resultIntent = new Intent(getPackageName());

        // Get the files/ subdirectory of internal storage
        privateRootDir = getFilesDir();

        // Get the files/images subdirectory
        songsDir = new File(privateRootDir, "songs");

        // create the directory first
        // https://stackoverflow.com/questions/36088699/error-open-failed-enoent-no-such-file-or-directory
        songsDir.mkdirs();

        // Get the files in the songs subdirectory
        songFiles = songsDir.listFiles();

        // Set the Activity's result to null to begin with
        setResult(Activity.RESULT_CANCELED, null);

        /*
        * Display the file names with the ListView fileListView.
        * Back the ListView with the array songFilenames, which
        * you can create by iterating through songFiles and
        * calling File.getAbsolutePath() for each File
         */
    }

    public void PlayAudio(View view) {
        /** Play Song */
        mySong.start();
    }

    public void PauseResumeAudio(View view) {
        /** Pause */
        if (PauseResumeFlag == 0) {
            mySong.pause();
            PausedLength = mySong.getCurrentPosition();

            /** Set flag and Button Text to Resume */
            PauseResumeFlag = 1;
            PauseResumeButton.setText("Resume");
        } else {
            /** Resume song */
            mySong.seekTo(PausedLength);
            mySong.start();

            /** Set flag and Button Text to Pause */
            PauseResumeFlag = 0;
            PauseResumeButton.setText("Pause");
        }


    }

    public void StopAudio(View view) {
        /** Stop song */
        mySong.release();
        mySong = MediaPlayer.create(this, R.raw.kimi_wo_nosete_trim);
    }

    // convert bytes fo length 2 into an integer
    public Long getLE2(byte[] buffer) {
        long val = buffer[1] & 0xFF;
        val = (val << 8) + (buffer[0] & 0xFF);
        return val / 1L;
    }

    public ArrayList<Double> readAudio_ArrayList(int rawID) {
        ArrayList<Double> WaveOut = new ArrayList<>();
        try {
            InputStream inputStream = this.getResources().openRawResource(rawID);
            int read;

            /** Header Details */
            byte[] bytes_tmp = new byte[44];
            read = inputStream.read(bytes_tmp, 0, bytes_tmp.length);

            /** Reading Wav file */
            byte[] bytes = new byte[2];
            Long longtmp;
            while (read != -1) {
                read = inputStream.read(bytes, 0, bytes.length);
                longtmp = getLE2(bytes);
                double val = longtmp.doubleValue();
                WaveOut.add(val);
            }

            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return WaveOut;
    }

    /**
     * Reads audio samples from a file (in WAVE, AU, AIFF, or MIDI format)
     * and returns them as a double array with values between –1.0 and +1.0.
     * The sound format must use 16-bit audio data with a sampling rate of 44,100.
     * The sound format can be either monoaural or stereo, and the bytes can
     * be stored in either little endian or big endian order.
     *
     * @param rawID the name of the audio file
     * @return the array of samples
     */
    public ArrayList<Double> read(int rawID) {


        // extract the audio data and convert to a double[] with each sample between -1 and +1
        try {
            // create AudioInputStream from file
            InputStream inputStream = this.getResources().openRawResource(rawID);

            Queue<Double> queue = new LinkedList<Double>();

            // 4K buffer (must be a multiple of 2 for mono or 4 for stereo)
            byte[] bytes = new byte[READ_BUFFER_SIZE];
            int count;
            while ((count = inputStream.read(bytes, 0, READ_BUFFER_SIZE)) != -1) {

                // little endian, monoaural
                for (int i = 0; i < count / 2; i++) {
                    double sample = ((short) (((bytes[2 * i + 1] & 0xFF) << 8) | (bytes[2 * i] & 0xFF))) / ((double) MAX_16_BIT);
                    queue.add(sample);
                }

            }
            inputStream.close();
            Object[] objectArray = queue.toArray();
            ArrayList<Double> doubleArray = new ArrayList<Double>();
            for (int i = 0; i < objectArray.length; i++) {
                doubleArray.add((double) objectArray[i]);
            }
            return doubleArray;
        } catch (IOException ioe) {
            throw new IllegalArgumentException("could not read '" + rawID + "'", ioe);
        }
    }


    public void LoadAudio(View view) {
        /** Read Audio */
        ArrayList<Double> amplitudes = read(R.raw.kimi_wo_nosete_trim);
        String outputFileName = "output.ly";
        Double[] sound = amplitudes.toArray(new Double[0]);
        String[] args = new String[]{"3000", "0", outputFileName};
        KimiWoNosete.writeNotes(args, sound, view);



        /** create intent for the file containing notes */
        File requestFile = new File(songsDir, outputFileName);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", requestFile);
        String mime = getContentResolver().getType(uri);
        Intent fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT, uri);
        fileIntent.setDataAndType(uri, mime);
        Intent.createChooser(fileIntent, "Open file with:");

        /** try to invoke the intent */
        try {
            startActivity(fileIntent);
        } catch (ActivityNotFoundException e) {
            // Define what your app should do if no activity can handle the intent.
        }
    }
}