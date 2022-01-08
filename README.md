# android-photo-booth
Simple party photo booth app for Android

Takes four pictures a few seconds apart and creates a photo collage. These can be printed and automatically uploaded to a shared Google Photos album.

Originally wrote this app for a birthday party after looking into photo booth rental and realizing that many of them were just an iPad, a light, a printer and some props. Since I already had a tripod, a ring light, a printer and an Android tablet I could create my own, but I couldn't find an app that I liked.

Here is my setup:
![Setup](/pics/setup.jpg?raw=true "Photo Booth Setup")

I bought a TECELKS tripod mount adapter online for under $10 to attach the tablet to the tripod which was just big enough to hold my 10" Lenovo tablet in landscape mode.

## Backgrounds

You can add your own background for the collages to the assets/backgrounds folder. Background images should have a 3x2 aspect ratio so that they will print correctly. I recommend 1920x1280. The photos are laid out on the background such that the bottom right corner is not used so that is a good place to put a message or a logo. To change which background image is used, modify the *background* property in SettingsService.kt.

## Printing

The collages are created with a 3x2 aspect ratio which works well with 4x6 inch photo paper. Printing support on Android is kind of finicky. When you hit the print button in the app it launches the standard Android print dialog where you can choose the printer. If you have a printer on your wifi network it should show up there although you may need to add a print service in Settings->Connected Devices->Preferences->Printing if it doesn't show up at first. After you have done this once, the same printer should automatically be selected the next time you print a photo although with my setup it sometimes didn't and I had to pick the printer again.

## Google Photos

The photo collages are automatically uploaded to a Google Photos Album. You can set the name of the album by modifying *albumName* in SettingsService.kt. If the album does not already, exist it will be created and made shareable.

You will need to get a client ID and a client secret from your Google Cloud Platform account for Google Photos. Follow the directions here: https://developers.google.com/photos/library/guides/get-started. Note that even this is an Android app, you need to create a credential of type "Web Application" in order to get a client secret. You can leave authorized origins and redirect URL blank.

Once you have the client ID and secret, make a file in the project root named apikeys.properties and add the client ID and secret to it as follows (the double quotes are required):

```
GOOGLE_PHOTOS_CLIENT_ID="client id goes here"
GOOGLE_PHOTOS_CLIENT_SECRET="<lient secret goes here"
```

Photos are also added to the gallery on the device so if you have the Google Photos app installed, they will be backed up to Google Photos by the app even if you don't configure the client ID/secret. However they will not be added to a shared album. If the photos are uploaded using this app, they are uploaded to a shared album and the app displays a QR code with the link to it so that your party guests can get digital copies.








