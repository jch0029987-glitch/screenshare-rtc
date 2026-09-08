# Keep TV activity entry points
-keep public class com.jeremy.rootwebrtcstreamer.tv.TvMainActivity { *; }

# Retain Android WebView and WebChromeClient interface hooks
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * extends android.webkit.WebViewClient { *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { *; }
