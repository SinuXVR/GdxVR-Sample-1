package com.sinuxvr.sample;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/** Класс VR камеры
 * Данные об ориентации берутся из VRSensorManager при вызове update() */

public class VRCamera {
    private PerspectiveCamera leftCam;   // Левая камера
    private PerspectiveCamera rightCam;  // Правая камера
    private Vector3 position;            // Позиция VR камеры
    private float parallax;              // Расстояние между камерами
    private Vector3 direction;           // Вектор направления VR камеры
    private Vector3 up;                  // Вектор UP VR камеры
    private Vector3 upDirCross;          // Векторное произведение up и direction (понадобится в части 2, сейчас не трогаем)

    /** Конструктор */
    public VRCamera(float fov, float parallax, float near, float far) {
        this.parallax = parallax;
        leftCam = new PerspectiveCamera(fov, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight());
        leftCam.near = near;
        leftCam.far = far;
        leftCam.update();
        rightCam = new PerspectiveCamera(fov, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight());
        rightCam.near = near;
        rightCam.far = far;
        rightCam.update();
        position = new Vector3(0, 0, 0);
        direction = new Vector3(0, 0, 1);
        up = new Vector3(0, 1, 0);
        upDirCross = new Vector3().set(direction).crs(up).nor();
    }

    /** Обновление ориентации камеры */
    public void update() {
        Quaternion headQuaternion = GdxVR.vrSensorManager.getHeadQuaternion();

        // Из-за обхода стандартного механизма вращения камеры необходимо вручную
        // получать векторы ее направления из кватерниона
        direction.set(0, 0, 1);
        headQuaternion.transform(direction);
        up.set(0, 1, 0);
        headQuaternion.transform(up);
        upDirCross.set(direction);
        upDirCross.crs(up).nor();

        // Вычисление углов вращения камер из кватерниона
        float angle = 2 * (float)Math.acos(headQuaternion.w);
        float s = 1f / (float)Math.sqrt(1 - headQuaternion.w * headQuaternion.w);
        float vx = headQuaternion.x * s;
        float vy = headQuaternion.y * s;
        float vz = headQuaternion.z * s;

        // Вращение левой камеры
        leftCam.view.idt(); // Сброс матрицы вида
        leftCam.view.translate(parallax, 0, 0); // Перенос в начало координат + parallax по X
        leftCam.view.rotateRad(vx, vy, vz, -angle); // Поворот кватернионом
        leftCam.view.translate(-position.x, -position.y, -position.z); // Смещение в position
        leftCam.combined.set(leftCam.projection);
        Matrix4.mul(leftCam.combined.val, leftCam.view.val);

        // Вращение правой камеры
        rightCam.view.idt(); // Сброс матрицы вида
        rightCam.view.translate(-parallax, 0, 0); // Перенос в начало координат + parallax по X
        rightCam.view.rotateRad(vx, vy, vz, -angle); // Поворот кватернионом
        rightCam.view.translate(-position.x, -position.y, -position.z); // Смещение в position
        rightCam.combined.set(rightCam.projection);
        Matrix4.mul(rightCam.combined.val, rightCam.view.val);
    }

    /** Изменение местоположения камеры */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    /** Возврат левой камеры */
    public PerspectiveCamera getLeftCam() {
        return leftCam;
    }

    /** Возврат правой камеры */
    public PerspectiveCamera getRightCam() {
        return rightCam;
    }

    /** Возврат позиции, направления и вектора UP камеры, а так же их векторного произведения*/
    public Vector3 getPosition() { return position; }
    public Vector3 getDirection() { return direction; }
    public Vector3 getUp() { return up; }
    public Vector3 getUpDirCross() { return upDirCross; }
}