package com.sinuxvr.sample;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {

	private VRSensorManagerAndroid vrSensorManagerAndroid;    // Менеджер датчиков

	/** Инициализация приложения */
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

		// Запрет на отключение экрана и использование датчиков имплементацией libgdx
		config.useWakelock = true;
		config.useAccelerometer = false;
		config.useGyroscope = false;
		config.useCompass = false;
		config.numSamples = 2;

		// Создание своего листенера данных с датчиков (поэтому useAccelerometer и т.п. не нужны)
		vrSensorManagerAndroid = new VRSensorManagerAndroid(this.getContext());
		initialize(new GdxVR(vrSensorManagerAndroid), config);
	}

	/** Обработка паузы приложения - отключение листенера датчиков */
	@Override
	public void onPause() {
		vrSensorManagerAndroid.endTracking();
		super.onPause();
	}

	/** При возвращении - снова зарегистрировать листенеры датчиков */
	@Override
	public void onResume() {
		super.onResume();
		vrSensorManagerAndroid.startTracking();
	}
}
