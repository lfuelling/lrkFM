package io.lerk.lrkFM;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

import io.lerk.lrkFM.consts.Preference;
import io.lerk.lrkFM.util.PrefUtils;

import static io.lerk.lrkFM.consts.Preference.BACKUP_QUOTA_EXCEEDED;

/**
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class LrkFMBackupAgent extends BackupAgentHelper {

    private static final String BACKUP_KEY =  "backup";

    @Override
    public void onCreate() {
        super.onCreate();
        addHelper(BACKUP_KEY, new SharedPreferencesBackupHelper(this, Preference.Store.CLOUD_BACKED.getName()));
    }

    @Override
    public void onQuotaExceeded(long backupDataBytes, long quotaBytes) {
        super.onQuotaExceeded(backupDataBytes, quotaBytes);
        new PrefUtils<Boolean>(BACKUP_QUOTA_EXCEEDED).setValue(true);
    }
}
