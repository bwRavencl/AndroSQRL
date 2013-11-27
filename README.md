![AndroSQRL Logo](https://github.com/bwRavencl/AndroSQRL/raw/master/AndroSQRL/res/drawable-hdpi/ic_launcher.png 
"AndroSQRL")
##AndroSQRL

The AndroSQRL project aims to create an user-friendly, open-source SQRL-Client for Android 
devices.  
For information on SQRL itself please refer to: https://www.grc.com/sqrl/sqrl.htm  
This project is very loosely based on geier54's POC for an Android SQRL-Client 
(https://github.com/geir54/android-sqrl).

#####Used Libraries:
- Android.Ed25519 (https://github.com/dazoe/Android.Ed25519)
- ZXing (https://code.google.com/p/zxing)
- ZXScanLib (https://github.com/LivotovLabs/zxscanlib)
- scrypt (https://github.com/wg/scrypt)

#####Important Note:
This software is currently in a highly experimental state and should under no circumstances be 
used in a production environment!

#####License Information:
AndroSQRL is licensed under the Apache 2.0 license.  
For full license details please refer to: http://www.apache.org/licenses/LICENSE-2.0.html

#####Build Instructions:
1. git submodule init
2. git submodule update
3. Import the AndroSQRL, Android.Ed25519 and CaptureActivity projects into Eclipse
