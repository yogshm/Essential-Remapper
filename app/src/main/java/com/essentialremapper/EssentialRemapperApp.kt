package com.essentialremapper

import android.app.Application
import com.essentialremapper.data.repository.DefaultSettingsRepository
import com.essentialremapper.data.repository.SettingsRepository
import com.essentialremapper.data.storage.SettingsDataStore
import com.essentialremapper.domain.device.DeviceRepository

class EssentialRemapperApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var deviceRepository: DeviceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val dataStore = SettingsDataStore(this)
        settingsRepository = DefaultSettingsRepository(dataStore)
        deviceRepository = DeviceRepository()
    }

    companion object {
        lateinit var instance: EssentialRemapperApp
            private set
    }
}
