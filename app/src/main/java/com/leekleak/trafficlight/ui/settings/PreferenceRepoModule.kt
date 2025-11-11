package com.leekleak.trafficlight.ui.settings

import org.koin.dsl.module

val preferenceRepoModule = module { single{ PreferenceRepo(get()) } }