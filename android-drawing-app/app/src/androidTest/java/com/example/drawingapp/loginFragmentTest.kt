package com.example.drawingapp

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.drawingapp.fragments.loginFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
@RunWith(AndroidJUnit4::class)
class loginFragmentTest {

    @Test
    fun testLoginFragment_viewsAreDisplayed() {
        launchFragmentInContainer<loginFragment>()
        onView(withId(R.id.emailInput)).check(matches(isDisplayed()))
        onView(withId(R.id.passwordInput)).check(matches(isDisplayed()))
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
        onView(withId(R.id.registerButton)).check(matches(isDisplayed()))
    }

    @Test
    fun testLoginFragment_loginButtonClick() {
        launchFragmentInContainer<loginFragment>()
        onView(withId(R.id.emailInput)).perform(typeText("test@example.com"))
        onView(withId(R.id.passwordInput)).perform(typeText("password"))
        onView(withId(R.id.loginButton)).perform(click())
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
    }

    @Test
    fun testLoginFragment_registerButtonClick() {
        launchFragmentInContainer<loginFragment>()
        onView(withId(R.id.emailInput)).perform(typeText("test@example.com"))
        onView(withId(R.id.passwordInput)).perform(typeText("password"))
        onView(withId(R.id.registerButton)).perform(click())
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
    }

    @Test
    fun testLoginFragment_emptyEmailInput() {
        launchFragmentInContainer<loginFragment>()
        onView(withId(R.id.passwordInput)).perform(typeText("password"))
        onView(withId(R.id.loginButton)).perform(click())
        onView(withText("邮箱和密码不能为空")).check(matches(isDisplayed()))
    }

    @Test
    fun testLoginFragment_emptyPasswordInput() {
        launchFragmentInContainer<loginFragment>()
        onView(withId(R.id.emailInput)).perform(typeText("test@example.com"))
        onView(withId(R.id.loginButton)).perform(click())
        onView(withText("邮箱和密码不能为空")).check(matches(isDisplayed()))
    }

    @Test
    fun testLoginFragment_invalidEmailInput() {
        launchFragmentInContainer<loginFragment>()
        onView(withId(R.id.emailInput)).perform(typeText("invalid_email"))
        onView(withId(R.id.passwordInput)).perform(typeText("password"))
        onView(withId(R.id.loginButton)).perform(click())
        // 此处需要根据你的实际逻辑添加断言，例如检查是否显示错误提示
    }

    @Test
    fun testLoginFragment_shortPasswordInput() {
        launchFragmentInContainer<loginFragment>()
        onView(withId(R.id.emailInput)).perform(typeText("test@example.com"))
        onView(withId(R.id.passwordInput)).perform(typeText("short"))
        onView(withId(R.id.loginButton)).perform(click())
        // 此处需要根据你的实际逻辑添加断言，例如检查是否显示错误提示
    }
}