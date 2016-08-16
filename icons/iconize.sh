RES_PATH=../mockingbird/src/main/res

convert mockingbird_512x512.png -scale 192 $RES_PATH/mipmap-xxxhdpi/ic_launcher.png
convert mockingbird_512x512.png -scale 144 $RES_PATH/mipmap-xxhdpi/ic_launcher.png
convert mockingbird_512x512.png -scale 96 $RES_PATH/mipmap-xhdpi/ic_launcher.png
convert mockingbird_512x512.png -scale 72 $RES_PATH/mipmap-hdpi/ic_launcher.png
convert mockingbird_512x512.png -scale 48 $RES_PATH/mipmap-mdpi/ic_launcher.png
