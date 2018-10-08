package it.siber93.wifidiirectbroadcast;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import static android.content.Context.SENSOR_SERVICE;

/**
 * class that manages the human position and approximates it using sensors.
 * This class is initiated when the switch button is pressed.
 * This must be extension of position listener and should implement a thread for position
 * approximation using sensor.
 * ATTENTION!! Latitude, Longitude, Accuracy, Bearing and speed must be calculated at the same time
 * in order to have consistent values between them. So this class must have a sampling rate possibly fixed
 * and each property must duly sample.
 *
 */
public class HumanLocalService{

    SensorManager sensorManager;
    SensorEventListener rvListener;             //Listener for rotationVector
    SensorEventListener stepCountListener;      //Listener for stepCounter
    Sensor rotationVectorSensor;                //RotationVector sensor
    Sensor stepCount;                           //StepCounter sensor
    double scX;                                 //Actual Latitude
    double scY;                                 //Actual Longitude
    double lat;                                 //Last latitude received from GPS
    double lon;                                 //Last Longitude received from GPS
    double acc;                                 //Accuracy
    float[] angle = new float[3];               //Rotation vector values of azimuth pitch and roll in rad
    float angolo;                               //Azimuth filtered
    float azimuth;                              //Azimuth value after a step
    long step;                                  //Number of steps from the last gps location received
    long step2;                                 //Number of steps for speed calculation
    double stepLength;
    double r_earth;                             //Earth_radius
    double speed;                               //Movement speed of human between steps
    long startTime;                             //Time variables useful for speed calculation
    long endTime;
    long timestamp;
    boolean first;                              //flag for accuracy calculation
    boolean fst;                                //flag for starting time calculation
    LocationManager locationManager;
    Runnable rn;                                //Runnable for GPS updates over time
    Handler hnd;                                //Handler for GPS updates over time

    /**
     * class constructor
     * @param con application context
     * @param a person height
     * @return nothing
     */
    public HumanLocalService(Context con, int a){
        scX = 0;
        scY = 0;
        lat = 0;
        lon = 0;
        step = 0;
        step2 = 0;
        angolo = 0;
        azimuth = 0;
        acc = 0;
        first = true;
        fst = true;
        stepLength = a*0.415;
        speed = 1;
        r_earth = (float)6378.137;
        sensorManager = (SensorManager)con.getSystemService(SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        stepCount = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        locationManager = (LocationManager) con.getSystemService(Context.LOCATION_SERVICE);

        //RotationVectorListener initialization
        rvListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float offset = (float)Math.toRadians(10);           //Offset for azimuth approximation
                float[] rotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(
                        rotationMatrix, sensorEvent.values);
                //Here the rotationMatrix is remapped to follow device orientation
                float[] remappedRotationMatrix = new float[16];
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedRotationMatrix);
                // Convert to orientations
                float[] orientations = new float[4];
                SensorManager.getOrientation(remappedRotationMatrix, orientations);
                angle = orientations;

                //Azimuth approximation, if the difference is less than offset the azimuth is not modified
                if (angolo == 0) angolo = angle[0];
                else if (Math.abs(angolo-angle[0])>offset) angolo = angle[0];
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        //Listener Registration
        sensorManager.registerListener(rvListener,
                rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

        //StepCounterListener initialization
        stepCountListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                step++;
                step2++;
                timestamp = System.currentTimeMillis();
                if (fst == true) {
                    startTime = System.nanoTime();      //After first step start calculate time in order to calculate speed
                    fst = false;
                }else if(step2%5==0){
                    step2 = 0;
                    endTime = System.nanoTime();
                    double sp = 5*(stepLength / 100) / ((endTime - startTime) / Math.pow(10, 9));
                    if (sp < 4) speed = sp;             //Calculate speed after every step
                    startTime=endTime;
                }
                if (angle != null) {
                    //Calculate Actual longitude and latitude
                    azimuth = angolo;
                    scX += (getDistance(Math.cos(azimuth))/r_earth)*(180/Math.PI)/Math.cos(scY*Math.PI/180);
                    scY += (getDistance(Math.sin(azimuth))/r_earth)*(180/Math.PI);
                    if (step%30==0) {
                        hnd.removeCallbacks(rn);
                        getCurrentLocation();   //ask GPS location every 30 steps
                        hnd.postDelayed(rn, 25000);
                    }
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        //Listener Registration
        sensorManager.registerListener(stepCountListener,
                stepCount, SensorManager.SENSOR_DELAY_FASTEST);
        //Handler to ask GPS coordinates
        hnd = new Handler();
        rn = new Runnable() {
            @Override
            public void run() {
                getCurrentLocation();
                //Schedule handler to call GPS updates every 25 seconds
                hnd.postDelayed(
                        rn,
                        25000);
            }
        };
        hnd.postDelayed(rn,500);
    }

    /**
     * distance calculation using number of step
     * @param st number of step
     * @return distance in km
     */
    //function to calculate distance in km
    public double getDistance(double st){
        double distance = (float)(st*stepLength)/(float)100000;
        return distance;
    }

    /**
     * update actual coordinate using those from GPS
     * @return nothing
     */
    //function to update Actual Coordinate with the GPS
    public void updateCoord(double x, double y){
        if (first!=true) {
            double dlat = Math.abs(x * Math.PI / 180 - scX * Math.PI / 180);
            double dlon = Math.abs(y * Math.PI / 180 - scY * Math.PI / 180);
            double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) + Math.cos(scX * Math.PI / 180) * Math.cos(x * Math.PI / 180) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double d = r_earth * c;
            acc = d * 1000;
        }else first = false;
        scX = x;
        scY = y;
    }

    /**
     * coordinate lat and lon GPS calculation
     * @return nothing
     */
    //function to get current lat and lon from GPS
    public void getCurrentLocation() {

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                try
                {
                    if(locationManager!= null)
                    {
                        Location l = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        lat = l.getLatitude();
                        lon = l.getLongitude();
                        updateCoord(lat, lon);
                    }
                }
                catch (SecurityException e)
                {
                    Log.d("DEBUG", e.getMessage());
                }

                locationManager.removeUpdates(this);
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };
        try
        {
            // Register the listener with the Location Manager to receive location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
        catch (SecurityException e)
        {
            Log.d("DEBUG", e.getMessage());
        }
    }

    /**
     * last calculated latitude
     * @return latitude in degree
     */
    double getCurrentLongitude(){
        return scY;
        //return  10.578581;
    }

    /**
     * last calculated longitude
     * @return longitude in degree
     */
    double getCurrentLatitude(){
        return scX;
        //return 44.742336;
    }

    /**
     * timestamp in which the last position has been acquired
     * @return timestamp in milliseconds
     */
    long getCurrentPositionTimeStamp(){
        return timestamp;
        //return System.currentTimeMillis();
    }

    /**
     * Last position accuracy
     * @return accuracy in meters
     */
    double getAccuracy(){
        return acc;
        //return 1;
    }

    /**
     * Last position bearing(Azimuth)
     * @return Azimuth in degree
     */
    float getCurrentBearing(){
        return (float)Math.toDegrees(azimuth);
        //return 180;
    }

    /**
     * Speed calculated at the timestamp time
     * @return speed in m/s
     */
    double getCurrentspeed(){
         return speed;
        //return 1;
    }
}
