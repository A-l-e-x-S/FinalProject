package com.example.finalproject

import com.cloudinary.android.MediaManager
import android.content.Context

object CloudinaryConfig {
    fun init(context: Context) {
        val config: HashMap<String, String> = HashMap()
        config["cloud_name"] = "dirws9eer"
        config["api_key"] = "758552218373873"
        config["api_secret"] = "1TqgUtoE7cOZ7uHp4GKmorndL2M"
        MediaManager.init(context, config)
    }
}