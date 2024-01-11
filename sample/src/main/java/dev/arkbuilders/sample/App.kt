package dev.arkbuilders.sample

import android.app.Application
import dev.arkbuilders.arklib.data.folders.FoldersRepo
import dev.arkbuilders.arklib.initArkLib
import dev.arkbuilders.arklib.initRustLogger

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        FoldersRepo.init(this)
        System.loadLibrary("arklib")
        initArkLib()
        initRustLogger()
    }
}