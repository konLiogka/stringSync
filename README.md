# Application

This is a guitar tuner app. Download the application from [here](https://github.com/konLiogka/guitarTuner/blob/main/app-debug.apk).
It utilizes the YIN algorithm for pitch detection, made thanks to this [paper](http://audition.ens.fr/adc/pdf/2002_JASA_YIN.pdf). [Here's](https://www.youtube.com/watch?v=pbleU_p67YU&t=7s) a video showing how it works.  
> [!NOTE]
> This app may not work properly(lag) on older phones!! While the app is finished, there are still some things that I need to change and there may be some small bugs.  
 


# Description
This is my first serious programming project. I chose this type of application because its not something common while I was also looking to replace my tuner apps with something free, open source and easy to use. This app was made for guitars, however I found out that it can work on pianos too. Still, it's tuned specifically for guitars so I wouldn't recommend using it on any other instruments. I have also added the option
of creating a custom tuning.


# How it works
The application was made solely using Java in android studio.
The pitch detection algorithm, while not completely handmade, does not use any external libraries.

* First of all windowing is applied to the signal with a buffer size of 8192, may be too heavy for older devices. 
* After computing the windowed signal, it is sent to the YIN algorithm. It basically works by detecting the lowest frequency fundamental of the signal, as there are several pitch frequencies for music notes. 
* The first step describes the autocorrelation function where the initial signal is compared to itself but from a delayed time period. This way the algorithm can find the highest peak which is going to indicate where the period starts/ends. 
* On the other hand, the cumulative mean normalised difference function helps with improving the imperfect periodicity that may occur due to zero-lag dip problem. 
* Then an absolute threshold is applied so we filter out the frequencies we dont want. 
  The paper suggests a threshold of 0.1 for less errors.
  As a note is let to ring, it will be cut at a specific amplitude depending on the threshold. There's also octave based thresholding which essentially splits the range into sub octaves and selects the best estimate lag.
* Next we need to calculate the fundamental frequency of the signal in the correct position, avoiding errors due to noise and disimilar sampling period. 
  This can be done using parabolic interpolation which fits a parabola, resulting in a smoother pitch. It is a very commonly used function in pitch detection algorithms
  for discrete/digital signals as it fits a parabola between 3 points, essentially creating a curve.

The program has multiple tunings pre-included as well as automatic tuning. Manual tuning is way more accurate than automatic.

> [!IMPORTANT]
> There is a notable octave jumping in automatic tuning.
> This is not a problem with manual tuning because I have set a specific value of cents that the note can range based on the selected string.

 ( old images )
## Manual Tuning

 ![standard](https://github.com/konLiogka/guitarTuner/assets/78957746/5513d7a5-05aa-44d5-a252-7ee9bd64b0cd)


## Automatic Tuning
 
![auto](https://github.com/konLiogka/guitarTuner/assets/78957746/e9e8a400-777c-43aa-9ba4-7a9251356732)  


Known bugs: απορροφητήρες 
