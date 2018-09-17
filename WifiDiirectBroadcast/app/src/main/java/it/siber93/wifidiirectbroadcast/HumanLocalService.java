package it.siber93.wifidiirectbroadcast;

/**
 * class that manages the human position and approximates it using sensors
 */
public class HumanLocalService {

    double getCurrentLongitude(){
        return 10.57833;
    }

    double getCurrentLatitude(){
        return 44.74240;
    }

    long getCurrentPositionTimeStamp(){
        return System.currentTimeMillis();
    }

    double getAccuracy(){
        return 1;
    }

    double getCurrentBearing(){
        return 180;
    }

    double getCurrentspeed(){
        return 2;
    }


}
