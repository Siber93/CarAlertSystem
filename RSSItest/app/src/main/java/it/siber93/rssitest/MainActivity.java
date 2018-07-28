package it.siber93.rssitest;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Environment;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    // Beacon discovery variables
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager;

    // Redraws a plot whenever an update is received:
    private class MyPlotUpdater implements Observer {
        Plot plot;

        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }

        @Override
        public void update(Observable o, Object arg) {
            plot.redraw();
        }
    }

    // Plot graphic object
    private XYPlot dynamicPlot;

    // Plot updater, observer of the data struct
    private MyPlotUpdater plotUpdater;

    // True = the system is enabled to receive beacons/signal, False = system in idle
    private boolean onRecording = false;

    // The list of the series
    private ArrayList<SampleDynamicSeries> mySeries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Beacon manager creation
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.bind(this);

        // Plot initialization
        // get handles to our View defined in layout.xml:
        dynamicPlot = (XYPlot) findViewById(R.id.plot);

        // Initialize the data-observer/plot-updater
        plotUpdater = new MyPlotUpdater(dynamicPlot);

        // only display whole numbers in domain labels
        dynamicPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
                setFormat(new DecimalFormat("0"));

        // Initialize data struct
        mySeries = new ArrayList<>();
        mySeries.add(new SampleDynamicSeries(0, "beacon"));

        // Creating the formatter for graph line 1
        LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                Color.rgb(0, 200, 0), null, null, null);
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(10);

        // Add serie 1 to the plot
        dynamicPlot.addSeries(mySeries.get(0),
                formatter1);


        // thin out domain tick labels so they dont overlap each other:
        dynamicPlot.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
        dynamicPlot.setDomainStepValue(5);

        dynamicPlot.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
        dynamicPlot.setRangeStepValue(10);

        dynamicPlot.getGraph().getLineLabelStyle(
                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));

        // Freeze the range boundaries:
        dynamicPlot.setRangeBoundaries(-100, 100, BoundaryMode.FIXED);

        // create a dash effect for domain and range grid lines:
        /*DashPathEffect dashFx = new DashPathEffect(
                new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
        dynamicPlot.getGraph().getDomainGridLinePaint().setPathEffect(dashFx);
        dynamicPlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);*/

    }

    // ######### Menu ###############

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.start:
                onRecording = true;
                return true;
            case R.id.stop:
                onRecording = false;
                return true;
            case R.id.export:
                for (SampleDynamicSeries x:mySeries)
                {
                    x.export();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
    // ######### Menu ###############


    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Log.i(TAG, "The first beacon I see is about "+beacons.iterator().next().getDistance()+" meters away.");
                }
            }
        });

        try
        {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        }
        catch (RemoteException e)
        {

        }
    }


    class SampleDynamicSeries implements XYSeries {
        private ArrayList<Double> y_values;                             // y values of the series
        private int seriesIndex;                                        // Index of the series in the series_array
        private String title;                                           // Name of the serie

        public SampleDynamicSeries(int seriesIndex, String title) {
            this.y_values = new ArrayList<>();
            this.seriesIndex = seriesIndex;
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int size() {
            return y_values.size();
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) { return y_values.get(index); }

        public void addElement(Double val)
        {
            y_values.add(val);
        }

        public void resetSerie()
        {
            y_values.clear();
        }

        // Export the series in csv file
        public boolean export()
        {
            File directoryDownload =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            //Creates a new folder in DOWNLOAD directory
            File logDir = new File (directoryDownload, "RSSItest");
            logDir.mkdirs();

            // Create new file
            File file = new File(logDir, title+".csv");

            FileOutputStream outputStream = null;
            try
            {
                file.createNewFile();
                outputStream = new FileOutputStream(file, true);
                for (int i = 0; i < y_values.size(); i ++)
                {
                    outputStream.write((y_values.get(i) + "\n").getBytes());
                }
                outputStream.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
            return true;
        }

    }
}
