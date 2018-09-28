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
    float angolo;                               //Azimuth approximation
    float azimuth;                              //Azimuth value after a step
    float[] rvVal = new float[3];               //Rotation vector values in degree
    long step;                                  //Number of steps from the last gps location received
    double stepLength;
    double r_earth;                             //Earth_radius
    double speed;                               //Movement speed of human between steps
    long startTime;
    long endTime;
    long curTime;
    LocationManager locationManager;
    Context cnt;


    /**
     * class constructor
     * @param con application context
     * @return nothing
     */
    public HumanLocalService(Context con){
        scX = 0;
        scY = 0;
        lat = 0;
        lon = 0;
        step = 0;
        angolo = 0;
        azimuth = 0;
        acc = 0;
        stepLength =78;
        speed = 0.1;
        cnt = con;
        r_earth = (float)6378.137;
        sensorManager = (SensorManager)con.getSystemService(SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        stepCount = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        locationManager = (LocationManager) con.getSystemService(Context.LOCATION_SERVICE);


        //((WiFiServiceDiscoveryActivity)con).appendStatus("CIAO");

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
                //Azimuth approximation, if the difference is less than offset the azimuth is approximated
                if (angolo == 0) angolo = angle[0];
                else if (Math.abs(angolo-angle[0])<offset) angolo=(angolo+angle[0])/2;
                else angolo = angle[0];
                for(int i = 0; i < 3; i++) {
                    orientations[i] = (float)(Math.toDegrees(orientations[i]));
                }
                rvVal = orientations;
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
                curTime = System.nanoTime();
                if (step == 1) {
                    startTime = System.nanoTime();      //After first step start calculate time in order to calculate speed
                }else if(step%5==0){
                    endTime = System.nanoTime();
                    double sp = 5*(stepLength / 100) / ((endTime - startTime) / Math.pow(10, 9));
                    if (sp < 4) speed = sp;             //Calculate speed after every step
                    //((WiFiServiceDiscoveryActivity)cnt).appendStatus(String.valueOf(" speed "+speed));
                    startTime=endTime;
                }
                if (angle != null) {
                    //Calculate Actual longitude and latitude
                    azimuth = angolo;
                    scX += (getDistance(Math.cos(azimuth))/r_earth)*(180/Math.PI)/Math.cos(scY*Math.PI/180);
                    scY += (getDistance(Math.sin(azimuth))/r_earth)*(180/Math.PI);
                    if (step%30==0) getCurrentLocation();   //ask GPS location every 30 steps
                }
            }
            // step length = altezza * 0.415 o 0.413 altrimenti 78 cm o 70 cm
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        //Listener Registration
        sensorManager.registerListener(stepCountListener,
                stepCount, SensorManager.SENSOR_DELAY_FASTEST);
        getCurrentLocation();
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
        double dlat = Math.abs(x*Math.PI/180-scX*Math.PI/180);
        double dlon = Math.abs(y*Math.PI/180-scY*Math.PI/180);
        double a = Math.sin(dlat/2)*Math.sin(dlat/2)+Math.cos(scX * Math.PI / 180)*Math.cos(x * Math.PI / 180)*Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = r_earth * c;
        acc = d*1000;
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
        return curTime;
        //return System.currentTimeMillis();
    }

    /**
     * Last position accuracy
     * @return accuracy in meters
     */
    double getAccuracy(){
        //((WiFiServiceDiscoveryActivity)cnt).appendStatus(String.valueOf(" accuracy: "+acc));
        return acc;
        //return 1;
    }

    /**
     * Last position bearing(Azimuth)
     * @return Azimuth in degree
     */
    double getCurrentBearing(){
        return Math.toDegrees(azimuth);
        //return 180;
    }

    /**
     * Speed calculated at the timestamp time
     * @return speed in m/s
     */
    double getCurrentspeed(){
        long ttime = System.nanoTime();
        //if time passed from last step is more than 2 sec then the human has stopped move and return 0.1
        if ((startTime == endTime) && (((ttime - curTime) / Math.pow(10, 9))>=2)){
            return 0.1;
        }else return speed;
        //return 1;
    }
}
