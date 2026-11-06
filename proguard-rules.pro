# فقط برای نسخه "lite" استفاده می‌شود (نسخه عادی minify ندارد).

# فقط حذف کدهای استفاده‌نشده؛ بدون تغییر نام و بدون بهینه‌سازی تهاجمی (ریسک کمتر)
-dontobfuscate
-dontoptimize

# اخطار کلاس‌های ناموجود (مثل javax.script روی اندروید) خطای ساخت نشود
-dontwarn **

# NewPipeExtractor و موتور جاوااسکریپت Rhino
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

# نگه داشتن enumها و اطلاعات لازم برای reflection
-keepclassmembers enum * { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations
