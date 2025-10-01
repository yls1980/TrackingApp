package com.trackingapp.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.trackingapp.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun appLaunchesSuccessfully() {
        // Проверяем, что приложение запускается
        onView(withText("Трекер маршрутов"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun permissionScreenIsDisplayedWhenNoLocationPermission() {
        // Проверяем отображение экрана разрешений
        onView(withText("Требуется разрешение на местоположение"))
            .check(matches(isDisplayed()))
        
        onView(withText("Предоставить разрешение"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun navigationButtonsAreVisible() {
        // Проверяем наличие кнопок навигации
        onView(withContentDescription("Маршруты"))
            .check(matches(isDisplayed()))
        
        onView(withContentDescription("Настройки"))
            .check(matches(isDisplayed()))
    }
}



