package com.bartex.flags

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ResultDialog(val total: Int, val totalGuesses :Int, val onResultListener:OnResultListener): DialogFragment() {

    interface OnResultListener{
        fun resetQuiz()
    }

    @SuppressLint("StringFormatMatches")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity )
        builder.setMessage(
                getString(R.string.results, totalGuesses, total*100/totalGuesses.toDouble())
        )

        // "Reset Quiz" Button
        builder.setPositiveButton(R.string.reset_quiz,
                DialogInterface.OnClickListener { dialog, id ->
                    onResultListener.resetQuiz()}
        )
        // return the AlertDialog
        return builder.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }
}