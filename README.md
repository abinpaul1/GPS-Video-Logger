# GPS-Video-Logger<br>
<a href="https://play.google.com/store/apps/details?id=app.gps_video_logger"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="75"></a>
<a href="https://apt.izzysoft.de/fdroid/index/apk/app.gps_video_logger"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" height="75"></a>


[![Releases](http://img.shields.io/github/release/abinpaul1/GPS-Video-Logger.svg?label=%20release%20&color=green)](https://github.com/abinpaul1/GPS-Video-Logger/releases) [![GitHub license](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/abinpaul1/GPS-Video-Logger/blob/master/LICENSE)<br>
Record and playback video and live location data simultaneously.

<img src = "Screenshots/Launch.png" width ="200" /> <img src = "Screenshots/Camera.png" width ="200" /> <img src = "Screenshots/Journeys.png" width ="200" /> <img src = "Screenshots/Playback.png" width ="200" />

## Description

GPS Video Logger is an ad-free open source lightweight app to record video and geographical position simultaneously. 

The app offers two modes of recording : Video Mode and Timelapse Mode

The app provides a video player to view your video and location on map side by side. (Video and GPS player)

Recorded journeys are stored directly in your Internal Storage at : <br>
```Android / data / app.gps_video_logger / files / GPS_Video_Logger /```
(Video as mp4 and track as a GPX file)<br>
<br>


## Reference documents

[Code of conduct](CODE_OF_CONDUCT.md)

[Repository License](LICENSE)

[Privacy Policy](https://github.com/abinpaul1/GPS-Video-Logger/wiki/Privacy-Policy)

## Frequently Asked Questions
<b>Q</b> - <i>I've just installed the App, but it doesn't read the GPS Signal.</i><br>
<b>A</b> - Please reboot your Device, go in an open Area and try to repeat your test.

<b>Q</b> - <i>How can I backup/copy my Journeys?</i><br>
<b>A</b> - You can see your journey files in a folder in Internal_Storage : ```Android / data / app.gps_video_logger / files / GPS_Video_Logger /``` . Each journey has a video file in mp4 format and track file(location) in GPX format. Both these files will have the same name (name of your journey). Copy both these files to succesfully copy your journey.

<b>Q</b> - <i>My horizontal videos appear vertical during playback</i><br>
<b>A</b> - Ensure you have enabled the  "Auto Screen Rotate" feature before starting the recording. 

<b>Q</b> - <i>How to import recorded videos from older version of the app?</i><br>
<b>A</b> - In earlier versions, the video and track file was located in `GPS_Video_Logger` folder, directly in Internal Storage. Please copy all the files in that folder to the new location ```Android / data / app.gps_video_logger / files / GPS_Video_Logger /``` and the app will detect them and allow playback.

<strong>Feel free to open issues in the repository for any kind of queries or suggestion</strong>
