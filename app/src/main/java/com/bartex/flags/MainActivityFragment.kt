package com.bartex.flags

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import java.io.IOException
import java.security.SecureRandom
import java.util.*
import kotlin.collections.ArrayList

class MainActivityFragment:Fragment(), ResultDialog.OnResultListener {
    // String used when logging error messages
    private val TAG = "FlagQuiz Activity"

    //todo
    // Количество флагов - для разработки - 2 , а вообще - 10 или задавать в настройках
    private val FLAGS_IN_QUIZ = 2

    // Имена файлов с флагами-
    //это имена файлов с изображениями флагов для текущего набора выбранных регионов
    private var fileNameList  : MutableList<String> = ArrayList()
    // Страны текущей викторины -
    // переменная содержит имена файлов с флагами для стран, используемых в текущей игре
    private var quizCountriesList : MutableList<String> = ArrayList()
    // Регионы текущей викторины
    private var regionsSet  : Set<String>? = null
    // Правильная страна для текущего флага
    private var correctAnswer : String? = null
    // Количество попыток -
    // хранится общее количество правильных и неправильных ответов игрока до настоящего момента
    private var totalGuesses   = 0
    // Количество правильных ответов -
    //если пользователь завершит викторину, это значение будет равно FLAGS_IN_QUIZ
    private var correctAnswers  = 0
    // Количество строк с кнопками вариантов
    private var guessRows = 0
    // Генератор случайных чисел
    private var random : SecureRandom = SecureRandom()
    // Для задержки загрузки следующего флага
    private lateinit var handler : Handler
    // Анимация неправильного ответа
    private var shakeAnimation : Animation? = null
    // Макет с викториной
    private var quizLinearLayout  : LinearLayout? = null
    // Номер текущего вопроса
    private lateinit var questionNumberTextView  : TextView
    // Для вывода флага
    private var flagImageView  : ImageView? = null
    // Строки с кнопками
    private var guessLinearLayouts : Array<LinearLayout?> = arrayOfNulls(4)
    // Для правильного ответа
    private lateinit var answerTextView : TextView

    // Настройка MainActivityFragment при создании представления
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val view: View = inflater.inflate(R.layout.fragment_main, container, false)

        handler  = Handler(requireActivity().mainLooper)

        // Загрузка анимации для неправильных ответов
        shakeAnimation = AnimationUtils.loadAnimation(
            activity,
            R.anim.incorrect_shake
        )
        shakeAnimation?.setRepeatCount(3)  // Анимация повторяется 3 раза

        // Получение ссылок на компоненты графического интерфейса
        quizLinearLayout = view.findViewById<View>(R.id.quizLinearLayout) as LinearLayout
        questionNumberTextView = view.findViewById<View>(R.id.questionNumberTextView) as TextView
        answerTextView = view.findViewById<View>(R.id.answerTextView) as TextView
        flagImageView = view.findViewById<View>(R.id.flagImageView) as ImageView

        guessLinearLayouts = arrayOfNulls(4)
        guessLinearLayouts[0] = view.findViewById<View>(R.id.row1LinearLayout) as LinearLayout
        guessLinearLayouts[1] = view.findViewById<View>(R.id.row2LinearLayout) as LinearLayout
        guessLinearLayouts[2] = view.findViewById<View>(R.id.row3LinearLayout) as LinearLayout
        guessLinearLayouts[3] = view.findViewById<View>(R.id.row4LinearLayout) as LinearLayout


        // Настройка слушателей для кнопок ответов
        //Перебираем строки в Array<LinearLayout?> - в каждой строке проходим
        // по всем детям LinearLayout, соторых считаем в row.childCount
        //В каждой строке находим кнопку по индексу колонки и устанавливаем слушатель
        for (row in guessLinearLayouts) {
            for (column in 0 until row!!.childCount) {
                val button = row.getChildAt(column) as Button
                button.setOnClickListener(guessButtonListener)
            }
        }

        // Назначение текста questionNumberTextView
        questionNumberTextView.text = getString(R.string.question, 1, FLAGS_IN_QUIZ)
        return view // return the fragment's view for display
    }

    // Обновление guessRows на основании значения SharedPreferences
    fun updateGuessRows(sharedPreferences: SharedPreferences) {
        // Получение количества отображаемых вариантов ответа
        val choices = sharedPreferences.getString(MainActivity.CHOICES, 2.toString())
        choices?. let{
            guessRows = it.toInt() / 2
        }
        // Сначала все компоненты LinearLayout скрываются
        for (layout in guessLinearLayouts){
            layout?.visibility = View.GONE
        }
        // Отображение нужных компонентов LinearLayout
        for (row in 0 until guessRows) {
            guessLinearLayouts[row]?.visibility = View.VISIBLE
        }
    }

    // Обновление выбранных регионов по данным из SharedPreferences
    fun updateRegions(sharedPreferences: SharedPreferences) {
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, mutableSetOf())
    }

    // Настройка и запуск следующей серии вопросов
    override  fun resetQuiz() {
        // Использование AssetManager для получения имен файлов изображений
        val assets = requireActivity().assets
        // очищаем fileNameList, чтобы подготовиться к загрузке имен файлов изображений
        // только для географических регионов,включенных в викторину.
        fileNameList.clear()
        try {
            // Перебрать все регионы
            regionsSet?. let{
                for (region in regionsSet!!) {
                    // Для каждого региона вызывается метод list класса AssetManager
                    //для получения массива имен файлов с изображениями флагов
                    val paths:Array<String>? = assets.list(region)
                    // из имени файла удаляется расширение .png, а оставшиеся
                    // имена помещаются в fileNameList
                    paths?. let{
                        for (path in paths){
                            fileNameList.add(path.replace(".png", ""))
                        }
                    }

                }
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Error loading image file names", exception)
        }
        correctAnswers = 0  // Сброс количества правильных ответов
        totalGuesses = 0 //  Сброс общего количества попыток
        quizCountriesList.clear()  // Очистка предыдущего списка стран
        var flagCounter = 1
        val numberOfFlags = fileNameList.size

        // Добавление FLAGS_IN_QUIZ штук  случайных файлов в quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ) {
            val randomIndex = random.nextInt(numberOfFlags)

            // Получение случайного имени файла
            val filename = fileNameList[randomIndex]

            // Если файл еще не был выбран, добавляем его в список файлов для викторины
            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename)  // Добавить файл в список
                ++flagCounter
            }
        }
        loadNextFlag() // Запустить викторину загрузкой первого флага
    }

    // Загрузка следующего флага после правильного ответа
    private fun loadNextFlag() {
        // Получение имени файла следующего флага и удаление его из списка
        val nextImage = quizCountriesList.removeAt(0)
        correctAnswer = nextImage // Обновление правильного ответа
        answerTextView.text = "" // Очистка answerTextView

        // Отображение номера текущего вопроса
        questionNumberTextView.text = getString(
            R.string.question, correctAnswers + 1, FLAGS_IN_QUIZ
        )

        // Извлечение региона из имени следующего изображения
        val region = nextImage.substring(0, nextImage.indexOf('-'))

        // Использование AssetManager для загрузки следующего изображения
        val assets = requireActivity().assets

        // Получение объекта InputStream для ресурса следующего флага
       // и попытка использования InputStream
        try {
            assets.open("$region/$nextImage.png").use { stream ->
                // Загрузка графики в виде объекта Drawable и вывод на flagImageView
                val flag = Drawable.createFromStream(stream, nextImage)
                flagImageView?.setImageDrawable(flag)
                animate(false) // Анимация появления флага на экране
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Error loading $nextImage", exception)
        }
        Collections.shuffle(fileNameList) // Перестановка имен файлов

        // Помещение правильного ответа в конец fileNameList
        val correct = fileNameList.indexOf(correctAnswer)
        fileNameList.add(fileNameList.removeAt(correct))

        // Добавление 2, 4, 6 или 8 кнопок в зависимости от значения guessRows
        for (row in 0 until guessRows) {
            // Размещение кнопок в currentTableRow
            for (column in 0 until guessLinearLayouts[row]!!.childCount) {
                // Получение ссылки на Button
                val newGuessButton = guessLinearLayouts[row]!!.getChildAt(column) as Button
                newGuessButton.isEnabled = true

                // Назначение названия страны текстом newGuessButton
                val filename = fileNameList[row * 2 + column]
                newGuessButton.text = getCountryName(filename)
            }
        }

        // Случайная замена одной кнопки правильным ответом
        val row = random.nextInt(guessRows) // Выбор случайной строки
        val column = random.nextInt(2) // Выбор случайного столбца
        val randomRow = guessLinearLayouts[row] // Получение строки
        val countryName = getCountryName(correctAnswer)
        (randomRow!!.getChildAt(column) as Button).text = countryName
    }

    // Метод разбирает имя файла с флагом и возвращает название страны
    private fun getCountryName(name: String?): String {
        return name!!.substring(name.indexOf('-') + 1).replace('_', ' ')
    }

    // Весь макет quizLinearLayout появляется или исчезает с экрана
    private fun animate(animateOut: Boolean) {
        // Предотвращение анимации интерфейса для первого флага
        if (correctAnswers == 0) return

        // Вычисление координат центра
        val centerX = (quizLinearLayout!!.left +
                quizLinearLayout!!.right) / 2 // calculate center x
        val centerY = (quizLinearLayout!!.top +
                quizLinearLayout!!.bottom) / 2 // calculate center y

        // Вычисление радиуса анимации
        val radius = Math.max(
            quizLinearLayout!!.width,
            quizLinearLayout!!.height
        )
        val animator: Animator

        // Если изображение должно исчезать с экрана
        if (animateOut) {
            // Создание круговой анимации
            animator = ViewAnimationUtils.createCircularReveal(
                quizLinearLayout, centerX, centerY, radius.toFloat(), 0f
            )
            animator.addListener(
                object : AnimatorListenerAdapter() {
                    // Вызывается при завершении анимации
                    override fun onAnimationEnd(animation: Animator) {
                        loadNextFlag()
                    }
                }
            )
        } else {  // Если макет quizLinearLayout должен появиться
            animator = ViewAnimationUtils.createCircularReveal(
                quizLinearLayout, centerX, centerY, 0f, radius.toFloat()
            )
        }
        animator.duration = 500  // Анимация продолжительностью 500 мс
        animator.start() // Начало анимации
    }

    // Вызывается при нажатии кнопки ответа
    private val guessButtonListener =
        View.OnClickListener { v ->
            val guessButton = v as Button
            val guess = guessButton.text.toString()
            val answer = getCountryName(correctAnswer)
            ++totalGuesses  // Увеличение количества попыток пользователя
            if (guess == answer) {  // Если ответ правилен
                ++correctAnswers  // Увеличить количество правильных ответов

                // Правильный ответ выводится зеленым цветом
                answerTextView.text = answer
                answerTextView.setTextColor(
                    resources.getColor(
                        R.color.correct_answer,
                        requireContext().theme
                    )
                )
                disableButtons()  // Блокировка всех кнопок ответов

                // Если пользователь правильно угадал FLAGS_IN_QUIZ флагов
                if (correctAnswers == FLAGS_IN_QUIZ) {
                    // DialogFragment для вывода статистики и перезапуска
                     //в отдельном файле сделан, а не внутри фрагмента
                    ResultDialog(FLAGS_IN_QUIZ, totalGuesses, this )
                            .show(requireActivity().supportFragmentManager, "ResultDialog")
                } else { // Ответ правильный, но викторина не закончена
                    // Загрузка следующего флага после двухсекундной задержки
                    handler.postDelayed(
                        {
                            animate(true)  // Анимация исчезновения флага
                        }, 2000
                    ) // 2000 миллисекунд для двухсекундной задержки
                }
            } else { // Неправильный ответ
                flagImageView!!.startAnimation(shakeAnimation)  // Встряхивание

                // Сообщение "Incorrect!" выводится красным шрифтом
                answerTextView.setText(R.string.incorrect_answer)
                answerTextView.setTextColor(
                        resources.getColor(
                                R.color.incorrect_answer, requireContext().theme
                        )
                )
                guessButton.isEnabled = false  // Блокировка неправильного ответа
            }
        }

    // Вспомогательный метод, блокирующий все кнопки
    private fun disableButtons() {
        for (row in 0 until guessRows) {
            val guessRow = guessLinearLayouts[row]
            for (i in 0 until guessRow!!.childCount) guessRow.getChildAt(i).isEnabled = false
        }
    }

}