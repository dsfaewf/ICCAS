package com.example.testfolder.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.testfolder.R

class LoadingAnimation(activityContext: Context,
                       loadingBackgroundLayout: ConstraintLayout,
                       loadingImage: ImageView,
                       loadingText: TextView,
                       loadingTextDetail: TextView,
                       loadingTextDetail2: TextView,
                       loadingMessage: String) {
    //하위 5개의 변수는 로딩 이미지를 위해 선언함.
    private var loadingBackgroundLayout: ConstraintLayout
    private var loadingImage: ImageView
    private var loadingText: TextView
    private var loadingTextDetail: TextView
    private var loadingTextDetail2: TextView
    private var rotateAnimation: Animation
    private val handler = Handler(Looper.getMainLooper())
    private val loadingMessage: String

    init {
        // 로딩 이미지와 텍스트 초기화
        this.loadingBackgroundLayout = loadingBackgroundLayout
        this.loadingImage = loadingImage
        this.loadingText = loadingText
        //애니메이션 초기화
        this.rotateAnimation = AnimationUtils.loadAnimation(activityContext, R.anim.rotate)
        this.loadingTextDetail = loadingTextDetail
        this.loadingTextDetail2 = loadingTextDetail2
        this.loadingMessage = loadingMessage
    }

    fun showLoading() { //로딩이미지 표출용
        loadingBackgroundLayout.visibility = View.VISIBLE
        loadingImage.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        loadingTextDetail.visibility = View.VISIBLE
        loadingTextDetail2.visibility = View.VISIBLE
        loadingImage.startAnimation(rotateAnimation)
        startLoadingTextAnimation()
    }

    fun hideLoading() { //로딩이미지 다시 없애는 용도
        loadingBackgroundLayout.visibility = View.GONE
        loadingImage.visibility = View.GONE
        loadingText.visibility = View.GONE
        loadingTextDetail.visibility = View.GONE
        loadingTextDetail2.visibility = View.GONE
        loadingImage.clearAnimation()
        handler.removeCallbacksAndMessages(null) // 애니메이션 중지
    }

    private fun startLoadingTextAnimation() { //...을 로딩과 함께 애니메이션으로 움직이도록
        var dotCount = 0
        handler.post(object : Runnable {
            override fun run() {
                dotCount++
                if (dotCount > 3) {
                    dotCount = 0
                }
                val dots = ".".repeat(dotCount)
                loadingText.text = "$loadingMessage$dots"
                handler.postDelayed(this, 500) // 500ms마다 업데이트
            }
        })
    }
}