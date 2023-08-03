package guo.yifan.thesheetofmusicjava;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    /** -------- Init ---- */
    // Template song
    public MediaPlayer mySong;
    private static final int MAX_16_BIT = 32768;


    // Button Params
    Button PauseResumeButton;
    public int PauseResumeFlag, PausedLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PauseResumeButton = findViewById(R.id.PauseButton);
        mySong = MediaPlayer.create(this, R.raw.kimi_wo_nosete_trim);

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

    public void LoadAudio(View view) {
        /** Read Audio */
        ArrayList<Double> amplitudes = readAudio_ArrayList(R.raw.kimi_wo_nosete_trim);
        int NumSamples = amplitudes.size();
        Double[] sound = amplitudes.toArray(new Double[0]);
        String[] args = new String[]{"3000", "0", "kimi_wo_nosete_trim.wav"};
        KimiWoNosete.writeNotes(args, sound, view);
    }
}