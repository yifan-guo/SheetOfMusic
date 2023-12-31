package guo.yifan.thesheetofmusicjava;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

public class LilypadFileGenerator {

    private static final String TAG = "SheetOfMusic";

    public static void main (String[] args, Context ctx, File songFile) throws java.io.IOException{
        //prepare to read from file
//        FileReader reader = new FileReader("test.txt");
//        BufferedReader buffer = new BufferedReader(reader);
        final int MARK_LIMIT = 1000;
        String myLine;

        //setup output file to write to
//        FileWriter writer = new FileWriter("output.ly");
//        FileOutputStream writer = ctx.openFileOutput(outputFileName, Context.MODE_PRIVATE);

        FileOutputStream writer = new FileOutputStream(songFile);
        writer.write("\\absolute {\n".getBytes());

        //variables to record magnitude and key on piano for every line & compare max magnitudes across adjacent samples
        Deque<Double> deque = new LinkedList<Double>();

        int lastMark = -1;

        ArrayList<ArrayList<Note>> totalNotes = new ArrayList<ArrayList<Note>>();

        //get data from each line in test.txt
        for ( int i = 0; i < args.length; i++) {
            myLine = args[i];
            if (myLine.contains("START_OF_SAMPLE")) {
                String[] summary = myLine.split(" ");
                double max_sample = Double.parseDouble(summary[1]);//get max magnitude of that sample

                    if (deque.size() == 0) {
                        deque.addLast(max_sample);
                    }
                    else if (deque.size() == 1 && max_sample > deque.getLast()) {// add the loudest note to end of double-ended queue
                        deque.addLast(max_sample);
//                        buffer.mark(MARK_LIMIT);
                        lastMark = i;
                    }
                    else if (deque.size() == 1 && max_sample <= deque.getLast()) { // add the quieter note to the beginning of the queue
                        while (!deque.isEmpty()) {
                            deque.removeFirst();
                        }
                        deque.addLast(max_sample);
                    }
                    else if (deque.size() == 2 && max_sample < deque.getLast()) {   //peak
                        //retain the magnitude of the peak sample before removing it from the deque
                        double max_magnitude = deque.getLast();

                        while (!deque.isEmpty()) {
                            deque.removeFirst();
                        }
                        deque.addLast(max_sample);
//                        buffer.reset();

                        //to count the number of loudest notes in the sample
                        ArrayList<Note> notes = new ArrayList<Note>();

                        //write notes into music score
//                        while ((myLine = buffer.readLine()) != null && !(myLine = buffer.readLine()).contains("START_OF_SAMPLE")) {
                        for (int j = lastMark; j < i; j++) {
                            myLine = args[j];
                            if (!myLine.contains("START_OF_SAMPLE")) {
                                //parse line for note, magnitude, and index
                                String[] note = myLine.split(":");
                                String[] data = note[1].split(" ");      //data[0] is magnitude, data[1] is index
                                double magnitude = Double.parseDouble(data[0]);
                                int index = Integer.parseInt(data[1]);

                                if (magnitude >= max_magnitude * 0.65) {
                                    notes.add(new Note(note[0], magnitude, index));
                                }
                            }
                        }
                        if (notes.size() > 1) {//chords
                            Log.v(TAG, "write chord=" + notes.toString());
                            writeChord(notes, writer);
                        }
                        else if (notes.size() == 1) {//one note
                            Log.v(TAG, "write note=" + notes.toString());
                            writeNote(notes, writer);
                        }
//                        buffer.mark(MARK_LIMIT);
                        lastMark = i;

                        totalNotes.add(notes);
                    }
                    else {//deque.size() == 2 && max_sample >= deque.getLast()
                        deque.addLast(max_sample);
                        deque.removeFirst();
//                        buffer.mark(MARK_LIMIT);
                        lastMark = i;
                    }

            }
        }

        writer.write("\n}\n".getBytes());
        writer.write("\\version \"2.14.0\"".getBytes());
        writer.close();
    }

    public static void writeNote(ArrayList<Note> notes, FileOutputStream writer) throws java.io.IOException {
        for (Note s : notes) {
            int index = s.getIndex();
            if (index < 3 ){
                writer.write((s.getNote() + ",,, ").getBytes());
            }
            else if (index < 15) {
                writer.write((s.getNote() + ",, ").getBytes());
            }
            else if (index < 27) {
                writer.write((s.getNote() + ", ").getBytes());
            }
            else if (index < 39) {
                writer.write((s.getNote() + " ").getBytes());
            }
            else if (index < 51) {
                writer.write((s.getNote() + "' ").getBytes());
            }
            else if (index < 63) {
                writer.write((s.getNote() + "'' ").getBytes());
            }
            else if (index < 75) {
                writer.write((s.getNote() + "''' ").getBytes());
            }
            else if (index < 87) {
                writer.write((s.getNote() + "'''' ").getBytes());
            }
            else {//87
                writer.write((s.getNote() + "''''' ").getBytes());
            }
        }
    }

    public static void writeChord(ArrayList<Note> notes, FileOutputStream writer) throws java.io.IOException{
        writer.write("<".getBytes());
        writeNote(notes, writer);
        writer.write("> ".getBytes());
    }
}
    
    
