package com.sinuxvr.sample;
import com.badlogic.gdx.math.Quaternion;

/** Интерфейс для взаимодействия с платформо-зависимым кодом */

interface VRSensorManager {

    /** Проверка наличия гироскопа */
    boolean isGyroAvailable();

    /** Проверка наличия магнитометра */
    boolean isMagAvailable();

    /** Регистрация листенеров */
    void startTracking();

    /** Отключение листенеров */
    void endTracking();

    /** Включение-выключение коррекции дрифта на лету
     * @param use - true - включено, false - отключено */
    void useDriftCorrection(boolean use);

    /** Получение вычисленного кватерниона ориентации головы
     * @return кватернион для вращения камеры */
    Quaternion getHeadQuaternion();
}