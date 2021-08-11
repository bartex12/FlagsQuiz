package com.bartex.flags

import android.os.Bundle
import android.preference.PreferenceFragment
import androidx.preference.PreferenceFragmentCompat

class SettingsActivityFragment: PreferenceFragmentCompat() {

    // Создание GUI настроек по файлу preferences.xml из res/xml
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
       setPreferencesFromResource(R.xml.preferences, rootKey) // load from XML


    }
}