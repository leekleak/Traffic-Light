package com.leekleak.trafficlight.database

import org.koin.dsl.module

val dayUsageRepoModule = module {single { DayUsageRepo(get()) }}