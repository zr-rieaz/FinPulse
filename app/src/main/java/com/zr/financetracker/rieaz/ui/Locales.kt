package com.zr.financetracker.rieaz.ui

object Locales {
    private val dictionary = mapOf(
        "appName" to Pair("FinPulse", "ফিনপালস"),
        "tabDashboard" to Pair("Dashboard", "ড্যাশবোর্ড"),
        "tabReports" to Pair("Reports", "বিলিং রিপোর্ট"),
        "tabLogs" to Pair("Transactions", "লেনদেন"),
        "tabNotepad" to Pair("Notepad", "নোটপ্যাড"),
        "tabBudgets" to Pair("Notepad", "নোটপ্যাড"),
        "tabSettings" to Pair("Settings", "সেটিংস"),
        "todaysSpent" to Pair("Today's Spend", "আজকের খরচ"),
        "monthlySavings" to Pair("Monthly Savings", "মাসিক সঞ্চয়"),
        "earned" to Pair("Earned", "মোট আয়"),
        "spent" to Pair("Spent", "মোট ব্যয়"),
        "netSavings" to Pair("Net Savings", "নেট সঞ্চয়"),
        "savingsPercent" to Pair("Savings %", "সঞ্চয় হার"),
        "spendPercent" to Pair("Spend %", "ব্যয় হার"),
        "topCategories" to Pair("Top Spending Categories", "সর্বোচ্চ খরচের ক্যাটাগরি সমূহ"),
        "addTransaction" to Pair("Add New Transaction", "নতুন লেনদেন যুক্ত করুন"),
        "expense" to Pair("Expense", "ব্যয় (খরচ)"),
        "income" to Pair("Income", "আয়"),
        "all" to Pair("All", "সব রেকর্ড"),
        "noTransactions" to Pair("No transaction records in this cycle", "এই চক্রে কোনো রেকর্ড নেই"),
        "billingReports" to Pair("Billing Reports", "বিলিং রিপোর্ট"),
        "statementLogs" to Pair("Statement Logs", "লেনদেন রেকর্ডসমূহ"),
        "monthlyBudgets" to Pair("Monthly Budgets", "মাসিক বাজেটসীমা"),
        "deviceOptions" to Pair("Device Options", "ডিভাইস সেটিংস"),
        "cycleRefreshes" to Pair("Cycle Refreshes in %d Days", "লিমিট চক্র রিফ্রেশ হবে %d দিনে"),
        "backupSaved" to Pair("Backup saved to Downloads!", "ডাউনলোডে ব্যাকআপ সেভ হয়েছে!"),
        "backupRestored" to Pair("Backup restored successfully!", "ব্যাকআপ সফলভাবে রিস্টোর হয়েছে!"),
        "searchPlaceholder" to Pair("Search transactions...", "লেনদেন রেকর্ড খুঁজুন..."),
        "factoryReset" to Pair("Factory Reset", "ফ্যাক্টরি রিসেট"),
        "exportJson" to Pair("Export JSON Backup", "JSON ব্যাকআপ এক্সপোর্ট"),
        "restoreFrom" to Pair("Restore from Backup", "ব্যাকআপ রিস্টোর করুন"),
        "settings" to Pair("Settings", "সেটিংস"),
        "dashboard" to Pair("Dashboard Overviews", "ড্যাশবোর্ড ওভারভিউ"),
        "today" to Pair("Today", "আজ"),
        "freeLifetimeUnl" to Pair("Free Lifetime Unlocked", "আজীবন ফ্রি আনলকড"),
        "proActive" to Pair("PRO Active", "প্রো অ্যাক্টিভ"),
        "activeCategoryBudgets" to Pair("Active Category Budgets", "চলতি ক্যাটাগরি বাজেট"),
        "category" to Pair("Category", "ক্যাটাগরি"),
        "merchant" to Pair("Merchant / Vendor", "মার্চেন্ট / ভেন্ডর"),
        "amount" to Pair("Amount", "পরিমাণ"),
        "date" to Pair("Date", "তারিখ"),
        "saveTransaction" to Pair("Save Transaction", "লেনদেন সংরক্ষণ করুন"),
        "editTransaction" to Pair("Edit Transaction", "লেনদেন সম্পাদনা করুন"),
        "cancel" to Pair("Cancel", "বাতিল"),
        "latestDailyActivity" to Pair("Latest Daily Activity", "সাম্প্রতিক লেনদেনসমূহ"),
        "titlePlaceholder" to Pair("e.g., Grocery Shopping, Uber ride", "যেমন: বাজার শপিং, উবার ভাড়া"),
        "merchantPlaceholder" to Pair("e.g., Shwapno, Foodpanda, Netflix", "যেমন: স্বপ্ন, ফুডপান্ডা, নেটফ্লিক্স"),
        "wipeLogs" to Pair("Wipe Logs", "লেনদেন রেকর্ড মুছুন"),
        "wipeConfirm" to Pair("Are you sure you want to clear all transaction records?", "আপনি কি নিশ্চিত যে সব লেনদেন রেকর্ড মুছে ফেলতে চান?"),
        "yesClear" to Pair("Yes, Clear All", "হ্যাঁ, সব মুছুন"),
        "categories" to Pair("CATEGORIES", "ক্যাটাগরি"),
        "merchants" to Pair("MERCHANTS", "মার্চেন্ট"),
        "profileSettings" to Pair("Profile Settings", "প্রোফাইল সেটিংস"),
        "username" to Pair("Name", "ইউজার নেম"),
        "currency" to Pair("Currency Options", "কারেন্সি সেটিংস"),
        "language" to Pair("Language Settings", "ভাষা"),
        "saveChanges" to Pair("Save Profile Changes", "প্রোফাইল তথ্য সংরক্ষণ"),
        "notificationsSection" to Pair("Notifications & Alerts", "নোটিফিকেশন ও অ্যালার্ট"),
        "reminderToggle" to Pair("Transaction Reminders", "দৈনিক লেনদেন রিমাইন্ডার"),
        "alertsToggle" to Pair("Budget Limit Warnings (>80%)", "বাজেট লিমিট সতর্কবার্তা"),
        "budgetCeilings" to Pair("Category Budget Ceilings", "ক্যাটাগরি বাজেটসীমা সেটিংস"),
        "backupRestore" to Pair("Data Backup & Restore", "ডেটা ব্যাকআপ ও রিস্টোর"),
        "backupRestoreDesc" to Pair("Download your data as a JSON file or restore from a previous backup.", "আপনার সকল ডেটা ব্যাকআপ JSON ফাইল হিসেবে সেভ করুন বা পূর্বের ব্যাকআপ রিস্টোর করুন।"),
        "factoryResetButton" to Pair("Factory Reset App", "ফ্যাক্টরি রিসেট করুন"),
        "dangerZone" to Pair("Danger Zone", "ঝুঁকিপূর্ণ সেটিংস"),
        "resetPrompt" to Pair("Please type \"RESET\" to confirm factory reset.", "ফ্যাক্টরি রিসেট নিশ্চিত করতে \"RESET\" লিখুন।"),
        "invalidBackup" to Pair("Invalid backup file! Please use a valid FinPulse JSON.", "অকার্যকর ব্যাকআপ ফাইল! সঠিক ফিনপালস JSON ফাইল ব্যবহার করুন।"),
        "restoreConfirm" to Pair("This will replace all your current data with the backup data. Continue?", "এটি আপনার বর্তমান সকল ডেটা ব্যাকআপ ফাইল দ্বারা প্রতিস্থাপন করবে। আপনি কি নিশ্চিত?"),
        "yesRestore" to Pair("Yes, Restore", "হ্যাঁ, রিস্টোর করুন"),
        "update" to Pair("Update", "আপডেট"),
        "budgetCeilingUpdated" to Pair("Budget ceiling updated!", "বাজেটসীমা আপডেট হয়েছে!"),
        "profileSavedSuccess" to Pair("Profile settings saved successfully!", "প্রোফাইল সেটিংস সংরক্ষিত হয়েছে!"),
        "cycleMonth" to Pair("Cycle Month", "হিসাব চক্র মাস"),
        "categoryBreakdown" to Pair("Category Breakdown", "ক্যাটাগরি বিস্তারিত"),
        "totalExpense" to Pair("Total Exp.", "মোট ব্যয়"),
        "addTransactionBtn" to Pair("+ Add Transaction", "+ লেনদেন যুক্ত করুন"),
        "undo" to Pair("Undo", "পুনশ্চ"),
        "deleted" to Pair("Transaction deleted", "লেনদেন ডিলিট করা হয়েছে"),
        "proBadge" to Pair("PRO Unlocked", "প্রো আনলক"),
        "deleteTransactionTitle" to Pair("Delete Transaction", "লেনদেন মুছে ফেলুন"),
        "deleteTransactionConfirm" to Pair("Are you sure you want to delete this transaction?", "আপনি কি নিশ্চিত যে আপনি এই লেনদেনটি মুছে ফেলতে চান?"),
        "ok" to Pair("OK", "ঠিক আছে"),
        "budgetTrend" to Pair("Spending Trend (Cumulative)", "ব্যয় বিবর্তন ট্রেইন্ড (যৌগিক)"),
        "cycleRefreshesDays" to Pair("Cycle Refreshes in %d Days", "লিমিট চক্র রিফ্রেশ হবে %d দিনে"),
        "balance" to Pair("Balance", "ব্যালেন্স"),
        "yearlySavings" to Pair("Yearly Savings", "বার্ষিক সঞ্চয়"),
        "changeYear" to Pair("Change Year", "বছর পরিবর্তন"),
        "notepadTitle" to Pair("Quick Notepad", "কুইক নোটপ্যাড"),
        "searchNotes" to Pair("Search notes...", "নোট খুঁজুন..."),
        "newNote" to Pair("New Note", "নতুন নোট"),
        "editNote" to Pair("Edit Note", "নোট সম্পাদনা"),
        "noteTitlePlaceholder" to Pair("Title (e.g. Budget plan, market list)", "শিরোনাম (যেমন: মাসিক বাজারের তালিকা)"),
        "noteContentPlaceholder" to Pair("Write down financial plans, thoughts, or quick notes...", "এখানে আপনার হিসাব বা প্রয়োজনীয় নোট লিখে রাখুন..."),
        "noNotes" to Pair("No notes yet. Tap '+' to write your first note!", "কোনো নোট নেই। নতুন নোট লিখতে '+' বাটন চাপুন!"),
        "saveNote" to Pair("Save Note", "নোট সংরক্ষণ করুন"),
        "deleteNoteConfirm" to Pair("Are you sure you want to delete this note?", "আপনি কি নিশ্চিত যে এই নোটটি মুছে ফেলতে চান?"),
        "contactUs" to Pair("Contact Us", "যোগাযোগ করুন"),
        "contactUsDesc" to Pair("Feel free to reach out for suggestions or feedback", "পরামর্শ বা যেকোনো সহায়তার জন্য সরাসরি যোগাযোগ করতে পারেন"),
        "whatsapp" to Pair("WhatsApp", "হোয়াটসঅ্যাপ"),
        "githubProfile" to Pair("GitHub", "গিটহাব"),
        "developedByRieaz" to Pair("Developed by Rieaz", "ডেভেলপ করেছেন রিয়াজ (Rieaz)"),
        "byRieaz" to Pair("by Rieaz", "রিয়াজ"),
        "aboutDeveloper" to Pair("About & Developer", "অ্যাপ ও ডেভেলপার পরিচিতি"),
        "leadDeveloper" to Pair("Lead Developer & Architect", "মূল সফটওয়্যার ডেভেলপার"),
        "developerName" to Pair("Rieaz", "রিয়াজ (Rieaz)"),
        "appVersionLabel" to Pair("Version 2.5.3 (Build 48)", "ভার্সন ২.৫.৩ (বিল্ড ৪৮)"),
        "appDescription" to Pair("FinPulse is an offline-first, high-precision personal finance and budget manager developed by Rieaz.", "ফিনপালস হলো রিয়াজ কর্তৃক তৈরি সম্পূর্ণ অফলাইন, নির্ভরযোগ্য এবং আধুনিক ব্যক্তিগত আয়-ব্যয় ট্র্যাকিং অ্যাপ।"),
        "privacyGuarantee" to Pair("100% Offline & Private", "১০০% অফলাইন ও নিরাপদ ডেটা"),
        "handcraftedTag" to Pair("Engineered with Precision", "নিখুঁত দক্ষতায় প্রস্তুত"),
        "allRightsReserved" to Pair("© 2026 FinPulse by Rieaz. All rights reserved.", "© ২০২৬ ফিনপালস বাই রিয়াজ। সর্বস্বত্ব সংরক্ষিত।")
    )

    fun getString(key: String, languageCode: String): String {
        val entry = dictionary[key] ?: return key
        return if (languageCode == "bn") entry.second else entry.first
    }

    fun translateCategory(category: String, languageCode: String): String {
        val isBn = languageCode == "bn"
        return when (category.lowercase().trim()) {
            "food" -> if (isBn) "খাবার ও রেস্তোরাঁ" else "Food"
            "rent" -> if (isBn) "বাড়িভাড়া" else "Rent"
            "transport" -> if (isBn) "পরিবহন ও যাতায়াত" else "Transport"
            "health" -> if (isBn) "চিকিৎসা ও স্বাস্থ্য" else "Health"
            "education" -> if (isBn) "শিক্ষা ও পড়াশোনা" else "Education"
            "entertainment" -> if (isBn) "বিনোদন" else "Entertainment"
            "utility" -> if (isBn) "ইউটিলিটি বিল" else "Utility"
            "shopping" -> if (isBn) "কেনাকাটা ও শপিং" else "Shopping"
            "income" -> if (isBn) "আয় ও উপার্জিত বেতন" else "Income"
            "other" -> if (isBn) "অন্যান্য খরচ" else "Other"
            else -> category
        }
    }
}
