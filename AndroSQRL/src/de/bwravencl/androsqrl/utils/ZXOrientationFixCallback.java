/*
 * Copyright 2013 Matteo Hausner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bwravencl.androsqrl.utils;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import eu.livotov.zxscan.ZXUserCallback;

public class ZXOrientationFixCallback implements ZXUserCallback {

	public static final long DELAY = 50L;
	public static final long PERIOD = 150L;

	private Timer timer;

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
		timer = new Timer();
		timer.scheduleAtFixedRate(timerTask, DELAY, PERIOD);
	}

	@Override
	public void onScannerActivityDestroyed(Activity activity) {
		if (timer != null)
			timer.cancel();
	}

	@Override
	public void onScannerActivityCreated(Activity activity) {
	}

	@Override
	public boolean onCodeRead(String code) {
		if (timer != null)
			timer.cancel();

		return true;
	}
}
