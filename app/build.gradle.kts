import org.jetbrains.kotlin.storage.CacheResetOnProcessCanceled.enabled

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.musicplayeradvance"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.musicplayeradvance"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding =true
        dataBinding =true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)




    implementation ("androidx.palette:palette-ktx:1.0.0")


    // ViewModel

    implementation ("androidx.lifecycle:lifecycle-viewmodel:2.3.1")
    // LiveData
    implementation ("androidx.lifecycle:lifecycle-livedata:2.3.1")
    // Lifecycles only (without ViewModel or LiveData)
    implementation ("androidx.lifecycle:lifecycle-runtime:2.3.1")

    // Saved state module for ViewModel
    implementation ("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.3.1")
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)

    // Annotation processor
    annotationProcessor ("androidx.lifecycle:lifecycle-compiler:2.3.1")

    //Google
    implementation ("com.google.android.gms:play-services-auth:19.0.0")

//    implementation ("com.google.firebase:firebase-database:20.0.0")


//    implementation ("com.google.firebase:firebase-auth:20.0.4")

    //Gif
    implementation ("pl.droidsonroids.gif:android-gif-drawable:1.2.17")

    //Facebook
    implementation ("com.facebook.android:facebook-android-sdk:[5,6)")
    implementation ("com.facebook.android:facebook-login:5.15.3")

    //Profile pic
    implementation ("com.github.bumptech.glide:glide:4.11.0")
    implementation ("com.github.bumptech.glide:compiler:4.11.0")


    //Music Player
    implementation ("com.google.android.exoplayer:exoplayer:2.14.2")
    implementation ("com.google.android.exoplayer:extension-mediasession:2.14.2")
    implementation ("androidx.media2:media2-exoplayer:1.1.2")

    //Crop profile
    implementation ("com.github.CanHub:Android-Image-Cropper:3.2.1")


    //Google Pallete (Background)
    implementation ("androidx.appcompat:appcompat:1.1.0")


    //Round pic
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    //Slideup Panel
    implementation ("com.sothree.slidinguppanel:library:3.4.0")

    //VideoLayout
    implementation ("com.github.AsynctaskCoffee:VideoLayout:1.3")



    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)






}