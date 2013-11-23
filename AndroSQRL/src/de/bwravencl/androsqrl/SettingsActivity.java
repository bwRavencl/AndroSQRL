package de.bwravencl.androsqrl;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import de.bwravencl.androsqrl.R;

public class SettingsActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
	}
}
