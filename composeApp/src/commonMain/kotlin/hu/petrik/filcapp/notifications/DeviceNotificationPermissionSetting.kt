package hu.petrik.filcapp.notifications

import androidx.compose.runtime.Composable

/** Android OS notification permission, distinct from account-wide email/push settings. */
@Composable
expect fun DeviceNotificationPermissionSetting()
