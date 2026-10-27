# 🌿 স্বাস্থ্য জ্ঞান - Android App

**Body & Health Knowledge** — বাংলা আয়ুর্বেদ স্বাস্থ্যতথ্য অ্যাপ

## আর্কিটেকচার: Offline-First

```
বান্ডেলড ডেটা (assets/health_articles.json)
    ↓ প্রথম ইনস্টলে স্বয়ংক্রিয়ভাবে Room DB-তে লোড
    ↓ সব সময় অফলাইনে কাজ করে

Firebase Firestore (নতুন ডেটা)
    ↓ ইন্টারনেট সংযোগে SyncWorker চলে (প্রতি ৬ ঘণ্টা)
    ↓ শুধু নতুন আর্টিকেল Room DB-তে যোগ হয়
    ↓ এরপর অফলাইনেও দেখা যাবে
```

## ফিচার
- ✅ ইনস্ট্যান্ট সার্চ (Room DB, 300ms debounce)
- ✅ ক্যাটাগরি ফিল্টার (চোখ, পেট, ঘুম...)
- ✅ ট্যাগ সিস্টেম (ভেষজ/ওষুধ আলাদা)
- ✅ বুকমার্ক
- ✅ শেয়ার ও কপি
- ✅ Dark Mode
- ✅ Firebase নতুন ডেটা অটো-সিঙ্ক
- ✅ Admin Panel (Firebase-এ নতুন ডেটা যোগ)

## সেটআপ
1. `google-services.json` Firebase থেকে ডাউনলোড করে `app/` ফোল্ডারে রাখুন
2. `health_articles.json` → `app/src/main/assets/` ফোল্ডারে রাখুন
3. Android Studio-তে Sync করুন
4. Run করুন
