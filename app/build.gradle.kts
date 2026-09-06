plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.example.pokerar"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pokerar"
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ARCore
    implementation("com.google.ar:core:1.40.0")
    implementation("com.google.ar.sceneform:core:1.15.0")
    implementation("com.google.ar.sceneform.ux:sceneform-ux:1.15.0")
    
    // MLKit Vision
    implementation("com.google.mlkit:vision-common:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // OpenCV
    implementation("org.opencv:opencv-android:4.8.0")
    
    // Android
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("com.google.android.material:material:1.10.0")
    
    // Camera
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
