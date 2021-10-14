# GPS-Video-Logger<br>
[![Releases](http://img.shields.io/github/release/abinpaul1/GPS-Video-Logger.svg?label=%20release%20&color=green)](https://github.com/abinpaul1/GPS-Video-Logger/releases) [![GitHub license](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/abinpaul1/GPS-Video-Logger/blob/master/LICENSE)<br>
Record and playback video and live location data simultaneously.

<img src = "Screenshots/Launch.png" width ="200" /> <img src = "Screenshots/Camera.png" width ="200" /> <img src = "Screenshots/Journeys.png" width ="200" /> <img src = "Screenshots/Playback.png" width ="200" />

## Description

GPS Video Logger is a simple Android App to record video and geographical position simultaneously.<br>
GPS logging and video tracking is done in an optimized manner.<br>
The app offers two modes of recording : Video Mode and Timelapse Mode<br>
The app provides a video player to view your video and location on map side by side. (Video and GPS player)<br>
Recorded journeys are stored directly in your Internal Storage at : <br>
```Android / data / app.gps_video_logger / files / GPS_Video_Logger /```
(Video as mp4 and track as a GPX file)<br>
<br>
The application can be downloaded from this repository.[GPSVideoLogger](https://github.com/abinpaul1/GPS-Video-Logger/blob/master/app/release/app-release.apk)<br>
It will shortly be made available on Google Play Store.


## Reference documents

[Code of conduct](CODE_OF_CONDUCT.md)

[Repository License](LICENSE)

[Privacy Policy](https://github.com/abinpaul1/GPS-Video-Logger/wiki/Privacy-Policy)

## Frequently Asked Questions
<b>Q</b> - <i>I've just installed the App, but it doesn't read the GPS Signal.</i><br>
<b>A</b> - Please reboot your Device, go in an open Area and try to repeat your test.

<b>Q</b> - <i>How can I backup/copy my Journeys?</i><br>
<b>A</b> - You can see your journey files in a folder in Internal_Storage/Android/data/app.gps_video_logger/. Each journey has a video file in mp4 format and track file(location) in GPX format. Both these files will have the same name (name of your journey). Copy both these files to succesfully copy your journey.

<b>Q</b> - <i>My horizontal videos appear vertical during playback</i><br>
<b>A</b> - Ensure you have enabled the  "Auto Screen Rotate" feature before starting the recording. 

<b>Q</b> - <i>How to import recorded videos from older version of the app?</i><br>
<b>A</b> - In earlier versions, the video and track file was located in `GPS_Video_Logger` folder, dirrectly on Internal Storage. Please copy all the files in that folder to the new location ```Android / data / app.gps_video_logger / files / GPS_Video_Logger /``` and the app will detect them and allow playback.

<strong>Feel free to open issues in the repository for any kind of queries or suggestion</strong>
