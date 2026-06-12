plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ysu.capstone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ysu.capstone"
        minSdk = 32
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "NAVER_CLIENT_ID", "\"ZTmNCXfSOzymZ9uqqzl6\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"iH7R9QM1lU\"")
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
    buildFeatures {
        buildConfig = true // buildConfig 기능 활성화
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    packagingOptions {
        exclude ("META-INF/NOTICE.md")
        exclude ("META-INF/NOTICE")
        exclude ("META-INF/LICENSE.md")
        exclude ("META-INF/LICENSE")
    }
}


dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.airbnb.android:lottie:3.4.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.prolificinteractive:material-calendarview:1.4.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.android.libraries.places:places:3.5.0")
    implementation("com.naver.maps:map-sdk:3.19.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    implementation("com.google.android.flexbox:flexbox:3.0.0")
//    implementation("androidx.work:work-runtime:2.9.0")
//    implementation("androidx.work:work-runtime-ktx:2.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

//    네이버
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.14.2")

}
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.airbnb.android:lottie:3.4.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.prolificinteractive:material-calendarview:1.4.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.android.libraries.places:places:3.5.0")
    implementation("com.naver.maps:map-sdk:3.19.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
//    implementation("androidx.work:work-runtime:2.9.0")
//    implementation("androidx.work:work-runtime-ktx:2.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}