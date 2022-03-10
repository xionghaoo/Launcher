package xh.zero.launcher.utils

import android.view.animation.Animation
import android.view.animation.CycleInterpolator

import android.view.animation.RotateAnimation
import android.view.animation.TranslateAnimation







class AnimUtil {
    companion object {
        /**
         * 晃动动画
         *
         *
         * 那么CycleInterpolator是干嘛用的？？
         * Api demo里有它的用法，是个摇头效果！
         *
         * @param counts 1秒钟晃动多少下
         * @return Animation
         */
        fun shakeAnimation(counts: Int): Animation? {
            val rotateAnimation: Animation = RotateAnimation(
                0f,
                3f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotateAnimation.interpolator = CycleInterpolator(counts.toFloat())
            rotateAnimation.repeatCount = -1
            rotateAnimation.duration = 2000
            return rotateAnimation
        }

        /**
         * 晃动动画
         *
         *
         * 那么CycleInterpolator是干嘛用的？？
         * Api demo里有它的用法，是个摇头效果！
         *
         * @param counts 1秒钟晃动多少下
         * @return Animation
         */
        fun shakeTranslateAnimation(counts: Int): Animation? {
            val translateAnimation: Animation = TranslateAnimation(0f, 10f, 0f, 0f)
            translateAnimation.interpolator = CycleInterpolator(counts.toFloat())
            translateAnimation.repeatCount = 100000
            translateAnimation.duration = 1000
            return translateAnimation
        }
    }
}