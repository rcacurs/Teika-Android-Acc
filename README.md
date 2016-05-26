# SmartWear-AS
Repository for android applications used with wearable sensor systems developed in EDI (www.edi.lv)

## Importing project in Android Studio
To use this project clone repository with git:

```
git clone git@git.edi.lv:HealthGroup/SmartWear-AS.git
```

Import project in Android Studio: **File**->**New**->**Import Project...** and select cloned directory.

## Android studio project module description
* **bluetooth-lib** - module containing code for Bluetooth communication with sensor devices. Communication is done using Serial Port Profile. This module is used by all other modules that require bluetooth connection
* **calibration-java-app** - module for java application code for magnetometer calibration. Program has no GUI and should be used from command line.
* **core-app** - module for android application code for surface reconstruction from accelerometer and magnetometer data. Application provides 3D visualization of back and feedback for posture monitoring.
* **graphics3d** - module used in android applications for 3D surface visualization.
* **head-and-posture** - contains code for android application for head state monitoring and posture monitoring. For 
processing only accelerometer data is used.
* **head-tilt** - contains code for android application used for head tilt monitoring using one accelerometer sensor.
* **smartwear-processing-lib** contains java code for accelerometer/magnetometer data processing for orientation estimation and surface reconstruction.
* **visualisation-3d-display-java-app** - java application (desktop) that reconstructs sensor grid shape and prepares data for visualization on volumetric 3D display (developed in EuroLCD company). This is command line application, and it continously generates .obj file with current shape of the surface.
* **yes-no** - android application for alternative communication. Accelerometer sensor is attached to head and head movement is mapped to android screen. The screen contains two segments that represent *positive* and *negative* reply.