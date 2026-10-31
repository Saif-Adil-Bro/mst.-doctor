# IconKitchen-কম্প্যাটিবল অ্যাপ আইকন সেটআপ প্ল্যান

## ১. লক্ষ্য
অ্যাপে একটি সুন্দর ডিফল্ট/ডামি হেলথ আইকন সেট করা হবে এবং ফাইল স্ট্রাকচার হুবহু **IconKitchen (icon.kitchen)**-এর স্ট্যান্ডার্ড এক্সপোর্ট ফরম্যাটে তৈরি করা হবে—যাতে আপনি IconKitchen থেকে ডাউনলোড করা `res/` ফোল্ডারের ফাইলগুলো `app/src/main/res/`-এ পেস্ট বা রিপ্লেস করলেই কোনো কোড পরিবর্তন ছাড়াই সরাসরি আপনার আইকন সিঙ্ক হয়ে যায়।

---

## ২. যা যা তৈরি ও কনফিগার করা হবে

### ক. `AndroidManifest.xml` আপডেট
- বর্তমানে `android:icon="@android:drawable/sym_def_app_icon"` দেওয়া আছে। এটি পরিবর্তন করে স্ট্যান্ডার্ড লঞ্চার আইকন রেফারেন্স দেওয়া হবে:
  - `android:icon="@mipmap/ic_launcher"`
  - `android:roundIcon="@mipmap/ic_launcher_round"`

### খ. IconKitchen-এর হুবহু ফোল্ডার ও ফাইল স্ট্রাকচার তৈরি (`app/src/main/res/`)
IconKitchen যখন Android আইকন জেনারেট করে, তখন নিচের ফাইলগুলো তৈরি করে। আমরা ঠিক একই নামে ফাইলগুলো প্রস্তুত করে রাখব:
1. **`res/values/ic_launcher_background.xml`**
   - `<color name="ic_launcher_background">#2E7D32</color>`
2. **`res/mipmap-anydpi-v26/ic_launcher.xml` এবং `ic_launcher_round.xml`**
   - Adaptive Icon কনফিগারেশন যা `@color/ic_launcher_background`, `@mipmap/ic_launcher_foreground` এবং `@mipmap/ic_launcher_monochrome` ব্যবহার করবে (IconKitchen-এর ডিফল্ট XML স্ট্রাকচার)।
3. **সকল ডেনসিটি ফোল্ডার (`mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, `mipmap-xxxhdpi`):**
   - প্রতিটি ফোল্ডারে ৪টি করে ফাইল থাকবে:
     - `ic_launcher.png`
     - `ic_launcher_round.png`
     - `ic_launcher_foreground.png`
     - `ic_launcher_monochrome.png`

---

## ৩. আপনি কীভাবে আপনার আইকন পেস্ট করবেন
- IconKitchen থেকে ডাউনলোড করা ZIP ফাইলের ভেতর থাকা `android/res/` ফোল্ডারের সব ফাইল কপি করে সরাসরি প্রজেক্টের `app/src/main/res/` ফোল্ডারে পেস্ট (Replace) করে দিলেই সাথে সাথে আপনার আইকন কাজ করবে।
