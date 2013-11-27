/*Copyright 2013 Matteo Hausner

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package de.bwravencl.androsqrl.utils;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import eu.livotov.zxscan.ZXUserCallback;

public class ZXOrientationFixCallback implements ZXUserCallback {

	public static final long WAIT_TIME = 60L; // TODO: make sure that this value
												// is large enough for slower
												// devices

	@Override
	public void onScannerActivityResumed(final Activity captureActivity) {
		// Wait for WAIT_TIME ms to ensure that cameraManager has opened
		final TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				// Call onConfigurationChanged to trigger
				// forceSetCameraOrientation()
				captureActivity.onConfigurationChanged(null);
			}
		};
		new Timer().schedule(timerTask, WAIT_TIME);
	}

	@Override
	public void onScannerActivityDestroyed(Activity activity) {
	}

	@Override
	public void onScannerActivityCreated(Activity activity) {
	}

	@Override
	public boolean onCodeRead(String code) {
		return false;
	}
}
