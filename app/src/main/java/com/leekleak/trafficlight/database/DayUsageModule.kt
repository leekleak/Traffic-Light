package com.leekleak.trafficlight.database

import org.koin.dsl.module

val dayUsageModule = module {single { DayUsageRepository(get()) }}