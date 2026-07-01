package com.example.sensorrecorder;

public class SensorData {
    private long _id;
    private String timestamp;
    private String sensorType;
    private float value;

    public SensorData() {}

    public SensorData(String timestamp, String sensorType, float value) {
        this.timestamp = timestamp;
        this.sensorType = sensorType;
        this.value = value;
    }

    // Getter & Setter
    public long getId() { return _id; }
    public void setId(long _id) { this._id = _id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }

    public float getValue() { return value; }
    public void setValue(float value) { this.value = value; }
}