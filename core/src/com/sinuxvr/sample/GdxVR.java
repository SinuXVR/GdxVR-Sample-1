package com.sinuxvr.sample;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

/** Главный класс приложения, здесь производим инициализацию камеры, модели и выполняем отрисовку
 * Этот пример распространяется под лицензией Apache License 2.0
 * Оригинальная игра: Mosquito Invasion VR
 * Ссылка на GooglePlay: https://play.google.com/store/apps/details?id=com.sinux.mosquitoinvasion
 *
 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
 * ★ Если Вы хотите поддержать автора, то можете сделать это через:   ★
 * ★ Яндекс.Деньги: https://money.yandex.ru/to/410014808100617        ★
 * ★ Paypal: https://www.paypal.me/sinuxvr                            ★
 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
 *
 */

class GdxVR extends ApplicationAdapter {

	static VRSensorManager vrSensorManager;  // Менеджер для получения данных с датчиков
	private int scrHeight, scrHalfWidth;     // Для хранения размеров viewport
	private AssetManager assets;			 // Загрузчик ресурсов
	private ModelBatch modelBatch;			 // Пакетник для модели
	private ModelInstance roomInstance;      // Экземпляр модели комнаты
	private VRCamera vrCamera;               // VR камера

	/** Конструктор */
	GdxVR(VRSensorManager vrSensorManager) {
		GdxVR.vrSensorManager = vrSensorManager;
	}

	/** Инициализация и загрузка ресурсов */
	@Override
	public void create () {
		// Размеры экрана
		scrHalfWidth = Gdx.graphics.getWidth() / 2;
		scrHeight = Gdx.graphics.getHeight();

		// Загрузка модели из файла
		modelBatch = new ModelBatch();
		assets = new  AssetManager();
		assets.load("room.g3db", Model.class);
		assets.finishLoading();
		Model roomModel = assets.get("room.g3db");
		roomInstance = new ModelInstance(roomModel);

		// Создание камеры (fov, parallax, near, far) и установка позиции
		vrCamera = new VRCamera(90, 0.4f, 0.1f, 30f);
		vrCamera.setPosition(-1.7f, 3f, 3f);

		// Разрешаем коррекцию дрифта при помощи компаса
		vrSensorManager.useDriftCorrection(true);
	}

	/** Отрисовка стереопары осуществляется при помощи изменения viewport-а */
	@Override
	public void render () {
		// Очистка экрана
		Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		// Обновление параметров камеры
		vrCamera.update();

		// Отрисовка сцены для левого глаза
		Gdx.gl.glViewport(0, 0, scrHalfWidth, scrHeight);
		modelBatch.begin(vrCamera.getLeftCam());
		modelBatch.render(roomInstance);
		modelBatch.end();

		// Отрисовка сцены для правого глаза
		Gdx.gl.glViewport(scrHalfWidth, 0, scrHalfWidth, scrHeight);
		modelBatch.begin(vrCamera.getRightCam());
		modelBatch.render(roomInstance);
		modelBatch.end();
	}

	/** Высвобождение ресурсов */
	@Override
	public void dispose () {
		modelBatch.dispose();
		assets.dispose();
	}
}
