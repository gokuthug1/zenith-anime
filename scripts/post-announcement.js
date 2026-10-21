/**
 * Zenith Anime — Discord Webhook Announcement Script
 * Sends formatted announcements to Discord as Zenith Bot with custom avatar and role mention.
 *
 * Usage:
 *   node scripts/post-announcement.js [optional_webhook_url]
 */

const DEFAULT_WEBHOOK_URL = 'https://discord.com/api/webhooks/1561832124498190376/FWJ2oWAk2sPb45HkAiylwNJomXzW958s6-mczMxZmePdODD9362MpSzREglxV_WUE_ZG';

const webhookUrl = process.argv[2] || process.env.DISCORD_ANNOUNCE_WEBHOOK || DEFAULT_WEBHOOK_URL;

const announcementText = `<@&1560147127416651856> 

# 🌟 **ZENITH ANIME WEB STREAMING EDITION IS OFFICIALLY LIVE!** 🌟

I'm excited to announce that **Zenith Anime** is no longer just for Android — the official **Web Streaming Client** is now live and ready to stream on any browser and any device! 🎉

It works even if you’re on a **PC, Mac, Laptop, iPad, Tablet, Chromebook, or Phone**, you can now stream all your favorite anime directly in your browser with zero installation required!

---

### 🌐 **Stream Now (Free & No Ads)**
> 🔗 **Web App:** https://gokuthug1.github.io/zenith-anime/
> 📦 **Android APK:** https://gokuthug1.github.io/zenith-anime/landing.html
> 💻 **GitHub Repo:** https://github.com/gokuthug1/zenith-anime

---

### ✨ **What's New in Zenith Web:**

* 🎬 **Multi-Server Streaming:** Instant 1-click switching between high-speed video servers (**AniEmbed**, **Viduki**, **MegaPlay**, and **VidSrc**).
* 🎧 **Sub & Dub Support:** Effortlessly toggle between English Subbed and Dubbed audio streams.
* 📱 **True Responsive Layout:** Automatic screen-size adaptation — clean top navigation on desktops/laptops, and a floating glassmorphic dock on mobile/tablets.
* 🎨 **8 Luminous Theme Accents:** Customize your look in Settings (Emerald, Cyan, Sakura, Violet, Amber, Crimson, Indigo, and Gold).
* ⏱️ **Local-First Progress:** Automatically saves your watched episodes and "Continue Watching" progress right in your browser.
* 💎 **100% Free Forever:** No paywalls, no subscriptions, and no monetization.

---

### 💬 **Feedback & Bug Reports**
Please use <#1560149234211364894> and let me know your thoughts, feature requests, or any issues you encounter. Enjoy the streams! 🍿✨`;

async function postAnnouncement() {
  try {
    const res = await fetch(webhookUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        content: announcementText,
        username: 'Zenith Bot',
        avatar_url: 'https://raw.githubusercontent.com/gokuthug1/zenith-anime/master/docs/assets/ic_zenith_logo.png',
        allowed_mentions: {
          roles: ['1560147127416651856']
        }
      })
    });

    if (res.ok || res.status === 204) {
      console.log('✅ Announcement successfully posted to Discord as Zenith Bot!');
    } else {
      const err = await res.text();
      console.error('❌ Failed to post announcement:', res.status, err);
    }
  } catch (err) {
    console.error('❌ Error sending webhook:', err);
  }
}

postAnnouncement();
