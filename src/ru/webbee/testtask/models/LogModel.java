package ru.webbee.testtask.models;
import java.time.LocalDateTime;

public class LogModel {
    public enum OperationType {
        BALANCE,
        TRANSFERRED,
        WITHDREW,
        RECEIVED
    }

    private LocalDateTime timestamp;
    private String username;
    private OperationType operationType;
    private double amount;
    private String targetUsername;

    public LocalDateTime getTimestamp() {return timestamp;}
    public String getUsername() {return username;}
    public OperationType getOperationType() {return operationType;}
    public double getAmount() {return amount;}
    public String getTargetUsername() {return targetUsername;}

    public LogModel(LocalDateTime timestamp, String username, OperationType operationType, double amount, String targetUsername) {
        this.timestamp = timestamp;
        this.username = username;
        this.operationType = operationType;
        this.amount = amount;
        this.targetUsername = targetUsername;
    }
}
