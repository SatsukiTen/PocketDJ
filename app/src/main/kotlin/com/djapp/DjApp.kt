package com.djapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * T-003: Hilt DIセットアップ
 * @HiltAndroidApp がHiltのコード生成を有効化する。
 */
@HiltAndroidApp
class DjApp : Application()
