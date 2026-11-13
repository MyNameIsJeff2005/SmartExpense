package com.example.smartexpense.utils

/** Tài khoản LOCKED: đăng nhập bị từ chối trên thiết bị này (mục 3.1.2) */
class LockedAccountException(message: String) : Exception(message)
