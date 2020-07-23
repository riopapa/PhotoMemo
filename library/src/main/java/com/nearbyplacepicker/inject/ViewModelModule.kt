package com.nearbyplacepicker.inject

import com.nearbyplacepicker.viewmodel.PlaceConfirmDialogViewModel
import com.nearbyplacepicker.viewmodel.PlacePickerViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel { PlacePickerViewModel(get()) }

    viewModel { PlaceConfirmDialogViewModel(get()) }

}