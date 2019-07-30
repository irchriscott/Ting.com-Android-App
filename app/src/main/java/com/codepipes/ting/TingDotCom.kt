package com.codepipes.ting

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class TingDotCom : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ting_dot_com)
    }

    override fun onDestroy() {
        super.onDestroy()
        finishAffinity()
        System.exit(0)
    }
}
