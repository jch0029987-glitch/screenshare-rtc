# Add project specific ProGuard rules here.
# By default, the contents of this file are subject to preservation directives
# specified in the corresponding configuration files.

-dontwarn org.webrtc.**
-keep class org.webrtc.** { *; }

# Keep C/C++ native entry points or JNI if used later
-keepclasseswithmembernames class * {
    native <methods>;
}
