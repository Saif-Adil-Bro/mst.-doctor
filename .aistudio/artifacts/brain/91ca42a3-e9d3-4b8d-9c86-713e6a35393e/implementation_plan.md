# গিটহাব অ্যাকশনে Appetize.io স্বয়ংক্রিয় APK ডিপ্লয়মেন্ট প্ল্যান

অ্যাপের বর্তমান গিটহাব অ্যাকশন ওয়ার্কফ্লোর ভেতরেই APK বিল্ড সম্পন্ন হওয়ার পর স্বয়ংক্রিয়ভাবে **Appetize.io**-তে APK পুশ করার স্টেপ যুক্ত করা হচ্ছে। এতে প্রতিটি নতুন পুশ বা ম্যানুয়াল রান করার সাথে সাথে Appetize-এ অ্যাপটি আপলোড বা আপডেট হয়ে যাবে এবং গিটহাব অ্যাকশন সামারিতে সরাসরি **লাইভ প্রিভিউ লিংক (`publicURL`)** ও **`publicKey`** প্রিন্ট হবে।

## User Review & Critical Decisions

> [!IMPORTANT]
> আপনার নিশ্চিত করা পছন্দ অনুযায়ী নিচের সিদ্ধান্তগুলো চূড়ান্ত করা হয়েছে:

- **সিদ্ধান্ত ১ (একই ওয়ার্কফ্লোতে ইন্টিগ্রেশন)**: আলাদা কোনো ফাইল না বানিয়ে বর্তমান APK বিল্ড ওয়ার্কফ্লোর ভেতরেই আর্টিফ্যাক্ট আপলোডের পরের ধাপে Appetize ডিপ্লয়মেন্ট স্টেপ যুক্ত করা হবে।
- **সিদ্ধান্ত ২ (নতুন আপলোড ও এক্সিস্টিং অ্যাপ আপডেট—দুটিই সাপোর্ট)**:
  - গিটহাব সিক্রেটে শুধুমাত্র `APPETIZE_API_TOKEN` দেওয়া থাকলে এটি স্বয়ংক্রিয়ভাবে নতুন অ্যাপ হিসেবে আপলোড করবে এবং নতুন `publicKey` ও লাইভ লিংক তৈরি করবে।
  - আর যদি `APPETIZE_PUBLIC_KEY` সিক্রেটও যুক্ত করা থাকে, তবে নতুন অ্যাপ না বানিয়ে আগের একই লিংকের অ্যাপটিকেই আপডেট করবে।
- **সিদ্ধান্ত ৩ (নিরাপদ ফলব্যাক ও লাইভ প্রিভিউ সামারি)**: যদি কোনো কারণে টোকেন সেট না থাকে, তবে বিল্ড যেন ফেইল না করে একটি স্পষ্ট নির্দেশনা দেখিয়ে স্কিপ করে; আর টোকেন থাকলে আপলোড শেষে গিটহাব অ্যাকশনের **Job Summary**-তে ক্লিকযোগ্য লাইভ এমুলেটর লিংক দেখায়।

---

## 1. Overview & Core Concept

- **What It Does**: গিটহাবে কোড পুশ করলে বা অ্যাকশন ট্যাব থেকে রান করলে প্রথমে Debug APK বিল্ড হবে, গিটহাব আর্টিফ্যাক্টে সেভ হবে এবং পরক্ষণেই Appetize REST API-এর মাধ্যমে APK ফাইলটি Appetize ক্লাউড এমুলেটরে পুশ হবে।
- **Target Audience / Persona**: ডেভেলপার ও টেস্টার, যারা প্রতিবার নতুন পরিবর্তনের পর ব্রাউজারেই Appetize লিংকে ক্লিক করে লাইভ অ্যাপটি টেস্ট করতে চান।
- **Key Value**: ম্যানুয়ালি APK ডাউনলোড করে Appetize ওয়েবসাইটে আপলোড করার ঝামেলা সম্পূর্ণ দূর হবে।

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **গিটহাব সিক্রেট কনফিগারেশন**:
     - রিপোজিটরির `Settings → Secrets and variables → Actions`-এ গিয়ে:
       - `APPETIZE_API_TOKEN`: আপনার Appetize API Token (বাধ্যতামূলক)।
       - `APPETIZE_PUBLIC_KEY`: (ঐচ্ছিক) একই অ্যাপ লিংক বারবার আপডেট করতে চাইলে প্রথমবার পাওয়া `publicKey` এখানে সেট করে রাখা যাবে।
  2. **স্বয়ংক্রিয় বিল্ড ও পুশ**:
     - `main` বা `master` ব্রাঞ্চে পুশ করলে অথবা `Run workflow` বাটনে ক্লিক করলে APK তৈরি হয়ে Appetize-এ আপলোড হবে।
  3. **গিটহাব অ্যাকশন সামারি কার্ড**:
     - অ্যাকশন রান শেষ হলে নিচে একটি সুন্দর মার্কডাউন টেবিল ও বাটন তৈরি হবে, যেখানে **Live Appetize URL (`https://appetize.io/app/...`)**, **Public Key**, এবং **App Version** সরাসরি দেখা যাবে।

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Appetize REST API (`X-API-KEY` ও Basic Auth ডুয়াল কম্প্যাটিবিলিটি)**
  - *Chosen Approach*: `curl` ব্যবহার করে মাল্টিপার্ট ফর্ম-ডেটা (`-F "file=@..." -F "platform=android"`) এবং `X-API-KEY` ও `-u "$TOKEN:"` হেডারসহ Appetize v1 এন্ডপয়েন্টে পাঠানো হবে।
  - *Why*: থার্ড-পার্টি গিটহাব অ্যাকশন প্লাগইন অনেক সময় ডিপেন্ডেন্সি বা নোড ভার্সনের কারণে ভেঙে যায়; সরাসরি অফিশিয়াল REST API ও `jq` ব্যবহার করলে এটি শতভাগ নির্ভরযোগ্য ও দ্রুত কাজ করে।
- **Decision 2: টোকেন না থাকলে গ্রেসফুল স্কিপ (Non-Breaking Build)**
  - *Chosen Approach*: যদি রিপোজিটরিতে এখনো `APPETIZE_API_TOKEN` যোগ করা না হয়ে থাকে, তবে APK বিল্ড সফল থাকবে এবং সামারিতে কীভাবে টোকেন যোগ করতে হবে তার গাইড দেখাবে।
  - *Why*: এতে সিক্রেট সেট করার আগেও আপনার সাধারণ APK বিল্ড কখনোই আটকে যাবে না।

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Pipeline Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    GitHub Actions CI/CD Pipeline                        │
├─────────────────────────────────────────────────────────────────────────┤
│  1. Checkout Code & Setup JDK 17 + Gradle Cache                         │
│                              │                                          │
│                              ▼                                          │
│  2. Inject Firebase Config & Run `./gradlew assembleDebug`              │
│                              │                                          │
│                              ▼                                          │
│  3. Upload APK to GitHub Actions Artifact                               │
│                              │                                          │
│                              ▼                                          │
│  4. Push APK to Appetize.io REST API                                    │
│     ├─ If `APPETIZE_PUBLIC_KEY` exists:                                 │
│     │    POST https://api.appetize.io/v1/apps/{publicKey}               │
│     └─ Else (New Upload):                                               │
│          POST https://api.appetize.io/v1/apps                           │
│                              │                                          │
│                              ▼                                          │
│  5. Parse JSON Response (`jq`) & Write Live URL to `$GITHUB_STEP_SUMMARY`│
└─────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping

1. **APK Discovery**: বিল্ড হওয়া `app/build/outputs/apk/debug/*.apk` ফাইলটি স্বয়ংক্রিয়ভাবে শনাক্ত করবে।
2. **Dynamic Endpoint Routing**:
   - `APPETIZE_PUBLIC_KEY` থাকলে `https://api.appetize.io/v1/apps/$APPETIZE_PUBLIC_KEY`-এ আপডেট রিকোয়েস্ট পাঠাবে।
   - না থাকলে `https://api.appetize.io/v1/apps`-এ নতুন অ্যাপ তৈরি করবে।
3. **Summary Output**: রেসপন্স থেকে `publicKey` ও `publicURL` বের করে গিটহাব অ্যাকশনের সামারিতে ক্লিকযোগ্য প্রিভিউ লিংক প্রিন্ট করবে।
