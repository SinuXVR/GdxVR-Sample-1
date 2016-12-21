package com.sinuxvr.sample;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/** Реализация листенера датчиков под Android. Вычисляет и предоставляет готовый кватернион
 * ориентации устройства в пространстве для камеры в зависимости от имеющихся датчиков в телефоне.
 * Поддерживаемые варианты: акселерометр, акселерометр + магнитометр, гироскоп + акселерометр,
 * гироскоп + акселерометр + магнитометр */

class VRSensorManagerAndroid implements VRSensorManager {

    /** Перечень режимов работы в зависимости от наличия датчиков */
    private enum VRControlMode { ACC_ONLY, ACC_GYRO, ACC_MAG, ACC_GYRO_MAG }

    private SensorManager sensorManager;                // Сенсорный менеджер
    private SensorEventListener accelerometerListener;  // Листенер акселерометра
    private SensorEventListener gyroscopeListener;      // Листенер гироскопа
    private SensorEventListener compassListener;        // Листенер магнитометра
    private Context context;                            // Контекст приложения

    /** Массивы для получения данных */
    private final float[] accelerometerValues = new float[3];   // Акселерометр
    private final float[] gyroscopeValues = new float[3];       // Гироскоп
    private final float[] magneticFieldValues = new float[3];   // Магнитометр
    private final boolean gyroAvailable;                        // Флаг наличия гироскопа
    private final boolean magAvailable;                         // Флаг наличия магнитометра
    private volatile boolean useDC;                             // Использовать ли магнитометр

    /** Кватернионы и векторы для нахождения ориентации, итоговый результат в headOrientation */
    private final Quaternion gyroQuaternion;
    private final Quaternion deltaQuaternion;
    private final Vector3 accInVector;
    private final Vector3 accInVectorTilt;
    private final Vector3 magInVector;
    private final Quaternion headQuaternion;
    private VRControlMode vrControlMode;

    /** Конструктор */
    VRSensorManagerAndroid(Context context) {
        this.context = context;
        // Получение сенсорного менеджера
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // Проверка наличия датчиков (акселерометр есть всегда 100%, наверное)
        magAvailable = (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null);
        gyroAvailable = (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null);
        useDC = false;

        // Определение режима работы в зависимости от имеющихся датчиков
        vrControlMode = VRControlMode.ACC_ONLY;
        if (gyroAvailable) vrControlMode = VRControlMode.ACC_GYRO;
        if (magAvailable) vrControlMode = VRControlMode.ACC_MAG;
        if (gyroAvailable && magAvailable) vrControlMode = VRControlMode.ACC_GYRO_MAG;

        // Инициализация кватернионов
        gyroQuaternion = new Quaternion(0, 0, 0, 1);
        deltaQuaternion = new Quaternion(0, 0, 0, 1);
        accInVector = new Vector3(0, 10, 0);
        accInVectorTilt = new Vector3(0, 0, 0);
        magInVector = new Vector3(1, 0, 0);
        headQuaternion = new Quaternion(0, 0, 0, 1);

        // Регистрация датчиков
        startTracking();
    }

    /** Возврат наличия гироскопа */
    @Override
    public boolean isGyroAvailable() {
        return gyroAvailable;
    }

    /** Возврат наличия магнитометра */
    @Override
    public boolean isMagAvailable() {
        return magAvailable;
    }

    /** Старт трекинга - регистрация листенеров */
    @Override
    public void startTracking() {
        // Акселерометр инициализируется при любом раскладе
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        accelerometerListener = new SensorListener(this.accelerometerValues, this.magneticFieldValues, this.gyroscopeValues);
        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        // Магнитометр
        if (magAvailable) {
            sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            Sensor compass = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).get(0);
            compassListener = new SensorListener(this.accelerometerValues, this.magneticFieldValues, this.gyroscopeValues);
            sensorManager.registerListener(compassListener, compass, SensorManager.SENSOR_DELAY_GAME);
        }
        // Гироскоп
        if (gyroAvailable) {
            sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            Sensor gyroscope = sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
            gyroscopeListener = new SensorListener(this.gyroscopeValues, this.magneticFieldValues, this.gyroscopeValues);
            sensorManager.registerListener(gyroscopeListener, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    /** Остановка трекинга - отключение листенеров */
    @Override
    public void endTracking() {
        if (sensorManager != null) {
            if (accelerometerListener != null) {
                sensorManager.unregisterListener(accelerometerListener);
                accelerometerListener = null;
            }
            if (gyroscopeListener != null) {
                sensorManager.unregisterListener(gyroscopeListener);
                gyroscopeListener = null;
            }
            if (compassListener != null) {
                sensorManager.unregisterListener(compassListener);
                compassListener = null;
            }
            sensorManager = null;
        }
    }

    /** Включение-выключение использования магнитометра на лету */
    @Override
    public void useDriftCorrection(boolean useDC) {
        // Реально листенер магнитометра не отключается, просто игнорируем его при вычислениях
        this.useDC = useDC;
    }

    /** Вычисление и возврат кватерниона ориентации */
    @Override
    public synchronized Quaternion getHeadQuaternion() {
        // Выбираем последовательность действий в зависимости от режима управления
        switch (vrControlMode) {
            // Управление одним акселерометром
            case ACC_ONLY: updateAccData(0.1f);
                // Вращение по Yaw наклонами головы из стороны в сторону (как во всяких гонках)
                headQuaternion.setFromAxisRad(0, 1, 0, -MathUtils.sin(accelerometerValues[0] / 200f)).mul(gyroQuaternion).nor();
                gyroQuaternion.set(headQuaternion);
                break;

            // Акселерометр + магнитометр (если в телефоне стоит вменяемый компас, то данная комбинация
            // ведет себя почти как гироскоп, получается этакая эмуляция гиро)
            case ACC_MAG: updateAccData(0.2f);
                if (!useDC) {
                    headQuaternion.setFromAxisRad(0, 1, 0, -MathUtils.sin(accelerometerValues[0] / 200f)).mul(gyroQuaternion).nor();
                    gyroQuaternion.set(headQuaternion);
                } else updateMagData(1f, 0.05f);
                break;

            // Гироскоп + акселерометр
            case ACC_GYRO: updateGyroData(0.1f);
                updateAccData(0.02f);
                break;

            // Все три датчика - must have, но только если компас откалиброван
            case ACC_GYRO_MAG: float dQLen = updateGyroData(0.1f);
                updateAccData(0.02f);
                if (useDC) updateMagData(dQLen, 0.005f);
        }

        return headQuaternion;
    }

    /** Логика определения ориентации
     * Интегрирование показаний гироскопа в кватернион
     * @param driftThreshold - порог для отсечения дрифта покоя
     * @return - длина кватерниона deltaQuaternion */
    private synchronized float updateGyroData(float driftThreshold) {
        float wX = gyroscopeValues[0];
        float wY = gyroscopeValues[1];
        float wZ = gyroscopeValues[2];

        // Интегрирование показаний гироскопа
        float l = Vector3.len(wX, wY, wZ);
        float dtl2 = Gdx.graphics.getDeltaTime() * l * 0.5f;
        if (l > driftThreshold) {
            float sinVal = MathUtils.sin(dtl2) / l;
            deltaQuaternion.set(sinVal * wX, sinVal * wY, sinVal * wZ, MathUtils.cos(dtl2));
        } else deltaQuaternion.set(0, 0, 0, 1);
        gyroQuaternion.mul(deltaQuaternion);
        return l;
    }

    /** Коррекция Tilt при помощи акселерометра
     * @param filterAlpha - коэффициент фильтрации */
    private synchronized void updateAccData(float filterAlpha) {
        // Преобразование значений акселерометра в инерциальные координаты
        accInVector.set(accelerometerValues[0], accelerometerValues[1], accelerometerValues[2]);
        gyroQuaternion.transform(accInVector);
        accInVector.nor();

        // Вычисление нормализованной оси вращения между accInVector и UP(0, 1, 0)
        float xzLen = 1f / Vector2.len(accInVector.x, accInVector.z);
        accInVectorTilt.set(-accInVector.z * xzLen, 0, accInVector.x * xzLen);

        // Вычисление угла между вектором accInVector и UP(0, 1, 0)
        float fi = (float)Math.acos(accInVector.y);

        // Получение Tilt-скорректированного кватерниона по данным акселерометра
        headQuaternion.setFromAxisRad(accInVectorTilt, filterAlpha * fi).mul(gyroQuaternion).nor();
        gyroQuaternion.set(headQuaternion);
    }

    /** Коррекция угла по Yaw магнитометром
     * @param dQLen - длина кватерниона deltaQuaternion
     * @param filterAlpha - коэффициент фильтрации
     * Коррекция производится только в движении */
    private synchronized void updateMagData(float dQLen, float filterAlpha) {
        // Проверка длины deltaQuaternion для коррекции только в движении
        if (dQLen < 0.1f) return;
        // Преобразование значений магнитометра в инерциальные координаты
        magInVector.set(magneticFieldValues[0], magneticFieldValues[1], magneticFieldValues[2]);
        gyroQuaternion.transform(magInVector);

        // Вычисление корректирующего Yaw угла с магнитометра
        float theta = MathUtils.atan2(magInVector.z, magInVector.x);

        // Коррекция ориентации
        headQuaternion.setFromAxisRad(0, 1, 0, filterAlpha * theta).mul(gyroQuaternion).nor();
        gyroQuaternion.set(headQuaternion);
    }

    /** Своя имплементация класса сенсорного листенера (копипаст из AndroidInput) */
    private class SensorListener implements SensorEventListener {
        final float[] accelerometerValues;
        final float[] magneticFieldValues;
        final float[] gyroscopeValues;

        SensorListener (float[] accelerometerValues, float[] magneticFieldValues, float[] gyroscopeValues) {
            this.accelerometerValues = accelerometerValues;
            this.magneticFieldValues = magneticFieldValues;
            this.gyroscopeValues = gyroscopeValues;
        }

        // Смена точности (нас не интересует)
        @Override
        public void onAccuracyChanged (Sensor arg0, int arg1) { }

        // Получение данных от датчиков
        @Override
        public synchronized void onSensorChanged (SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues[0] = -event.values[1];
                accelerometerValues[1] = event.values[0];
                accelerometerValues[2] = event.values[2];
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticFieldValues[0] = -event.values[1];
                magneticFieldValues[1] = event.values[0];
                magneticFieldValues[2] = event.values[2];
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                gyroscopeValues[0] = -event.values[1];
                gyroscopeValues[1] = event.values[0];
                gyroscopeValues[2] = event.values[2];
            }
        }
    }
}