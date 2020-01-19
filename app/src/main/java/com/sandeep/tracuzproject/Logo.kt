package com.sandeep.tracuzproject

import android.animation.AnimatorInflater
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_logo.*
import java.lang.Exception

class Logo : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_logo)
        supportActionBar?.hide()
        val timeThread = object : Thread(){
            override fun run() {
                try {
                    sleep(4500)
                    val i = Intent(this@Logo, MainActivity::class.java)
                    startActivity(i)
                    finish()
                }catch (ex: Exception){
                    ex.printStackTrace()
                }
            }
        }
        timeThread.start()
        logonanim()
    }
    private fun logonanim(){
        val rotateLogo = AnimatorInflater.loadAnimator(this@Logo, R.animator.alpha)
        rotateLogo.apply {
            setTarget(tracLogo)
            start()
        }
    }
}
