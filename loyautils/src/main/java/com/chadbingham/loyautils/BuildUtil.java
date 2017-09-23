package com.chadbingham.loyautils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import timber.log.Timber;

import static android.content.pm.PackageManager.GET_ACTIVITIES;

public class BuildUtil {

	private static final int LINE_LENGTH = 30; //half of line length

	private static PackageInfo packageInfo;

	private static PackageInfo getPackageInfo(Context context) {
		try {
			if (packageInfo != null) return packageInfo;
			return packageInfo = context
					.getPackageManager()
					.getPackageInfo(context.getPackageName(), GET_ACTIVITIES);
		} catch (PackageManager.NameNotFoundException e) {
			Timber.e("getPackageInfo: %s", e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public static String printAppStartMessage(Context context) {
		String vs = "Version: " + getVersionName(context);
		if (vs.length() % 2 != 0) {
			//needs to be even length
			vs += " ";
		}

		final int length = vs.length();
		final int side = LINE_LENGTH;
		final int full = length + (LINE_LENGTH * 2);
		final int trueCenter = (int) (full * .5);

		String date = DateTime.now().toString(DateTimeFormat.longDateTime());
		if (date.length() % 2 != 0) {
			//needs to be even length
			date += " ";
		}

		final int dateSide = (int) (trueCenter - (date.length() * .5));
		return //put top line
				"|" +
				buildLine('-', full) +
				'|' +
				'\n' +

				//append version
				'|' +
				buildLine(' ', side) +
				vs +
				buildLine(' ', side) +
				'|' +
				'\n' +

				//add date
				'|' +
				buildLine(' ', dateSide) +
				date +
				buildLine(' ', dateSide) +
				'|' +
				'\n' +

				//put bottom line
				'|' +
				buildLine('_', full) +
				"|\n";
	}

	private static StringBuilder buildLine(char c, int length) {
		final StringBuilder lineBuilder = new StringBuilder();
		for (int j = 0; j < length; j++) {
			lineBuilder.append(c);
		}
		return lineBuilder;
	}

	/* Version Name i.e. 0.0.0.165-alpha */
	public static String getVersionName(Context context) {
		if (getPackageInfo(context) != null) {
			return packageInfo.versionName;
		}
		return "";
	}

	/* Version Name i.e. 10165 */
	public static int getVersionCode(Context context) {
		if (getPackageInfo(context) != null) {
			return packageInfo.versionCode;
		}
		return 0;
	}

	public static boolean isRunningOnEmulator() {
		return Build.PRODUCT.contains("sdk")
			   || Build.HARDWARE.contains("goldfish")
			   || Build.FINGERPRINT.contains("generic");
	}

}
