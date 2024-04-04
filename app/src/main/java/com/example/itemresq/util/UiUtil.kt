package com.example.itemresq.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.itemresq.activity.LoginActivity
import com.example.itemresq.database.FirebaseManager

object UiUtil {

    fun showToast(context: Context, message : String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun showDeleteAccountDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Usuń konto")
            .setMessage("Czy jesteś pewny, że chcesz na stałe usunąć konto i wszystkie opublikowane z niego zgłoszenia?")
            .setPositiveButton("Usuń") { dialog, which ->
                dialog.dismiss()
                FirebaseManager.deleteAccountAndReports(
                    onSuccess = { success ->
                        showToast(context, success)
                        context.startActivity(Intent(context, LoginActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        })
                    },
                    onFailure = { error ->
                        showToast(context, error)
                    }
                )
            }
            .setNegativeButton("Anuluj") { dialog, which ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

}