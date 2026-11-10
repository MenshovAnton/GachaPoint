# v0.1.0
- First Version

# v0.1.1
- Fixed subscription calculations in the piggy bank: accumulated pulls from past subscriptions no longer reset upon expiration and are now retained in the piggy bank permanently.
- Accurate statistics tracking: the subscription calendar and statistics now calculate missed and claimed rewards strictly within the active period.

# v0.1.2
- Pull History Deletion: Added the ability to delete entries in edit mode with automatic pity recalculation.
- Text Wrapping: Long pull names now properly adapt to screen width.
- Bulk Addition: Fixed date assignment when adding 10-pulls at once.
- Minor UI fixes and improvements.

# v0.1.3
- UI settings: added options to select the app language and theme
- Accessibility: vibration feedback can now be disabled
- Optimization
- Minor UI fixes and improvements.

# v0.1.4
- Database import and export: you can now export data and restore progress from a file (.db).
- Silent mode compliance: when silent mode is enabled, sound effects will not be played.
- Minor fixes and UI improvements.

# v0.2.0
- Modular Home Screen: customizable widgets (Quick Actions, Piggy Bank, Statistics, Pity Counter) with toggleable visibility and flexible ordering.
- Comprehensive Statistics Screen: in-depth analytics on all pulls, 50/50 win rates, average pity, subscription history, and branded summary card export for sharing.
- Calendar & Daily Tasks Tracker: added daily quest tracking and a multi-selection mode for bulk date marking in the calendar.
- Upgraded Piggy Bank: automated progress tracking for dailies and subscription rewards, quick targets (+1, +2, +6 pity goals), and unified Room database storage.
- Feedback & Community Hub: refreshed support screen with direct community links (Telegram, GitHub Issues) and a built-in issue submission form with device diagnostics.
- Material 3 Design & Architecture: updated app styling, enhanced banner and game selection components, and improved database migration (v1 → v2).
- Performance & Stability: eliminated fragment view memory leaks during navigation, fixed notification tap action to open the app, and optimized background workers.


# v0.2.1
- Smart Carousel & Game Visibility: customize which games appear via Settings or the quick-edit button at the end of the pills carousel.
- Mono-Mode (Single Game): when only one game is active, the pills carousel is completely hidden across all screens (Home, Journal, Piggy Bank, Statistics, Tracker), freeing up space for widgets.
- Default Startup Game: choose your favorite game to load immediately on app launch without manual switching.
- Smooth Fluid Animations: seamless container transitions for carousel expand/collapse, animated pill insertion/removal glide (DiffUtil), and zero-flicker selection.
- Android 8.0+ Notification Channels: separate system channels for "Subscription Reminders", "Daily Tasks", and "Important Events" with independent sound and vibration preferences.
- "Smart Silence": intelligent notification dispatching checks today's progress — if your dailies and subscription are already claimed, you won't be disturbed. Disabled games are silenced automatically.
- Pull Reordering in Journal: drag-and-drop pull reordering with smooth slide-in animations and instant pity recalculation.
- Precise Piggy Bank Currency Input: track exact currency amounts (Primogems, Stellar Jades, Polychromes) alongside pulls, featuring a quick-input dialog.
- Stability & Compatibility: fixed storage permissions on Android 8–9 (API ≤ 28), added local diagnostic file logger, and optimized database sync lifecycles.
- Piggy Bank Hotfix: cascading deduction of pulls and gems across all sources (including subscriptions and dailies), plus an option in the menu to clear auto-savings.

# v0.3.0
- Promo Codes Aggregator: built-in hub for up-to-date codes sourced from an open repository, categorized into active, limited stream drops, and permanent codes. Features 1-tap copying, direct links to official redemption pages, and redeemed status tracking.
- Banner Schedule & Rerun Archive: comprehensive banner timeline across phases and patches, countdown timers for active banners, 5★ and 4★ character rosters with visual avatars, and historical rerun intervals.
- Full Honkai Impact 3rd Support: added 4th HoYoverse title with complete gacha logic, custom pity thresholds (90/100), Crystals currency, and Battlesuit Supply passes.
- Visual Character Catalog & Wish Avatars: item database with high-quality character portraits, autocompletion search suggestions in Journal, and automatic patch version tagging under each summon.
- Floating Island Navigation & Polished UI: modern elevated bottom navigation bar, harmonized Material 3 card styling, smoother game transitions, and elimination of progress gauge flicker.
- Express Onboarding: fast initial setup on first launch to easily pick active games and configure preferences in seconds.
- Enhanced Piggy Bank & Quick Converter: exact remainder currency tracking (+120 gems/jades/polychromes), built-in two-way currency converter, and refined cascade deductions for subscription/daily savings.
- Smart Notifications & Deep Linking: tapping notifications now navigates directly into the target screen and game context; added silent filtering for disabled games and timely alerts for promo codes and expiring banners.
- Dedicated Project Support Screen: new section in the More menu featuring an interactive Hall of Fame of supporters and options to contribute to ongoing development.
- Performance & Architecture: upgraded to Room database schema v5, implemented bandwidth-friendly HTTP caching (ETag / If-None-Match), and improved overall background worker reliability.

# v0.2.2
- In-App Help & FAQ: comprehensive visual guide explaining key GachaPoint features (Piggy Bank mechanics, Wish Journal, pity rules, database backups) located in the More menu.
- Interactive Empty States: informative illustrated placeholders in Journal, Piggy Bank, and Statistics offering quick-action buttons for seamless onboarding.
- Project Support: added a direct DonationAlerts link to the "About App" screen for anyone wishing to support future development.
- Future Date Guard: confirmation prompt prevents accidental marking of subscriptions or daily tasks on future calendar dates.
- Carousel & Navigation Stability: fixed carousel scroll conflicts when switching tabs and eliminated pill disappearance during fast navigation.
- Compact Screen Adaptability: polished layout margins, text wrapping, and dialog sizing for small-screen and high-density devices.
- Architecture & Database Optimization: streamlined Room initialization lifecycles, decoupled background WorkManager workers, and cleaned up unused dependencies.
