package com.example.holly_renfrew_weight_tracker_project;

public class WeightEntry {

    private final int id;
    private final double weight;
    private final String date;

    public WeightEntry(int id, double weight, String date) {
        this.id = id;
        this.weight = weight;
        this.date = date != null ? date : "";
    }

    public int getId() {
        return id;
    }

    public double getWeight() {
        return weight;
    }

    public String getDate() {
        return date;
    }
}
