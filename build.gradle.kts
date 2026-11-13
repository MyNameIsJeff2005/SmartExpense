// File Gradle gốc: chỉ khai báo plugin (áp dụng ở module app), không thêm dependency nào ở đây
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}
