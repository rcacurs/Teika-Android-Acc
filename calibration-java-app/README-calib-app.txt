calibration-app.jar ir program for magnetometer data calibration for
shape sensing sensor system.

! Before usage sensor system must be paired via bluetooth
via OS bluetooth manager

execution:
    java -jar calibration-app.jar [number_of_samples]
    
    [number_of_samples] - optional paremeter how many samles to acquire. If not set default value
                          of 500 samples are set. Required Time for sample acquisition can be calculated
                          number_of_samples/10 = time for sample acuisition [s]. For 500 samples it is 50 
                          seconds.
program work flow:
    1) programs outputs list of paired bluetooth devices on system with indexes
    2) user must input index of device corresponding to bluetooth device name of sensor system
    3) program notifies that data acquisition will start in x seconds
    4) progress bar is shown to to show data acquisition progress (sensor system must 
       be rotated in various positions during this time)
    5) calibration data is outputed in calibration_data.csv file
    