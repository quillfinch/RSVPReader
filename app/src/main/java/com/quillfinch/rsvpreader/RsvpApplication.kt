package com.quillfinch.rsvpreader

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class RsvpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
    }
}
