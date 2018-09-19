package it.siber93.wifidiirectbroadcast;

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
public class HumanLocalService {


    // TODO create a constructor where start the thraeds or position approximation


    /**
     * last calculated latitude
     * @return latitude in degree
     */
    double getCurrentLongitude(){
        return 10.57833;
    }

    /**
     * last calculated longitude
     * @return longitude in degree
     */
    double getCurrentLatitude(){
        return 44.74240;
    }

    /**
     * timestamp in which the last position has been acquired
     * @return timestamp in milliseconds
     */
    long getCurrentPositionTimeStamp(){
        return System.currentTimeMillis();
    }

    /**
     * Last position accuracy
     * @return accuracy in meters
     */
    double getAccuracy(){
        return 1;
    }

    /**
     * Last position bearing(Azimuth)
     * @return Azimuth in degree
     */
    double getCurrentBearing(){
        return 180;
    }

    /**
     * Speed calculated at the timestamp time
     * @return speed in m/s
     */
    double getCurrentspeed(){
        return 2;
    }


}
