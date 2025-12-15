package com.example.holly_renfrew_weight_tracker_project;

/**
 * Model class representing a single weight entry.
 */
public class WeightEntry {
    private final int id;
    private double weight;
    private final String date;

    public WeightEntry(int id, double weight, String date) {
        this.id = id;
        this.weight = weight;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getDisplayText() {
        return date + ": " + weight + " lbs";
    }

    public String getDate() {
        return date;
    }
}
