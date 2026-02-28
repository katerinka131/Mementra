package com.example.mementra.utils

import android.Manifest
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX

/**
 * Вспомогательный класс для работы с разрешениями
 * Использует библиотеку PermissionX
 */
object PermissionHelper {

    /**
     * Запросить разрешения для камеры
     */
    fun requestCameraPermission(
        activity: FragmentActivity,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        PermissionX.init(activity)
            .permissions(Manifest.permission.CAMERA)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Для добавления фото к воспоминаниям необходим доступ к камере",
                    "Разрешить",
                    "Отмена"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "Необходимо вручную разрешить доступ к камере в настройках",
                    "Перейти в настройки",
                    "Отмена"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    }

    /**
     * Запросить разрешения для камеры (для Fragment)
     */
    fun requestCameraPermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        PermissionX.init(fragment)
            .permissions(Manifest.permission.CAMERA)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Для добавления фото к воспоминаниям необходим доступ к камере",
                    "Разрешить",
                    "Отмена"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "Необходимо вручную разрешить доступ к камере в настройках",
                    "Перейти в настройки",
                    "Отмена"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    }

    /**
     * Запросить разрешения для записи аудио
     */
    fun requestAudioPermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        PermissionX.init(fragment)
            .permissions(Manifest.permission.RECORD_AUDIO)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Для записи голосовых заметок необходим доступ к микрофону",
                    "Разрешить",
                    "Отмена"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "Необходимо вручную разрешить доступ к микрофону в настройках",
                    "Перейти в настройки",
                    "Отмена"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    }

    /**
     * Запросить разрешения для хранилища (для Android < 13)
     */
    fun requestStoragePermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ использует новые разрешения для медиа
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            // Старые версии Android
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        PermissionX.init(fragment)
            .permissions(permissions)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Для доступа к фотографиям необходимо разрешение на чтение хранилища",
                    "Разрешить",
                    "Отмена"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "Необходимо вручную разрешить доступ к хранилищу в настройках",
                    "Перейти в настройки",
                    "Отмена"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    }

    /**
     * Запросить разрешения для геолокации
     */
    fun requestLocationPermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        PermissionX.init(fragment)
            .permissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Для определения вашего местоположения на карте необходим доступ к геолокации",
                    "Разрешить",
                    "Отмена"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "Необходимо вручную разрешить доступ к геолокации в настройках",
                    "Перейти в настройки",
                    "Отмена"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    }

    /**
     * Запросить все необходимые разрешения для приложения
     */
    fun requestAllPermissions(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: (List<String>) -> Unit = {}
    ) {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Добавляем разрешения для хранилища в зависимости от версии Android
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        PermissionX.init(fragment)
            .permissions(permissions)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Для полноценной работы приложения необходимы следующие разрешения:\n" +
                            "• Камера - для фото воспоминаний\n" +
                            "• Микрофон - для голосовых заметок\n" +
                            "• Геолокация - для привязки к карте\n" +
                            "• Хранилище - для сохранения фото",
                    "Разрешить",
                    "Отмена"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "Необходимо вручную разрешить доступ в настройках приложения",
                    "Перейти в настройки",
                    "Отмена"
                )
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied(deniedList)
                }
            }
    }
}

