package com.example.sensorrecorder;

public class SensorRecord {
    private int id;
    private float light;
    private float temp;
    private long time;

    public SensorRecord(int id, float light, float temp, long time) {
        this.id = id;
        this.light = light;
        this.temp = temp;
        this.time = time;
    }

    // 无参构造
    public SensorRecord(float light, float temp, long time) {
        this.light = light;
        this.temp = temp;
        this.time = time;
    }

    public int getId() { return id; }
    public float getLight() { return light; }
    public float getTemp() { return temp; }
    public long getTime() { return time; }
}