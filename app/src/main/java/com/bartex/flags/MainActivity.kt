package com.bartex.flags

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    companion object{
        // Ключи для чтения данных из SharedPreferences
        val CHOICES = "pref_numberOfChoices"
        val REGIONS = "pref_regionsToInclude"

        const val TAG = "33333"
    }
    // Включение портретного режима
    private var phoneDevice = true
    // Настройки изменились? При первом включении это вызывает запуск викторины в onStart
    private var preferencesChanged = true

    // Настройка MainActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        // Задание значений по умолчанию в файле SharedPreferences
        //Логический признак, определяющий, должны ли значения по умолчанию
        //сбрасываться при каждом вызове метода setDefaultValues, — значение false
        //указывает, что значения настроек по умолчанию должны задаваться только
        //при первом вызове этого метода.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // Регистрация слушателя для изменений SharedPreferences
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(preferencesChangeListener)

        // Определение размера экрана
        // Чтобы определить категорию размера экрана устройства,значение screenLayout объединяется
        //с маской Configuration.SCREENLAYOUT_SIZE_MASK при помощи поразрядного
        //оператора И (&)
        val screenSize = resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK

        //Далее screenSize сравнивается с константами SCREENLAYOUT_
        //SIZE_LARGE и SCREENLAYOUT_SIZE_XLARGE класса Configuration.
        //Если одна из проверок даст положительный результат, значит, приложение
        //выполняется на планшетном устройстве
        // Для планшетного устройства phoneDevice присваивается false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) phoneDevice = false // not a phone-sized device

        Log.d(TAG, "onCreate: phoneDevice = $phoneDevice")
        // На телефоне разрешена только портретная ориентация
        if (phoneDevice) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
         
    }

    //При первом запуске приложения метод onStart вызывается после onCreate.
    //В этом случае вызов onStart гарантирует, что приложение будет правильно
    //инициализировано в состоянии по умолчанию при установке и первом за-
    //пуске или в соответствии с обновленной конфигурацией пользователя при
    //последующих запусках.
    //  Если приложение выполняется в портретной ориентации, а пользователь
    //открывает SettingsActivity, активность MainActivity приостанавливается
    //на время отображения SettingsActivity. Когда пользователь возвращается
    //к MainActivity, снова вызывается метод onStart. На этот раз вызов обе-
    //спечивает необходимое изменение конфигурации, если пользователь внес
    //изменения в настройки.
    override fun onStart() {
        super.onStart()
        if (preferencesChanged) {
            // После задания настроек по умолчанию инициализировать
            // MainActivityFragment и запустить викторину
            val quizFragment: MainActivityFragment? = supportFragmentManager.findFragmentById(
                R.id.quizFragment
            ) as MainActivityFragment?
            quizFragment?.updateGuessRows(
                PreferenceManager.getDefaultSharedPreferences(this)
            )
            quizFragment?.updateRegions(
                PreferenceManager.getDefaultSharedPreferences(this)
            )
            quizFragment?.resetQuiz()
            preferencesChanged = false
        }
    }

    // Меню отображается на телефоне или планшете в портретной ориентации
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Получение текущей ориентации устройства
        val orientation = resources.configuration.orientation

        // Отображение меню приложения только в портретной ориентаци
        //Если метод onCreateOptionsMenu
        //возвращает true, это означает, что меню должно отображаться на экране
        return if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Заполнение меню
            menuInflater.inflate(R.menu.menu_main, menu)
            true
        } else false
    }

    // Отображает SettingsActivity при запуске на телефоне
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId ==R.id.action_settings){
            val preferencesIntent = Intent(this, SettingsActivity::class.java)
            startActivity(preferencesIntent)
        }
        return super.onOptionsItemSelected(item)
    }

    // Слушатель изменений в конфигурации SharedPreferences приложения
    // Вызывается при изменении настроек приложения
    private val preferencesChangeListener = OnSharedPreferenceChangeListener {
        sharedPreferences, key -> // key - ключ, который изменился

        preferencesChanged = true // Пользователь изменил настройки
        val quizFragment: MainActivityFragment? = supportFragmentManager.findFragmentById(
            R.id.quizFragment
        ) as MainActivityFragment?
        if (key == CHOICES) { // Изменилось число вариантов
            quizFragment?.updateGuessRows(sharedPreferences)
            quizFragment?.resetQuiz()
        } else if (key == REGIONS) { // Изменились регионы
            val regions = sharedPreferences.getStringSet(REGIONS, null)
            if (regions != null && regions.size > 0) {
                quizFragment?.updateRegions(sharedPreferences)
                quizFragment?.resetQuiz()
            } else {
                // Хотя бы один регион - по умолчанию Северная Америка
                val editor = sharedPreferences.edit()
                regions?.add(getString(R.string.default_region))
                editor.putStringSet(REGIONS, regions)
                editor.apply()
                Toast.makeText(
                    this@MainActivity,
                    R.string.default_region_message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        Toast.makeText(
            this@MainActivity,
            R.string.restarting_quiz,
            Toast.LENGTH_SHORT
        ).show()
    }

}