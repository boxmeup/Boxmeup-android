package com.boxmeup.app.scans;

import com.google.zxing.integration.android.IntentResult;

public interface ScanResult {
    boolean processResult(IntentResult scanResult);
}
